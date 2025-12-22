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
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.VehicleCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.VehicleRelatedOffence;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.VehicleCodeRefDataEnricher;
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
public class VehicleCodeRefDataEnricherTest {
    private static final String DEFENDANT_ID = "1234243";
    private static final String VEHICLE_CODE = "1";
    @Mock
    private Metadata metadata;
    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @InjectMocks
    private VehicleCodeRefDataEnricher vehicleCodeRefDataEnricher;

    @Test
    public void testShouldPopulateVehicleCodeRefDataWhenVehicleCodeFound() {
        when(referenceDataQueryService.retrieveVehicleCodes()).thenReturn(getMockVehicleCodeReferenceData());
        final List<DefendantsWithReferenceData> defendantsWithReferenceDataList = asList(getMockDefendantsWithReferenceData(VEHICLE_CODE), getMockDefendantsWithReferenceData(VEHICLE_CODE));
        vehicleCodeRefDataEnricher.enrich(defendantsWithReferenceDataList);
        assertNotNull(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getVehicleCodesReferenceData());
        assertThat(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getVehicleCodesReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getVehicleCodesReferenceData().get(0), isA(VehicleCodeReferenceData.class));
        assertThat(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getVehicleCodesReferenceData().get(0).getCode(), is(VEHICLE_CODE));

        assertNotNull(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getVehicleCodesReferenceData());
        assertThat(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getVehicleCodesReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getVehicleCodesReferenceData().get(0), isA(VehicleCodeReferenceData.class));
        assertThat(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getVehicleCodesReferenceData().get(0).getCode(), is(VEHICLE_CODE));
        verify(referenceDataQueryService, times(1)).retrieveVehicleCodes();
    }

    @Test
    public void testShouldNotPopulateVehicleCodeRefDataWhenVehicleCodeIsNull() {
        final DefendantsWithReferenceData defendantsWithReferenceData = getMockDefendantsWithReferenceData(null);
        vehicleCodeRefDataEnricher.enrich(defendantsWithReferenceData);
        assertTrue(defendantsWithReferenceData.getReferenceDataVO().getVehicleCodesReferenceData().isEmpty());
    }

    private DefendantsWithReferenceData getMockDefendantsWithReferenceData(String vehicleCode) {
        final VehicleRelatedOffence vehicleRelatedOffence = VehicleRelatedOffence.vehicleRelatedOffence().withVehicleCode(vehicleCode).build();
        final Offence offence = Offence.offence().withVehicleRelatedOffence(vehicleRelatedOffence).build();
        final List<Offence> offences = new ArrayList<>();
        offences.add(offence);

        final Defendant defendant = new Defendant.Builder().withId(DEFENDANT_ID).withOffences(offences).build();
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(defendant);

        return new DefendantsWithReferenceData(defendants);
    }

    private List<VehicleCodeReferenceData> getMockVehicleCodeReferenceData() {
        List<VehicleCodeReferenceData> vehicleCodeReferenceData = new ArrayList<>();
        vehicleCodeReferenceData.add(getVehicleCodeReferenceData("1"));
        vehicleCodeReferenceData.add(getVehicleCodeReferenceData("2"));
        return vehicleCodeReferenceData;
    }

    private VehicleCodeReferenceData getVehicleCodeReferenceData(String code) {
        return VehicleCodeReferenceData.vehicleCodeReferenceData()
                .withCode(code)
                .withDescription("Large Goods Vehicle")
                .withId(UUID.randomUUID())
                .withSeqNum(20)
                .withValidFrom("2019-04-01")
                .build();
    }
}