package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.AddressDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantLegacy;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.OffenceDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.OffenceLegacy;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.OrganisationInformationDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.PersonalInformationDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.SelfDefinedInformationDetails;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * J17/J25 parity test for PR #172 review findings PCF-17 (@MapsId removal, IN without parentheses) and
 * PCF-18 (BC-04, primitive OffenceLegacy.orderIndex on a nullable column).
 *
 * J25 side: hand-written repositories over the test persistence unit, opened with the J25 production persistence.xml
 * properties (as CaseGraphPersistenceParityTest does). Everything from the first @Test down is identical to the J17 copy;
 * only this harness and the jakarta imports differ.
 */
public class ViewstoreMappingParityTest {

    private static final String TEST_PERSISTENCE_UNIT = "prosecutioncasefile-test-persistence-unit";
    private static final Map<String, String> PRODUCTION_PERSISTENCE_UNIT_PROPERTIES = Optional.ofNullable(System.getProperty("pcf.entityCopyObserver"))
            .map(entityCopyObserver -> Map.of("hibernate.event.merge.entity_copy_observer", entityCopyObserver))
            .orElse(Map.of());

    private static EntityManagerFactory entityManagerFactory;

    private EntityManager entityManager;
    private final CaseDetailsRepository caseDetailsRepository = new CaseDetailsRepository();
    private final DefendantRepository defendantRepository = new DefendantRepository();
    private final OffenceLegacyRepository offenceLegacyRepository = new OffenceLegacyRepository();
    private final DefendantLegacyRepository defendantLegacyRepository = new DefendantLegacyRepository();

    @BeforeAll
    public static void createEntityManagerFactory() {
        entityManagerFactory = Persistence.createEntityManagerFactory(TEST_PERSISTENCE_UNIT, PRODUCTION_PERSISTENCE_UNIT_PROPERTIES);
    }

    @AfterAll
    public static void closeEntityManagerFactory() {
        entityManagerFactory.close();
    }

    @BeforeEach
    public void beginTransaction() throws Exception {
        entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        injectEntityManagerInto(caseDetailsRepository);
        injectEntityManagerInto(defendantRepository);
        injectEntityManagerInto(offenceLegacyRepository);
        injectEntityManagerInto(defendantLegacyRepository);
    }

    @AfterEach
    public void rollbackTransaction() {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
        entityManager.close();
    }

    private void injectEntityManagerInto(final Object repository) throws Exception {
        final Field field = repository.getClass().getDeclaredField("entityManager");
        field.setAccessible(true);
        field.set(repository, entityManager);
    }

    // PCF-18 / BC-04: Liquibase 001 created order_index INT without NOT NULL (hbm2ddl also leaves it nullable in 5.4, 6.6 and 7.3; the ALTER only guards that).
    // J17 golden: Hibernate 5.4 does NOT default the NULL to 0; find fails with a PersistenceException carrying PropertyAccessException
    // <- IllegalArgumentException "Can not set int field ...OffenceLegacy.orderIndex to null value".
    @Test
    public void pcf18_shouldFailToLoadOffenceLegacyWhoseOrderIndexIsNull() {
        makeOrderIndexNullableAsInLiquibase();
        final UUID offenceId = randomUUID();
        final String code = "PCF18-" + offenceId;
        offenceLegacyRepository.save(offenceLegacy(offenceId, code));
        entityManager.flush();
        setOrderIndexToNull(code);
        entityManager.clear();

        final List<Object> loaded = new ArrayList<>();
        final Throwable thrown = thrownBy(() -> loaded.add(offenceLegacyRepository.findBy(offenceId).getOrderIndex()));

        assertPrimitiveNullFailure(thrown, loaded);
    }

