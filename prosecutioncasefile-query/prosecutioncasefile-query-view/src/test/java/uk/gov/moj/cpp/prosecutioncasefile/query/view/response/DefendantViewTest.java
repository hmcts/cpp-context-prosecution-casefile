package uk.gov.moj.cpp.prosecutioncasefile.query.view.response;


import static com.google.common.collect.Sets.newHashSet;
import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Gender.FEMALE;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Language;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.AddressDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.OffenceDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.PersonalInformationDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.SelfDefinedInformationDetails;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

public class DefendantViewTest {

    @Test
    public void shouldGetViewModelForDefendant() {
        final DefendantDetails defendant = getDefaultDefendantDetails();
        final OffenceDetails offenceDetails = defendant.getOffences().iterator().next();
        final PersonalInformationDetails personalInformationDetails = defendant.getPersonalInformation();
        final SelfDefinedInformationDetails selfDefinedInformationDetails = defendant.getSelfDefinedInformation();
        final AddressDetails addressDetails = personalInformationDetails.getAddress();
        final uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.ContactDetails contactDetails = personalInformationDetails.getContactDetails().get();

        final DefendantView defendantView = new DefendantView(defendant);
        final Offence offenceView = defendantView.getOffences().get(0);
        final PersonalInformation personalInformationView = defendantView.getPersonalInformation();
        final SelfDefinedInformation selfDefinedInformationView = defendantView.getSelfDefinedInformation();
        final Address addressView = personalInformationView.getAddress();
        final ContactDetails contactDetailsView = personalInformationView.getContactDetails();

        assertThat(defendantView.getDefendantId(), is(defendant.getDefendantId()));
        assertThat(defendantView.getAsn(), is(defendant.getAsn()));
        assertThat(defendantView.getProsecutorDefendantReference(), is(defendant.getProsecutorDefendantReference()));
        assertThat(defendantView.getNumPreviousConvictions(), is(defendant.getNumPreviousConvictions()));
        assertThat(defendantView.getDocumentationLanguage(), is(defendant.getDocumentationLanguage()));
        assertThat(defendantView.getHearingLanguage(), is(defendant.getHearingLanguage()));
        assertThat(defendantView.getLanguageRequirement(), is(defendant.getLanguageRequirement()));
        assertThat(defendantView.getNationalInsuranceNumber(), is(defendant.getNationalInsuranceNumber()));
        assertThat(defendantView.getDriverNumber(), is(defendant.getDriverNumber()));
        assertThat(defendantView.getPostingDate(), is(defendant.getPostingDate()));
        assertThat(defendantView.getIdpcMaterialId(), is(defendant.getIdpcMaterialId()));

        assertThat(offenceView.getOffenceId(), is(offenceDetails.getOffenceId()));
        assertThat(offenceView.getOffenceCode(), is(offenceDetails.getOffenceCode()));
        assertThat(offenceView.getOffenceLocation(), is(offenceDetails.getOffenceLocation()));
        assertThat(offenceView.getOffenceWording(), is(offenceDetails.getOffenceWording()));
        assertThat(offenceView.getOffenceWordingWelsh(), is(offenceDetails.getOffenceWordingWelsh()));
        assertThat(offenceView.getOffenceCommittedDate(), is(offenceDetails.getOffenceCommittedDate()));
        assertThat(offenceView.getOffenceCommittedEndDate(), is(offenceDetails.getOffenceCommittedEndDate()));
        assertThat(offenceView.getOffenceDateCode(), is(offenceDetails.getOffenceDateCode()));
        assertThat(offenceView.getOffenceSequenceNumber(), is(offenceDetails.getOffenceSequenceNumber()));
        assertThat(offenceView.getChargeDate(), is(offenceDetails.getChargeDate()));
        assertThat(offenceView.getStatementOfFacts(), is(offenceDetails.getStatementOfFacts()));
        assertThat(offenceView.getStatementOfFactsWelsh(), is(offenceDetails.getStatementOfFactsWelsh()));
        assertThat(offenceView.getAppliedCompensation(), is(offenceDetails.getAppliedCompensation()));
        assertThat(offenceView.getBackDuty(), is(offenceDetails.getBackDuty()));
        assertThat(offenceView.getBackDutyDateFrom(), is(offenceDetails.getBackDutyDateFrom()));
        assertThat(offenceView.getBackDutyDateTo(), is(offenceDetails.getBackDutyDateTo()));

        assertThat(personalInformationView.getTitle(), is(personalInformationDetails.getTitle()));
        assertThat(personalInformationView.getFirstName(), is(personalInformationDetails.getFirstName()));
        assertThat(personalInformationView.getLastName(), is(personalInformationDetails.getLastName()));
        assertThat(personalInformationView.getOccupation(), is(personalInformationDetails.getOccupation()));
        assertThat(personalInformationView.getOccupationCode(), is(personalInformationDetails.getOccupationCode()));

        assertThat(addressView.getAddress1(), is(addressDetails.getAddress1()));
        assertThat(addressView.getAddress2(), is(addressDetails.getAddress2()));
        assertThat(addressView.getAddress3(), is(addressDetails.getAddress3()));
        assertThat(addressView.getAddress4(), is(addressDetails.getAddress4()));
        assertThat(addressView.getAddress5(), is(addressDetails.getAddress5()));
        assertThat(addressView.getPostcode(), is(addressDetails.getPostcode()));

        assertThat(contactDetailsView.getHome(), is(contactDetails.getHome()));
        assertThat(contactDetailsView.getMobile(), is(contactDetails.getMobile()));
        assertThat(contactDetailsView.getWork(), is(contactDetails.getWork()));
        assertThat(contactDetailsView.getPrimaryEmail(), is(contactDetails.getPrimaryEmail()));
        assertThat(contactDetailsView.getSecondaryEmail(), is(contactDetails.getSecondaryEmail()));

        assertThat(selfDefinedInformationView.getNationality(), is(selfDefinedInformationDetails.getNationality()));
        assertThat(selfDefinedInformationView.getAdditionalNationality(), is(selfDefinedInformationDetails.getAdditionalNationality()));
        assertThat(selfDefinedInformationView.getGender(), is(selfDefinedInformationDetails.getGender()));
        assertThat(selfDefinedInformationView.getDateOfBirth(), is(selfDefinedInformationDetails.getDateOfBirth()));
        assertThat(selfDefinedInformationView.getEthnicity(), is(selfDefinedInformationDetails.getEthnicity()));
    }

