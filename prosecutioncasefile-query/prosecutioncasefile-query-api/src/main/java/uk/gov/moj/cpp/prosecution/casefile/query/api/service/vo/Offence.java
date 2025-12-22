package uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@SuppressWarnings("squid:S00107")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Offence {

    private final String id;
    private final String plea;
    private final String title;
    private final String titleWelsh;
    private final String legislation;
    private final String legislationWelsh;
    private final String wording;
    private final String wordingWelsh;
    private final Boolean endorsable;
    private final Boolean imprisonable;
    private final Boolean hasPlea;
    private final Boolean onlinePleaReceived;
    private final BigDecimal compensation; // sjp acocp flow
    private final BigDecimal aocpStandardPenalty; // sjp acocp flow

    public Offence(final String id, final String plea, final String title, final String titleWelsh, final String legislation,
                   final String legislationWelsh, final String wording, final String wordingWelsh, final Boolean endorsable, final Boolean imprisonable,
                   final Boolean hasPlea, final Boolean onlinePleaReceived, final BigDecimal compensation, final BigDecimal aocpStandardPenalty) {

        this.id = id;
        this.plea = plea;
        this.title = title;
        this.titleWelsh = titleWelsh;
        this.legislation = legislation;
        this.legislationWelsh = legislationWelsh;
        this.wording = wording;
        this.wordingWelsh = wordingWelsh;
        this.endorsable = endorsable;
        this.imprisonable = imprisonable;
        this.hasPlea = hasPlea;
        this.onlinePleaReceived = onlinePleaReceived;
        this.compensation = compensation;
        this.aocpStandardPenalty = aocpStandardPenalty;
    }

    public String getId() {
        return id;
    }

    public String getPlea() {
        return plea;
    }

    public String getTitle() {
        return title;
    }

    public String getTitleWelsh() {
        return titleWelsh;
    }

    public String getLegislation() {
        return legislation;
    }

    public String getLegislationWelsh() {
        return legislationWelsh;
    }

    public String getWording() {
        return wording;
    }

    public String getWordingWelsh() {
        return wordingWelsh;
    }

    public Boolean getEndorsable() {
        return endorsable;
    }

    public Boolean getImprisonable() {
        return imprisonable;
    }

    public Boolean getHasPlea() {
        return hasPlea;
    }

    public Boolean getOnlinePleaReceived() {
        return onlinePleaReceived;
    }
    public BigDecimal getCompensation() {
        return compensation;
    }
    public BigDecimal getAocpStandardPenalty() {
        return aocpStandardPenalty;
    }

    public static Offence.Builder offence() {
        return new Offence.Builder();
    }

    public static class Builder {
        private String id;
        private String plea;
        private String title;
        private String titleWelsh;
        private String legislation;
        private String legislationWelsh;
        private String wording;
        private String wordingWelsh;
        private Boolean endorsable;
        private Boolean imprisonable;
        private Boolean hasPlea;
        private Boolean onlinePleaReceived;
        private BigDecimal compensation;
        private BigDecimal aocpStandardPenalty;

        public Offence.Builder withId(final String id) {
            this.id = id;
            return this;
        }

        public Offence.Builder withPlea(final String plea) {
            this.plea = plea;
            return this;
        }

        public Offence.Builder withTitle(final String title) {
            this.title = title;
            return this;
        }

        public Offence.Builder withTitleWelsh(final String titleWelsh) {
            this.titleWelsh = titleWelsh;
            return this;
        }

        public Offence.Builder withLegislation(final String legislation) {
            this.legislation = legislation;
            return this;
        }

        public Offence.Builder withLegislationWelsh(final String legislationWelsh) {
            this.legislationWelsh = legislationWelsh;
            return this;
        }

        public Offence.Builder withWording(final String wording) {
            this.wording = wording;
            return this;
        }

        public Offence.Builder withWordingWelsh(final String wordingWelsh) {
            this.wordingWelsh = wordingWelsh;
            return this;
        }

        public Offence.Builder withEndorsable(final Boolean endorsable) {
            this.endorsable = endorsable;
            return this;
        }

        public Offence.Builder withImprisonable(final Boolean imprisonable) {
            this.imprisonable = imprisonable;
            return this;
        }

        public Offence.Builder withHasPlea(final Boolean hasPlea) {
            this.hasPlea = hasPlea;
            return this;
        }

        public Offence.Builder withOnlinePleaReceived(final Boolean onlinePleaReceived) {
            this.onlinePleaReceived = onlinePleaReceived;
            return this;
        }

        public Offence.Builder withCompensation(final BigDecimal compensation) {
            this.compensation = compensation;
            return this;
        }

        public Offence.Builder withAocpStandardPenalty(final BigDecimal aocpStandardPenalty) {
            this.aocpStandardPenalty = aocpStandardPenalty;
            return this;
        }

        public Offence build() {
            return new Offence(id, plea, title, titleWelsh, legislation, legislationWelsh, wording,
                    wordingWelsh, endorsable, imprisonable, hasPlea, onlinePleaReceived, compensation, aocpStandardPenalty);
        }
    }
}
