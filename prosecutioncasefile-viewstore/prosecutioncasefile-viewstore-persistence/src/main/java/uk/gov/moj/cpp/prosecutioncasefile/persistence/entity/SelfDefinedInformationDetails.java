package uk.gov.moj.cpp.prosecutioncasefile.persistence.entity;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Gender;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity
@Table(name = "self_defined_information")
public class SelfDefinedInformationDetails implements Serializable {

    @Id
    @Column(name = "self_defined_information_id", unique = true, nullable = false)
    private String selfDefinedInformationId;

    @Column(name = "additional_nationality")
    private String additionalNationality;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "ethnicity")
    private String ethnicity;

    @Column(name = "gender")
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "nationality")
    private String nationality;


    public SelfDefinedInformationDetails(final String additionalNationality,
                                         final LocalDate dateOfBirth,
                                         final String ethnicity,
                                         final Gender gender,
                                         final String nationality) {
        this.additionalNationality = additionalNationality;
        this.dateOfBirth = dateOfBirth;
        this.ethnicity = ethnicity;
        this.gender = gender;
        this.nationality = nationality;
    }

    public SelfDefinedInformationDetails() {

    }

    public String getSelfDefinedInformationId() {
        return selfDefinedInformationId;
    }

    public void setSelfDefinedInformationId(String selfDefinedInformationId) {
        this.selfDefinedInformationId = selfDefinedInformationId;
    }

    public String getAdditionalNationality() {
        return additionalNationality;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getEthnicity() {
        return ethnicity;
    }

    public Gender getGender() {
        return gender;
    }

    public String getNationality() {
        return nationality;
    }

    public void setAdditionalNationality(String additionalNationality) {
        this.additionalNationality = additionalNationality;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setEthnicity(String ethnicity) {
        this.ethnicity = ethnicity;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }
}
