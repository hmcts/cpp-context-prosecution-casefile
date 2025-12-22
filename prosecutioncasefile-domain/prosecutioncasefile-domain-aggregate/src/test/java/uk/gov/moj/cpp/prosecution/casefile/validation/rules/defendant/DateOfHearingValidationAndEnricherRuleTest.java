package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DATE_OF_HEARING_EARLIER_THAN_OFFENCE_COMMITTED_DATE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DATE_OF_HEARING_IN_THE_PAST;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.DEFENDANT_DATE_OF_HEARING;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DateOfHearingValidationAndEnricherRuleTest {

    private static final String DATE_OF_HEARING = "2050-10-03";
    private static final String OFFENCE_START_DATE_AFTER_HEARING = "2051-10-03";
    private static final String OFFENCE_START_DATE_BEFORE_HEARING = "2049-10-03";
    private static final String PAST_DATE_OF_HEARING = "2006-11-03";

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    DefendantWithReferenceData defendantWithReferenceData;

    @Test
    public void shouldReturnEmptyListWhenDateOfHearingIsInFutureAndAfterOffenceStartDate() {
        when(defendantWithReferenceData.getDefendant().getInitialHearing().getDateOfHearing()).thenReturn(DATE_OF_HEARING);
        when(defendantWithReferenceData.getDefendant().getOffences()).thenReturn(getOffences(OFFENCE_START_DATE_BEFORE_HEARING));
        when(defendantWithReferenceData.getReferenceDataVO()).thenReturn(new ReferenceDataVO());

        Optional<Problem> optionalProblem = new DateOfHearingValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnProblemWhenDateOfHearingIsBeforeOffenceStartDate() {
        when(defendantWithReferenceData.getDefendant().getInitialHearing().getDateOfHearing()).thenReturn(DATE_OF_HEARING);
        when(defendantWithReferenceData.getDefendant().getOffences()).thenReturn(getOffences(OFFENCE_START_DATE_AFTER_HEARING));
        when(defendantWithReferenceData.getReferenceDataVO()).thenReturn(new ReferenceDataVO());

        final Optional<Problem> optionalProblem = new DateOfHearingValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.get().getCode(), is(DATE_OF_HEARING_EARLIER_THAN_OFFENCE_COMMITTED_DATE.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(DEFENDANT_DATE_OF_HEARING.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(DATE_OF_HEARING));
    }


    @Test
    void shouldReturnValidWhenMCCWithListNewHearing() {
        when(defendantWithReferenceData.isMCCWithListNewHearing()).thenReturn(true);
        final Optional<Problem> optionalProblem = new DateOfHearingValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertTrue(optionalProblem.isEmpty());

    }

    private List<Offence> getOffences(final String offenceCommittedDate) {
        return Collections.singletonList(Offence.offence()
                .withOffenceCommittedDate(convertToLocalDate(offenceCommittedDate))
                .build());
    }

    private LocalDate convertToLocalDate(final String date) {
        return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}