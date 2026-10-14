package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.IndividualAliasDetail;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.OffenceDetails;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * J17/J25 parity test for PR #172 review findings PCF-02 and PCF-03.
 *
 * The same scenarios exist on the J17 copy (main, Hibernate 5, DeltaSpike, production persistence unit).
 * Here the test persistence unit is used. Like the J25 production persistence.xml it does not set
 * hibernate.event.merge.entity_copy_observer, so Hibernate's default (disallow) applies, as it does on J17.
 * Run with -Dpcf.entityCopyObserver=allow to see the Hibernate 6 behaviour with the relaxed PCF-02 setting.
 */
class CaseGraphPersistenceParityTest {

    private static final String TEST_PERSISTENCE_UNIT = "prosecutioncasefile-test-persistence-unit";
    private static final Map<String, String> PRODUCTION_PERSISTENCE_UNIT_PROPERTIES = Optional.ofNullable(System.getProperty("pcf.entityCopyObserver"))
            .map(entityCopyObserver -> Map.of("hibernate.event.merge.entity_copy_observer", entityCopyObserver))
            .orElse(Map.of());

    private static EntityManagerFactory entityManagerFactory;

    private EntityManager entityManager;
    private final CaseDetailsRepository caseDetailsRepository = new CaseDetailsRepository();
    private final DefendantRepository defendantRepository = new DefendantRepository();
    private final BusinessValidationErrorRepository businessValidationErrorRepository = new BusinessValidationErrorRepository();

    @BeforeAll
    static void createEntityManagerFactory() {
        entityManagerFactory = Persistence.createEntityManagerFactory(TEST_PERSISTENCE_UNIT, PRODUCTION_PERSISTENCE_UNIT_PROPERTIES);
    }

    @AfterAll
    static void closeEntityManagerFactory() {
        entityManagerFactory.close();
    }

    @BeforeEach
    void beginTransaction() throws Exception {
        entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        injectEntityManagerInto(caseDetailsRepository);
        injectEntityManagerInto(defendantRepository);
        injectEntityManagerInto(businessValidationErrorRepository);
    }

    @AfterEach
    void rollbackTransaction() {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
        entityManager.close();
    }

    // PCF-03: individualAliases became an EAGER bag next to the EAGER offences set (J17: LAZY)
    @Test
    void pcf03_shouldNotDuplicateAliasesWhenCaseIsReloaded() {
        final UUID caseId = randomUUID();
        caseDetailsRepository.save(caseWith(caseId, defendant("D1-" + caseId, asList(alias("ALIAS-A"), alias("ALIAS-B")),
                offence(randomUUID(), "CODE-1"), offence(randomUUID(), "CODE-2"))));
        entityManager.flush();
        entityManager.clear();

        final CaseDetails reloaded = caseDetailsRepository.findBy(caseId);

        assertThat(reloaded.getDefendants(), hasSize(1));
        assertThat(aliasLastNames(reloaded.getDefendants().iterator().next()), containsInAnyOrder("ALIAS-A", "ALIAS-B"));
    }

    // PCF-03: same check through DefendantRepository.findBy (DefendantChangedListener, DefendantIdpcAddedListener)
    @Test
    void pcf03_shouldNotDuplicateAliasesWhenDefendantIsReloaded() {
        final UUID caseId = randomUUID();
        final String defendantId = "D1-" + caseId;
        caseDetailsRepository.save(caseWith(caseId, defendant(defendantId, asList(alias("ALIAS-A"), alias("ALIAS-B")),
                offence(randomUUID(), "CODE-1"), offence(randomUUID(), "CODE-2"))));
        entityManager.flush();
        entityManager.clear();

        final DefendantDetails reloaded = defendantRepository.findBy(defendantId);

        assertThat(aliasLastNames(reloaded), containsInAnyOrder("ALIAS-A", "ALIAS-B"));
    }

    // PCF-03: same check through the JPQL load path (findCaseDetailsByProsecutionCaseReference)
    @Test
    void pcf03_shouldNotDuplicateAliasesWhenCaseIsQueriedByProsecutionReference() {
        final UUID caseId = randomUUID();
        final String prosecutionCaseReference = "URN-" + caseId;
        final CaseDetails caseDetails = caseWith(caseId, defendant("D1-" + caseId, asList(alias("ALIAS-A"), alias("ALIAS-B")),
                offence(randomUUID(), "CODE-1"), offence(randomUUID(), "CODE-2")));
        caseDetails.setProsecutionCaseReference(prosecutionCaseReference);
        caseDetailsRepository.save(caseDetails);
        entityManager.flush();
        entityManager.clear();

        final CaseDetails reloaded = caseDetailsRepository.findCaseDetailsByProsecutionCaseReference(prosecutionCaseReference);

        assertThat(reloaded.getDefendants(), hasSize(1));
        assertThat(aliasLastNames(reloaded.getDefendants().iterator().next()), containsInAnyOrder("ALIAS-A", "ALIAS-B"));
    }

    // PCF-02: the persistence.xml comment says SJP prosecutions share OffenceDetails across defendants.
    // One offence row can only belong to one defendant, so a successful merge means one defendant silently loses it.
    @Test
    void pcf02_shouldFailLoudlyWhenTwoDefendantsCarryTheSameOffenceId() {
        final UUID caseId = randomUUID();
        final UUID sharedOffenceId = randomUUID();
        final CaseDetails caseDetails = caseWith(caseId,
                defendant("D1-" + caseId, emptyList(), offence(sharedOffenceId, "CODE-D1")),
                defendant("D2-" + caseId, emptyList(), offence(sharedOffenceId, "CODE-D2")));

        final Throwable thrown = thrownBy(() -> {
            caseDetailsRepository.save(caseDetails);
            entityManager.flush();
        });

        // J17 golden: DeltaSpike save fails with EntityExistsException (different object with the same identifier)
        assertThat("graph was merged silently; owner(s) of the shared offence after reload: " + ownersAfterReload(thrown, sharedOffenceId),
                thrown, notNullValue());
    }

