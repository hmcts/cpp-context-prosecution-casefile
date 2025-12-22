package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.BailStatusReferenceData.bailStatusReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_CUSTODY_STATUS_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.DEFENDANT_CUSTODY_STATUS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.CustodyStatusValidationAndEnricherRule.CHARGE_CASE_TYPE;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.BailStatusReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CustodyStatusValidationAndEnricherRuleTest {

    public static final String STATUS_DESCRIPTION = "In Custody";
    public static final String DATE_VALID_FROM = "2005-05-05";
    private static String VALID_CUSTODY_STATUS = "A";
    private static String INVALID_CUSTODY_STATUS = "B";
    private static String BAIL_STATUS_CODE = "Z";


    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    DefendantWithReferenceData defendantWithReferenceData;

    @Test
    public void shouldReturnEmptyListWhenCustodyStatusIsValid() {
        when(defendantWithReferenceData.getCaseDetails().getInitiationCode()).thenReturn(CHARGE_CASE_TYPE);
        when(defendantWithReferenceData.getDefendant().getCustodyStatus()).thenReturn(VALID_CUSTODY_STATUS);
        when(referenceDataQueryService.retrieveBailStatuses()).thenReturn(buildBailStatusReferenceData());
        when(defendantWithReferenceData.getReferenceDataVO()).thenReturn(new ReferenceDataVO());

        Optional<Problem> optionalProblem = new CustodyStatusValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));

        //should use cached value when invoked second time
        optionalProblem = new CustodyStatusValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
        verify(referenceDataQueryService, times(1)).retrieveBailStatuses();
    }

    @Test
    public void shouldReturnEmptyListWhenCaseTypeIsNotC() {
        when(defendantWithReferenceData.getCaseDetails().getInitiationCode()).thenReturn("S");

        final Optional<Problem> optionalProblem = new CustodyStatusValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldNotReturnEmptyListWhenCaseTypeIsNotMCC() {
        when(defendantWithReferenceData.getCaseDetails().getInitiationCode()).thenReturn("O");
        when(defendantWithReferenceData.isMCC()).thenReturn(true);

        final Optional<Problem> optionalProblem = new CustodyStatusValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(true));
    }
    @Test
    public void shouldReturnEmptyListWhenCaseTypeIsNotMCC() {
        when(defendantWithReferenceData.getCaseDetails().getInitiationCode()).thenReturn("O");
        when(defendantWithReferenceData.isMCC()).thenReturn(false);

        final Optional<Problem> optionalProblem = new CustodyStatusValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyListWhenCorporateDefendant() {
        when(defendantWithReferenceData.getCaseDetails().getInitiationCode()).thenReturn(CHARGE_CASE_TYPE);
        when(defendantWithReferenceData.getDefendant().getOrganisationName()).thenReturn("Microsoft");

        final Optional<Problem> optionalProblem = new CustodyStatusValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnProblemWhenCaseTypeIsCAndCustodyStatusNotProvided() {
        when(defendantWithReferenceData.getCaseDetails().getInitiationCode()).thenReturn(CHARGE_CASE_TYPE);
        when(defendantWithReferenceData.getDefendant().getCustodyStatus()).thenReturn(null);

        final Optional<Problem> optionalProblem = new CustodyStatusValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(true));
        assertThat(optionalProblem.get().getCode(), is(DEFENDANT_CUSTODY_STATUS_INVALID.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(DEFENDANT_CUSTODY_STATUS.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(""));
    }

    @Test
    public void shouldReturnProblemWhenCustodyStatusIsInvalid() {
        when(defendantWithReferenceData.getCaseDetails().getInitiationCode()).thenReturn(CHARGE_CASE_TYPE);
        when(defendantWithReferenceData.getDefendant().getCustodyStatus()).thenReturn(INVALID_CUSTODY_STATUS);
        when(referenceDataQueryService.retrieveBailStatuses()).thenReturn(buildBailStatusReferenceData());
        when(defendantWithReferenceData.getReferenceDataVO()).thenReturn(new ReferenceDataVO());

        final Optional<Problem> optionalProblem = new CustodyStatusValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(true));
        assertThat(optionalProblem.get().getCode(), is(DEFENDANT_CUSTODY_STATUS_INVALID.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(DEFENDANT_CUSTODY_STATUS.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(INVALID_CUSTODY_STATUS));
    }

    @Test
    public void shouldReturnNoProblemWhenhereAreBailStatusPresent() {
        when(defendantWithReferenceData.getCaseDetails().getInitiationCode()).thenReturn(CHARGE_CASE_TYPE);
        when(defendantWithReferenceData.getDefendant().getCustodyStatus()).thenReturn(VALID_CUSTODY_STATUS);
        when(referenceDataQueryService.retrieveBailStatuses()).thenReturn(buildBailStatusReferenceData());
        when(defendantWithReferenceData.getReferenceDataVO()).thenReturn(new ReferenceDataVO());
        when(defendantWithReferenceData.getReferenceDataVO()).thenReturn(new ReferenceDataVO());

        Optional<Problem> optionalProblem = new CustodyStatusValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));

    }

    @Test
    public void shouldReturnProblemWhenThereInvalidCustodyStatisIsPassed() {
        when(defendantWithReferenceData.getCaseDetails().getInitiationCode()).thenReturn(CHARGE_CASE_TYPE);
        when(defendantWithReferenceData.getDefendant().getCustodyStatus()).thenReturn(VALID_CUSTODY_STATUS);

        final List<BailStatusReferenceData> bailStatusRefDataInvalid = singletonList(bailStatusReferenceData().withStatusCode(INVALID_CUSTODY_STATUS).build());
        when(referenceDataQueryService.retrieveBailStatuses()).thenReturn(bailStatusRefDataInvalid);
        final List<BailStatusReferenceData> bailStatusReferenceDataList = getBailStatusReferenceDataWithValidCustodyStatus();
        when(defendantWithReferenceData.getReferenceDataVO().getBailStatusReferenceData()).thenReturn(bailStatusReferenceDataList);

        final Optional<Problem> optionalProblem = new CustodyStatusValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(true));
    }

    private List<BailStatusReferenceData> buildBailStatusReferenceData() {
        return singletonList(bailStatusReferenceData()
                .withStatusCode(VALID_CUSTODY_STATUS)
                .build());
    }

    private List<BailStatusReferenceData> getBailStatusReferenceDataWithValidCustodyStatus() {
        final List<BailStatusReferenceData> bailStatusReferenceDataList  = new ArrayList<BailStatusReferenceData>();
        final BailStatusReferenceData bailStatusRefData = new BailStatusReferenceData(UUID.randomUUID(), 1, BAIL_STATUS_CODE, STATUS_DESCRIPTION, DATE_VALID_FROM);
        bailStatusReferenceDataList.add(bailStatusRefData);

        final ReferenceDataVO refDataVO =  new ReferenceDataVO();
        refDataVO.setBailStatusReferenceData(bailStatusReferenceDataList);
        return bailStatusReferenceDataList;
    }
}