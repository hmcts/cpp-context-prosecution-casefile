package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;


import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.APPLIED_PROSECUTOR_COSTS;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.ASN;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.DEFENDANT_ID;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.DOCUMENTATION_LANGUAGE;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.DRIVER_NUMBER;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.HEARING_LANGUAGE;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.LANGUAGE_REQUIREMENTS;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.NATIONAL_INSURANCE_NUMBER;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.NUM_PREVIOUS_CONVICTIONS;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.POSTING_DATE;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.PROSECUTOR_DEFENDANT_REFERENCE;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.SPECIFIC_REQUIREMENTS;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.createFirstDefendantOffence;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.createFirstDefendantPersonalInformation;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.createFirstDefendantSelfDefinedInformation;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.OffenceDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.PersonalInformationDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.SelfDefinedInformationDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils;

import java.util.List;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class DefendantRepositoryTest {

    @Inject
    private DefendantRepository defendantRepository;

    @Test
    public void shouldFindDefendantById() {
        final DefendantDetails defendantDetails = TestUtils.createFirstDefendant();

        final PersonalInformationDetails personalInformation = defendantDetails.getPersonalInformation();
        personalInformation.setDefendantDetails(defendantDetails);

        final SelfDefinedInformationDetails selfDefinedInformation = defendantDetails.getSelfDefinedInformation();
        selfDefinedInformation.setDefendantDetails(defendantDetails);

        defendantRepository.save(defendantDetails);

        final List<DefendantDetails> defendantDetailsList = defendantRepository.findByDefendantId(TestUtils.DEFENDANT_ID.toString());

        assertThat(defendantDetailsList.size(), is(1));
        final DefendantDetails defendantDetailsSvd = defendantDetailsList.get(0);

        assertDefendantsMatch(defendantDetails, defendantDetailsSvd);
    }

    @Test
    public void shouldConstructDefendantDetails() {
        final PersonalInformationDetails personalInformationDetails = createFirstDefendantPersonalInformation();
        final SelfDefinedInformationDetails selfDefinedInformationDetails = createFirstDefendantSelfDefinedInformation();
        final OffenceDetails offenceDetails = createFirstDefendantOffence();

        final DefendantDetails defendantDetails = new DefendantDetails(DEFENDANT_ID.toString(), ASN, DOCUMENTATION_LANGUAGE,
                HEARING_LANGUAGE, LANGUAGE_REQUIREMENTS, SPECIFIC_REQUIREMENTS, NUM_PREVIOUS_CONVICTIONS, POSTING_DATE, DRIVER_NUMBER, NATIONAL_INSURANCE_NUMBER,
                PROSECUTOR_DEFENDANT_REFERENCE, APPLIED_PROSECUTOR_COSTS, personalInformationDetails, selfDefinedInformationDetails, singleton(offenceDetails), null, null,null);

        final DefendantDetails defendantDetailsSvd = new DefendantDetails();
        defendantDetailsSvd.setDefendantId(DEFENDANT_ID.toString());
        defendantDetailsSvd.setAsn(ASN);
        defendantDetailsSvd.setDocumentationLanguage(DOCUMENTATION_LANGUAGE);
        defendantDetailsSvd.setHearingLanguage(HEARING_LANGUAGE);
        defendantDetailsSvd.setLanguageRequirement(LANGUAGE_REQUIREMENTS);
        defendantDetailsSvd.setSpecificRequirements(SPECIFIC_REQUIREMENTS);
        defendantDetailsSvd.setNumPreviousConvictions(NUM_PREVIOUS_CONVICTIONS);
        defendantDetailsSvd.setPostingDate(POSTING_DATE);
        defendantDetailsSvd.setDriverNumber(DRIVER_NUMBER);
        defendantDetailsSvd.setNationalInsuranceNumber(NATIONAL_INSURANCE_NUMBER);
        defendantDetailsSvd.setProsecutorDefendantReference(PROSECUTOR_DEFENDANT_REFERENCE);
        defendantDetailsSvd.setAppliedProsecutorCosts(APPLIED_PROSECUTOR_COSTS);

        defendantDetailsSvd.setPersonalInformation(personalInformationDetails);
        defendantDetailsSvd.setSelfDefinedInformation(selfDefinedInformationDetails);
        defendantDetailsSvd.setOffences(singleton(offenceDetails));
        assertDefendantsMatch(defendantDetails, defendantDetailsSvd);
    }

    private void assertDefendantsMatch(final DefendantDetails defendantDetails, final DefendantDetails defendantDetailsSvd) {
        assertThat(defendantDetails.getDefendantId(), equalTo(defendantDetailsSvd.getDefendantId()));
        assertThat(defendantDetails.getAsn(), equalTo(defendantDetailsSvd.getAsn()));
        assertThat(defendantDetails.getHearingLanguage(), equalTo(defendantDetailsSvd.getHearingLanguage()));
        assertThat(defendantDetails.getAppliedProsecutorCosts(), equalTo(defendantDetailsSvd.getAppliedProsecutorCosts()));
        assertThat(defendantDetails.getNationalInsuranceNumber(), equalTo(defendantDetailsSvd.getNationalInsuranceNumber()));
        assertThat(defendantDetails.getPersonalInformation().getPersonalInformationId(), equalTo(defendantDetailsSvd.getPersonalInformation().getPersonalInformationId()));
        assertThat(defendantDetails.getSelfDefinedInformation().getSelfDefinedInformationId(), equalTo(defendantDetailsSvd.getSelfDefinedInformation().getSelfDefinedInformationId()));
        assertThat(defendantDetails.getSpecificRequirements(), equalTo(defendantDetailsSvd.getSpecificRequirements()));
        assertThat(defendantDetails.getLanguageRequirement(), equalTo(defendantDetailsSvd.getLanguageRequirement()));
        assertThat(defendantDetails.getDocumentationLanguage(), equalTo(defendantDetailsSvd.getDocumentationLanguage()));
        assertThat(defendantDetails.getNumPreviousConvictions(), equalTo(defendantDetailsSvd.getNumPreviousConvictions()));
        assertThat(defendantDetails.getDocumentationLanguage(), equalTo(defendantDetailsSvd.getDocumentationLanguage()));
        assertThat(defendantDetails.getPostingDate(), equalTo(defendantDetailsSvd.getPostingDate()));
        assertThat(defendantDetails.getDriverNumber(), equalTo(defendantDetailsSvd.getDriverNumber()));
        assertThat(defendantDetails.getOffences().size(), equalTo(defendantDetailsSvd.getOffences().size()));
        assertThat(defendantDetails.getProsecutorDefendantReference(), equalTo(defendantDetailsSvd.getProsecutorDefendantReference()));
    }

}
