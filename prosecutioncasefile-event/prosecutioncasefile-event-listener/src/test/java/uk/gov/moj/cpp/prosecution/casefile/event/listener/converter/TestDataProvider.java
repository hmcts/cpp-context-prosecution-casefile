package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.AlcoholRelatedOffence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Gender;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Language;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ParentGuardianInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Plea;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.VehicleRelatedOffence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Verdict;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.VerdictType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class TestDataProvider {

    public static final String ADDRESS_LINE_1 = "Flat 8";
    public static final String ADDRESS_LINE_2 = "Lant House";
    public static final String ADDRESS_LINE_3 = "London";
    public static final String ADDRESS_LINE_4 = "Greater London";
    public static final String ADDRESS_LINE_5 = "United Kingdom";
    public static final String POSTCODE = "SE1 1PJ";

    public static final String TITLE = "Mr";
    public static final String FIRST_NAME = "John";
    public static final String LAST_NAME = "Doe";
    public static final String OCCUPATION = "Developer";
    public static final Integer OCCUPATION_CODE = 14;

    public static final String HOME = "754-3010";
    public static final String MOBILE = "07911 123456";
    public static final String WORK = "07911 123451";
    public static final String PRIMARY_EMAIL = "john.doe@hmcts.net";
    public static final String SECONDARY_EMAIL = "john.doe@kainos.com";

    public static final String NATIONALITY = "English";
    public static final String ADDITIONAL_NATIONALITY = "Welsh";
    public static final LocalDate DOB = LocalDate.of(1989, 4, 18);
    public static final String ETHNICITY = "white";
    public static final Gender GENDER = Gender.MALE;

    public static final UUID CASE_ID = randomUUID();
    public static final String CASE_REFERENCE = "TVL12345";
    public static final BigDecimal APPLIED_PROSECUTOR_COSTS = new BigDecimal(85);
    public static final String AUTHORITY = "GAEAA01";
    public static final String INFORMANT = "John";

    private static final UUID MOT_REASON_ID = randomUUID();
    public static final UUID OFFENCE_ID = randomUUID();
    public static final BigDecimal APPLIED_COMPENSATION = new BigDecimal(30.00);
    public static final BigDecimal BACK_DUTY = new BigDecimal(150.10);
    public static final LocalDate BACK_DUTY_DATE_FROM = LocalDate.of(2011, 1, 1);
    public static final LocalDate BACK_DUTY_DATE_TO = LocalDate.of(2015, 1, 1);
    public static final LocalDate CHARGE_DATE = LocalDate.of(2017, 11, 8);
    public static final String OFFENCE_CODE = "OFCODE12";
    public static final LocalDate OFFENCE_COMMITTED_DATE = LocalDate.of(2017, 6, 1);
    public static final LocalDate OFFENCE_COMMITTED_END_DATE = LocalDate.of(2017, 6, 20);
    public static final Integer OFFENCE_DATE_CODE = 15;
    public static final String OFFENCE_LOCATION = "London";
    public static final Integer OFFENCE_SEQUENCE_NUMBER = 3;
    public static final String OFFENCE_TITLE = "Offence Title";
    public static final String OFFENCE_TITLE_WELSH = "Offence Title (Welsh)";
    public static final String OFFENCE_WORDING = "TV Licence not paid";
    public static final String OFFENCE_WORDING_WELSH = "TV Licence not paid (Welsh)";
    public static final String STATEMENT_OF_FACTS = "Prosecution charge wording";
    public static final String STATEMENT_OF_FACTS_WELSH = "Prosecution charge wording (Welsh)";

    public static final String DEFENDANT_ID = "64fad682-f4d3-4566-868d-7621fd20ae2c";
    public static final String ASN = "arrest/summons";
    public static final Language DOCUMENTATION_LANGUAGE = Language.E;
    public static final Language HEARING_LANGUAGE = Language.W;
    public static final String LANGUAGE_REQUIREMENTS = "No Language Requirements";
    public static final String SPECIFIC_REQUIREMENTS = "No Specific Requirements";
    public static final Integer NUM_OF_PREVIOUS_CONVICTIONS = 3;
    public static final LocalDate POSTING_DATE = LocalDate.of(2015, 1, 1);
    public static final LocalDate VERDICT_DATE = LocalDate.of(2015, 1, 2);
    public static final LocalDate PLEA_DATE = LocalDate.of(2015, 1, 3);
    public static final String DRIVER_NUMBER = "AS";
    public static final String NATIONAL_INSURANCE_NUMBER = "AA123456C";
    public static final String PROSECUTOR_DEFENDANT_REFERENCE = "1d-1n-t4st";
    private static final String ORGANISATION_NAME = "TFL";
    private static final String CRO_NUMBER = "CRO";
    private static final String PNC_IDENTIFIER = "PNC";
    private static final String VEHICLE_CODE = "VehicleCode";
    private static final Integer ALCOHOL_LEVEL_AMOUNT = 40;
    private static final String ALCOHOL_LEVEL_METHOD = "A";
    private static final String OFFENDER_CODE = "XYZ";
    private static final String VEHICLE_MAKE = "Ford";
    private static final String VEHICLE_REGISTRATION_MARK = "AA11 ABC";

    public static SelfDefinedInformation createSelfDefinedInformation() {
        return new SelfDefinedInformation(ADDITIONAL_NATIONALITY, DOB, ETHNICITY, GENDER, NATIONALITY);
    }

    public static Offence createOffence() {
        return Offence.offence()
                .withAlcoholRelatedOffence(alcoholRelatedOffence())
                .withAppliedCompensation(APPLIED_COMPENSATION)
                .withBackDuty(BACK_DUTY)
                .withBackDutyDateFrom(BACK_DUTY_DATE_FROM)
                .withBackDutyDateTo(BACK_DUTY_DATE_TO)
                .withChargeDate(CHARGE_DATE)
                .withMotReasonId(MOT_REASON_ID)
                .withOffenceCode(OFFENCE_CODE)
                .withOffenceCommittedDate(OFFENCE_COMMITTED_DATE)
                .withOffenceCommittedEndDate(OFFENCE_COMMITTED_END_DATE)
                .withOffenceDateCode(OFFENCE_DATE_CODE)
                .withOffenceId(OFFENCE_ID)
                .withOffenceLocation(OFFENCE_LOCATION)
                .withOffenceSequenceNumber(OFFENCE_SEQUENCE_NUMBER)
                .withOffenceTitle(OFFENCE_TITLE)
                .withOffenceTitleWelsh(OFFENCE_TITLE_WELSH)
                .withOffenceWording(OFFENCE_WORDING)
                .withOffenceWordingWelsh(OFFENCE_WORDING_WELSH)
                .withPlea(createPlea())
                .withStatementOfFacts(STATEMENT_OF_FACTS)
                .withStatementOfFactsWelsh(STATEMENT_OF_FACTS_WELSH)
                .withVehicleMake(VEHICLE_MAKE)
                .withVehicleRegistrationMark(VEHICLE_REGISTRATION_MARK)
                .withVehicleRelatedOffence(vehicleRelatedOffence())
                .withVerdict(createVerdict())
                .build();
    }

    public static Offence createOffenceWithoutId() {
        return Offence.offence()
                .withAlcoholRelatedOffence(alcoholRelatedOffence())
                .withAppliedCompensation(APPLIED_COMPENSATION)
                .withBackDuty(BACK_DUTY)
                .withBackDutyDateFrom(BACK_DUTY_DATE_FROM)
                .withBackDutyDateTo(BACK_DUTY_DATE_TO)
                .withChargeDate(CHARGE_DATE)
                .withMotReasonId(MOT_REASON_ID)
                .withOffenceCode(OFFENCE_CODE)
                .withOffenceCommittedDate(OFFENCE_COMMITTED_DATE)
                .withOffenceCommittedEndDate(OFFENCE_COMMITTED_END_DATE)
                .withOffenceDateCode(OFFENCE_DATE_CODE)
                .withOffenceLocation(OFFENCE_LOCATION)
                .withOffenceSequenceNumber(OFFENCE_SEQUENCE_NUMBER)
                .withOffenceTitle(OFFENCE_TITLE)
                .withOffenceTitleWelsh(OFFENCE_TITLE_WELSH)
                .withOffenceWording(OFFENCE_WORDING)
                .withOffenceWordingWelsh(OFFENCE_WORDING_WELSH)
                .withPlea(createPlea())
                .withStatementOfFacts(STATEMENT_OF_FACTS)
                .withStatementOfFactsWelsh(STATEMENT_OF_FACTS_WELSH)
                .withVehicleMake(VEHICLE_MAKE)
                .withVehicleRegistrationMark(VEHICLE_REGISTRATION_MARK)
                .withVehicleRelatedOffence(vehicleRelatedOffence())
                .withVerdict(createVerdict())
                .build();
    }

    private static AlcoholRelatedOffence alcoholRelatedOffence() {
        return AlcoholRelatedOffence.alcoholRelatedOffence()
                .withAlcoholLevelAmount(ALCOHOL_LEVEL_AMOUNT)
                .withAlcoholLevelMethod(ALCOHOL_LEVEL_METHOD)
                .build();
    }

    public static VehicleRelatedOffence vehicleRelatedOffence() {
        return VehicleRelatedOffence.vehicleRelatedOffence().withVehicleCode(VEHICLE_CODE).build();
    }

    public static Address createAddress() {
        return new Address(ADDRESS_LINE_1, ADDRESS_LINE_3, ADDRESS_LINE_2, ADDRESS_LINE_4, ADDRESS_LINE_5, POSTCODE);
    }

    public static ContactDetails createContactDetails() {
        return new ContactDetails(HOME, MOBILE, PRIMARY_EMAIL, SECONDARY_EMAIL, WORK);

    }

    private static Plea createPlea() {
        return Plea.plea().withPleaDate(PLEA_DATE).withPleaValue("GUILTY").build();
    }

    private static Verdict createVerdict() {
        return Verdict.verdict().withVerdictDate(VERDICT_DATE)
                .withVerdictType(VerdictType.verdictType()
                        .withId(randomUUID())
                        .withCategory("Guilty")
                        .withCategoryType("GUILTY")
                        .build()).build();
    }

    public static PersonalInformation createPersonalInformation() {
        return new PersonalInformation(createAddress(), createContactDetails(),
                FIRST_NAME, null, null, LAST_NAME, null, OCCUPATION, OCCUPATION_CODE, TITLE);
    }

    public static Individual createIndividual() {
        return new Individual(null, null, null, null, DRIVER_NUMBER, NATIONAL_INSURANCE_NUMBER, null,
                createParentGuardianIndividualInformation(), null, createPersonalInformation(), createSelfDefinedInformation());
    }

    public static ParentGuardianInformation createParentGuardianIndividualInformation() {
        return new ParentGuardianInformation(DOB, GENDER, ETHNICITY, createPersonalInformation(), ETHNICITY, null, null, null);
    }

    public static Defendant createDefendant() {
        return Defendant.defendant().withAsn(ASN)
                .withAppliedProsecutorCosts(APPLIED_PROSECUTOR_COSTS)
                .withDocumentationLanguage(DOCUMENTATION_LANGUAGE)
                .withHearingLanguage(HEARING_LANGUAGE)
                .withId(DEFENDANT_ID)
                .withIndividual(createIndividual())
                .withLanguageRequirement(LANGUAGE_REQUIREMENTS)
                .withNumPreviousConvictions(NUM_OF_PREVIOUS_CONVICTIONS)
                .withOffences(singletonList(createOffence()))
                .withPostingDate(POSTING_DATE)
                .withProsecutorDefendantReference(PROSECUTOR_DEFENDANT_REFERENCE)
                .withSpecificRequirements(SPECIFIC_REQUIREMENTS)
                .build();
    }

    public static Defendant createCorporateDefendant() {
        return Defendant.defendant()
                .withId(DEFENDANT_ID)
                .withProsecutorDefendantReference(PROSECUTOR_DEFENDANT_REFERENCE)
                .withAppliedProsecutorCosts(APPLIED_PROSECUTOR_COSTS)
                .withOrganisationName(ORGANISATION_NAME)
                .withCroNumber(CRO_NUMBER)
                .withPncIdentifier(PNC_IDENTIFIER)
                .withOffences(singletonList(createOffence()))
                .withPostingDate(POSTING_DATE)
                .build();
    }

    public static Prosecutor createProsecutor() {
        return Prosecutor.prosecutor().withInformant(INFORMANT).withProsecutingAuthority(AUTHORITY).build();
    }

    public static CaseDetails createCaseDetails() {
        return CaseDetails.caseDetails()
                .withCaseId(CASE_ID)
                .withProsecutor(createProsecutor())
                .withProsecutorCaseReference(CASE_REFERENCE).build();
    }

    public static Prosecution createProsecution() {
        return Prosecution.prosecution().withCaseDetails(createCaseDetails()).withChannel(Channel.SPI).withDefendants(singletonList(createDefendant())).build();
    }

}
