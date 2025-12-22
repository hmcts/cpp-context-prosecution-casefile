package uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContactDetails {

    private final String email;
    private final String home;
    private final String work;
    private final String mobile;

    public ContactDetails(final String email, final String home, final String work, final String mobile) {
        this.email = email;
        this.home = home;
        this.work = work;
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public String getHome() {
        return home;
    }

    public String getWork() {
        return work;
    }

    public String getMobile() {
        return mobile;
    }

}
