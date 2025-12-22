package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;


import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_PARENT_GUARDIAN_SELF_DEFINED_ETHNICITY_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.PARENT_GUARDIAN_SELF_DEFINED_ETHNICITY;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfdefinedEthnicityReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ParentGuardianSelfDefinedEthnicityValidationAndEnricherRuleTest {

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    DefendantWithReferenceData defendantWithReferenceData;

    @Test
    public void shouldReturnEmptyListWhenNoParentGuardianSelfDefinedEthnicity() {
        when(defendantWithReferenceData.getDefendant().getIndividual().getParentGuardianInformation().getSelfDefinedEthnicity()).thenReturn(null);
        final Optional<Problem> optionalProblem = new ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyListWhenSelfDefinedEthnicityMatches() {
        final String ethnicityCode = "1";
        final String ethnicityDescription = "White - North European";
        final SelfdefinedEthnicityReferenceData selfDefinedEthnicityReferenceData = new SelfdefinedEthnicityReferenceData(ethnicityCode,
                ethnicityDescription,
                randomUUID(), 1,
                now().minusMonths(2).toString(),
                now().plusMonths(1).toString());

        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setSelfdefinedEthnicityReferenceData((asList(selfDefinedEthnicityReferenceData)));

        when(defendantWithReferenceData.getDefendant().getIndividual().getParentGuardianInformation().getSelfDefinedEthnicity()).thenReturn(ethnicityCode);
        when(defendantWithReferenceData.getReferenceDataVO()).thenReturn(referenceDataVO);

        final Optional<Problem> optionalProblem = new ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnViolationWhenSelfDefinedEthnicityDoesNotMatch() {
        final String ethnicityCode = "4";
        final String ethnicityDescription = "White - South European";

        final SelfdefinedEthnicityReferenceData selfDefinedEthnicityReferenceData = new SelfdefinedEthnicityReferenceData("1",
                ethnicityDescription,
                randomUUID(), 1,
                now().minusMonths(2).toString(),
                now().plusMonths(1).toString());

        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setSelfdefinedEthnicityReferenceData(asList(selfDefinedEthnicityReferenceData));

        when(defendantWithReferenceData.getDefendant().getIndividual().getParentGuardianInformation().getSelfDefinedEthnicity()).thenReturn(ethnicityCode);
        when(defendantWithReferenceData.getReferenceDataVO()).thenReturn(new ReferenceDataVO());
        when(referenceDataQueryService.retrieveSelfDefinedEthnicity()).thenReturn(asList(selfDefinedEthnicityReferenceData));

        final Optional<Problem> optionalProblem = new ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(true));
        assertThat(optionalProblem.get().getCode(), is( DEFENDANT_PARENT_GUARDIAN_SELF_DEFINED_ETHNICITY_INVALID.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is( PARENT_GUARDIAN_SELF_DEFINED_ETHNICITY.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(ethnicityCode));
    }
}
