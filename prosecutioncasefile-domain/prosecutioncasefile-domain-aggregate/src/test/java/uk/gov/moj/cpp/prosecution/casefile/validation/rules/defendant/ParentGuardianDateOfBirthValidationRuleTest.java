package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_PARENT_GUARDIAN_DATE_OF_BIRTH_IN_FUTURE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.PARENT_GUARDIAN_DATE_OF_BIRTH;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ParentGuardianDateOfBirthValidationRuleTest {

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    DefendantWithReferenceData defendantWithReferenceData;

    @Test
    public void shouldReturnEmptyListWhenNoParentGuardian() {
        when(defendantWithReferenceData.getDefendant().getIndividual().getParentGuardianInformation()).thenReturn(null);
        final Optional<Problem> optionalProblem = new ParentGuardianDateOfBirthValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyListWhenNoParentGuardianDateOfBirth() {
        when(defendantWithReferenceData.getDefendant().getIndividual().getParentGuardianInformation().getDateOfBirth()).thenReturn(null);
        final Optional<Problem> optionalProblem = new ParentGuardianDateOfBirthValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyListWhenParentGuardianDateOfBirthInThePast() {
        final LocalDate DOB = LocalDate.now().minusYears(10);
        when(defendantWithReferenceData.getDefendant().getIndividual().getParentGuardianInformation()
                .getDateOfBirth()).thenReturn(DOB);
        final Optional<Problem> optionalProblem = new ParentGuardianDateOfBirthValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnViolationWhenParentGuardianDateOfBirthInTheFuture() {
        final LocalDate DOB = LocalDate.now().plusMonths(1);
        when(defendantWithReferenceData.getDefendant().getIndividual().getParentGuardianInformation()
                .getDateOfBirth()).thenReturn(DOB);
        final Optional<Problem> optionalProblem = new ParentGuardianDateOfBirthValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(true));
        assertThat(optionalProblem.get().getCode(), is(DEFENDANT_PARENT_GUARDIAN_DATE_OF_BIRTH_IN_FUTURE.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(PARENT_GUARDIAN_DATE_OF_BIRTH.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(DOB.toString()));
    }

}
