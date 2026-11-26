package uk.gov.moj.cpp.prosecutioncasefile.persistence.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class IndividualAliasDetail implements Serializable {

    @Column(name = "title")
    private String title;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "given_name_2")
    private String givenName2;

    @Column(name = "given_name_3")
    private String givenName3;

    @Column(name = "last_name")
    private String lastName;

    public IndividualAliasDetail() {
    }

    public IndividualAliasDetail(final String title, final String firstName, final String givenName2, final String givenName3, final String lastName) {
       this.title = title;
       this.firstName = firstName;
       this.lastName = lastName;
       this.givenName2 = givenName2;
       this.givenName3 = givenName3;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getGivenName2() {
        return givenName2;
    }

    public void setGivenName2(final String givenName2) {
        this.givenName2 = givenName2;
    }

    public String getGivenName3() {
        return givenName3;
    }

    public void setGivenName3(final String givenName3) {
        this.givenName3 = givenName3;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }
}
