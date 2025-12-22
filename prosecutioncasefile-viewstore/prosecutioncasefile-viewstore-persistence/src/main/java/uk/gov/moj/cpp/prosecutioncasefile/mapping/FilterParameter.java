package uk.gov.moj.cpp.prosecutioncasefile.mapping;

public class FilterParameter {
    private final String court;
    private final String caseType;
    private final String urn;
    private final String hearingDateFrom;
    private final String hearingDateTo;

    public FilterParameter(final String court, final String caseType, final String urn, final String hearingDateFrom, final String hearingDateTo) {
        this.court = court;
        this.caseType = caseType;
        this.urn = urn;
        this.hearingDateFrom = hearingDateFrom;
        this.hearingDateTo = hearingDateTo;
    }

    public String getCourt() {
        return court;
    }

    public String getCaseType() {
        return caseType;
    }

    public String getUrn() {
        return urn;
    }

    public String getHearingDateFrom() {
        return hearingDateFrom;
    }

    public String getHearingDateTo() {
        return hearingDateTo;
    }

    public static FilterParameterBuilder filterParameterBuilder() {
        return new FilterParameterBuilder();
    }

    public static final class FilterParameterBuilder {
        private String court;
        private String caseType;
        private String urn;
        private String hearingDateFrom;
        private String hearingDateTo;

        public FilterParameterBuilder withCourt(String court) {
            this.court = court;
            return this;
        }

        public FilterParameterBuilder withCaseType(String caseType) {
            this.caseType = caseType;
            return this;
        }

        public FilterParameterBuilder withUrn(String urn) {
            this.urn = urn;
            return this;
        }

        public FilterParameterBuilder withHearingDateFrom(String hearingDateFrom) {
            this.hearingDateFrom = hearingDateFrom;
            return this;
        }

        public FilterParameterBuilder withHearingDateTo(String hearingDateTo) {
            this.hearingDateTo = hearingDateTo;
            return this;
        }

        public FilterParameter build() {
            return new FilterParameter(court, caseType, urn, hearingDateFrom, hearingDateTo);
        }
    }
}