    // PCF-18 / BC-04: same NULL reached through DefendantLegacy.offences (EAGER, @OrderBy("orderIndex ASC"))
    @Test
    public void pcf18_shouldFailToLoadDefendantLegacyWhoseOffenceOrderIndexIsNull() {
        makeOrderIndexNullableAsInLiquibase();
        final UUID defendantId = randomUUID();
        final String code = "PCF18-" + defendantId;
        defendantLegacyRepository.save(new DefendantLegacy(defendantId, randomUUID(), "POLICE-1", randomUUID(), randomUUID(), "URN-" + defendantId,
                new HashSet<>(asList(offenceLegacy(randomUUID(), code), offenceLegacy(randomUUID(), "OTHER-" + defendantId)))));
        entityManager.flush();
        setOrderIndexToNull(code);
        entityManager.clear();

        final List<Object> loaded = new ArrayList<>();
        final Throwable thrown = thrownBy(() -> loaded.add(defendantLegacyRepository.findBy(defendantId).getOffences().size()));

        assertPrimitiveNullFailure(thrown, loaded);
    }

    // PCF-17 / P-4: production path DefendantToDefendantDetails (constructor) -> CaseDetailsRepository.save (ProsecutionReceivedListener:95, SjpProsecutionReceivedListener:51)
    @Test
    public void pcf17_shouldKeyInformationRowsByDefendantIdWhenIndividualDefendantsAreSaved() {
        final UUID caseId = randomUUID();
        final String firstDefendantId = randomUUID().toString();
        final String secondDefendantId = randomUUID().toString();
        caseDetailsRepository.save(caseWith(caseId,
                individualDefendant(firstDefendantId, "Alice", LocalDate.of(1980, 1, 1)),
                individualDefendant(secondDefendantId, "Bob", LocalDate.of(1990, 2, 2))));
        entityManager.flush();
        entityManager.clear();

        final DefendantDetails first = defendantRepository.findBy(firstDefendantId);
        final DefendantDetails second = defendantRepository.findBy(secondDefendantId);

        assertThat(informationIds(first), contains(firstDefendantId, firstDefendantId, null));
        assertThat(informationIds(second), contains(secondDefendantId, secondDefendantId, null));
        assertThat(defendantForeignKeys(firstDefendantId), contains(firstDefendantId, firstDefendantId, null));
        assertThat(defendantForeignKeys(secondDefendantId), contains(secondDefendantId, secondDefendantId, null));
        assertThat(first.getPersonalInformation().getFirstName(), is("Alice"));
        assertThat(second.getPersonalInformation().getFirstName(), is("Bob"));
        assertThat(first.getSelfDefinedInformation().getDateOfBirth(), is(LocalDate.of(1980, 1, 1)));
        assertThat(second.getSelfDefinedInformation().getDateOfBirth(), is(LocalDate.of(1990, 2, 2)));
        assertThat(rowCount("personal_information", "personal_information_id", firstDefendantId), is(1L));
        assertThat(rowCount("self_defined_information", "self_defined_information_id", firstDefendantId), is(1L));
    }

    // PCF-17 / P-4: corporate branch of DefendantToDefendantDetails (OrganisationInformationDetails)
    @Test
    public void pcf17_shouldKeyOrganisationInformationRowByDefendantIdWhenCorporateDefendantIsSaved() {
        final UUID caseId = randomUUID();
        final String defendantId = randomUUID().toString();
        caseDetailsRepository.save(caseWith(caseId, corporateDefendant(defendantId, "Acme Ltd")));
        entityManager.flush();
        entityManager.clear();

        final DefendantDetails reloaded = defendantRepository.findBy(defendantId);

        assertThat(informationIds(reloaded), contains(null, null, defendantId));
        assertThat(defendantForeignKeys(defendantId), contains(null, null, defendantId));
        assertThat(reloaded.getOrganisationInformation().getOrganisationName(), is("Acme Ltd"));
        assertThat(reloaded.getOrganisationInformation().getAddress().getPostcode(), is("AC1 1ME"));
        assertThat(rowCount("organisation_information", "organisation_information_id", defendantId), is(1L));
    }

