package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Language;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StatementOfFactsWelshValidationRuleTest {

    private static final String STATEMENT_OF_FACTS = "Statement Of Facts";
    private static final String STATEMENT_OF_FACTS_WELSH = "Welsh Statement Of Facts";

    private static final UUID offenceId1WithoutSOF = randomUUID();
    private static final UUID offenceId2WithoutSOF = randomUUID();

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    DefendantWithReferenceData defendantWithReferenceData;

    @Test
    public void shouldReturnEmptyListWhenStatementOfFactsIsPresentForSummonsCaseWithSummonsCodeMAndDocumentationLanguageW() {
        when(defendantWithReferenceData.getDefendant().getOffences()).thenReturn(buildMockOffencesWithStatementOfFacts());
        when(defendantWithReferenceData.getCaseDetails().getSummonsCode()).thenReturn("M");
        when(defendantWithReferenceData.getDefendant().getDocumentationLanguage()).thenReturn(Language.W);


        Optional<Problem> optionalProblem = new StatementOfFactsWelshValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyListWhenStatementOfFactsIsNotPresentForSummonsCaseWithSummonsCodeNotM() {
        when(defendantWithReferenceData.getDefendant().getOffences()).thenReturn(buildMockOffencesWithoutStatementOfFacts());
        when(defendantWithReferenceData.getCaseDetails().getSummonsCode()).thenReturn("A");

        Optional<Problem> optionalProblem = new StatementOfFactsWelshValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyListWhenStatementOfFactsIsNotPresentForSummonsCaseWithSummonsCodeMAndDocumentationLanguageNotW() {
        when(defendantWithReferenceData.getDefendant().getOffences()).thenReturn(buildMockOffencesWithStatementOfFacts());
        when(defendantWithReferenceData.getCaseDetails().getSummonsCode()).thenReturn("M");
        when(defendantWithReferenceData.getDefendant().getDocumentationLanguage()).thenReturn(Language.E);

        Optional<Problem> optionalProblem = new StatementOfFactsWelshValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnProblemWhenStatementOfFactsWelshIsNotPresentForSummonsCaseWithSummonsCaseMAndDocumentLanguageW() {
        when(defendantWithReferenceData.getDefendant().getOffences()).thenReturn(buildMockOffencesWithoutStatementOfFacts());
        when(defendantWithReferenceData.getCaseDetails().getSummonsCode()).thenReturn("M");
        when(defendantWithReferenceData.getDefendant().getDocumentationLanguage()).thenReturn(Language.W);

        final Optional<Problem> optionalProblem = new StatementOfFactsWelshValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(true));

        optionalProblem.ifPresent(problem -> {
            final List<ProblemValue> problemValues = problem.getValues();
            assertThat(problemValues.size(), is(2));
            assertThat(problemValues, hasItem(hasProperty("id", is(offenceId1WithoutSOF.toString()))));
            assertThat(problemValues, hasItem(hasProperty("id", is(offenceId2WithoutSOF.toString()))));
        });
    }

    private List<Offence> buildMockOffencesWithStatementOfFacts() {
        return singletonList(offence()
                .withStatementOfFacts(STATEMENT_OF_FACTS)
                .withStatementOfFactsWelsh(STATEMENT_OF_FACTS_WELSH)
                .build());
    }

    private List<Offence> buildMockOffencesWithoutStatementOfFacts() {
        return asList(
                offence().withOffenceId(offenceId1WithoutSOF).build(),
                offence().withStatementOfFactsWelsh(STATEMENT_OF_FACTS_WELSH).withStatementOfFacts(STATEMENT_OF_FACTS).build(),
                offence().withOffenceId(offenceId2WithoutSOF).build());
    }
}