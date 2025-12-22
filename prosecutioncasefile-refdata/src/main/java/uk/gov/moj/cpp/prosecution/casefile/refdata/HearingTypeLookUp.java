package uk.gov.moj.cpp.prosecution.casefile.refdata;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingType;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingTypes;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class HearingTypeLookUp {

    public static final String FIRST_HEARING_CODE = "FHG";

    private HearingTypeLookUp(){

    }

    public static Optional<HearingType> findHearingType(List<Defendant> defendants, HearingTypes hearingTypes) {

        final Optional<String> inputHearingTypeCode = defendants.stream()
                .filter(d -> (Objects.nonNull(d.getInitialHearing()) && Objects.nonNull(d.getInitialHearing().getHearingTypeCode())))
                .map(d -> d.getInitialHearing().getHearingTypeCode()).findFirst();

        return inputHearingTypeCode
                .map(s -> hearingTypes.getHearingtypes()
                        .stream().filter(x -> x.getHearingCode().equalsIgnoreCase(s)).findAny())
                .orElseGet(() -> hearingTypes.getHearingtypes()
                        .stream().filter(x -> x.getHearingCode().equalsIgnoreCase(FIRST_HEARING_CODE)).findAny());

    }

}