    // PCF-17 / P-4: DefendantChangedListener:43-51 - find defendant, replace personal information keeping its id, change DOB, save
    @Test
    public void pcf17_shouldReplacePersonalInformationWhenDefendantChanges() {
        final UUID caseId = randomUUID();
        final String changedDefendantId = randomUUID().toString();
        final String otherDefendantId = randomUUID().toString();
        caseDetailsRepository.save(caseWith(caseId,
                individualDefendant(changedDefendantId, "Alice", LocalDate.of(1980, 1, 1)),
                individualDefendant(otherDefendantId, "Bob", LocalDate.of(1990, 2, 2))));
        entityManager.flush();
        entityManager.clear();

        final DefendantDetails defendantDetails = defendantRepository.findBy(changedDefendantId);
        final PersonalInformationDetails personalInformationDetails = personalInformation("Carol");
        personalInformationDetails.setPersonalInformationId(defendantDetails.getPersonalInformation().getPersonalInformationId());
        defendantDetails.setPersonalInformation(personalInformationDetails);
        defendantDetails.getSelfDefinedInformation().setDateOfBirth(LocalDate.of(1985, 5, 5));
        defendantRepository.save(defendantDetails);
        entityManager.flush();
        entityManager.clear();

        final DefendantDetails changed = defendantRepository.findBy(changedDefendantId);
        final DefendantDetails other = defendantRepository.findBy(otherDefendantId);
        assertThat(changed.getPersonalInformation().getFirstName(), is("Carol"));
        assertThat(changed.getSelfDefinedInformation().getDateOfBirth(), is(LocalDate.of(1985, 5, 5)));
        assertThat(informationIds(changed), contains(changedDefendantId, changedDefendantId, null));
        assertThat(defendantForeignKeys(changedDefendantId), contains(changedDefendantId, changedDefendantId, null));
        assertThat(rowCount("personal_information", "personal_information_id", changedDefendantId), is(1L));
        assertThat(other.getPersonalInformation().getFirstName(), is("Bob"));
        assertThat(other.getSelfDefinedInformation().getDateOfBirth(), is(LocalDate.of(1990, 2, 2)));
    }

    // PCF-17 / P-4: SjpProsecutionReceivedListener:43-51 - re-receive removes the defendants and the case, then saves the new graph (same ids) in one transaction
    @Test
    public void pcf17_shouldReplaceInformationRowsWhenSjpProsecutionIsReceivedAgain() {
        final UUID caseId = randomUUID();
        final String individualId = randomUUID().toString();
        final String corporateId = randomUUID().toString();
        caseDetailsRepository.save(caseWith(caseId,
                individualDefendant(individualId, "Alice", LocalDate.of(1980, 1, 1)),
                corporateDefendant(corporateId, "Acme Ltd")));
        entityManager.flush();
        entityManager.clear();

        final CaseDetails replacement = caseWith(caseId,
                individualDefendant(individualId, "Alicia", LocalDate.of(1981, 1, 1)),
                corporateDefendant(corporateId, "Acme Holdings Ltd"));
        final Throwable thrown = thrownBy(() -> {
            final CaseDetails existingCase = caseDetailsRepository.findBy(caseId);
            existingCase.getDefendants().forEach(defendant -> defendantRepository.remove(defendant));
            caseDetailsRepository.remove(existingCase);
            caseDetailsRepository.save(replacement);
            entityManager.flush();
        });

        assertThat(describe(thrown), thrown, nullValue());
        entityManager.clear();
        final DefendantDetails individual = defendantRepository.findBy(individualId);
        final DefendantDetails corporate = defendantRepository.findBy(corporateId);
        assertThat(individual.getPersonalInformation().getFirstName(), is("Alicia"));
        assertThat(individual.getSelfDefinedInformation().getDateOfBirth(), is(LocalDate.of(1981, 1, 1)));
        assertThat(corporate.getOrganisationInformation().getOrganisationName(), is("Acme Holdings Ltd"));
        assertThat(informationIds(individual), contains(individualId, individualId, null));
        assertThat(informationIds(corporate), contains(null, null, corporateId));
        assertThat(defendantForeignKeys(individualId), contains(individualId, individualId, null));
        assertThat(defendantForeignKeys(corporateId), contains(null, null, corporateId));
        assertThat(rowCount("personal_information", "personal_information_id", individualId), is(1L));
        assertThat(rowCount("organisation_information", "organisation_information_id", corporateId), is(1L));
    }

