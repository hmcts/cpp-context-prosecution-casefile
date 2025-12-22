package uk.gov.moj.cpp.prosecution.casefile.refdata;


import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ReferenceDataCountryNationality;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.NationalityRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NationalityRefDataEnricherTest {

    @Mock
    private ReferenceDataVO referenceDataVO;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Mock
    private DefendantsWithReferenceData defendantsWithReferenceData;

    @InjectMocks
    private NationalityRefDataEnricher nationalityRefDataEnricher;


    @Test
    public void testShouldPopulateNationalityRefDataEnricherWhenNotFound() {
        when(referenceDataQueryService.retrieveCountryNationality()).thenReturn(getMockNationalitiesRefData());
        final DefendantsWithReferenceData defendantsWithReferenceData = getMockCountryNationalityRefData("US");
        nationalityRefDataEnricher.enrich(defendantsWithReferenceData);
        assertNotNull(defendantsWithReferenceData.getReferenceDataVO().getCountryNationalityReferenceData());
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getCountryNationalityReferenceData().size(), is(2));
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getCountryNationalityReferenceData().get(0), isA(ReferenceDataCountryNationality.class));
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getCountryNationalityReferenceData().get(0).getCjsCode(), is("2"));
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getCountryNationalityReferenceData().get(1).getCjsCode(), is("3"));
        verify(referenceDataQueryService, times(1)).retrieveCountryNationality();

    }

    @Test
    public void testShouldPopulateNationalityRefDataEnricherWhenNotFoundMultiData() {
        when(referenceDataQueryService.retrieveCountryNationality()).thenReturn(getMockNationalitiesRefData());
        final List<DefendantsWithReferenceData> defendantsWithReferenceDataList = asList(getMockCountryNationalityRefData("US"), getMockCountryNationalityRefData("US"));
        nationalityRefDataEnricher.enrich(defendantsWithReferenceDataList);
        assertNotNull(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getCountryNationalityReferenceData());
        assertThat(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getCountryNationalityReferenceData().size(), is(2));
        assertThat(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getCountryNationalityReferenceData().get(0), isA(ReferenceDataCountryNationality.class));
        verify(referenceDataQueryService, times(1)).retrieveCountryNationality();
    }

    @Test
    public void testShouldNotPopulateNationalityRefDataEnricherWhenFound() {
        when(defendantsWithReferenceData.getReferenceDataVO()).thenReturn(referenceDataVO);

        when(referenceDataVO.getCountryNationalityReferenceData()).thenReturn(getMockNationalitiesRefData());
        nationalityRefDataEnricher.enrich(defendantsWithReferenceData);

        verify(referenceDataQueryService, times(1)).retrieveCountryNationality();
    }

    private List<ReferenceDataCountryNationality> getMockNationalitiesRefData() {
        List<ReferenceDataCountryNationality> referenceData = new ArrayList<>();
        referenceData.add(getReferenceDataCountryNationality("2", "US"));
        referenceData.add(getReferenceDataCountryNationality("3", null));
        referenceData.add(getReferenceDataCountryNationality("1", "UK"));
        return referenceData;
    }

    private ReferenceDataCountryNationality getReferenceDataCountryNationality(String cjsCode, String isoCode) {
        return ReferenceDataCountryNationality.referenceDataCountryNationality()
                .withIsoCode(isoCode)
                .withCjsCode(cjsCode)
                .withNationality("American")
                .build();
    }

    private DefendantsWithReferenceData getMockCountryNationalityRefData(String nationality) {
        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation
                .selfDefinedInformation().withNationality(nationality)
                .build();
        final Individual individual = Individual.individual()
                .withSelfDefinedInformation(selfDefinedInformation)
                .build();
        final Defendant defendant = new Defendant.Builder()
                .withIndividual(individual)
                .build();
        final Defendant defendantWithIsoCodeNationality = new Defendant.Builder()
                .withIndividual(Individual.individual()
                        .withSelfDefinedInformation(SelfDefinedInformation
                                .selfDefinedInformation().withNationality("3")
                                .build())
                        .build())
                .build();
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(defendant);
        defendants.add(defendantWithIsoCodeNationality);

        return new DefendantsWithReferenceData(defendants);
    }

}
