package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.CASE_ID;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.FIRST_DEFENDANT_CASE_ID;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.INVALID_PROSECUTOR_CASE_REFERENCE;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.ORIGINATING_ORGANISATION;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.PROSECUTOR_AUTHORITY;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.PROSECUTOR_CASE_REFERENCE;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.PROSECUTOR_CASE_REFERENCE2;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.PROSECUTOR_INFORMANT;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.createFirstDefendantCaseDetails;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CivilFees;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.PersonalInformationDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.SelfDefinedInformationDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.NoResultException;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CaseDetailsRepositoryTest {

    @Inject
    private CaseDetailsRepository caseDetailsRepository;

    @Test
    public void shouldFindCaseDetailsByProsecutionCaseReference() {
        final CaseDetails caseDetails = TestUtils.createFirstDefendantCaseDetails();

        final DefendantDetails defendantDetails = caseDetails.getDefendants().stream().findFirst().get();

        final PersonalInformationDetails personalInformation = defendantDetails.getPersonalInformation();
        personalInformation.setDefendantDetails(defendantDetails);

        final SelfDefinedInformationDetails selfDefinedInformation = defendantDetails.getSelfDefinedInformation();
        selfDefinedInformation.setDefendantDetails(defendantDetails);

        caseDetailsRepository.save(caseDetails);
        final CaseDetails caseDetailsSvd = caseDetailsRepository.findCaseDetailsByProsecutionCaseReference(TestUtils.PROSECUTOR_CASE_REFERENCE);
        assertCaseDetailsMatch(caseDetails, caseDetailsSvd);
    }

    @Test
    public void shouldConstructCaseDetails() {
        final CaseDetails caseDetailsSvd = createFirstDefendantCaseDetails();
        final CaseDetails caseDetails = new CaseDetails(FIRST_DEFENDANT_CASE_ID,
                PROSECUTOR_CASE_REFERENCE,
                PROSECUTOR_INFORMANT,
                PROSECUTOR_AUTHORITY,
                ORIGINATING_ORGANISATION,
                caseDetailsSvd.getDefendants(),
                Set.of( new CivilFees(UUID.randomUUID(), CASE_ID,"someFeeType",
                "someFeeStatus",
                "")));
        assertCaseDetailsMatch(caseDetails, caseDetailsSvd);
    }

    @Test
    public void shouldFindAllCaseDetailsByProsecutionCaseReferences() {
        final CaseDetails firstDefendantCaseDetails = TestUtils.createFirstDefendantCaseDetails();
        final DefendantDetails firstDefendantDetails = firstDefendantCaseDetails.getDefendants().stream().findFirst().get();
        final PersonalInformationDetails personalInformation = firstDefendantDetails.getPersonalInformation();
        personalInformation.setDefendantDetails(firstDefendantDetails);
        final SelfDefinedInformationDetails selfDefinedInformation = firstDefendantDetails.getSelfDefinedInformation();
        selfDefinedInformation.setDefendantDetails(firstDefendantDetails);
        caseDetailsRepository.save(firstDefendantCaseDetails);

        final CaseDetails secondDefendantCaseDetails = TestUtils.createSecondDefendantCaseDetails();
        final DefendantDetails secondDefendantDetails = secondDefendantCaseDetails.getDefendants().stream().findFirst().get();
        final PersonalInformationDetails secondDefendantPersonalInformation = secondDefendantDetails.getPersonalInformation();
        secondDefendantPersonalInformation.setDefendantDetails(secondDefendantDetails);
        final SelfDefinedInformationDetails secondDefendantSelfDefinedInformation= secondDefendantDetails.getSelfDefinedInformation();
        secondDefendantSelfDefinedInformation.setDefendantDetails(secondDefendantDetails);
        caseDetailsRepository.save(secondDefendantCaseDetails);


        final List<CaseDetails> resultedCaseDetails = caseDetailsRepository.findAllCaseDetailsByProsecutionCaseReferences(asList(TestUtils.PROSECUTOR_CASE_REFERENCE, PROSECUTOR_CASE_REFERENCE2));
        assertThat(resultedCaseDetails, notNullValue());
        assertThat(resultedCaseDetails.size(),is(2));
        assertCaseDetailsMatch(firstDefendantCaseDetails, resultedCaseDetails.get(0));
        assertCaseDetailsMatch(secondDefendantCaseDetails, resultedCaseDetails.get(1));
    }

    @Test
    public void shouldThrowException_whenGivenProsecutionCaseReference_notExist() {
        final CaseDetails firstDefendantCaseDetails = TestUtils.createFirstDefendantCaseDetails();
        final DefendantDetails firstDefendantDetails = firstDefendantCaseDetails.getDefendants().stream().findFirst().get();
        final PersonalInformationDetails personalInformation = firstDefendantDetails.getPersonalInformation();
        personalInformation.setDefendantDetails(firstDefendantDetails);
        final SelfDefinedInformationDetails selfDefinedInformation = firstDefendantDetails.getSelfDefinedInformation();
        selfDefinedInformation.setDefendantDetails(firstDefendantDetails);
        caseDetailsRepository.save(firstDefendantCaseDetails);

        final NoResultException expectedException = assertThrows(NoResultException.class, () ->caseDetailsRepository.findCaseDetailsByProsecutionCaseReference(INVALID_PROSECUTOR_CASE_REFERENCE));
        assertThat(expectedException.getMessage(), is("No entity found for query"));
    }

    private void assertCaseDetailsMatch(final CaseDetails caseDetails, final CaseDetails caseDetailsSvd) {
        assertThat(caseDetails.getCaseId(), equalTo(caseDetailsSvd.getCaseId()));
        assertThat(caseDetails.getProsecutionAuthority(), equalTo(caseDetailsSvd.getProsecutionAuthority()));
        assertThat(caseDetails.getProsecutionCaseReference(), equalTo(caseDetailsSvd.getProsecutionCaseReference()));
        assertThat(caseDetails.getProsecutorInformant(), equalTo(caseDetailsSvd.getProsecutorInformant()));
        assertThat(caseDetails.getProsecutorInformant(), equalTo(caseDetailsSvd.getProsecutorInformant()));
        assertThat(caseDetails.getDefendants().size(), equalTo(caseDetailsSvd.getDefendants().size()));
    }
}
