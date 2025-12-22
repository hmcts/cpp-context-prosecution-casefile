package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.FIRST_NAME;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.LAST_NAME;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.OCCUPATION;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.OCCUPATION_CODE;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.TITLE;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.createAddress;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.createContactDetails;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.createFirstDefendant;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.AddressDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.ContactDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.PersonalInformationDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.SelfDefinedInformationDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils;

import java.util.List;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class PersonalInformationRespositoryTest {

    @Inject
    private PersonalInformationRepository personalInformationRepository;

    @Inject
    private DefendantRepository defendantRepository;

    private PersonalInformationDetails personalInformationDetails;

    private DefendantDetails defendantDetails;

    @Before
    public void setUp() {
        createAndSaveDefendant();
    }

    @Test
    public void shouldUpdatePersonalInformation() {
        final String FIRST_NAME = "Percival";
        personalInformationDetails.setFirstName(FIRST_NAME);
        personalInformationRepository.save(personalInformationDetails);
        final List<PersonalInformationDetails> personalInformationDetailsList = personalInformationRepository.findAll();
        final PersonalInformationDetails personalInformationDetailsSvd = personalInformationDetailsList.get(0);
        assertThat(personalInformationDetailsSvd.getFirstName(), equalTo(FIRST_NAME));
        assertPersonalInformationMatches(personalInformationDetails, personalInformationDetailsSvd);
    }

    @Test
    public void shouldConstructPersonalInformation() {
        final DefendantDetails defendantDetails = createFirstDefendant();
        final AddressDetails addressDetails = createAddress();
        final ContactDetails contactDetails = createContactDetails();
        final PersonalInformationDetails personalInformationDetails = new PersonalInformationDetails(TITLE, FIRST_NAME, LAST_NAME, OCCUPATION, OCCUPATION_CODE, addressDetails, contactDetails);
        personalInformationDetails.setDefendantDetails(defendantDetails);

        final PersonalInformationDetails personalInformationDetailsSvd = new PersonalInformationDetails();
        personalInformationDetailsSvd.setFirstName(FIRST_NAME);
        personalInformationDetailsSvd.setTitle(TITLE);
        personalInformationDetailsSvd.setLastName(LAST_NAME);
        personalInformationDetailsSvd.setOccupation(OCCUPATION);
        personalInformationDetailsSvd.setOccupationCode(OCCUPATION_CODE);
        personalInformationDetailsSvd.setAddress(addressDetails);
        personalInformationDetailsSvd.setContactDetails(contactDetails);
        personalInformationDetailsSvd.setDefendantDetails(defendantDetails);

        assertPersonalInformationMatches(personalInformationDetails, personalInformationDetailsSvd);
    }

    private void assertPersonalInformationMatches(final PersonalInformationDetails personalInformationDetails, final PersonalInformationDetails personalInformationDetailsSvd) {
        assertThat(personalInformationDetails.getPersonalInformationId(), equalTo(personalInformationDetailsSvd.getPersonalInformationId()));
        assertThat(personalInformationDetails.getLastName(), equalTo(personalInformationDetailsSvd.getLastName()));
        assertThat(personalInformationDetails.getAddress().getAddress1(), equalTo(personalInformationDetailsSvd.getAddress().getAddress1()));
        assertThat(personalInformationDetails.getAddress().getAddress2(), equalTo(personalInformationDetailsSvd.getAddress().getAddress2()));
        assertThat(personalInformationDetails.getAddress().getAddress3(), equalTo(personalInformationDetailsSvd.getAddress().getAddress3()));
        assertThat(personalInformationDetails.getAddress().getAddress4(), equalTo(personalInformationDetailsSvd.getAddress().getAddress4()));
        assertThat(personalInformationDetails.getAddress().getAddress5(), equalTo(personalInformationDetailsSvd.getAddress().getAddress5()));
        assertThat(personalInformationDetails.getAddress().getPostcode(), equalTo(personalInformationDetailsSvd.getAddress().getPostcode()));

        assertThat(personalInformationDetails.getOccupation(), equalTo(personalInformationDetailsSvd.getOccupation()));
        assertThat(personalInformationDetails.getTitle(), equalTo(personalInformationDetailsSvd.getTitle()));
        assertThat(personalInformationDetails.getOccupationCode(), equalTo(personalInformationDetailsSvd.getOccupationCode()));
        assertThat(personalInformationDetails.getDefendantDetails().getNationalInsuranceNumber(), equalTo(personalInformationDetailsSvd.getDefendantDetails().getNationalInsuranceNumber()));

        assertThat(personalInformationDetails.getContactDetails().isPresent(), equalTo(true));

        assertThat(personalInformationDetails.getContactDetails().get().getHome(), equalTo(personalInformationDetailsSvd.getContactDetails().get().getHome()));
        assertThat(personalInformationDetails.getContactDetails().get().getMobile(), equalTo(personalInformationDetailsSvd.getContactDetails().get().getMobile()));
        assertThat(personalInformationDetails.getContactDetails().get().getPrimaryEmail(), equalTo(personalInformationDetailsSvd.getContactDetails().get().getPrimaryEmail()));
        assertThat(personalInformationDetails.getContactDetails().get().getSecondaryEmail(), equalTo(personalInformationDetailsSvd.getContactDetails().get().getSecondaryEmail()));
        assertThat(personalInformationDetails.getContactDetails().get().getWork(), equalTo(personalInformationDetailsSvd.getContactDetails().get().getWork()));
    }

    private void createAndSaveDefendant() {
        defendantDetails = TestUtils.createFirstDefendant();

        personalInformationDetails = defendantDetails.getPersonalInformation();
        personalInformationDetails.setDefendantDetails(defendantDetails);

        final SelfDefinedInformationDetails selfDefinedInformation = defendantDetails.getSelfDefinedInformation();
        selfDefinedInformation.setDefendantDetails(defendantDetails);

        defendantRepository.save(defendantDetails);
    }

}
