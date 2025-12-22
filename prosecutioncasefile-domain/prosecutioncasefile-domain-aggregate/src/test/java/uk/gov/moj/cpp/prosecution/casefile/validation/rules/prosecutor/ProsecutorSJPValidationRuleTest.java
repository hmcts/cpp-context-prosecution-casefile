package uk.gov.moj.cpp.prosecution.casefile.validation.rules.prosecutor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.PROSECUTOR_NOT_RECOGNISED_AS_AN_AUTHORISED_SJP_PROSECUTOR;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.PROSECUTING_AUTHORITY;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.PROSECUTOR_NOT_AOCP_APPROVED;

import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutorSJPValidationRuleTest {

    public static final String ORIGINATING_ORGANISATION_CODE = "GAFTL00";

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Mock
    private ProsecutorsReferenceData prosecutorsReferenceData;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ProsecutionWithReferenceData prosecutionWithReferenceData;

    @Mock
    Defendant defendant;

    @Mock
    Offence offence;

    @Test
    public void shouldReturnEmptyListWhenOriginatingOrganisationIsValid() {
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getProsecutor().getReferenceData().getSjpFlag()).thenReturn(true);

        final Optional<Problem> optionalProblem = new ProsecutorSJPValidationRule().validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnProblemWhenOriginatingOrganisationIsNonSJP() {
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getProsecutor().getProsecutingAuthority()).thenReturn(ORIGINATING_ORGANISATION_CODE);
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getProsecutor().getReferenceData().getSjpFlag()).thenReturn(false);

        checkFailedValidation(new ProsecutorSJPValidationRule().validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst());
    }

    @Test
    public void shouldReturnProblemWhenOriginatingOrganisationIsUnknown() {
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getProsecutor().getProsecutingAuthority()).thenReturn(ORIGINATING_ORGANISATION_CODE);
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getProsecutor().getReferenceData()).thenReturn(null);

        checkFailedValidation(new ProsecutorSJPValidationRule().validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst());
    }

    @Test
    public void shouldReturnProblemWhenOriginatingOrganisationIsNotAocpEligibleAndOffenceHasAOCPOffer() {
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getProsecutor().getProsecutingAuthority()).thenReturn(ORIGINATING_ORGANISATION_CODE);
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getProsecutor().getReferenceData().getAocpApproved()).thenReturn(false);
        when(prosecutionWithReferenceData.getProsecution().getDefendants()).thenReturn(Arrays.asList(defendant));
        when(defendant.getOffences()).thenReturn(Arrays.asList(offence));
        when(offence.getProsecutorOfferAOCP()).thenReturn(true);

        checkFailedAOCPValidation(new ProsecutorAOCPValidationRule().validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst());
    }

    @Test
    public void shouldReturnReturnEmptyListWhenOriginatingOrganisationIsNotAocpEligibleAndOffenceHasNotAOCPOffer() {
        when(prosecutionWithReferenceData.getProsecution().getDefendants()).thenReturn(Arrays.asList(defendant));
        when(defendant.getOffences()).thenReturn(Arrays.asList(offence));
        when(offence.getProsecutorOfferAOCP()).thenReturn(false);

        final Optional<Problem> optionalProblem = new ProsecutorAOCPValidationRule().validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    private void checkFailedValidation(Optional<Problem> optionalProblem) {
        assertThat(optionalProblem.get().getCode(), is(PROSECUTOR_NOT_RECOGNISED_AS_AN_AUTHORISED_SJP_PROSECUTOR.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(PROSECUTING_AUTHORITY.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(ORIGINATING_ORGANISATION_CODE));
    }

    private void checkFailedAOCPValidation(Optional<Problem> optionalProblem) {
        assertThat(optionalProblem.get().getCode(), is(PROSECUTOR_NOT_AOCP_APPROVED.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(PROSECUTING_AUTHORITY.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(ORIGINATING_ORGANISATION_CODE));
    }
}
