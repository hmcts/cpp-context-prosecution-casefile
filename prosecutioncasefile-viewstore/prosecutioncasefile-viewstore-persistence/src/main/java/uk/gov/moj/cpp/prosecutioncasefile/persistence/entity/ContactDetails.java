package uk.gov.moj.cpp.prosecutioncasefile.persistence.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Embeddable
public class ContactDetails implements Serializable {

    private static final long serialVersionUID = -252659198824297763L;

    @Column(name = "contact_number_home")
    private String home;

    @Column(name = "contact_number_mobile")
    private String mobile;

    @Column(name = "primary_email")
    private String primaryEmail;

    @Column(name = "secondary_email")
    private String secondaryEmail;

    @Column(name = "contact_number_work")
    private String work;

    public ContactDetails() {

    }

    public ContactDetails(final String home,
                          final String mobile,
                          final String primaryEmail,
                          final String secondaryEmail,
                          final String work) {
        this.home = home;
        this.mobile = mobile;
        this.primaryEmail = primaryEmail;
        this.secondaryEmail = secondaryEmail;
        this.work = work;
    }

    public String getHome() {
        return home;
    }

    public void setHome(final String home) {
        this.home = home;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(final String mobile) {
        this.mobile = mobile;
    }

    public String getPrimaryEmail() {
        return primaryEmail;
    }

    public void setPrimaryEmail(final String primaryEmail) {
        this.primaryEmail = primaryEmail;
    }

    public String getSecondaryEmail() {
        return secondaryEmail;
    }

    public void setSecondaryEmail(final String secondaryEmail) {
        this.secondaryEmail = secondaryEmail;
    }

    public String getWork() {
        return work;
    }

    public void setWork(final String work) {
        this.work = work;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
