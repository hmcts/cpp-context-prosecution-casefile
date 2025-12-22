package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.core.courts.Address.address;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Address;

import java.util.function.Function;

public class AddressMapper {

    public static final Function<Address, uk.gov.justice.core.courts.Address> convertAddress = sourceAddress ->
            ofNullable(sourceAddress)
                    .map(address -> address()
                            .withAddress1(address.getAddress1())
                            .withAddress2(address.getAddress2())
                            .withAddress3(address.getAddress3())
                            .withAddress4(address.getAddress4())
                            .withAddress5(address.getAddress5())
                            .withPostcode(address.getPostcode())
                            .build())
                    .orElse(null);

    private AddressMapper() {
    }
}
