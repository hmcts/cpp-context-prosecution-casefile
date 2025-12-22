package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static java.util.UUID.randomUUID;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.CpsDefendantOffences.cpsDefendantOffences;
import static uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.CpsOffenceDetails.cpsOffenceDetails;
import static uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.ProsecutionCaseSubject.prosecutionCaseSubject;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.ApplicationsForDirectionsGroup.applicationsForDirectionsGroup;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsOffences.cpsOffences;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.Defence.defence;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.Defendants.defendants;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.DisplayEquipmentYesGroup.displayEquipmentYesGroup;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.DynamicFormAnswers.dynamicFormAnswers;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.PendingLinesOfEnquiryYesGroup.pendingLinesOfEnquiryYesGroup;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.Prosecution.prosecution;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutorGroup.prosecutorGroup;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.Witnesses.witnesses;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.ProcessReceivedCpsServeBcm;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.SubmissionStatus;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PetFormData;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PointOfLawYesGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionCaseSubject;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionComplianceNoGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionComplianceYesGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutorServeEvidenceYesGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServeBcmProcessed;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SlaveryOrExploitationYesGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.VariationStandardDirectionsProsecutorYesGroup;

import java.time.LocalDate;
import java.util.UUID;


@ExtendWith(MockitoExtension.class)
public class BaseCpsServeMaterialHandlerTest {

    private BaseCpsServeMaterialHandler baseCpsServeMaterialHandler = new BaseCpsServeMaterialHandler();

    @Test
    public void convertToCpsServeBcmSubmitted() {

        final uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.ProcessReceivedCpsServeBcm processReceivedCpsServeBcm = ProcessReceivedCpsServeBcm
                .processReceivedCpsServeBcm()
                .withSubmissionId(randomUUID())
                .withProsecutionCaseSubject(prosecutionCaseSubject()
                        .withUrn("urn123")
                        .withProsecutingAuthority(randomUUID().toString())
                        .build())
                .withCpsDefendantOffences(asList(cpsDefendantOffences()
                        .withAsn("asn1")
                        .withCpsOffenceDetails(asList(cpsOffenceDetails()
                                .withCjsOffenceCode("cjsOffenceCode")
                                .withOffenceWording("offenceWording")
                                .withOffenceDate(LocalDate.now())
                                .build()))
                        .build()))
                .build();
        final SubmissionStatus submissionStatus = SubmissionStatus.SUCCESS;
        final ReceivedCpsServeBcmProcessed receivedCpsServeBcmProcessed = baseCpsServeMaterialHandler.convertToCpsServeBcmSubmitted(processReceivedCpsServeBcm, submissionStatus);
        assertThat(receivedCpsServeBcmProcessed.getSubmissionId(), is(processReceivedCpsServeBcm.getSubmissionId()));
        assertThat(receivedCpsServeBcmProcessed.getProsecutionCaseSubject().getProsecutingAuthority(), is(processReceivedCpsServeBcm.getProsecutionCaseSubject().getProsecutingAuthority()));
        assertThat(receivedCpsServeBcmProcessed.getCpsDefendantOffences(), hasSize(1));
    }



    @Test
    public void convertCpsProsecutionCaseSubject() {
        final uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.ProsecutionCaseSubject prosecutionCaseSubject = prosecutionCaseSubject().withUrn("urn1").build();
        final ProsecutionCaseSubject result = baseCpsServeMaterialHandler.convertCpsProsecutionCaseSubject(prosecutionCaseSubject);
        assertThat(result.getUrn(), is(prosecutionCaseSubject.getUrn()));

    }

    @Test
    public void convertPetFormData() {
        final UUID defendantId = randomUUID();
        final uk.gov.moj.cps.prosecutioncasefile.domain.event.PetFormData petFormData = uk.gov.moj.cps.prosecutioncasefile.domain.event.PetFormData.petFormData()
                .withDefence(defence()
                        .withDefendants(asList(defendants().withId(defendantId)
                                .withCpsOffences(asList(cpsOffences()
                                        .withOffenceCode("offenceCode")
                                        .withWording("wording")
                                        .build()))

                                .build()))
                        .build())
                .withProsecution(prosecution()
                        .withWitnesses(asList(witnesses().withFirstName("firstWitness").build()))
                        .withDynamicFormAnswers(
                                dynamicFormAnswers()
                                        .withAdditionalInformation("AdditionalInformation")
                                        .withApplicationsForDirectionsGroup(applicationsForDirectionsGroup()
                                                .withVariationStandardDirectionsProsecutorYesGroup(VariationStandardDirectionsProsecutorYesGroup.variationStandardDirectionsProsecutorYesGroup().withVariationStandardDirectionsProsecutorYesGroupDetails("test1").build())
                                                .build())
                                        .withProsecutorGroup(prosecutorGroup()
                                                .withDisplayEquipmentYesGroup(displayEquipmentYesGroup().withDisplayEquipmentDetails("Y").build())
                                                .withPendingLinesOfEnquiryYesGroup(pendingLinesOfEnquiryYesGroup().withPendingLinesOfEnquiryYesGroup("Y").build())
                                                .withPointOfLawYesGroup(PointOfLawYesGroup.pointOfLawYesGroup().withPointOfLawDetails("Y").build())
                                                .withProsecutionComplianceNoGroup(ProsecutionComplianceNoGroup.prosecutionComplianceNoGroup().withProsecutionComplianceDetailsNo("Y").build())
                                                .withProsecutionComplianceYesGroup(ProsecutionComplianceYesGroup.prosecutionComplianceYesGroup().withProsecutionComplianceDetailsYes("Y").build())
                                                .withProsecutorServeEvidenceYesGroup(ProsecutorServeEvidenceYesGroup.prosecutorServeEvidenceYesGroup().withProsecutorServeEvidenceDetails("Y").build())
                                                .withSlaveryOrExploitationYesGroup(SlaveryOrExploitationYesGroup.slaveryOrExploitationYesGroup().withSlaveryOrExploitationDetails("Y").build())
                                                .build())

                                        .build())
                        .build())
                .build();
        final PetFormData result = baseCpsServeMaterialHandler.convertPetFormData(petFormData);
        assertThat(result.getDefence().getDefendants(), hasSize(1));
        assertThat(result.getDefence().getDefendants().get(0).getId(), is(defendantId));
        assertThat(result.getProsecution().getWitnesses(), hasSize(1));
        assertThat(result.getProsecution().getDynamicFormAnswers().getProsecutorGroup().getDisplayEquipmentYesGroup().getDisplayEquipmentDetails(), is("Y"));

    }

}