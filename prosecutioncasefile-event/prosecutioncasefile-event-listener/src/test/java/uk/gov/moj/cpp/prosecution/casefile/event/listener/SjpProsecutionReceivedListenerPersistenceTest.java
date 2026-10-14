package uk.gov.moj.cpp.prosecution.casefile.event.listener;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.ProsecutionReceivedToCase;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.IndividualAliasDetail;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.OffenceDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorCaseDetailsRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.CaseDetailsRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.DefendantRepository;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionReceived;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

/**
 * Runs SjpProsecutionReceivedListener against real repositories on H2, with Hibernate's default merge settings
 * (no hibernate.event.merge.entity_copy_observer override), to show that its merge-based save needs no relaxed setting.
 */
@ExtendWith(MockitoExtension.class)
class SjpProsecutionReceivedListenerPersistenceTest {

    private static final String PERSISTENCE_UNIT = "prosecutioncasefile-listener-test-persistence-unit";

    private static EntityManagerFactory entityManagerFactory;

    @Mock
    private ProsecutionReceivedToCase prosecutionReceivedToCaseConverter;

    @Mock
    private Envelope<SjpProsecutionReceived> envelope;

    @Mock
    private SjpProsecutionReceived sjpProsecutionReceived;

    private EntityManager entityManager;
    private CaseDetailsRepository caseDetailsRepository;
    private SjpProsecutionReceivedListener listener;

    @BeforeAll
    static void createEntityManagerFactory() {
        entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
    }

    @AfterAll
    static void closeEntityManagerFactory() {
        entityManagerFactory.close();
    }

    @BeforeEach
    void createListenerWithRealRepositories() {
        entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        caseDetailsRepository = withEntityManager(new CaseDetailsRepository());
        listener = new SjpProsecutionReceivedListener();
        setField(listener, "prosecutionReceivedToCaseConverter", prosecutionReceivedToCaseConverter);
        setField(listener, "caseDetailsRepository", caseDetailsRepository);
        setField(listener, "defendantRepository", withEntityManager(new DefendantRepository()));
        setField(listener, "businessValidationErrorRepository", withEntityManager(new BusinessValidationErrorRepository()));
        setField(listener, "businessValidationErrorCaseDetailsRepository", withEntityManager(new BusinessValidationErrorCaseDetailsRepository()));
    }

    @AfterEach
    void rollbackTransaction() {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
        entityManager.close();
    }

    @Test
    void prosecutionReceived_newCaseWithSeveralDefendantsAndOffences_should_saveTheWholeGraph() {
        final UUID caseId = randomUUID();
        final UUID offence1 = randomUUID();
        final UUID offence2 = randomUUID();
        final UUID offence3 = randomUUID();

        receive(caseWith(caseId,
                defendant("D1-" + caseId, asList(alias("ALIAS-A"), alias("ALIAS-B")), offence(offence1, "CODE-1"), offence(offence2, "CODE-2")),
                defendant("D2-" + caseId, emptyList(), offence(offence3, "CODE-3"))));

        final Map<String, Set<String>> offenceCodesByDefendant = offenceCodesByDefendant(reload(caseId));
        assertThat(offenceCodesByDefendant.get("D1-" + caseId), containsInAnyOrder("CODE-1", "CODE-2"));
        assertThat(offenceCodesByDefendant.get("D2-" + caseId), containsInAnyOrder("CODE-3"));
    }

    @Test
    void prosecutionReceived_caseReceivedAgainWithSameIds_should_replaceTheExistingGraph() {
        final UUID caseId = randomUUID();
        final UUID keptOffence = randomUUID();
        final UUID droppedOffence = randomUUID();
        final UUID addedOffence = randomUUID();
        receive(caseWith(caseId,
                defendant("D1-" + caseId, asList(alias("ALIAS-A")), offence(keptOffence, "OLD-CODE"), offence(droppedOffence, "DROPPED")),
                defendant("D2-" + caseId, emptyList(), offence(randomUUID(), "D2-CODE"))));
        entityManager.flush();
        entityManager.clear();

        assertDoesNotThrow(() -> receive(caseWith(caseId,
                defendant("D1-" + caseId, asList(alias("ALIAS-A")), offence(keptOffence, "NEW-CODE"), offence(addedOffence, "ADDED")))));

        final CaseDetails reloaded = reload(caseId);
        assertThat(reloaded.getDefendants(), hasSize(1));
        assertThat(offenceCodesByDefendant(reloaded).get("D1-" + caseId), containsInAnyOrder("NEW-CODE", "ADDED"));
        assertThat(entityManager.find(OffenceDetails.class, droppedOffence) == null, is(true));
    }

    @Test
    void prosecutionReceived_twoDefendantsCarryTheSameOffenceId_should_failInsteadOfDroppingAnOffence() {
        final UUID caseId = randomUUID();
        final UUID sharedOffenceId = randomUUID();
        final CaseDetails caseDetails = caseWith(caseId,
                defendant("D1-" + caseId, emptyList(), offence(sharedOffenceId, "CODE-D1")),
                defendant("D2-" + caseId, emptyList(), offence(sharedOffenceId, "CODE-D2")));

        final IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> receive(caseDetails));

        assertThat(thrown.getMessage(), containsString("Multiple representations of the same entity"));
    }

    private void receive(final CaseDetails convertedCase) {
        when(envelope.payload()).thenReturn(sjpProsecutionReceived);
        when(prosecutionReceivedToCaseConverter.convert(any())).thenReturn(convertedCase);

        listener.prosecutionReceived(envelope);
        entityManager.flush();
    }

    private CaseDetails reload(final UUID caseId) {
        entityManager.clear();
        return caseDetailsRepository.findBy(caseId);
    }

    private <T> T withEntityManager(final T repository) {
        setField(repository, "entityManager", entityManager);
        return repository;
    }

    private static Map<String, Set<String>> offenceCodesByDefendant(final CaseDetails caseDetails) {
        return caseDetails.getDefendants().stream().collect(toMap(
                DefendantDetails::getDefendantId,
                defendant -> defendant.getOffences().stream().map(OffenceDetails::getOffenceCode).collect(toSet())));
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
}
