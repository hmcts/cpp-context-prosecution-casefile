package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorDetails;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class BusinessValidationErrorRepositoryTest extends BaseTransactionalJunit4Test {
    private static final UUID ID = randomUUID();
    private static final String FIELDID = randomUUID().toString();
    private static final String DISPLAYNAME = "case.marker";
    private static final UUID CASEID = randomUUID();
    private static final UUID DEFENDANTID = randomUUID();
    private static final String FIELDNAME = "case.marker";
    private static final String COURTNAME = "Bootle";
    private static final String COURTLOCATION = "Leeds";
    private static final String CASETYPE = "CC";
    private static final String URN = "88GD6251318";
    private static final String DEFENDANTBAILSTATUS = "CONDITIONAL";
    private static final String FIRST_NAME = "Joe";
    private static final String LAST_NAME = "Root";
    private static final String ORGANISATION_NAME = "Org";

    private static final LocalDate DEFENDANTCHARGEDATE = LocalDate.now().minusMonths(2);
    private static final LocalDate DEFENDANTHEARINGDATE = LocalDate.now().plusMonths(1);
    private final String ERRORVALUE = "no marker ";

    /**
     * Caller-controlled filter values crafted to break out of a string-concatenated SQL/JPQL
     * predicate. Referenced by the injection-resistance tests below (OWASP A03:2021).
     */
    private static final String TAUTOLOGY_PAYLOAD = "Nowhere' OR '1'='1";
    private static final String COMMENT_TRUNCATION_PAYLOAD = COURTLOCATION + "' --";
    private static final String STACKED_DDL_PAYLOAD = COURTLOCATION + "'; DROP TABLE business_validation_errors; --";
    private static final String UNION_PAYLOAD = CASETYPE + "' UNION SELECT case_type FROM business_validation_errors WHERE '1'='1";

    @Inject
    private BusinessValidationErrorRepository businessValidationErrorsRepository;

    @Inject
    private EntityManager entityManager;

    @Test
    public void shouldReturnZeroIfThereIsNoOutstandingErrors() {
        final Long countOfCasesWithOutstandingErrors = businessValidationErrorsRepository.countOfCasesWithOutstandingErrors(empty(),empty());
        assertThat(countOfCasesWithOutstandingErrors, is(0L));
    }

    @Test
    public void shouldReturnAllOutstandingErrorsCountsWithoutParameters(){
        produceAndSaveBusinessValidationErrors(50);
        final Long countOfCasesWithOutstandingErrors = businessValidationErrorsRepository.countOfCasesWithOutstandingErrors(empty(),empty());
        assertThat(countOfCasesWithOutstandingErrors, is(50L));
    }

    @Test
    public void shouldReturnAllOutStandingErrorsCountsWithCourtLocationOnly(){
        produceAndSaveBusinessValidationErrors(50);
        BusinessValidationErrorDetails withCourHouseOnly = getBusinessValidationErrors();
        withCourHouseOnly.setCourtLocation ("NewCastle");
        withCourHouseOnly.setId(randomUUID());
        businessValidationErrorsRepository.save(withCourHouseOnly);
        final Long countOfCasesWithOutstandingErrors = businessValidationErrorsRepository.countOfCasesWithOutstandingErrors(Optional.of("NewCastle"),empty());
        assertThat(countOfCasesWithOutstandingErrors, is(1L));

    }

    @Test
    public void shouldReturnAllOutStandingErrorsCountsWithCourtLocationCaseType(){
        produceAndSaveBusinessValidationErrors(50);
        BusinessValidationErrorDetails withCourHouseandCaseType = getBusinessValidationErrors();
        withCourHouseandCaseType.setCourtLocation("Birmingham");
        withCourHouseandCaseType.setCaseType("NEW");
        withCourHouseandCaseType.setId(randomUUID());
        businessValidationErrorsRepository.save(withCourHouseandCaseType);
        final Long countOfCasesWithOutstandingErrors = businessValidationErrorsRepository.countOfCasesWithOutstandingErrors( Optional.of("Birmingham"),Optional.of("NEW"));
        assertThat(countOfCasesWithOutstandingErrors, is(1L));

    }

    private void produceAndSaveBusinessValidationErrors(final int i) {
        for (int j = 0; j <50 ; j++) {
            BusinessValidationErrorDetails businessValidationErrors = this.getBusinessValidationErrors();
            businessValidationErrors.setCaseId(randomUUID());
            businessValidationErrors.setId(randomUUID());
            for (int k = 0; k < new Random().nextInt(10); k++) {
                BusinessValidationErrorDetails differentErrorOfTheSameCase = this.getBusinessValidationErrors();
                differentErrorOfTheSameCase.setCaseId(businessValidationErrors.getCaseId());
                differentErrorOfTheSameCase.setId(randomUUID());
                businessValidationErrorsRepository.save(differentErrorOfTheSameCase);
            }
            businessValidationErrorsRepository.save(businessValidationErrors);

        }
    }

    @Test
    public void shouldFindBusinessValidationErrors() {
        final BusinessValidationErrorDetails businessValidationErrors = getBusinessValidationErrors();
        businessValidationErrorsRepository.save(businessValidationErrors);
        final BusinessValidationErrorDetails svdbusinessValidationErrors = businessValidationErrorsRepository.findBy(businessValidationErrors.getId());

        assertThat(svdbusinessValidationErrors.getId(), equalTo(businessValidationErrors.getId()));
        assertThat(svdbusinessValidationErrors.getErrorValue(), equalTo(businessValidationErrors.getErrorValue()));
        assertThat(svdbusinessValidationErrors.getFieldId(), equalTo(businessValidationErrors.getFieldId()));
        assertThat(svdbusinessValidationErrors.getDisplayName(), equalTo(businessValidationErrors.getDisplayName()));
        assertThat(svdbusinessValidationErrors.getCaseId(), equalTo(businessValidationErrors.getCaseId()));
        assertThat(svdbusinessValidationErrors.getDefendantId(), equalTo(businessValidationErrors.getDefendantId()));
        assertThat(svdbusinessValidationErrors.getFieldName(), equalTo(businessValidationErrors.getFieldName()));
        assertThat(svdbusinessValidationErrors.getCourtName(), equalTo(businessValidationErrors.getCourtName()));
        assertThat(svdbusinessValidationErrors.getCaseType(), equalTo(businessValidationErrors.getCaseType()));
        assertThat(svdbusinessValidationErrors.getUrn(), equalTo(businessValidationErrors.getUrn()));
        assertThat(svdbusinessValidationErrors.getDefendantBailStatus(), equalTo(businessValidationErrors.getDefendantBailStatus()));
        assertThat(svdbusinessValidationErrors.getDefendantChargeDate(), equalTo(businessValidationErrors.getDefendantChargeDate()));
        assertThat(svdbusinessValidationErrors.getDefendantHearingDate(), equalTo(businessValidationErrors.getDefendantHearingDate()));
        assertThat(svdbusinessValidationErrors.getFirstName(), equalTo(businessValidationErrors.getFirstName()));
        assertThat(svdbusinessValidationErrors.getLastName(), equalTo(businessValidationErrors.getLastName()));
        assertThat(svdbusinessValidationErrors.getOrganisationName(), equalTo(businessValidationErrors.getOrganisationName()));

        businessValidationErrorsRepository.remove(businessValidationErrors);
    }

    @Test
    public void shouldDeleteErrorWithCaseIdAndNullDefendantId(){
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final BusinessValidationErrorDetails caseLevelError = getBusinessValidationErrors(randomUUID(), caseId, null);
        final BusinessValidationErrorDetails defendantLevelError = getBusinessValidationErrors(randomUUID(), caseId, defendantId);
        businessValidationErrorsRepository.save(caseLevelError);
        businessValidationErrorsRepository.save(defendantLevelError);
        final List<BusinessValidationErrorDetails> errorsByCaseIdBeforeDeletion = businessValidationErrorsRepository.findByCaseId(caseId);
        assertThat(errorsByCaseIdBeforeDeletion, hasSize(2));

        businessValidationErrorsRepository.deleteByCaseIdAndDefendantIdIsNull(caseId);

        final List<BusinessValidationErrorDetails> errorsByCaseIdAfterDeletion = businessValidationErrorsRepository.findByCaseId(caseId);
        assertThat(errorsByCaseIdAfterDeletion, hasSize(1));
        assertThat(errorsByCaseIdAfterDeletion.get(0).getDefendantId(), is(defendantId));
    }

    @Test
    public void shouldDeleteErrorWithCaseIdAndDefendantFirstNameLastName(){
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final BusinessValidationErrorDetails caseLevelError = getBusinessValidationErrors(randomUUID(), caseId, null);
        final BusinessValidationErrorDetails defendantLevelError = getBusinessValidationErrors(randomUUID(), caseId, defendantId);
        businessValidationErrorsRepository.save(caseLevelError);
        businessValidationErrorsRepository.save(defendantLevelError);
        final List<BusinessValidationErrorDetails> errorsByCaseIdBeforeDeletion = businessValidationErrorsRepository.findByCaseId(caseId);
        assertThat(errorsByCaseIdBeforeDeletion, hasSize(2));

        businessValidationErrorsRepository.deleteByCaseIdAndFirstNameAndLastName(caseId, "Joe", "Root");

        final List<BusinessValidationErrorDetails> errorsByCaseIdAfterDeletion = businessValidationErrorsRepository.findByCaseId(caseId);
        assertThat(errorsByCaseIdAfterDeletion, hasSize(0));
    }



    /*
     * ------------------------------------------------------------------------------------------
     * Injection resistance for prosecutioncasefile.query.counts-cases-errors (OWASP A03:2021).
     *
     * The DROMOS Stage02.1 review flagged the courtLocation / caseType / region filter values
     * reaching this count query as a possible injection sink. countOfCasesWithOutstandingErrors
     * builds its predicate with the JPA Criteria API (DeltaSpike criteria().eqIgnoreCase(...)),
     * so each filter value is bound as a query parameter and never spliced into query text.
     * These tests assert that behaviour observably, against a real database, so the finding
     * cannot silently regress.
     *
     * region is filtered on a different table and is covered in ResolvedCasesRepositoryTest.
     * ------------------------------------------------------------------------------------------
     */

    @Test
    public void countOfCasesWithOutstandingErrors_withTautologyInCourtLocation_should_not_bypass_the_filter() {
        produceAndSaveBusinessValidationErrors(50);

        // Sanity check: there are 50 cases available to leak if the filter can be bypassed.
        assertThat(businessValidationErrorsRepository.countOfCasesWithOutstandingErrors(empty(), empty()), is(50L));

        final Long count = businessValidationErrorsRepository
                .countOfCasesWithOutstandingErrors(of(TAUTOLOGY_PAYLOAD), empty());

        // Bound as data: no stored court location equals the literal "Nowhere' OR '1'='1".
        // Had it been concatenated, the OR would have matched every row and returned 50.
        assertThat(count, is(0L));
    }

    @Test
    public void countOfCasesWithOutstandingErrors_withTautologyInCaseType_should_not_bypass_the_filter() {
        produceAndSaveBusinessValidationErrors(50);

        final Long count = businessValidationErrorsRepository
                .countOfCasesWithOutstandingErrors(empty(), of(TAUTOLOGY_PAYLOAD));

        assertThat(count, is(0L));
    }

    @Test
    public void countOfCasesWithOutstandingErrors_withCommentTruncationInCourtLocation_should_not_bypass_the_filter() {
        produceAndSaveBusinessValidationErrors(50);

        // "Leeds' --" would close the literal and comment out the rest of the predicate if
        // concatenated, matching all 50 Leeds cases.
        final Long count = businessValidationErrorsRepository
                .countOfCasesWithOutstandingErrors(of(COMMENT_TRUNCATION_PAYLOAD), empty());

        assertThat(count, is(0L));
    }

    @Test
    public void countOfCasesWithOutstandingErrors_withUnionSelectInCaseType_should_not_bypass_the_filter() {
        produceAndSaveBusinessValidationErrors(50);

        final Long count = businessValidationErrorsRepository
                .countOfCasesWithOutstandingErrors(empty(), of(UNION_PAYLOAD));

        assertThat(count, is(0L));
    }

    @Test
    public void countOfCasesWithOutstandingErrors_withStackedDropTableInCourtLocation_should_not_execute_it() {
        produceAndSaveBusinessValidationErrors(50);

        final Long count = businessValidationErrorsRepository
                .countOfCasesWithOutstandingErrors(of(STACKED_DDL_PAYLOAD), empty());

        assertThat(count, is(0L));

        // The stacked DROP TABLE was never executed: the table is still there with its rows.
        final Number rowCount = (Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM business_validation_errors")
                .getSingleResult();
        assertThat(rowCount.longValue() > 0L, is(true));
        assertThat(businessValidationErrorsRepository.countOfCasesWithOutstandingErrors(empty(), empty()), is(50L));
    }

    @Test
    public void countOfCasesWithOutstandingErrors_withInjectionStringStoredAsCourtLocation_should_match_it_as_a_literal_value() {
        produceAndSaveBusinessValidationErrors(50);

        // Store a row whose court location IS the injection string.
        final BusinessValidationErrorDetails errorWithInjectionLikeCourtLocation = getBusinessValidationErrors();
        errorWithInjectionLikeCourtLocation.setCourtLocation(TAUTOLOGY_PAYLOAD);
        errorWithInjectionLikeCourtLocation.setCaseId(randomUUID());
        errorWithInjectionLikeCourtLocation.setId(randomUUID());
        businessValidationErrorsRepository.save(errorWithInjectionLikeCourtLocation);

        final Long count = businessValidationErrorsRepository
                .countOfCasesWithOutstandingErrors(of(TAUTOLOGY_PAYLOAD), empty());

        // Exactly the one matching row — proving the value is compared as data, not parsed as SQL.
        assertThat(count, is(1L));
    }

    @Test
    public void countOfCasesWithOutstandingErrors_comparedAgainstAConcatenatedQuery_should_prove_the_payload_is_potent() {
        produceAndSaveBusinessValidationErrors(50);

        // Positive control. This is the vulnerable shape the security review describes: the
        // caller-controlled value concatenated into the query text without encoding.
        final String concatenatedQuery = "SELECT COUNT(DISTINCT e.caseId) FROM BusinessValidationErrorDetails e "
                + "WHERE e.courtLocation = '" + TAUTOLOGY_PAYLOAD + "'";
        final Long leakedByConcatenation = entityManager
                .createQuery(concatenatedQuery, Long.class)
                .getSingleResult();

        // Concatenated, the payload defeats the filter and returns every case.
        assertThat(leakedByConcatenation, is(50L));

        // The production repository, given the very same value, returns nothing.
        assertThat(businessValidationErrorsRepository
                .countOfCasesWithOutstandingErrors(of(TAUTOLOGY_PAYLOAD), empty()), is(0L));
    }

    private BusinessValidationErrorDetails getBusinessValidationErrors() {
        return getBusinessValidationErrors(ID, CASEID, DEFENDANTID);
    }

    private BusinessValidationErrorDetails getBusinessValidationErrors(final UUID id, final UUID caseId, final UUID defendantId) {
        final BusinessValidationErrorDetails businessValidationErrors = new BusinessValidationErrorDetails(
                id,
                ERRORVALUE,
                FIELDID,
                DISPLAYNAME,
                caseId,
                defendantId,
                FIELDNAME,
                COURTNAME,
                COURTLOCATION,
                CASETYPE,
                URN,
                DEFENDANTBAILSTATUS,
                FIRST_NAME,
                LAST_NAME,
                ORGANISATION_NAME,
                DEFENDANTCHARGEDATE,
                DEFENDANTHEARINGDATE,
                null
        );
        return businessValidationErrors;
    }
}
