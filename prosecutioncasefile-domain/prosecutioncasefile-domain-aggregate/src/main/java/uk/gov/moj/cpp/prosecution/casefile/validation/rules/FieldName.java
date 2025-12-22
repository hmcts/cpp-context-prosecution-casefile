package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

public enum FieldName {
    CASE_SUMMONS_CODE("summonsCode"),
    CASE_INITIATION_CODE("initiationCode"),
    CPS_ORGANISATION("cpsOrganisation"),
    DOCUMENT_TYPE("documentType"),
    PROSECUTOR_DEFENDANT_ID("prosecutorDefendantId"),
    DEFENDANT_NATIONALITY("individual_selfDefinedInformation_nationality"),
    DEFENDANT_PRIMARY_EMAIL_ADDRESS("individual_personalInformation_contactDetails_primaryEmail"),
    DEFENDANT_SECONDARY_EMAIL_ADDRESS("individual_personalInformation_contactDetails_secondaryEmail"),
    CORPORATE_DEFENDANT_PRIMARY_EMAIL_ADDRESS("emailAddress1"),
    CORPORATE_DEFENDANT_SECONDARY_EMAIL_ADDRESS("emailAddress2"),
    DEFENDANT_ADDITIONAL_NATIONALITY("individual_selfDefinedInformation_additional_nationality"),
    DEFENDANT_DOB("individual_selfDefinedInformation_dateOfBirth"),
    DEFENDANT_INITIATION_CODE("defendantInitiationCode"),
    DEFENDANT_PERCEIVED_BIRTH_YEAR("individual_perceivedBirthYear"),
    DEFENDANT_OFFENDER_CODE("individual_offenderCode"),
    DEFENDANT_BAIL_CONDITIONS("individual_bailConditions"),
    DEFENDANT_CUSTODY_STATUS("custodyStatus"),
    DEFENDANT_OBSERVED_ETHNICITY("individual_personalInformation_observedEthnicity"),
    DEFENDANT_SELF_DEFINED_ETHNICITY("individual_selfDefinedInformation_ethnicity"),
    CASE_COURT_HEARING_LOCATION("initialHearing_courtHearingLocation"),
    DEFENDANT_DATE_OF_HEARING("initialHearing_dateOfHearing"),
    DEFENDANT_COURT_HEARING_LOCATION("initialHearing_courtHearingLocation"),
    OFFENCE_VEHICLE_CODE("offence_vehicleRelatedOffence_vehicleCode"),
    OFFENCE_ARREST_DATE("offence_arrestDate"),
    OFFENCE_CHARGE_DATE("offence_chargeDate"),
    OFFENCE_LAID_DATE("offence_laidDate"),
    OFFENCE_STATEMENT_OF_FACTS("offence_statementOfFacts"),
    OFFENCE_STATEMENT_OF_FACTS_WELSH("offence_statementOfFactsWelsh"),
    PARENT_GUARDIAN_SELF_DEFINED_ETHNICITY("individual_parentGuardianInformation_selfDefinedEthnicity"),
    PARENT_GUARDIAN_OBSERVED_ETHNICITY("individual_parentGuardianInformation_observedEthnicity"),
    PARENT_GUARDIAN_DATE_OF_BIRTH("individual_parentGuardianInformation_dateOfBirth"),
    PARENT_GUARDIAN_PRIMARY_EMAIL_ADDRESS("individual_parentGuardianInformation_personalInformation_contactDetails_primaryEmail"),
    PARENT_GUARDIAN_SECONDARY_EMAIL_ADDRESS("individual_parentGuardianInformation_personalInformation_contactDetails_secondaryEmail"),
    OFFENCE_ALCOHOL_LEVEL_METHOD("offence_alcoholRelatedOffence_alcoholLevelMethod"),
    OFFENCE_ALCOHOL_LEVEL_AMOUNT("offence_alcoholRelatedOffence_alcoholLevelAmount"),
    OFFENCE_CODE("offence_offenceCode"),
    OFFENCE_SEQUENCE_NO("offence_offenceSequenceNo"),
    OFFENCE_LOCATION("offence_offenceLocation"),
    OFFENCE_DATE_CODE("offence_offenceDateCode"),
    STATEMENT_OF_FACTS_WELSH("statementOfFactsWelsh"),
    CASE_MARKERS("caseMarkers"),
    OU_CODE("ouCode"),
    COURT_RECEIVED_TO("courtReceivedToCode"),
    COURT_RECEIVED_FROM("courtReceivedFromCode"),
    POLICE_FORCE_CODE("policeForceCode"),
    HEARING_TYPE_CODE("hearingTypeCode"),
    PROSECUTING_AUTHORITY("prosecutingAuthority") ,
    BACKDUTY_VALUE("offence_backduty_value"),
    BACKDUTY_FROMDATE("offence_backduty_fromDate"),
    BACKDUTY_TODATE("offence_backduty_toDate"),
    BACKDUTY_FROMDATE_TODATE("offence_backduty_fromDate_toDate"),
    PNC_ID("pncIdentifier"),
    DEFENDANT_POST_CODE("address_postcode"),
    PARENT_GUARDIAN_POST_CODE("individual_parentGuardianInformation_personalInformation_address_postcode"),
    DEFENDANT_INDIVIDUAL_POST_CODE("individual_personalInformation_address_postcode"),
    CRO_NUMBER("croNumber"),
    TITLE("title"),
    FORENAME("forename"),
    FORENAME2("forename2"),
    FORENAME3("forename3"),
    SURNAME("surename"),
    DATE_OF_BIRTH("dataOfBirth"),
    CPS_DEFENDANT_ID("cpsDefendantId"),
    ASN("asn"),
    COURT_APPLICATION_ID("courtApplicationId"),
    ORGANISATION_NAME("organisationName"),
    LIST_DEFENDANT_REQUESTS("listDefendantRequests");




    private final String value;

    FieldName(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}