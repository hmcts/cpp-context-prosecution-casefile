package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import static java.util.Optional.empty;
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

    @Inject
    private BusinessValidationErrorRepository businessValidationErrorsRepository;

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
