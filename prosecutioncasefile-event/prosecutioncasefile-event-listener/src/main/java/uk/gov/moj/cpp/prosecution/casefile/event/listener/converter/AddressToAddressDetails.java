package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.AddressDetails;

public class AddressToAddressDetails implements Converter<Address, AddressDetails> {

    @Override
    public AddressDetails convert(final Address address) {
        return new AddressDetails(address.getAddress1(),
                address.getAddress2(),
                address.getAddress3(),
                address.getAddress4(),
                address.getAddress5(),
                address.getPostcode());
    }
}
