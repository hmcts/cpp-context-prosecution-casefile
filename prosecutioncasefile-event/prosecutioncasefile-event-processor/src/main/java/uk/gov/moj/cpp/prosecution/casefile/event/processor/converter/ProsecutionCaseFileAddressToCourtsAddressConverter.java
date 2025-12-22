package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static uk.gov.justice.core.courts.Address.address;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address;

public class ProsecutionCaseFileAddressToCourtsAddressConverter implements Converter<Address, uk.gov.justice.core.courts.Address> {

    @Override
    public uk.gov.justice.core.courts.Address convert(final Address source) {
        return address()
                .withAddress1(source.getAddress1())
                .withAddress2(source.getAddress2())
                .withAddress3(source.getAddress3())
                .withAddress4(source.getAddress4())
                .withAddress5(source.getAddress5())
                .withPostcode(source.getPostcode())
                .build();
    }
}
