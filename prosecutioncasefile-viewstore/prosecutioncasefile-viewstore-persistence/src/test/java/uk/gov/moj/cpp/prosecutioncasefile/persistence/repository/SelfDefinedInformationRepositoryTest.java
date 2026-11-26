package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.ADDITIONAL_NATIONALITY;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.DATE_OF_BIRTH;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.ETHNICITY;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.GENDER;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.NATIONALITY;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.createFirstDefendant;

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
public class SelfDefinedInformationRepositoryTest {

    @Inject
    private SelfDefinedInformationRepository selfDefinedInformationRepository;

    @Inject
    private DefendantRepository defendantRepository;

    private SelfDefinedInformationDetails selfDefinedInformationDetails;

    private DefendantDetails defendantDetails;

    @Before
    public void setUp() {
        createAndSaveDefendant();
    }

    @Test
    public void shouldUpdateSelfDefinedInformation() {
        final String ADDITIONAL_NATIONALITY = "American";
        selfDefinedInformationDetails.setAdditionalNationality(ADDITIONAL_NATIONALITY);
        selfDefinedInformationRepository.save(selfDefinedInformationDetails);

        final List<SelfDefinedInformationDetails> selfDefinedInformationDetailsList = selfDefinedInformationRepository.findAll();
        assertThat(selfDefinedInformationDetailsList.size(), equalTo(1));
        final SelfDefinedInformationDetails selfDefinedInformationDetailsSvd = selfDefinedInformationDetailsList.get(0);
        assertThat(selfDefinedInformationDetailsSvd.getAdditionalNationality(), equalTo(ADDITIONAL_NATIONALITY));
        assertSelfDefinedInformationMatches(selfDefinedInformationDetails, selfDefinedInformationDetailsSvd);
    }

    @Test
    public void shouldConstructSelfDefinedInformation() {
        final DefendantDetails defendantDetails = createFirstDefendant();
        final SelfDefinedInformationDetails selfDefinedInformationDetails = new SelfDefinedInformationDetails(ADDITIONAL_NATIONALITY, DATE_OF_BIRTH, ETHNICITY, GENDER, NATIONALITY);
        selfDefinedInformationDetails.setDefendantDetails(defendantDetails);
        final SelfDefinedInformationDetails selfDefinedInformationDetailsSvd = new SelfDefinedInformationDetails();
        selfDefinedInformationDetailsSvd.setAdditionalNationality(ADDITIONAL_NATIONALITY);
        selfDefinedInformationDetailsSvd.setDateOfBirth(DATE_OF_BIRTH);
        selfDefinedInformationDetailsSvd.setEthnicity(ETHNICITY);
        selfDefinedInformationDetailsSvd.setGender(GENDER);
        selfDefinedInformationDetailsSvd.setNationality(NATIONALITY);
        selfDefinedInformationDetailsSvd.setDefendantDetails(defendantDetails);
        assertSelfDefinedInformationMatches(selfDefinedInformationDetails, selfDefinedInformationDetailsSvd);
    }

    private void assertSelfDefinedInformationMatches(final SelfDefinedInformationDetails selfDefinedInformationDetails, final SelfDefinedInformationDetails selfDefinedInformationDetailsSvd) {
        assertThat(selfDefinedInformationDetailsSvd.getSelfDefinedInformationId(), equalTo(selfDefinedInformationDetails.getSelfDefinedInformationId()));
        assertThat(selfDefinedInformationDetailsSvd.getEthnicity(), equalTo(selfDefinedInformationDetailsSvd.getEthnicity()));
        assertThat(selfDefinedInformationDetailsSvd.getDateOfBirth(), equalTo(selfDefinedInformationDetails.getDateOfBirth()));
        assertThat(selfDefinedInformationDetailsSvd.getDefendantDetails().getDefendantId(), equalTo(selfDefinedInformationDetails.getDefendantDetails().getDefendantId()));
        assertThat(selfDefinedInformationDetailsSvd.getGender(), equalTo(selfDefinedInformationDetails.getGender()));
        assertThat(selfDefinedInformationDetailsSvd.getNationality(), equalTo(selfDefinedInformationDetails.getNationality()));
    }

    private void createAndSaveDefendant() {
        defendantDetails = TestUtils.createFirstDefendant();

        final PersonalInformationDetails personalInformationDetails = defendantDetails.getPersonalInformation();
        personalInformationDetails.setDefendantDetails(defendantDetails);

        selfDefinedInformationDetails = defendantDetails.getSelfDefinedInformation();
        selfDefinedInformationDetails.setDefendantDetails(defendantDetails);

        defendantRepository.save(defendantDetails);
    }
}
