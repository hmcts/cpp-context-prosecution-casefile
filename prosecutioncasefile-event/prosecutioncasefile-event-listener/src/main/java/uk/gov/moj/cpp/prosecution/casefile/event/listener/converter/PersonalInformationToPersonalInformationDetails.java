package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.PersonalInformationDetails;

import javax.inject.Inject;

public class PersonalInformationToPersonalInformationDetails implements Converter<PersonalInformation, PersonalInformationDetails> {

    @Inject
    private AddressToAddressDetails addressToAddressDetails;

    @Inject
    private ContactDetailsToContactDetailsEntity contactDetailsToContactDetailsDetails;

    @Override
    public PersonalInformationDetails convert(final PersonalInformation personalInformation) {
        return new PersonalInformationDetails(
                personalInformation.getTitle(),
                personalInformation.getFirstName(),
                personalInformation.getLastName(),
                personalInformation.getOccupation(),
                personalInformation.getOccupationCode(),
                personalInformation.getAddress()!=null?addressToAddressDetails.convert(personalInformation.getAddress()):null,
                personalInformation.getContactDetails()!=null?contactDetailsToContactDetailsDetails.convert(personalInformation.getContactDetails()):null);
    }
}
