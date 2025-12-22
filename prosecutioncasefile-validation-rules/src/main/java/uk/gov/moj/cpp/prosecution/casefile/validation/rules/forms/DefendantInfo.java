package uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms;

import javax.json.JsonObject;

public class DefendantInfo {

    private String firstName;
    private String lastName;
    private String dob;
    private String organisationName;
    private JsonObject defendantData;

    public JsonObject getDefendantData() {
        return defendantData;
    }

    public void setDefendantData(final JsonObject defendantData) {
        this.defendantData = defendantData;
    }

    public String getOrganisationName() {
        return organisationName;
    }

    public void setOrganisationName(final String organisationName) {
        this.organisationName = organisationName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(final String dob) {
        this.dob = dob;
    }
}
