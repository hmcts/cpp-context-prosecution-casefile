package uk.gov.moj.cpp.prosecutioncasefile.persistence.util;

import static java.time.LocalDate.now;
import static java.util.Collections.singleton;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Gender;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Language;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.AddressDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.ContactDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.OffenceDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.PersonalInformationDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.SelfDefinedInformationDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class TestUtils {
    public static final UUID CASE_ID = randomUUID();
    public static final UUID FIRST_DEFENDANT_CASE_ID = fromString("cf65ac42-c3e8-4dc8-8521-32b7f0b85b00");
    public static final UUID SECOND_DEFENDANT_CASE_ID = fromString("bd05d47e-c993-412a-b08c-2955ea59809d");
    public static final UUID VALUE_DEFENDANT_ID = randomUUID();
    public static final String VALUE_OFFENCE_ID = randomUUID().toString();

    public static final UUID DEFENDANT_ID = randomUUID(); //"1d-1n-t3st"; // NON UUID
    public static final UUID OFFENCE_ID = randomUUID();
    public static final String CASE_URN = "CASEURN";
    public static final String ASN = "001";
    public static final Language DOCUMENTATION_LANGUAGE = Language.E;
    public static final Language HEARING_LANGUAGE = Language.W;
    public static final String LANGUAGE_REQUIREMENTS = "no special language needs";
    public static final String SPECIFIC_REQUIREMENTS = "no special requirements needed";
    public static final Integer NUM_PREVIOUS_CONVICTIONS = 3;
    public static final LocalDate POSTING_DATE = now();
    public static final String DRIVER_NUMBER = "AS";
    public static final String NATIONAL_INSURANCE_NUMBER = "AA123456C";
    public static final String PROSECUTOR_DEFENDANT_REFERENCE = "DEFENDANT-REF-01";

    public static final BigDecimal APPLIED_PROSECUTOR_COSTS = new BigDecimal(100);
    public static final String PROSECUTOR_AUTHORITY = "TFL";
    public static final String PROSECUTOR_CASE_REFERENCE = "TFL75947ZQ8UE";
    public static final String PROSECUTOR_CASE_REFERENCE2 = "TFL95000ZQ8UE";
    public static final String INVALID_PROSECUTOR_CASE_REFERENCE = "TFL0000000000";
    public static final String PROSECUTOR_INFORMANT = "John";
    public static final String ORIGINATING_ORGANISATION = "GAAAA01";

    public static final String FIRST_NAME = "John";
    public static final String LAST_NAME = "Doe";
    public static final String ADDRESS_1 = "Flat 8, Lant House";
    public static final String ADDRESS_2 = "Lant Street";
    public static final String ADDRESS_3 = "London";
    public static final String ADDRESS_4 = "Greater London";
    public static final String ADDRESS_5 = "UK";
    public static final BigDecimal APPLIED_COMPENSATION = new BigDecimal(200);
    public static final BigDecimal BACK_DUTY = new BigDecimal(340);
    public static final LocalDate BACK_DUTY_FROM = LocalDate.of(2002, 4, 1);
    public static final LocalDate BACK_DUTY_TO = LocalDate.of(2012, 4, 1);
    public static final LocalDate CHARGE_DATE = LocalDate.of(2017, 6, 25);
    public static final String HOME = "20 7234 3456";
    public static final String MOBILE = "07911 123456";
    public static final String OCCUPATION = "TA";
    public static final Integer OCCUPATION_CODE = 666;
    public static final String NATIONALITY = "Slovak";
    public static final String ADDITIONAL_NATIONALITY = "English";
    public static final Gender GENDER = Gender.MALE;
    public static final LocalDate DATE_OF_BIRTH = LocalDate.of(1989, 4, 18);
    public static final String ETHNICITY = "white";

    public static final String EMAIL = "john.doe@hmcts.com";
    public static final String SECONDARY_EMAIL = "john.doe@kainos.com";
    public static final String TITLE = "Mr";
    public static final String WORK = "Code Writer";

    public static final String OFFENCE_CODE = "OF61131";
    public static final Integer OFFENCE_DATE_CODE = 1;
    public static final LocalDate OFFENCE_COMMITTED_DATE = LocalDate.of(2017, 2, 2);
    public static final LocalDate OFFENCE_COMMITTED_END_DATE = LocalDate.of(2017, 4, 2);
    public static final String OFFENCE_LOCATION = "Croydon";
    public static final Integer OFFENCE_SEQUENCE_NUMBER = 2;
    public static final String OFFENCE_WORDING = "Forgot to pay the TV license again :'(";
    public static final String OFFENCE_WORDING_WELSH = "Forgot to pay the TV license again :'( --WELSH";
    public static final String STATEMENT_OF_FACT = "John Doe missed a TV license payment for the 2nd time";
    public static final String STATEMENT_OF_FACT_WELSH = "John Doe missed a TV license payment for the 2nd time --WELSH";
    public static final String VEHICLE_MAKE = "Ford";
    public static final String VEHICLE_REGISTRATION_MARK = "AA11 ABC";


    public static CaseDetails createFirstDefendantCaseDetails() {
        final CaseDetails caseDetails = new CaseDetails();
        setCaseDetails(caseDetails, FIRST_DEFENDANT_CASE_ID, PROSECUTOR_CASE_REFERENCE);
        caseDetails.setDefendants(singleton(createFirstDefendant()));
        return caseDetails;
    }
    public static CaseDetails createSecondDefendantCaseDetails() {
        final CaseDetails caseDetails = new CaseDetails();
        setCaseDetails(caseDetails, SECOND_DEFENDANT_CASE_ID, PROSECUTOR_CASE_REFERENCE2);
        caseDetails.setDefendants(singleton(createSecondDefendant()));
        return caseDetails;
    }

    private static void setCaseDetails(CaseDetails caseDetails, UUID caseId, String prosecutorCaseReference2) {
        caseDetails.setCaseId(caseId);
        caseDetails.setProsecutionAuthority(PROSECUTOR_AUTHORITY);
        caseDetails.setProsecutionCaseReference(prosecutorCaseReference2);
        caseDetails.setProsecutorInformant(PROSECUTOR_INFORMANT);
        caseDetails.setOriginatingOrganisation(ORIGINATING_ORGANISATION);
    }

    public static DefendantDetails createFirstDefendant() {
        final DefendantDetails defendant = new DefendantDetails();
        setDefendant(defendant, DEFENDANT_ID);
        defendant.setPersonalInformation(createFirstDefendantPersonalInformation());
        defendant.setSelfDefinedInformation(createFirstDefendantSelfDefinedInformation());
        defendant.setOffences(singleton(createFirstDefendantOffence()));
        return defendant;
    }

    public static DefendantDetails createSecondDefendant() {
        final DefendantDetails defendant = new DefendantDetails();
        setDefendant(defendant, randomUUID());
        defendant.setPersonalInformation(createSecondDefendantPersonalInformation());
        defendant.setSelfDefinedInformation(createSecondDefendantSelfDefinedInformation());
        defendant.setOffences(singleton(createSecondDefendantOffence()));
        return defendant;
    }

    private static void setDefendant(DefendantDetails defendant, UUID randomUUID) {
        defendant.setDefendantId(randomUUID.toString());
        defendant.setAsn(ASN);
        defendant.setDocumentationLanguage(DOCUMENTATION_LANGUAGE);
        defendant.setAppliedProsecutorCosts(APPLIED_PROSECUTOR_COSTS);
        defendant.setHearingLanguage(HEARING_LANGUAGE);
        defendant.setLanguageRequirement(LANGUAGE_REQUIREMENTS);
        defendant.setSpecificRequirements(SPECIFIC_REQUIREMENTS);
        defendant.setNumPreviousConvictions(NUM_PREVIOUS_CONVICTIONS);
        defendant.setPostingDate(POSTING_DATE);
        defendant.setDriverNumber(DRIVER_NUMBER);
        defendant.setNationalInsuranceNumber(NATIONAL_INSURANCE_NUMBER);
        defendant.setProsecutorDefendantReference(PROSECUTOR_DEFENDANT_REFERENCE);
    }

    public static SelfDefinedInformationDetails createFirstDefendantSelfDefinedInformation() {
        final SelfDefinedInformationDetails selfDefinedInformation = new SelfDefinedInformationDetails();
        setSelfDefinedInformation(selfDefinedInformation, DEFENDANT_ID);

        return selfDefinedInformation;
    }

    public static SelfDefinedInformationDetails createSecondDefendantSelfDefinedInformation() {
        final SelfDefinedInformationDetails selfDefinedInformation = new SelfDefinedInformationDetails();
        setSelfDefinedInformation(selfDefinedInformation, randomUUID());
        return selfDefinedInformation;
    }
    private static void setSelfDefinedInformation(SelfDefinedInformationDetails selfDefinedInformation, UUID defendantId) {
        selfDefinedInformation.setSelfDefinedInformationId(defendantId.toString());
        selfDefinedInformation.setDateOfBirth(DATE_OF_BIRTH);
        selfDefinedInformation.setEthnicity(ETHNICITY);
        selfDefinedInformation.setGender(GENDER);
        selfDefinedInformation.setNationality(NATIONALITY);
        selfDefinedInformation.setAdditionalNationality(ADDITIONAL_NATIONALITY);
    }

    public static AddressDetails createAddress() {
        final AddressDetails addressDetails = new AddressDetails();
        addressDetails.setAddress1(ADDRESS_1);
        addressDetails.setAddress2(ADDRESS_2);
        addressDetails.setAddress3(ADDRESS_3);
        addressDetails.setAddress4(ADDRESS_4);
        addressDetails.setAddress5(ADDRESS_5);
        return addressDetails;
    }

    public static ContactDetails createContactDetails() {
        final ContactDetails contactDetails = new ContactDetails();
        contactDetails.setHome(HOME);
        contactDetails.setMobile(MOBILE);
        contactDetails.setPrimaryEmail(EMAIL);
        contactDetails.setSecondaryEmail(SECONDARY_EMAIL);
        contactDetails.setWork(WORK);

        return contactDetails;
    }

    public static PersonalInformationDetails createFirstDefendantPersonalInformation() {
        final PersonalInformationDetails personalInformation = new PersonalInformationDetails();
        setPersonalInformation(personalInformation, DEFENDANT_ID);

        return personalInformation;
    }

    public static PersonalInformationDetails createSecondDefendantPersonalInformation() {
        final PersonalInformationDetails personalInformation = new PersonalInformationDetails();
        setPersonalInformation(personalInformation, randomUUID());

        return personalInformation;
    }

    private static void setPersonalInformation(PersonalInformationDetails personalInformation, UUID randomUUID) {
        personalInformation.setPersonalInformationId(randomUUID.toString());
        personalInformation.setTitle(TITLE);
        personalInformation.setFirstName(FIRST_NAME);
        personalInformation.setLastName(LAST_NAME);
        personalInformation.setOccupation(OCCUPATION);
        personalInformation.setOccupationCode(OCCUPATION_CODE);
        personalInformation.setContactDetails(createContactDetails());
        personalInformation.setAddress(createAddress());
    }

    public static OffenceDetails createFirstDefendantOffence() {
        final OffenceDetails offence = new OffenceDetails();
        setOffence(offence, OFFENCE_ID);

        return offence;
    }

    private static void setOffence(OffenceDetails offence, UUID offenceId) {
        offence.setOffenceId(offenceId);
        offence.setAppliedCompensation(APPLIED_COMPENSATION);
        offence.setBackDuty(BACK_DUTY);
        offence.setBackDutyDateFrom(BACK_DUTY_FROM);
        offence.setBackDutyDateTo(BACK_DUTY_TO);
        offence.setChargeDate(CHARGE_DATE);
        offence.setOffenceCode(OFFENCE_CODE);
        offence.setOffenceCommittedDate(OFFENCE_COMMITTED_DATE);
        offence.setOffenceCommittedEndDate(OFFENCE_COMMITTED_END_DATE);
        offence.setOffenceLocation(OFFENCE_LOCATION);
        offence.setOffenceSequenceNumber(OFFENCE_SEQUENCE_NUMBER);
        offence.setOffenceWording(OFFENCE_WORDING);
        offence.setOffenceWordingWelsh(OFFENCE_WORDING_WELSH);
        offence.setStatementOfFacts(STATEMENT_OF_FACT);
        offence.setStatementOfFactsWelsh(STATEMENT_OF_FACT_WELSH);
        offence.setOffenceDateCode(OFFENCE_DATE_CODE);
        offence.setVehicleMake(VEHICLE_MAKE);
        offence.setVehicleRegistrationMark(VEHICLE_REGISTRATION_MARK);
    }

    public static OffenceDetails createSecondDefendantOffence() {
        final OffenceDetails offence = new OffenceDetails();
        setOffence(offence, randomUUID());

        return offence;
    }

    public static BusinessValidationErrorDetails createDefendantLevelError() {
        final BusinessValidationErrorDetails defendantLevelError = new BusinessValidationErrorDetails();
        defendantLevelError.setCaseId(FIRST_DEFENDANT_CASE_ID);
        defendantLevelError.setCaseType("S");
        defendantLevelError.setCourtName("CourtName");
        defendantLevelError.setUrn(CASE_URN);
        defendantLevelError.setDefendantBailStatus("DefendantStatus");
        defendantLevelError.setDefendantChargeDate(LocalDate.now());
        defendantLevelError.setDefendantHearingDate(LocalDate.now());
        defendantLevelError.setDefendantId(VALUE_DEFENDANT_ID);
        defendantLevelError.setFieldName("DefendantFieldName");
        defendantLevelError.setDisplayName("DefendantDisplayFieldName");
        defendantLevelError.setErrorValue("Error Value");
        defendantLevelError.setFirstName(FIRST_NAME);
        defendantLevelError.setLastName(LAST_NAME);
        return defendantLevelError;
    }

    public static BusinessValidationErrorDetails createCaseLevelError() {
        final BusinessValidationErrorDetails caselevelError = new BusinessValidationErrorDetails();
        caselevelError.setCaseId(FIRST_DEFENDANT_CASE_ID);
        caselevelError.setCaseType("S");
        caselevelError.setCourtName("CourtName");
        caselevelError.setUrn(CASE_URN);
        caselevelError.setFieldName("CaseFieldName");
        caselevelError.setDisplayName("DisplayCaseFieldName");
        caselevelError.setErrorValue("Case Error Value");
        caselevelError.setId(randomUUID());
        return caselevelError;
    }

    public static BusinessValidationErrorDetails createOffenceLevelError() {
        final BusinessValidationErrorDetails offenceLevelError = new BusinessValidationErrorDetails();
        offenceLevelError.setCaseId(FIRST_DEFENDANT_CASE_ID);
        offenceLevelError.setCaseType("S");
        offenceLevelError.setCourtName("CourtName");
        offenceLevelError.setUrn(CASE_URN);
        offenceLevelError.setDefendantBailStatus("DefendantStatus");
        offenceLevelError.setDefendantChargeDate(LocalDate.now());
        offenceLevelError.setDefendantHearingDate(LocalDate.now());
        offenceLevelError.setDefendantId(VALUE_DEFENDANT_ID);
        offenceLevelError.setFieldName("OffenceFieldName");
        offenceLevelError.setFieldId(VALUE_OFFENCE_ID);
        offenceLevelError.setDisplayName("DisplayOffenceFieldName");
        offenceLevelError.setErrorValue("Offence Error Value");
        return offenceLevelError;
    }
}
