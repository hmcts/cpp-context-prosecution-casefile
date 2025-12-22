package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.PROSECUTOR_OUCODE_NOT_RECOGNISED;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.PROSECUTING_AUTHORITY;

import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutorReferenceDataValidationRuleTest {

    public static final String ORIGINATING_ORGANISATION_CODE = "GAFTL00";

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock
    ProsecutorsReferenceData prosecutorsReferenceData;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ProsecutionWithReferenceData prosecutionWithReferenceData;


    @Test
    public void shouldReturnEmptyListWhenOriginatingOrganisationIsValid() {
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getProsecutor().getProsecutingAuthority()).thenReturn(ORIGINATING_ORGANISATION_CODE);

        final Optional<Problem> optionalProblem = new ProsecutorReferenceDataValidationRule().validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnProblemWhenOriginatingOrganisationIsInvalid() {
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getProsecutor().getProsecutingAuthority()).thenReturn(ORIGINATING_ORGANISATION_CODE);
        when(prosecutionWithReferenceData.getReferenceDataVO().getProsecutorsReferenceData()).thenReturn(null);

        final Optional<Problem> optionalProblem = new ProsecutorReferenceDataValidationRule().validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.get().getCode(), is(PROSECUTOR_OUCODE_NOT_RECOGNISED.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(PROSECUTING_AUTHORITY.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(ORIGINATING_ORGANISATION_CODE));
    }
}
