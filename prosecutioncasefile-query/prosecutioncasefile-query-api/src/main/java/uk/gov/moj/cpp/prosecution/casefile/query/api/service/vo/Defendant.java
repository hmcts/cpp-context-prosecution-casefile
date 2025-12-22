package uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings({"squid:S2384"})
public class Defendant {

    private final String id;
    private final PersonalDetails personalDetails;
    private final List<Offence> offences;
    private final LegalEntityDefendant legalEntityDefendant;
    private final String pcqId;
    private final String initiationCode;

    public Defendant(final String id, final PersonalDetails personalDetails, final List<Offence> offences, final LegalEntityDefendant legalEntityDefendant, final String pcqId, final  String initiationCode ) {
        this.id = id;
        this.personalDetails = personalDetails;
        this.offences = offences;
        this.legalEntityDefendant = legalEntityDefendant;
        this.pcqId = pcqId;
        this.initiationCode = initiationCode;
    }

    public String getId() {
        return id;
    }

    public PersonalDetails getPersonalDetails() {
        return personalDetails;
    }

    public List<Offence> getOffences() {
        return offences;
    }

    public LegalEntityDefendant getLegalEntityDefendant() {
        return legalEntityDefendant;
    }

    public String getPcqId() {
        return pcqId;
    }

    public String getInitiationCode() {
        return initiationCode;
    }

    public static Defendant.Builder defendant() {
        return new Defendant.Builder();
    }

    public static class Builder {
        private String id;
        private String pcqId;
        private List<Offence> offences;
        private PersonalDetails personalDetails;
        private LegalEntityDefendant legalEntityDefendant;

        private  String initiationCode;

        public Defendant.Builder withId(final String id) {
            this.id = id;
            return this;
        }

        public Defendant.Builder withInitiationCode(final String initiationCode) {
            this.initiationCode = initiationCode;
            return this;
        }

        public Defendant.Builder withPersonalDetails(final PersonalDetails personalDetails) {
            this.personalDetails = personalDetails;
            return this;
        }

        public Defendant.Builder withLegalEntityDefendant(final LegalEntityDefendant legalEntityDefendant) {
            this.legalEntityDefendant = legalEntityDefendant;
            return this;
        }

        public Defendant.Builder withOffences(final List<Offence> offences) {
            this.offences = offences;
            return this;
        }

        public Defendant.Builder pcqId(final String pcqId) {
            this.pcqId = pcqId;
            return this;
        }


        public Defendant build() {
            return new Defendant(id, personalDetails, offences, legalEntityDefendant, pcqId, initiationCode);
        }
    }
}
