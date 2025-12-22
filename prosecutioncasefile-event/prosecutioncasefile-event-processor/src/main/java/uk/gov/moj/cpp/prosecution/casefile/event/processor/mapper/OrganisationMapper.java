package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.Optional.ofNullable;

import uk.gov.justice.core.courts.Organisation;

import java.util.function.Function;

public class OrganisationMapper {

    public static final Function<uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Organisation, Organisation> convertApplicantOrganisation =
            sourceApplicantOrganisation -> ofNullable(sourceApplicantOrganisation)
                    .map(organisation ->
                            Organisation.organisation()
                                    .withName(organisation.getName())
                                    .withIncorporationNumber((organisation.getIncorporationNumber()))
                                    .withRegisteredCharityNumber(organisation.getRegisteredCharityNumber())
                                    .withAddress(AddressMapper.convertAddress.apply(organisation.getAddress()))
                                    .withContact(ContactMapper.convertContact.apply(organisation.getContact()))
                                    .build()
                    ).orElse(null);

    private OrganisationMapper() {
    }
}