    // PCF-17 / P-4: ProsecutionReceivedListener:110-112 / 121-123 - find the case, add new defendants, cascade on flush
    @Test
    public void pcf17_shouldKeyInformationRowsByDefendantIdWhenDefendantIsAddedToExistingCase() {
        final UUID caseId = randomUUID();
        final String existingId = randomUUID().toString();
        final String addedId = randomUUID().toString();
        caseDetailsRepository.save(caseWith(caseId, individualDefendant(existingId, "Alice", LocalDate.of(1980, 1, 1))));
        entityManager.flush();
        entityManager.clear();

        final CaseDetails caseDetails = caseDetailsRepository.findBy(caseId);
        caseDetails.getDefendants().addAll(asList(individualDefendant(addedId, "Dave", LocalDate.of(1970, 7, 7))));
        entityManager.flush();
        entityManager.clear();

        final CaseDetails reloaded = caseDetailsRepository.findBy(caseId);
        final DefendantDetails added = defendantRepository.findBy(addedId);
        assertThat(reloaded.getDefendants(), hasSize(2));
        assertThat(informationIds(added), contains(addedId, addedId, null));
        assertThat(defendantForeignKeys(addedId), contains(addedId, addedId, null));
        assertThat(added.getPersonalInformation().getFirstName(), is("Dave"));
        assertThat(defendantRepository.findBy(existingId).getPersonalInformation().getFirstName(), is("Alice"));
    }

    // PCF-17 / P-4 latent trap: a fresh information entity attached through the setter, with no id (no production caller).
    // J17 golden: fails too - a PersistenceException carrying IdentifierGenerationException "attempted to assign id from null one-to-one property
    // [...PersonalInformationDetails.defendantDetails]" (the @MapsId derivation needs the back-reference that only the constructor sets).
    @Test
    public void pcf17_shouldRejectInformationEntityAttachedThroughSetterWithoutId() {
        final UUID caseId = randomUUID();
        final DefendantDetails defendant = new DefendantDetails();
        defendant.setDefendantId(randomUUID().toString());
        defendant.setOffences(emptySet());
        defendant.setPersonalInformation(personalInformation("Eve"));

        final Throwable thrown = thrownBy(() -> {
            caseDetailsRepository.save(caseWith(caseId, defendant));
            entityManager.flush();
        });

        assertThat("save succeeded without an information id", thrown, notNullValue());
        assertThat(describe(thrown), isPersistenceException(thrown), is(true));
        assertThat(describe(thrown), containsString("org.hibernate.id.IdentifierGenerationException"));
        assertThat(describe(thrown), containsString("PersonalInformationDetails"));
    }

    // PCF-17 / P-4: the generated DDL keeps the same columns after @MapsId removal (J17: the mappedBy side adds no column and no FK)
    @Test
    public void pcf17_shouldGenerateTheSameColumnsForDefendantAndInformationTables() {
        assertThat(columnsOf("DEFENDANT"), containsInAnyOrder("applied_prosecutor_costs", "asn", "case_id", "defendant_id",
                "defendant_initiation_code", "documentation_language", "driver_number", "hearing_language", "idpc_material_id",
                "language_requirement", "national_insurance_number", "number_previous_convictions", "organisation_information_id",
                "personal_information_id", "posting_date", "prosecutor_defendant_reference", "self_defined_information_id",
                "specific_requirements"));
        assertThat(columnsOf("PERSONAL_INFORMATION"), containsInAnyOrder("personal_information_id", "address_line_1", "address_line_2",
                "address_line_3", "address_line_4", "address_line_5", "postcode", "contact_number_home", "contact_number_mobile",
                "primary_email", "secondary_email", "contact_number_work", "first_name", "last_name", "occupation", "occupation_code", "title"));
        assertThat(columnsOf("SELF_DEFINED_INFORMATION"), containsInAnyOrder("self_defined_information_id", "additional_nationality",
                "date_of_birth", "ethnicity", "gender", "nationality"));
        assertThat(columnsOf("ORGANISATION_INFORMATION"), containsInAnyOrder("organisation_information_id", "address_line_1",
                "address_line_2", "address_line_3", "address_line_4", "address_line_5", "postcode", "organisation_name"));
    }

