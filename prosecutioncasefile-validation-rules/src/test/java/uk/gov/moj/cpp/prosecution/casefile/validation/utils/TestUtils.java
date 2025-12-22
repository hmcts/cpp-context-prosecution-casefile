package uk.gov.moj.cpp.prosecution.casefile.validation.utils;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplicationCase.courtApplicationCase;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplicationCase;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplicationType;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Person;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Respondent;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.ThirdParty;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Gender;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Language;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.AddressDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorSummary;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.ContactDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.OffenceDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.PersonalInformationDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.SelfDefinedInformationDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class TestUtils {

    public static final UUID CASE_ID = randomUUID();
    public static final UUID CASE_ID_2 = randomUUID();
    public static final UUID VALUE_DEFENDANT_ID = randomUUID();
    public static final UUID VALUE_DEFENDANT_ID_2 = randomUUID();
    public static final UUID VALUE_DEFENDANT_ID_3 = randomUUID();
    public static final String VALUE_OFFENCE_ID = randomUUID().toString();

    public static final UUID DEFENDANT_ID = randomUUID(); // NON UUID
    public static final UUID OFFENCE_ID = randomUUID();
    public static final String CASE_URN = "CASEURN";
    public static final String CASE_URN_2 = "ZQ8UE";
    public static final String COURT_NAME = "CourtName";
    public static final String CASE_TYPE = "S";
    public static final String BAIL_STATUS_CUSTODY = "C";
    public static final String BAIL_STATUS_REMAND = "L";
    public static final String ASN = "001";
    public static final Language DOCUMENTATION_LANGUAGE = Language.E;
    public static final Language HEARING_LANGUAGE = Language.W;
    public static final String LANGUAGE_REQUIREMENTS = "no special language needs";
    public static final String SPECIFIC_REQUIREMENTS = "no special requirements needed";
    public static final Integer NUM_PREVIOUS_CONVICTIONS = 3;
    public static final LocalDate POSTING_DATE = now();
    public static final String DRIVER_NUMBER = "AS";
    public static final String NATIONAL_INSURANCE_NUMBER = "AA123456C";

    public static final BigDecimal APPLIED_PROSECUTOR_COSTS = new BigDecimal(100);
    public static final String PROSECUTOR_AUTHORITY = "TFL";
    public static final String PROSECUTOR_CASE_REFERENCE1 = "TFL75000ZQ8UE";
    public static final String PROSECUTOR_CASE_REFERENCE2 = "TFL85000ZQ8UE";
    public static final String PROSECUTOR_CASE_REFERENCE3 = "TFL95000ZQ8UE";
    public static final String PROSECUTOR_CASE_REFERENCE_NOT_FOUND = "";
    public static final String PROSECUTOR_INFORMANT = "John";

    public static final String FIRST_NAME = "John";
    public static final String ORGANISATION_NAME = "ABC";
    public static final String LAST_NAME = "Doe";
    public static final String ADDRESS_1 = "Flat 8, Lant House";
    public static final String ADDRESS_2 = "Lant Street";
    public static final String ADDRESS_3 = "London";
    public static final String ADDRESS_4 = "Greate London";
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
    public static final LocalDate OFFENCE_COMMITTED_DATE = LocalDate.of(2017, 2, 2);
    public static final LocalDate OFFENCE_COMMITTED_END_DATE = LocalDate.of(2017, 4, 2);
    public static final String OFFENCE_LOCATION = "Croydon";
    public static final Integer OFFENCE_SEQUENCE_NUMBER = 2;
    public static final String OFFENCE_WORDING = "Forgot to pay the TV license again :'(";
    public static final String OFFENCE_WORDING_WELSH = "Forgot to pay the TV license again :'( --WELSH";
    public static final String STATEMENT_OF_FACT = "John Doe missed a TV license payment for the 2nd time";
    public static final String STATEMENT_OF_FACT_WELSH = "John Doe missed a TV license payment for the 2nd time --WELSH";
    public static final String POST_CODE = "ABC14DEF";


    public static CaseDetails createCaseDetails1() {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseId(CASE_ID);
        caseDetails.setProsecutionAuthority(PROSECUTOR_AUTHORITY);
        caseDetails.setProsecutionCaseReference(PROSECUTOR_CASE_REFERENCE1);
        caseDetails.setProsecutorInformant(PROSECUTOR_INFORMANT);
        caseDetails.setDefendants(singleton(createDefendant()));
        return caseDetails;
    }

    public static CaseDetails createCaseDetails2() {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseId(CASE_ID);
        caseDetails.setProsecutionAuthority(PROSECUTOR_AUTHORITY);
        caseDetails.setProsecutionCaseReference(PROSECUTOR_CASE_REFERENCE2);
        caseDetails.setProsecutorInformant(PROSECUTOR_INFORMANT);
        caseDetails.setDefendants(singleton(createDefendant()));
        return caseDetails;
    }

    public static CaseDetails createCaseDetails3() {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseId(CASE_ID);
        caseDetails.setProsecutionAuthority(PROSECUTOR_AUTHORITY);
        caseDetails.setProsecutionCaseReference(PROSECUTOR_CASE_REFERENCE3);
        caseDetails.setProsecutorInformant(PROSECUTOR_INFORMANT);
        caseDetails.setDefendants(singleton(createDefendant()));
        return caseDetails;
    }

    public static CaseDetails createCaseDetailsNotFound() {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseId(CASE_ID);
        caseDetails.setProsecutionAuthority(PROSECUTOR_AUTHORITY);
        caseDetails.setProsecutionCaseReference(PROSECUTOR_CASE_REFERENCE_NOT_FOUND);
        caseDetails.setProsecutorInformant(PROSECUTOR_INFORMANT);
        caseDetails.setDefendants(singleton(createDefendant()));
        return caseDetails;
    }

    public static DefendantDetails createDefendantWithDefendantIdUUID() {
        final DefendantDetails defendant = new DefendantDetails();
        defendant.setDefendantId(VALUE_DEFENDANT_ID.toString());
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
        defendant.setPersonalInformation(createPersonalInformation());
        defendant.setSelfDefinedInformation(createSelfDefinedInformation());
        defendant.setOffences(singleton(createOffence()));
        return defendant;
    }

    public static DefendantDetails createDefendant() {
        final DefendantDetails defendant = new DefendantDetails();
        defendant.setDefendantId(DEFENDANT_ID.toString());
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
        defendant.setPersonalInformation(createPersonalInformation());
        defendant.setSelfDefinedInformation(createSelfDefinedInformation());
        defendant.setOffences(singleton(createOffence()));
        return defendant;
    }

    private static SelfDefinedInformationDetails createSelfDefinedInformation() {
        final SelfDefinedInformationDetails selfDefinedInformation = new SelfDefinedInformationDetails();
        selfDefinedInformation.setSelfDefinedInformationId(DEFENDANT_ID.toString());
        selfDefinedInformation.setDateOfBirth(DATE_OF_BIRTH);
        selfDefinedInformation.setEthnicity(ETHNICITY);
        selfDefinedInformation.setGender(GENDER);
        selfDefinedInformation.setNationality(NATIONALITY);
        selfDefinedInformation.setAdditionalNationality(ADDITIONAL_NATIONALITY);

        return selfDefinedInformation;
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

    public static PersonalInformationDetails createPersonalInformation() {
        final PersonalInformationDetails personalInformation = new PersonalInformationDetails();
        personalInformation.setPersonalInformationId(DEFENDANT_ID.toString());
        personalInformation.setTitle(TITLE);
        personalInformation.setFirstName(FIRST_NAME);
        personalInformation.setLastName(LAST_NAME);
        personalInformation.setOccupation(OCCUPATION);
        personalInformation.setOccupationCode(OCCUPATION_CODE);
        personalInformation.setContactDetails(createContactDetails());
        personalInformation.setAddress(createAddress());

        return personalInformation;
    }

    public static OffenceDetails createOffence() {
        final OffenceDetails offence = new OffenceDetails();
        offence.setOffenceId(OFFENCE_ID);
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

        return offence;
    }

    public static BusinessValidationErrorDetails createDefendantLevelError() {
        final BusinessValidationErrorDetails defendantLevelError = new BusinessValidationErrorDetails();
        defendantLevelError.setCaseId(CASE_ID);
        defendantLevelError.setCaseType(CASE_TYPE);
        defendantLevelError.setCourtName(COURT_NAME);
        defendantLevelError.setUrn(CASE_URN);
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
        caselevelError.setCaseId(CASE_ID);
        caselevelError.setCaseType(CASE_TYPE);
        caselevelError.setCourtName(COURT_NAME);
        caselevelError.setUrn(CASE_URN);
        caselevelError.setFieldName("CaseFieldName");
        caselevelError.setDisplayName("DisplayCaseFieldName");
        caselevelError.setErrorValue("Case Error Value");
        return caselevelError;
    }

    public static BusinessValidationErrorDetails createOffenceLevelError() {
        final BusinessValidationErrorDetails offenceLevelError = new BusinessValidationErrorDetails();
        offenceLevelError.setCaseId(CASE_ID);
        offenceLevelError.setCaseType(CASE_TYPE);
        offenceLevelError.setCourtName(COURT_NAME);
        offenceLevelError.setUrn(CASE_URN);
        offenceLevelError.setDefendantChargeDate(LocalDate.now());
        offenceLevelError.setDefendantHearingDate(LocalDate.now());
        offenceLevelError.setDefendantId(VALUE_DEFENDANT_ID);
        offenceLevelError.setFieldName("OffenceFieldName");
        offenceLevelError.setFieldId(VALUE_OFFENCE_ID);
        offenceLevelError.setDisplayName("DisplayOffenceFieldName");
        offenceLevelError.setErrorValue("Offence Error Value");
        return offenceLevelError;
    }

    public static BusinessValidationErrorSummary newBusinessValidationErrorSummary(final UUID caseId) {
        final BusinessValidationErrorSummary businessValidationErrorSummary = new BusinessValidationErrorSummary();
        businessValidationErrorSummary.setCaseId(caseId);
        businessValidationErrorSummary.setCourtLocation(COURT_NAME);
        businessValidationErrorSummary.setDefendantHearingDate(now());
        businessValidationErrorSummary.setUrn(CASE_URN);
        businessValidationErrorSummary.setDefendantBailStatus(BAIL_STATUS_CUSTODY);
        businessValidationErrorSummary.setCaseType(CASE_TYPE);
        return businessValidationErrorSummary;
    }

    public static List<CourtApplicationCase> validCourtApplicationCases() {
        return asList(
                courtApplicationCase().withCaseURN("TFL75000ZQ8UE").build(),
                courtApplicationCase().withCaseURN("TFL85000ZQ8UE").build(),
                courtApplicationCase().withCaseURN("TFL95000ZQ8UE").build());
    }

    public static List<CourtApplicationCase> validSingleCourtApplicationCase() {
        return singletonList(courtApplicationCase().withCaseURN("TFL95000ZQ8UE").build());
    }

    public static CourtApplicationType validCourtApplicationType(final String code) {
        return CourtApplicationType.courtApplicationType().withCode(code).build();
    }

    public static CourtApplicationType invalidCourtApplicationType() {
        return CourtApplicationType.courtApplicationType().withCode(null).build();
    }

    public static Address buildAddress() {
        return Address.address()
                .withAddress1(ADDRESS_1)
                .withAddress2(ADDRESS_2)
                .withAddress3(ADDRESS_3)
                .withPostcode(POST_CODE)
                .build();
    }

    public static Person buildPerson(final String name, final Address address) {
        final Person.Builder builder = new Person.Builder()
                .withOccupation(OCCUPATION)
                .withOccupationCode(OCCUPATION_CODE.toString())
                .withAddress(address);
        ofNullable(name).ifPresent(builder::withFirstName);
        ofNullable(name).ifPresent(builder::withMiddleName);
        ofNullable(name).ifPresent(builder::withLastName);
        return builder.build();

    }

    public static uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Organisation buildOrganisation(final String organisationName, final Address address) {
        return uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Organisation.organisation()
                .withName(organisationName)
                .withAddress(address)
                .build();
    }

    // build third party
    public static ThirdParty buildThirdPartyPerson() {
        return ThirdParty.thirdParty()
                .withPersonDetails(buildPerson(FIRST_NAME, buildAddress()))
                .build();
    }

    public static ThirdParty buildThirdPartyOrganisation() {
        return ThirdParty.thirdParty()
                .withOrganisation(buildOrganisation(ORGANISATION_NAME, buildAddress()))
                .build();
    }

    public static ThirdParty buildThirdPartyPersonWithoutName() {
        return ThirdParty.thirdParty()
                .withPersonDetails(buildPerson(null, buildAddress()))
                .build();
    }

    public static ThirdParty buildThirdPartyPersonWithoutAddress() {
        return ThirdParty.thirdParty()
                .withPersonDetails(buildPerson(FIRST_NAME, null))
                .build();
    }

    public static ThirdParty buildThirdPartyPersonWithoutNameAndAddress() {
        return ThirdParty.thirdParty()
                .withPersonDetails(buildPerson(null, null))
                .build();
    }

    public static ThirdParty buildThirdPartyOrganisationWithoutNameAndAddress() {
        return ThirdParty.thirdParty()
                .withOrganisation(buildOrganisation("", null))
                .build();
    }

    // build respondent

    public static Respondent buildRespondentPerson() {
        return Respondent.respondent()
                .withPersonDetails(buildPerson(FIRST_NAME, buildAddress()))
                .build();
    }

    public static Respondent buildRespondentOrganisation() {
        return Respondent.respondent()
                .withOrganisation(buildOrganisation(ORGANISATION_NAME, buildAddress()))
                .build();
    }

    public static Respondent buildRespondentPersonWithoutName() {
        return Respondent.respondent()
                .withPersonDetails(buildPerson(null, buildAddress()))
                .build();
    }

    public static Respondent buildRespondentPersonWithoutAddress() {
        return Respondent.respondent()
                .withPersonDetails(buildPerson(FIRST_NAME, null))
                .build();
    }

    public static Respondent buildRespondentPersonWithoutNameAndAddress() {
        return Respondent.respondent()
                .withPersonDetails(buildPerson(null, null))
                .build();
    }

    public static Respondent buildRespondentOrganisationWithoutNameAndAddress() {
        return Respondent.respondent()
                .withOrganisation(buildOrganisation("", null))
                .build();
    }
}
