package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.ADDITIONAL_NATIONALITY;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.ADDRESS_LINE_1;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.ADDRESS_LINE_2;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.ADDRESS_LINE_3;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.ADDRESS_LINE_4;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.ADDRESS_LINE_5;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.APPLIED_PROSECUTOR_COSTS;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.ASN;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.AUTHORITY;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.BACK_DUTY;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.BACK_DUTY_DATE_FROM;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.BACK_DUTY_DATE_TO;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.CASE_ID;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.CASE_REFERENCE;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.CHARGE_DATE;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.DEFENDANT_ID;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.DOB;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.DOCUMENTATION_LANGUAGE;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.DRIVER_NUMBER;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.ETHNICITY;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.FIRST_NAME;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.GENDER;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.HEARING_LANGUAGE;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.HOME;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.INFORMANT;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.LANGUAGE_REQUIREMENTS;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.LAST_NAME;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.MOBILE;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.NATIONALITY;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.NATIONAL_INSURANCE_NUMBER;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.NUM_OF_PREVIOUS_CONVICTIONS;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.OCCUPATION;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.OCCUPATION_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.OFFENCE_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.OFFENCE_COMMITTED_DATE;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.OFFENCE_COMMITTED_END_DATE;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.OFFENCE_DATE_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.OFFENCE_ID;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.OFFENCE_LOCATION;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.OFFENCE_SEQUENCE_NUMBER;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.OFFENCE_WORDING;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.OFFENCE_WORDING_WELSH;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.POSTCODE;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.POSTING_DATE;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.PRIMARY_EMAIL;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.PROSECUTOR_DEFENDANT_REFERENCE;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.SECONDARY_EMAIL;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.SPECIFIC_REQUIREMENTS;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.STATEMENT_OF_FACTS;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.STATEMENT_OF_FACTS_WELSH;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.TITLE;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.WORK;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.AddressDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.ContactDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.OffenceDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.PersonalInformationDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.SelfDefinedInformationDetails;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;

public abstract class ConverterBaseTest {

    protected void assertAddressDetails(final AddressDetails addressDetails) {
        assertThat(addressDetails.getAddress1(), is(ADDRESS_LINE_1));
        assertThat(addressDetails.getAddress2(), is(ADDRESS_LINE_3));
        assertThat(addressDetails.getAddress3(), is(ADDRESS_LINE_2));
        assertThat(addressDetails.getAddress4(), is(ADDRESS_LINE_4));
        assertThat(addressDetails.getAddress5(), is(ADDRESS_LINE_5));
        assertThat(addressDetails.getPostcode(), is(POSTCODE));
    }

    protected void assertContactDetails(final ContactDetails contactDetails) {
        assertThat(contactDetails.getHome(), is(HOME));
        assertThat(contactDetails.getMobile(), is(MOBILE));
        assertThat(contactDetails.getPrimaryEmail(), is(PRIMARY_EMAIL));
        assertThat(contactDetails.getSecondaryEmail(), is(SECONDARY_EMAIL));
        assertThat(contactDetails.getWork(), is(WORK));
    }

    protected void assertPersonalInformationDetails(final PersonalInformationDetails personalInformationDetails) {
        assertThat(personalInformationDetails.getTitle(), CoreMatchers.is(TITLE));
        assertThat(personalInformationDetails.getFirstName(), CoreMatchers.is(FIRST_NAME));
        assertThat(personalInformationDetails.getLastName(), CoreMatchers.is(LAST_NAME));
        assertThat(personalInformationDetails.getOccupation(), CoreMatchers.is(OCCUPATION));
        assertThat(personalInformationDetails.getOccupationCode(), CoreMatchers.is(OCCUPATION_CODE));
        assertAddressDetails(personalInformationDetails.getAddress());
        assertContactDetails(personalInformationDetails.getContactDetails().get());
    }

    protected void assertSelfDefinedInformationDetails(final SelfDefinedInformationDetails selfDefinedInformationDetails) {
        assertThat(selfDefinedInformationDetails.getNationality(), Matchers.is(NATIONALITY));
        assertThat(selfDefinedInformationDetails.getAdditionalNationality(), Matchers.is(ADDITIONAL_NATIONALITY));
        assertThat(selfDefinedInformationDetails.getDateOfBirth().toString(), Matchers.is(DOB.toString()));
        assertThat(selfDefinedInformationDetails.getGender(), Matchers.is(GENDER));
        assertThat(selfDefinedInformationDetails.getEthnicity(), Matchers.is(ETHNICITY));
    }

