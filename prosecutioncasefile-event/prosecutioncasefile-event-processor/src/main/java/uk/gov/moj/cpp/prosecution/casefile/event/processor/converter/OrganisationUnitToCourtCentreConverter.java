package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static uk.gov.justice.core.courts.Address.address;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitReferenceData;

import java.util.UUID;

public class OrganisationUnitToCourtCentreConverter implements Converter<OrganisationUnitReferenceData, CourtCentre> {

    @Override
    public CourtCentre convert(final OrganisationUnitReferenceData source) {
        return courtCentre()
                .withId(UUID.fromString(source.getId()))
                .withName(source.getOucodeL3Name())
                .withAddress(address()
                        .withAddress1(source.getAddress1())
                        .withAddress2(source.getAddress2())
                        .withAddress3(source.getAddress3())
                        .withAddress4(source.getAddress4())
                        .withAddress5(source.getAddress5())
                        .withPostcode(source.getPostcode())
                        .withWelshAddress1(source.getWelshAddress1())
                        .withWelshAddress2(source.getWelshAddress2())
                        .withWelshAddress3(source.getWelshAddress3())
                        .withWelshAddress4(source.getWelshAddress4())
                        .withWelshAddress5(source.getWelshAddress5())
                        .build())
                .withCode(source.getOucode())
                .withCourtHearingLocation(source.getOucode())
                .withWelshCourtCentre(source.getIsWelsh())
                .build();
    }
}
