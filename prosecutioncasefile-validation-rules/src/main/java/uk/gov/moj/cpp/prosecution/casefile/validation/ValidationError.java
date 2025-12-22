package uk.gov.moj.cpp.prosecution.casefile.validation;

public enum ValidationError {
    APPLICATION_TYPE_NOT_FOUND("APPLICATION_TYPE_NOT_FOUND", "Application type is invalid or unknown."),
    COURT_PAYMENT_NOT_FOUND("COURT_PAYMENT_NOT_FOUND", "Court payment details not found."),
    COURT_LOCATION_REQUIRED("COURT_LOCATION_REQUIRED", "Court location is invalid or unknown."),
    CASE_NOT_FOUND("CASE_NOT_FOUND", "Case URN is invalid or unknown."),
    APPLICATION_DUE_DATE_INVALID("APPLICATION_DUE_DATE_INVALID", "Application due date is invalid."),
    THIRD_PARTY_DETAILS_REQUIRED("THIRD_PARTY_DETAILS_REQUIRED", "Full Name and/or Address of the Third Party have not been provided."),
    RESPONDENT_DETAILS_REQUIRED("RESPONDENT_DETAILS_REQUIRED", "Full Name and/or Address of the Respondent have not been provided."),
    DEFENDANT_ASN_NOT_FOUND("DEFENDANT_ASN_NOT_FOUND", "Defendant ASN is invalid or not found"),
    SUBJECT_INVALID("SUBJECT_INVALID", "Only one party should be marked as Subject, or Subject is not a defendant."),
    SUBJECT_REQUIRED("SUBJECT_REQUIRED", "Subject required."),
    DEFENDANT_DETAILS_NOT_FOUND("DEFENDANT_DETAILS_NOT_FOUND", "Defendant details not found."),
    ASN_NOT_MATCHED("ASN_NOT_MATCHED", "Asn not matched."),
    DEFENDANT_ID_NOT_MATCHED("DEFENDANT_ID_NOT_MATCHED", "Defendant id not matched."),
    NAME_NOT_MATCHED("NAME_NOT_MATCHED", "Name not matched."),
    INVALID_DEFENDANTS_PROVIDED("INVALID_DEFENDANTS_PROVIDED", "INVALID_DEFENDANTS_PROVIDED"),
    INVALID_OFFENCES_PROVIDED("INVALID_OFFENCES_PROVIDED", "INVALID_OFFENCES_PROVIDED"),
    CASE_URN_NOT_FOUND("CASE_URN_NOT_FOUND", "CASE_URN_NOT_FOUND");

    private final String code;
    private final String text;

    ValidationError(final String code, final String text) {
        this.code = code;
        this.text = text;
    }

    public String getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

}
