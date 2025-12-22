package uk.gov.moj.cpp.prosecution.casefile.event.listener.model;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class ErrorDefendantDetails implements Serializable {

    private String id;
    private String firstName;
    private String lastName;
    private String organisationName;
    private List<ErrorOffenceDetails> offences;

    public ErrorDefendantDetails(final String id, final String firstName, final String lastName, final String organisationName, final List<ErrorOffenceDetails> offences) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.organisationName = organisationName;
        this.offences = new ArrayList<>(offences);
    }

    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getOrganisationName() {
        return organisationName;
    }

    public List<ErrorOffenceDetails> getOffences() {
        return new ArrayList<>(offences);
    }
}