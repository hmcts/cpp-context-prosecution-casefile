package uk.gov.moj.cpp.prosecution.casefile.refdata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.refdata.HearingTypeLookUp.findHearingType;

import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingType;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingTypeLookUpTest {

    @InjectMocks
    private HearingTypeLookUp hearingTypeLookUp;
    private static final String FHG_HEARING_CODE = "FHG";
    private static final String FPTP_HEARING_CODE = "FPTP";

    @Test
    public void testShouldFindHearingType() {
        final HearingTypes hearingTypes = getMockHearingTypesReferenceData(FHG_HEARING_CODE);
        Optional<HearingType> hearingType = findHearingType(getMockDefendantList(FHG_HEARING_CODE), hearingTypes);
        assertNotNull(hearingType);
        assertThat(hearingType.get().getHearingCode(), is(FHG_HEARING_CODE));
    }

    @Test
    public void testShouldFindInputHearingType() {
        final HearingTypes hearingTypes = getMockHearingTypesReferenceData(FPTP_HEARING_CODE);
        Optional<HearingType> hearingType = findHearingType(getMockDefendantList(FPTP_HEARING_CODE), hearingTypes);
        assertNotNull(hearingType);
        assertThat(hearingType.get().getHearingCode(), is(FPTP_HEARING_CODE));
    }

    @Test
    public void testShouldFindDefaultHearingType() {
        final HearingTypes hearingTypes = getMockHearingTypesReferenceData(FHG_HEARING_CODE);
        Optional<HearingType> hearingType = findHearingType(getMockDefendantList(null), hearingTypes);
        assertNotNull(hearingType);
        assertThat(hearingType.get().getHearingCode(), is(FHG_HEARING_CODE));
    }

    private HearingTypes getMockHearingTypesReferenceData(final String hearingTypeCode) {
        List<HearingType> hearingTypesReferenceData = new ArrayList<>();
        hearingTypesReferenceData.add(HearingType.hearingType()
                .withId(UUID.randomUUID())
                .withSeqId(20)
                .withHearingCode(hearingTypeCode)
                .withHearingDescription(hearingTypeCode)
                .build()
        );
        return HearingTypes.hearingTypes().withHearingtypes(hearingTypesReferenceData).build();
    }

    private List<Defendant> getMockDefendantList(final String hearingTypeCode){
       return Collections.singletonList(Defendant.defendant()
               .withInitialHearing(InitialHearing.initialHearing()
                       .withHearingTypeCode(hearingTypeCode).build())
               .build());
    }
}
