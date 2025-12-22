package uk.gov.moj.cpp.prosecution.casefile.refdata;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ObservedEthnicityReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ParentGuardianInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.ObservedEthnicityRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ObservedEthnicityRefDataEnricherTest {

    private static final String DEFENDANT_ID = "1234243";
    @Mock
    private Metadata metadata;
    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @InjectMocks
    private ObservedEthnicityRefDataEnricher observedEthnicityRefDataEnricher;

    @Test
    public void testShouldPopulateObservedEthnicityWhenEthnicityFound() {
        when(referenceDataQueryService.retrieveObservedEthnicity()).thenReturn(getMockObservedEthnicityRefData());

        final DefendantsWithReferenceData defendantsWithReferenceData = getMockDefendantsWithReferenceData(1);
        observedEthnicityRefDataEnricher.enrich(defendantsWithReferenceData);
        assertNotNull(defendantsWithReferenceData.getReferenceDataVO().getObservedEthnicityReferenceData());
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getObservedEthnicityReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getObservedEthnicityReferenceData().get(0), isA(ObservedEthnicityReferenceData.class));
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getObservedEthnicityReferenceData().get(0).getEthnicityCode(), is("1"));
        verify(referenceDataQueryService, times(1)).retrieveObservedEthnicity();
    }

    @Test
    public void testShouldPopulateObservedEthnicityWhenEthnicityFoundMultiData() {
        when(referenceDataQueryService.retrieveObservedEthnicity()).thenReturn(getMockObservedEthnicityRefData());

        final List<DefendantsWithReferenceData> defendantsWithReferenceDataList = asList(getMockDefendantsWithReferenceData(1), getMockDefendantsWithReferenceData(1));
        observedEthnicityRefDataEnricher.enrich(defendantsWithReferenceDataList);
        assertNotNull(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getObservedEthnicityReferenceData());
        assertThat(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getObservedEthnicityReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getObservedEthnicityReferenceData().get(0), isA(ObservedEthnicityReferenceData.class));
        verify(referenceDataQueryService, times(1)).retrieveObservedEthnicity();
    }

    @Test
    public void testShouldPopulateObservedEthnicityWhenParentGuardianObservedEthnicityFound() {
        when(referenceDataQueryService.retrieveObservedEthnicity()).thenReturn(getMockObservedEthnicityRefData());

        final DefendantsWithReferenceData defendantsWithReferenceData = getMockDefendantsWithParentGuardianReferenceData("1");
        observedEthnicityRefDataEnricher.enrich(defendantsWithReferenceData);
        assertNotNull(defendantsWithReferenceData.getReferenceDataVO().getObservedEthnicityReferenceData());
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getObservedEthnicityReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getObservedEthnicityReferenceData().get(0), isA(ObservedEthnicityReferenceData.class));
        verify(referenceDataQueryService).retrieveObservedEthnicity();
    }

    @Test
    public void testShouldNotPopulateObservedEthnicityWhenEthnicityIsNull() {
        final DefendantsWithReferenceData defendantsWithReferenceData = getMockDefendantsWithReferenceData(null);
        observedEthnicityRefDataEnricher.enrich(defendantsWithReferenceData);
        assertTrue(defendantsWithReferenceData.getReferenceDataVO().getObservedEthnicityReferenceData().isEmpty());
        verify(referenceDataQueryService, times(1)).retrieveObservedEthnicity();
    }

    private List<ObservedEthnicityReferenceData> getMockObservedEthnicityRefData() {
        List<ObservedEthnicityReferenceData> referenceData = new ArrayList<>();
        referenceData.add(getEthinicityRefData("1"));
        referenceData.add(getEthinicityRefData("2"));
        return referenceData;
    }

    private ObservedEthnicityReferenceData getEthinicityRefData(String code) {
        return ObservedEthnicityReferenceData.observedEthnicityReferenceData()
                .withEthnicityCode(code)
                .withSeqNo(1)
                .withEthnicityDescription("White - North European")
                .withId(UUID.randomUUID())
                .build();
    }

    private DefendantsWithReferenceData getMockDefendantsWithReferenceData(Integer ethnicity) {
        final PersonalInformation personalInformation = PersonalInformation.personalInformation().withObservedEthnicity(ethnicity).build();
        final Individual individual = Individual.individual().withPersonalInformation(personalInformation).build();
        final Defendant defendant = new Defendant.Builder().withId(DEFENDANT_ID).withIndividual(individual).build();
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(defendant);

        return new DefendantsWithReferenceData(defendants);
    }

    private DefendantsWithReferenceData getMockDefendantsWithParentGuardianReferenceData(final String ethnicity) {
        final ParentGuardianInformation parentGuardianInformation = ParentGuardianInformation.parentGuardianInformation().withObservedEthnicity(ethnicity).build();
        final Individual individual = Individual.individual().withParentGuardianInformation(parentGuardianInformation).build();
        final Defendant defendant = new Defendant.Builder().withId(DEFENDANT_ID).withIndividual(individual).build();
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(defendant);

        return new DefendantsWithReferenceData(defendants);
    }
}
