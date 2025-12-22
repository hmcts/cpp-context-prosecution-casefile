package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.json.schemas.domains.sjp.commands.CreateSjpCase.createSjpCase;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution.prosecution;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor.prosecutor;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData.prosecutorsReferenceData;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionInitiated.sjpProsecutionInitiated;

import uk.gov.justice.json.schemas.domains.sjp.commands.CreateSjpCase;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionInitiated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SjpProsecutionToSjpCaseConverterTest {

    @Mock
    private uk.gov.justice.json.schemas.domains.sjp.commands.Defendant convertedDefendant;

    private CreateSjpCase expectedCreateSjpCase;
    private static final String RANDOM_CASE_REFERENCE = random(10);
    private static final BigDecimal APPLIED_PROSECUTOR_COSTS = BigDecimal.TEN;

    @Mock
    private static Defendant inputDefendant;

    @Mock
    private ProsecutionCaseFileDefendantToSjpDefendantConverter prosecutionCaseFileDefendantToSjpDefendantConverter;

    @InjectMocks
    private SjpProsecutionToSjpCaseConverter underTest;

    @BeforeEach
    public void init() {
        expectedCreateSjpCase = createSjpCase()
                .withId(UUID.randomUUID())
                .withEnterpriseId(random(10))
                .withUrn(RANDOM_CASE_REFERENCE)
                .withCosts(APPLIED_PROSECUTOR_COSTS)
                .withDefendant(convertedDefendant)
                .withProsecutingAuthority("TVL")
                .withPostingDate(LocalDate.of(2000, 1, 1))
                .build();

        when(inputDefendant.getPostingDate()).thenReturn(expectedCreateSjpCase.getPostingDate());
        when(inputDefendant.getAppliedProsecutorCosts()).thenReturn(APPLIED_PROSECUTOR_COSTS);
        when(prosecutionCaseFileDefendantToSjpDefendantConverter.convert(inputDefendant)).thenReturn(convertedDefendant);
    }

    @Test
    public void convertSjpProsecutionInitiated() {
        // GIVEN
        final SjpProsecutionInitiated input = sjpProsecutionInitiated()
                .withProsecution(prosecution()
                        .withCaseDetails(caseDetails()
                                .withCaseId(expectedCreateSjpCase.getId())
                                .withProsecutor(prosecutor()
                                        .withProsecutingAuthority("TVL")
                                        .withReferenceData(prosecutorsReferenceData()
                                                .withOucode("GAEAA01")
                                                .withShortName("TVL")
                                                .withId(randomUUID())
                                                .build())
                                        .build())
                                .withProsecutorCaseReference(RANDOM_CASE_REFERENCE)
                                .build())
                        .withDefendants(singletonList(inputDefendant))
                        .build())
                .withEnterpriseId(expectedCreateSjpCase.getEnterpriseId())
                .build();

        // WHEN
        final CreateSjpCase convertedCreateSjpCase = underTest.convert(input);

        // THEN
        assertThat(convertedCreateSjpCase, equalTo(expectedCreateSjpCase));
        assertThat(convertedDefendant.getLegalEntityName(), equalTo(input.getProsecution().getDefendants().get(0).getOrganisationName()));
        verify(prosecutionCaseFileDefendantToSjpDefendantConverter).convert(inputDefendant);
    }

}
