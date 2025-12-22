package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData.offenceReferenceData;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.AlcoholRelatedOffence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OffenceDrugLevelAmountValidationAndEnricherRuleTest {

    private static final String MOCK_OFFENCE_CODE = "MOCK CODE";

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @Mock
    private ReferenceDataVO referenceDataVO;
    @InjectMocks
    private OffenceDrugLevelAmountValidationAndEnricherRule offenceDrugLevelAmountValidationAndEnricherRule;

    @Test
    public void shouldReturnEmptyListWhenNoOffences() {
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(null);
        final Optional<Problem> optionalProblem = offenceDrugLevelAmountValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }


    @Test
    public void shouldReturnProblemWhenOffenceIsWithoutAlcoholLevelInfo() {
        when(referenceDataVO.getOffenceReferenceData()).thenReturn(getMockOffenceCodesReferenceData(MOCK_OFFENCE_CODE));
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(getOffence(MOCK_OFFENCE_CODE));
        final Optional<Problem> optionalProblem = offenceDrugLevelAmountValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(true));
    }

    @Test
    public void shouldReturnProblemWhenOffenceIsWithAlcoholLevelInfo() {
        when(referenceDataVO.getOffenceReferenceData()).thenReturn(getMockOffenceCodesReferenceData(MOCK_OFFENCE_CODE));
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(getOffenceWithAlcoholLevelInfo(MOCK_OFFENCE_CODE));
        final Optional<Problem> optionalProblem = offenceDrugLevelAmountValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }


    private DefendantWithReferenceData getMockDefendantWithReferenceData(final Offence offence) {
        final String DEFENDANT_ID = "1234243";
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode("S").build();
        final Defendant.Builder defendantBuilder = new Defendant.Builder().withId(DEFENDANT_ID).withInitiationCode("C");
        if (offence != null) {

            defendantBuilder
                    .withOffences(Arrays.asList(offence));

        }
        final Defendant defendant = defendantBuilder.build();
        final List<OffenceReferenceData> offenceReferenceData = new ArrayList<>();
        offenceReferenceData.add(new OffenceReferenceData.Builder()
                .withCjsOffenceCode(MOCK_OFFENCE_CODE)
                .build());

        return new DefendantWithReferenceData(defendant, referenceDataVO, caseDetails);
    }

    private Offence getOffence(final String offenceCode) {
        return Offence.offence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(offenceCode)
                .build();
    }

    private Offence getOffenceWithAlcoholLevelInfo(final String offenceCode) {
        return Offence.offence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(offenceCode)
                .withAlcoholRelatedOffence(AlcoholRelatedOffence.alcoholRelatedOffence().withAlcoholLevelAmount(1).withAlcoholLevelMethod("A").build())
                .build();
    }

    private List<OffenceReferenceData> getMockOffenceCodesReferenceData(final String offenceCode) {
        return Arrays.asList(offenceReferenceData().withCjsOffenceCode(offenceCode).withDrugsOrAlcoholRelated("Y")
                .build()
        );
    }

}