package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import uk.gov.justice.json.schemas.domains.sjp.ContactDetails;
import uk.gov.justice.services.common.converter.Converter;

import java.util.Optional;

public class ProsecutionCaseFileContactDetailsToSjpContactDetailsConverter implements Converter<uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails, ContactDetails> {

    @Override
    public ContactDetails convert(final uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails source) {
        return Optional.ofNullable(source)
                .map(contactDetails -> ContactDetails.contactDetails()
                        .withBusiness(source.getWork())
                        .withMobile(source.getMobile())
                        .withHome(source.getHome())
                        .withEmail(source.getPrimaryEmail())
                        .withEmail2(source.getSecondaryEmail())
                        .build())
                .orElse(null);
    }

}
