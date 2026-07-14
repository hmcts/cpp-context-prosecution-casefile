package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static java.time.LocalDate.now;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DATE_OF_HEARING_MORE_THAN_31DAYS_IN_PAST;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.DEFENDANT_DATE_OF_HEARING;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DateOfHearingMoreThan31DaysInPastValidationAndEnricherRuleTest {

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    DefendantWithReferenceData defendantWithReferenceData;

    @Test
    public void shouldReturnEmptyListWhenDateOfHearingIsInFuture() {
        when(defendantWithReferenceData.getDefendant().getInitialHearing().getDateOfHearing()).thenReturn(now().plusDays(5).format(ISO_LOCAL_DATE));

        final Optional<Problem> optionalProblem = new DateOfHearingMoreThan31DaysInPastValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyListWhenDateOfHearingIsExactly31DaysInPast() {
        when(defendantWithReferenceData.getDefendant().getInitialHearing().getDateOfHearing()).thenReturn(now().minusDays(31).format(ISO_LOCAL_DATE));

        final Optional<Problem> optionalProblem = new DateOfHearingMoreThan31DaysInPastValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyListWhenMigratedCaseIsInactive() {
        when(defendantWithReferenceData.isInactiveMigratedCase()).thenReturn(true);

        final Optional<Problem> optionalProblem = new DateOfHearingMoreThan31DaysInPastValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnValidWhenMCCWithListNewHearing() {
        when(defendantWithReferenceData.isMCCWithListNewHearing()).thenReturn(true);

        final Optional<Problem> optionalProblem = new DateOfHearingMoreThan31DaysInPastValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertTrue(optionalProblem.isEmpty());
    }

    @Test
    public void shouldReturnProblemWhenDateOfHearingIsMoreThan31DaysInPast() {
        final String hearingDate = now().minusDays(32).format(ISO_LOCAL_DATE);
        when(defendantWithReferenceData.getDefendant().getInitialHearing().getDateOfHearing()).thenReturn(hearingDate);

        final Optional<Problem> optionalProblem = new DateOfHearingMoreThan31DaysInPastValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(true));
        assertThat(optionalProblem.get().getCode(), is(DATE_OF_HEARING_MORE_THAN_31DAYS_IN_PAST.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(DEFENDANT_DATE_OF_HEARING.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(hearingDate));
    }

}
