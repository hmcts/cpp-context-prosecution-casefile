package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.ContactDetails;

public class ContactDetailsToContactDetailsEntity implements Converter<uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails, ContactDetails> {

    @Override
    public ContactDetails convert(final uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails contactDetails) {
        return new ContactDetails(contactDetails.getHome(),
                contactDetails.getMobile(),
                contactDetails.getPrimaryEmail(),
                contactDetails.getSecondaryEmail(),
                contactDetails.getWork());
    }
}
