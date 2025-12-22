package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper.GIVEN_NAME_2;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper.GIVEN_NAME_3;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper.buildProsecutionWithReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.AlcoholLevelMethodReferenceData.alcoholLevelMethodReferenceData;

import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.prosecution.casefile.domain.ParamsVO;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseFileDefendantToCCDefendantConverterTest {

    private static final String EITHER_WAY = "Either Way";

    private ProsecutionCaseFileDefendantToCCDefendantConverter converter;

    @Spy
    private ProsecutionCaseToCCPersonDefendantConverter prosecutionCaseToCCPersonDefendantConverterToCCOffenceConverter;

    @Spy
    private ProsecutionCaseFileToCCLegalEntityDefendantConverter prosecutionCaseFileToCCLegalEntityDefendantConverter;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @BeforeEach
    public void setUp() {
        converter =  new ProsecutionCaseFileDefendantToCCDefendantConverter();

        var prosecutionCaseFileOffenceToCourtsOffenceConverter = new ProsecutionCaseFileOffenceToCourtsOffenceConverter();
        ReflectionUtil.setField(prosecutionCaseFileOffenceToCourtsOffenceConverter, "referenceDataQueryService", referenceDataQueryService);

        ReflectionUtil.setField(converter, "prosecutionCaseToCCPersonDefendantConverterToCCOffenceConverter", prosecutionCaseToCCPersonDefendantConverterToCCOffenceConverter);
        ReflectionUtil.setField(converter, "prosecutionCaseFileToCCLegalEntityDefendantConverter", prosecutionCaseFileToCCLegalEntityDefendantConverter);
        ReflectionUtil.setField(converter, "prosecutionCaseFileOffenceToCourtsOffenceConverter",  prosecutionCaseFileOffenceToCourtsOffenceConverter);
    }

    @Test
    public void convertToCourtsDefendant() {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = buildProsecutionWithReferenceData(EITHER_WAY);
        final List<Defendant> defendants = prosecutionWithReferenceData.getProsecution().getDefendants();
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setCaseId(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseId());
        paramsVO.setReferenceDataVO(prosecutionWithReferenceData.getReferenceDataVO());
        paramsVO.setInitiationCode("J");

        when(referenceDataQueryService.retrieveAlcoholLevelMethods()).thenReturn(asList(alcoholLevelMethodReferenceData().withMethodCode("A").withMethodDescription("Blood").build(),
                alcoholLevelMethodReferenceData().withMethodCode("B").withMethodDescription("Breath").build()));

        final List<uk.gov.justice.core.courts.Defendant> courtsDefendants = converter.convert(defendants, paramsVO);

        assertThat(defendants.size(), equalTo(courtsDefendants.size()));
        assertThat(defendants.get(0).getOffences().size(), equalTo(courtsDefendants.get(0).getOffences().size()));
        assertThat(defendants.get(0).getOffences().get(0).getLaidDate(), equalTo(LocalDate.of(2019, 1, 1)));
        assertThat("offenceLegalisation", equalTo(courtsDefendants.get(0).getOffences().get(0).getOffenceLegislation()));
        assertAliasMiddleNameValues(courtsDefendants);
        assertThat(courtsDefendants.get(0).getOffences().get(0).getOffenceFacts().getAlcoholReadingAmount(), is(500));
        assertThat(courtsDefendants.get(0).getOffences().get(0).getOffenceFacts().getAlcoholReadingMethodCode(), is("A"));
        assertThat(courtsDefendants.get(0).getOffences().get(0).getOffenceFacts().getAlcoholReadingMethodDescription(), is("Blood"));
    }

    @Test
    public void convertToCourtsDefendantWithNoAliases() {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = buildProsecutionWithReferenceData(EITHER_WAY, true);
        final List<Defendant> defendants = prosecutionWithReferenceData.getProsecution().getDefendants();
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setCaseId(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseId());
        paramsVO.setReferenceDataVO(prosecutionWithReferenceData.getReferenceDataVO());
        paramsVO.setInitiationCode("J");

        when(referenceDataQueryService.retrieveAlcoholLevelMethods()).thenReturn(asList(alcoholLevelMethodReferenceData().withMethodCode("A").withMethodDescription("Blood").build(),
                alcoholLevelMethodReferenceData().withMethodCode("B").withMethodDescription("Breath").build()));

        final List<uk.gov.justice.core.courts.Defendant> courtsDefendants = converter.convert(defendants, paramsVO);

        assertThat(defendants.size(), equalTo(courtsDefendants.size()));
        assertNull(courtsDefendants.get(0).getAliases());
        assertNull(courtsDefendants.get(1).getAliases());
    }

    private void assertAliasMiddleNameValues(final List<uk.gov.justice.core.courts.Defendant> courtsDefendants) {
        assertThat(courtsDefendants.get(0).getAliases().get(0).getMiddleName(), is(GIVEN_NAME_2 + " " + GIVEN_NAME_3));
        assertThat(courtsDefendants.get(0).getAliases().get(1).getMiddleName(), is(GIVEN_NAME_2));
        assertThat(courtsDefendants.get(0).getAliases().get(2).getMiddleName(), is(GIVEN_NAME_3));
        assertThat(courtsDefendants.get(0).getAliases().get(3).getMiddleName(), is(nullValue()));
    }
}