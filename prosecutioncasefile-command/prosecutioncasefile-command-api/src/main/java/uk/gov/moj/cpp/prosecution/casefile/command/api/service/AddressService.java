package uk.gov.moj.cpp.prosecution.casefile.command.api.service;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class AddressService {

    private static final String POSTCODE = "postcode";

    private AddressService() {

    }

    /**
     * Replaces postcode in address JsonObject with normalized value
     *
     * @return JsonObject containing the converted postcode
     */
    public static JsonObject normalizePostcodeInAddress(final JsonObject addressJsonObject) {
        if (isNull(addressJsonObject)) {
            return null;
        }

        if (!addressJsonObject.containsKey(POSTCODE)) {
            return addressJsonObject;
        }

        final JsonObjectBuilder addressObjectBuilder = createObjectBuilderWithFilter(addressJsonObject, field -> !field.equals(POSTCODE));
        addressObjectBuilder.add(POSTCODE, normalizePostcode(addressJsonObject
                .getString(POSTCODE)));

        return addressObjectBuilder.build();
    }

    /**
     * Converts the postcode to uppercase and separates it with space
     *
     * @return normalized postcode String
     */
    public static String normalizePostcode(final String postcode) {
        final String uppercaseTrimmed = postcode.toUpperCase().replaceAll("\\s", EMPTY);

        return new StringBuilder(uppercaseTrimmed)
                .insert(uppercaseTrimmed.length() - 3, SPACE)
                .toString();
    }

}
