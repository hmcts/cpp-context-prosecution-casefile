package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.Optional.ofNullable;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.ContactNumber;

import java.util.function.Function;

public class ContactMapper {

    public static final Function<ContactNumber, uk.gov.justice.core.courts.ContactNumber> convertContact =
            sourceContact -> ofNullable(sourceContact)
                    .map(contact ->
                            uk.gov.justice.core.courts.ContactNumber.contactNumber()
                                    .withWork(contact.getWork())
                                    .withHome(contact.getHome())
                                    .withMobile(contact.getMobile())
                                    .withPrimaryEmail(contact.getPrimaryEmail())
                                    .withSecondaryEmail(contact.getSecondaryEmail())
                                    .withFax(contact.getFax())
                                    .build()).orElse(null);

    private ContactMapper() {
    }
}