    protected void assertOffenceDetails(final OffenceDetails offenceDetails) {
        assertThat(offenceDetails.getOffenceId(), is(OFFENCE_ID));
        assertThat(offenceDetails.getOffenceCode(), is(OFFENCE_CODE));
        assertThat(offenceDetails.getBackDuty(), is(BACK_DUTY));
        assertThat(offenceDetails.getBackDutyDateFrom(), is(BACK_DUTY_DATE_FROM));
        assertThat(offenceDetails.getBackDutyDateTo(), is(BACK_DUTY_DATE_TO));
        assertThat(offenceDetails.getChargeDate(), is(CHARGE_DATE));
        assertThat(offenceDetails.getOffenceCode(), is(OFFENCE_CODE));
        assertThat(offenceDetails.getOffenceCommittedDate(), is(OFFENCE_COMMITTED_DATE));
        assertThat(offenceDetails.getOffenceCommittedEndDate(), is(OFFENCE_COMMITTED_END_DATE));
        assertThat(offenceDetails.getOffenceDateCode(), is(OFFENCE_DATE_CODE));
        assertThat(offenceDetails.getOffenceLocation(), is(OFFENCE_LOCATION));
        assertThat(offenceDetails.getOffenceSequenceNumber(), is(OFFENCE_SEQUENCE_NUMBER));
        assertThat(offenceDetails.getOffenceWording(), is(OFFENCE_WORDING));
        assertThat(offenceDetails.getOffenceWordingWelsh(), is(OFFENCE_WORDING_WELSH));
        assertThat(offenceDetails.getStatementOfFacts(), is(STATEMENT_OF_FACTS));
        assertThat(offenceDetails.getStatementOfFactsWelsh(), is(STATEMENT_OF_FACTS_WELSH));
    }

    protected void assertCaseDetails(final uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails caseDetails) {
        assertThat(caseDetails.getCaseId(), Matchers.is(CASE_ID));
        assertThat(caseDetails.getProsecutionCaseReference(), Matchers.is(CASE_REFERENCE));
        assertThat(caseDetails.getProsecutionAuthority(), Matchers.is(AUTHORITY));
        assertThat(caseDetails.getProsecutorInformant(), Matchers.is(INFORMANT));

        assertThat(caseDetails.getDefendants().size(), is(1));
        assertDefendantDetails(caseDetails.getDefendants().stream().findFirst().get());
    }

    protected void assertDefendantDetails(final DefendantDetails defendantDetails) {
        assertDefendantDetails(defendantDetails, DEFENDANT_ID);
    }

    protected void assertDefendantDetails(final DefendantDetails defendantDetails, final String defendantId) {
        assertThat(defendantDetails.getDefendantId(), is(defendantId));
        assertThat(defendantDetails.getAppliedProsecutorCosts(), is(APPLIED_PROSECUTOR_COSTS));
        assertThat(defendantDetails.getAsn(), is(ASN));
        assertThat(defendantDetails.getDocumentationLanguage(), is(DOCUMENTATION_LANGUAGE));
        assertThat(defendantDetails.getHearingLanguage(), is(HEARING_LANGUAGE));
        assertThat(defendantDetails.getLanguageRequirement(), is(LANGUAGE_REQUIREMENTS));
        assertThat(defendantDetails.getSpecificRequirements(), is(SPECIFIC_REQUIREMENTS));
        assertThat(defendantDetails.getNumPreviousConvictions(), is(NUM_OF_PREVIOUS_CONVICTIONS));
        assertThat(defendantDetails.getPostingDate(), is(POSTING_DATE));
        assertThat(defendantDetails.getDriverNumber(), is(DRIVER_NUMBER));
        assertThat(defendantDetails.getNationalInsuranceNumber(), is(NATIONAL_INSURANCE_NUMBER));
        assertThat(defendantDetails.getProsecutorDefendantReference(), is(PROSECUTOR_DEFENDANT_REFERENCE));
        assertPersonalInformationDetails(defendantDetails.getPersonalInformation());
        assertSelfDefinedInformationDetails(defendantDetails.getSelfDefinedInformation());
        assertThat(defendantDetails.getOffences().size(), is(1));
        assertOffenceDetails(defendantDetails.getOffences().stream().findAny().get());
    }

    protected void assertCorporateDefendantDetails(final DefendantDetails defendantDetails) {
        assertThat(defendantDetails.getDefendantId(), is(DEFENDANT_ID));
        assertThat(defendantDetails.getProsecutorDefendantReference(), is(PROSECUTOR_DEFENDANT_REFERENCE));
        assertThat(defendantDetails.getPostingDate(), is(POSTING_DATE));
        assertThat(defendantDetails.getOffences().size(), is(1));
        assertOffenceDetails(defendantDetails.getOffences().stream().findAny().get());
    }
}