    // PCF-02: SjpProsecutionReceivedListener re-receive path - find, remove defendants and case, save the new graph, in one transaction
    @Test
    void pcf02_shouldReplaceExistingCaseWhenSjpProsecutionIsReceivedAgain() {
        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();
        final String defendantId = "D1-" + caseId;
        caseDetailsRepository.save(caseWith(caseId, defendant(defendantId, emptyList(), offence(offenceId, "OLD-CODE"))));
        entityManager.flush();
        entityManager.clear();

        final CaseDetails replacement = caseWith(caseId, defendant(defendantId, emptyList(), offence(offenceId, "NEW-CODE")));
        final Throwable thrown = thrownBy(() -> {
            final CaseDetails existingCase = caseDetailsRepository.findBy(caseId);
            existingCase.getDefendants().forEach(defendant -> defendantRepository.remove(defendant));
            caseDetailsRepository.remove(existingCase);
            caseDetailsRepository.save(replacement);
            entityManager.flush();
        });

        assertThat(describe(thrown), thrown, nullValue());
        entityManager.clear();
        final CaseDetails reloaded = caseDetailsRepository.findBy(caseId);
        assertThat(reloaded.getDefendants(), hasSize(1));
        assertThat(offenceCodes(reloaded.getDefendants().iterator().next()), contains("NEW-CODE"));
    }

    // PCF-02: ProsecutionReceivedListener.prosecutionDefendantsAddedReceived (SPI channel) - find the case, add the new
    // defendants, then delete their business validation errors. Here the added defendant reuses an offence id that an
    // existing defendant already owns, as ResolvedCasesIT.shouldResolveCaseWhenInvalidOffenceAlcoholLevelMethodAndErrorCorrectionSubmitted sends.
    // This path cascades a persist on flush, not a merge, so entity_copy_observer does not apply.
    @Test
    void pcf02_shouldFailWhenAddedDefendantReusesAnExistingOffenceId() {
        final UUID caseId = randomUUID();
        final UUID sharedOffenceId = randomUUID();
        caseDetailsRepository.save(caseWith(caseId, defendant(randomUUID().toString(), emptyList(), offence(sharedOffenceId, "CODE-D1"))));
        entityManager.flush();
        entityManager.clear();

        final Throwable thrown = thrownBy(() -> {
            final CaseDetails caseDetails = caseDetailsRepository.findBy(caseId);
            caseDetails.getDefendants().addAll(asList(defendant(randomUUID().toString(), emptyList(), offence(sharedOffenceId, "CODE-D2"))));
            caseDetails.getDefendants().forEach(details -> businessValidationErrorRepository.deleteByDefendantId(UUID.fromString(details.getDefendantId())));
            entityManager.flush();
        });

        assertThat("graph was saved silently; owner(s) of the shared offence after reload: " + ownersAfterReload(thrown, sharedOffenceId),
                thrown, notNullValue());
        // J17 golden: EntityExistsException "... already associated with the session" (Hibernate 5 drops the NonUniqueObjectException cause)
        assertThat(describe(thrown), containsString("A different object with the same identifier value was already associated"));
    }

    private String ownersAfterReload(final Throwable thrown, final UUID offenceId) {
        if (thrown != null) {
            return "n/a";
        }
        entityManager.clear();
        return entityManager.createQuery("select o.defendant.defendantId from OffenceDetails o where o.offenceId = :offenceId", String.class)
                .setParameter("offenceId", offenceId)
                .getResultList()
                .toString();
    }

    private static CaseDetails caseWith(final UUID caseId, final DefendantDetails... defendants) {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseId(caseId);
        caseDetails.setDefendants(new HashSet<>(asList(defendants)));
        return caseDetails;
    }

    private static DefendantDetails defendant(final String defendantId, final List<IndividualAliasDetail> aliases, final OffenceDetails... offences) {
        final DefendantDetails defendant = new DefendantDetails();
        defendant.setDefendantId(defendantId);
        defendant.setOffences(new HashSet<>(asList(offences)));
        defendant.setIndividualAliases(aliases);
        return defendant;
    }

    private static OffenceDetails offence(final UUID offenceId, final String offenceCode) {
        final OffenceDetails offence = new OffenceDetails();
        offence.setOffenceId(offenceId);
        offence.setOffenceCode(offenceCode);
        return offence;
    }

    private static IndividualAliasDetail alias(final String lastName) {
        return new IndividualAliasDetail("Mr", "Alias", null, null, lastName);
    }

    private static List<String> aliasLastNames(final DefendantDetails defendant) {
        return defendant.getIndividualAliases().stream().map(IndividualAliasDetail::getLastName).collect(toList());
    }

    private static List<String> offenceCodes(final DefendantDetails defendant) {
        return defendant.getOffences().stream().map(OffenceDetails::getOffenceCode).collect(toList());
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
        Throwable root = thrown;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return thrown.getClass().getName() + " (root " + root.getClass().getName() + ": " + root.getMessage() + ")";
    }

    private void injectEntityManagerInto(final Object repository) throws Exception {
        final Field field = repository.getClass().getDeclaredField("entityManager");
        field.setAccessible(true);
        field.set(repository, entityManager);
    }
}