    // PCF-17 / P-9.3: J25 wrote IN :prosecutionCaseReferences without parentheses (J17: in (:prosecutionCaseReferences)); SubmitCCApplicationApi:84
    @Test
    public void pcf17_shouldFindCasesByTwoProsecutionCaseReferences() {
        final List<UUID> caseIds = saveCasesWithReferences("REF-A", "REF-B", "REF-C");
        entityManager.flush();
        entityManager.clear();

        final List<UUID> found = caseIdsOf(caseDetailsRepository.findAllCaseDetailsByProsecutionCaseReferences(
                asList(referenceFor(caseIds, 0), referenceFor(caseIds, 2))));

        assertThat(found, containsInAnyOrder(caseIds.get(0), caseIds.get(2)));
    }

    @Test
    public void pcf17_shouldFindCaseBySingleProsecutionCaseReference() {
        final List<UUID> caseIds = saveCasesWithReferences("REF-A", "REF-B");
        entityManager.flush();
        entityManager.clear();

        final List<UUID> found = caseIdsOf(caseDetailsRepository.findAllCaseDetailsByProsecutionCaseReferences(
                singletonList(referenceFor(caseIds, 1))));
        final List<UUID> unknown = caseIdsOf(caseDetailsRepository.findAllCaseDetailsByProsecutionCaseReferences(
                singletonList("UNKNOWN-" + caseIds.get(1))));

        assertThat(found, contains(caseIds.get(1)));
        assertThat(unknown, is(empty()));
    }

    private static void assertPrimitiveNullFailure(final Throwable thrown, final List<Object> loaded) {
        assertThat("loaded " + loaded + " instead of failing", thrown, notNullValue());
        assertThat(describe(thrown), isPersistenceException(thrown), is(true));
        assertThat(describe(thrown), containsString("org.hibernate.PropertyAccessException"));
        assertThat(describe(thrown), containsString("OffenceLegacy.orderIndex"));
    }