    @Test
    public void shouldGetViewModelForDefendantWithoutContactDetails() {
        final DefendantDetails defendant = getDefaultDefendantDetails();
        defendant.getPersonalInformation().setContactDetails(null);

        final DefendantView defendantView = new DefendantView(defendant);

        assertThat(defendantView.getPersonalInformation().getContactDetails(), nullValue());
    }

    @Test
    public void shouldGetViewModelForDefendantWithoutAddress() {
        final DefendantDetails defendant = getDefaultDefendantDetails();
        defendant.getPersonalInformation().setAddress(null);

        final DefendantView defendantView = new DefendantView(defendant);

        assertThat(defendantView.getPersonalInformation().getAddress(), nullValue());
    }

    private DefendantDetails getDefaultDefendantDetails() {
        final AddressDetails addressDetails = new AddressDetails();
        addressDetails.setAddress1("Pinnacle Apartments");
        addressDetails.setPostcode("CR0 2GE");

        final uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.ContactDetails contactDetails = new uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.ContactDetails();
        contactDetails.setMobile("07003422324");
        contactDetails.setPrimaryEmail("test@test.com");

        final PersonalInformationDetails personalInformation = new PersonalInformationDetails();
        personalInformation.setTitle("Miss");
        personalInformation.setFirstName("Regina");
        personalInformation.setLastName("Becks");
        personalInformation.setAddress(addressDetails);
        personalInformation.setContactDetails(contactDetails);

        final SelfDefinedInformationDetails selfDefinedInformation = new SelfDefinedInformationDetails();
        selfDefinedInformation.setNationality("FR");
        selfDefinedInformation.setDateOfBirth(LocalDate.of(2000, 01, 01));
        selfDefinedInformation.setGender(FEMALE);

        final OffenceDetails offence = new OffenceDetails();
        offence.setOffenceId(randomUUID());
        offence.setAppliedCompensation(BigDecimal.TEN);
        offence.setChargeDate(LocalDate.of(2018, 1, 10));
        offence.setOffenceCode("PS0001");
        offence.setOffenceDateCode(4);
        offence.setOffenceCommittedDate(LocalDate.of(2018, 1, 1));
        offence.setOffenceWording("TV Licence not paid");
        offence.setStatementOfFacts("John Doe missed a TV license payment for the 5th time");

        final DefendantDetails defendantDetails = new DefendantDetails();
        defendantDetails.setDefendantId(randomUUID().toString());
        defendantDetails.setAsn("arrest/summons");
        defendantDetails.setDocumentationLanguage(Language.E);
        defendantDetails.setHearingLanguage(Language.E);
        defendantDetails.setNationalInsuranceNumber("QQ123456C");
        defendantDetails.setPostingDate(now());
        defendantDetails.setProsecutorDefendantReference("REF");
        defendantDetails.setPersonalInformation(personalInformation);
        defendantDetails.setSelfDefinedInformation(selfDefinedInformation);
        defendantDetails.setOffences(newHashSet(offence));
        defendantDetails.setIdpcMaterialId(randomUUID());

        return defendantDetails;
    }
}
