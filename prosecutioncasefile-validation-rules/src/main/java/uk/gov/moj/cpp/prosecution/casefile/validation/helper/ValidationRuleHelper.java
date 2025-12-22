package uk.gov.moj.cpp.prosecution.casefile.validation.helper;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.DEFENDANTS;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Organisation;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Person;

import javax.json.JsonObject;

public class ValidationRuleHelper {


    private ValidationRuleHelper() {
    }

    public static boolean isOrganisationInvalid(final Organisation organisation) {
        return nonNull(organisation) && (
                isEmpty(organisation.getName()) ||
                        isNull(organisation.getAddress()) ||
                        isEmpty(organisation.getAddress().getAddress1()) ||
                        isEmpty(organisation.getAddress().getPostcode())
        );
    }

    public static boolean isPersonDetailsInvalid(final Person person) {
        return nonNull(person) && (
                isEmpty(person.getFirstName()) ||
                        isEmpty(person.getLastName()) ||
                        isNull(person.getAddress()) ||
                        isEmpty(person.getAddress().getAddress1()) ||
                        isEmpty(person.getAddress().getPostcode())
        );
    }

    public static boolean isValidNameAndAddress(final Person person, final Organisation organisation) {
        //When the Full Name and/or Address of have not been provided
        return !isPersonDetailsInvalid(person) && !isOrganisationInvalid(organisation);
    }


    public static JsonObject getDefendantByName(final JsonObject matchedObject, final String nameKey) {

        return matchedObject
                .getJsonArray(DEFENDANTS)
                .getValuesAs(JsonObject.class)
                .stream()
                .filter(o -> {
                    if (o.containsKey("organisationName") && o.getString("organisationName").equals(nameKey)) {
                        return true;
                    }

                    final String compositeKey = o
                            .getJsonObject("personalInformation")
                            .getString("firstName")
                            .concat(o.getJsonObject("personalInformation")
                                    .getString("lastName"))
                            .concat(o.getJsonObject("selfDefinedInformation")
                                    .getString("dateOfBirth"));

                    return compositeKey.equals(nameKey);
                })
                .findFirst().orElse(null);
    }



    public static JsonObject getDefendantByProsecutorDefendantReference(final JsonObject matchedObject, final String prosecutorDefendantReference) {

        return matchedObject
                .getJsonArray(DEFENDANTS)
                .getValuesAs(JsonObject.class)
                .stream()
                .filter(o -> o.containsKey("prosecutorDefendantReference") && o.getString("prosecutorDefendantReference").equals(prosecutorDefendantReference))
                .findFirst().orElse(null);
    }

    public static String getValueAsString(final JsonObject jsonObject, final String key) {

        return jsonObject.containsKey(key) ? jsonObject.getString(key) : null;
    }
}