    // javax (J17) and jakarta (J25) PersistenceException are different types, so match by name up the class hierarchy
    private static boolean isPersistenceException(final Throwable thrown) {
        for (Class<?> type = thrown.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getName().endsWith(".persistence.PersistenceException")) {
                return true;
            }
        }
        return false;
    }

    private void makeOrderIndexNullableAsInLiquibase() {
        entityManager.createNativeQuery("alter table offence_legacy alter column order_index set null").executeUpdate();
    }

    private void setOrderIndexToNull(final String code) {
        final int updated = entityManager.createNativeQuery("update offence_legacy set order_index = null where code = ?1")
                .setParameter(1, code)
                .executeUpdate();
        assertThat(updated, is(1));
    }

    private List<String> defendantForeignKeys(final String defendantId) {
        final Object[] row = (Object[]) entityManager.createNativeQuery(
                        "select personal_information_id, self_defined_information_id, organisation_information_id from defendant where defendant_id = ?1")
                .setParameter(1, defendantId)
                .getSingleResult();
        final List<String> values = new ArrayList<>();
        for (final Object value : row) {
            values.add(Objects.toString(value, null));
        }
        return values;
    }

    private Long rowCount(final String table, final String idColumn, final String id) {
        final Object count = entityManager.createNativeQuery("select count(*) from " + table + " where " + idColumn + " = ?1")
                .setParameter(1, id)
                .getSingleResult();
        return ((Number) count).longValue();
    }

    private List<String> columnsOf(final String table) {
        final List<?> columns = entityManager.createNativeQuery(
                        "select lower(column_name) from information_schema.columns where upper(table_name) = ?1 and upper(table_schema) = 'PUBLIC' order by 1")
                .setParameter(1, table)
                .getResultList();
        return columns.stream().map(String::valueOf).collect(toList());
    }

    private List<UUID> saveCasesWithReferences(final String... referencePrefixes) {
        final List<UUID> caseIds = new ArrayList<>();
        for (final String referencePrefix : referencePrefixes) {
            final UUID caseId = randomUUID();
            final CaseDetails caseDetails = caseWith(caseId);
            caseDetails.setProsecutionCaseReference(referencePrefix + "-" + caseId);
            caseDetailsRepository.save(caseDetails);
            caseIds.add(caseId);
        }
        return caseIds;
    }

    private static String referenceFor(final List<UUID> caseIds, final int index) {
        return "REF-" + (char) ('A' + index) + "-" + caseIds.get(index);
    }

    private static List<UUID> caseIdsOf(final List<CaseDetails> cases) {
        return cases.stream().map(CaseDetails::getCaseId).collect(toList());
    }

    private static List<String> informationIds(final DefendantDetails defendant) {
        return asList(
                defendant.getPersonalInformation() == null ? null : defendant.getPersonalInformation().getPersonalInformationId(),
                defendant.getSelfDefinedInformation() == null ? null : defendant.getSelfDefinedInformation().getSelfDefinedInformationId(),
                defendant.getOrganisationInformation() == null ? null : defendant.getOrganisationInformation().getOrganisationInformationId());
    }

    private static CaseDetails caseWith(final UUID caseId, final DefendantDetails... defendants) {
        return new CaseDetails(caseId, null, "informant", "TFL", null, new HashSet<>(asList(defendants)), null);
    }

    private static DefendantDetails individualDefendant(final String defendantId, final String firstName, final LocalDate dateOfBirth) {
        return new DefendantDetails(defendantId, "ASN-" + firstName, null, null, null, null, null, null, null, null, null, null,
                personalInformation(firstName),
                new SelfDefinedInformationDetails(null, dateOfBirth, null, null, "British"),
                new HashSet<>(asList(offence(randomUUID(), "CODE-" + firstName))),
                null,
                null,
                "C");
    }

    private static DefendantDetails corporateDefendant(final String defendantId, final String organisationName) {
        return new DefendantDetails(defendantId, null, null, null, null, null, null, null, null, null, null, null,
                null,
                null,
                new HashSet<>(asList(offence(randomUUID(), "CODE-" + organisationName))),
                null,
                new OrganisationInformationDetails(organisationName, new AddressDetails("1 Acme Way", null, null, null, null, "AC1 1ME")),
                "O");
    }

    private static PersonalInformationDetails personalInformation(final String firstName) {
        return new PersonalInformationDetails("Mx", firstName, "Last-" + firstName, null, null,
                new AddressDetails("1 " + firstName + " Street", null, null, null, null, "AB1 2CD"), null);
    }

    private static OffenceDetails offence(final UUID offenceId, final String offenceCode) {
        final OffenceDetails offence = new OffenceDetails();
        offence.setOffenceId(offenceId);
        offence.setOffenceCode(offenceCode);
        return offence;
    }

    private static OffenceLegacy offenceLegacy(final UUID offenceId, final String code) {
        final OffenceLegacy offence = new OffenceLegacy();
        offence.setOffenceId(offenceId);
        offence.setCode(code);
        offence.setOrderIndex(7);
        return offence;
    }

    private static Throwable thrownBy(final Runnable action) {
        try {
            action.run();
            return null;
        } catch (final RuntimeException e) {
            return e;
        }
    }

    private static String describe(final Throwable thrown) {
        if (thrown == null) {
            return "no exception";
        }
        final StringBuilder chain = new StringBuilder();
        Throwable current = thrown;
        while (current != null) {
            chain.append(chain.length() == 0 ? "" : " <- ").append(current.getClass().getName()).append(": ").append(current.getMessage());
            current = current.getCause() == current ? null : current.getCause();
        }
        return chain.toString();
    }

}
