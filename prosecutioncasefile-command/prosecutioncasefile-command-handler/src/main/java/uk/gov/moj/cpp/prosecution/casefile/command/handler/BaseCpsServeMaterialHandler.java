package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.ProcessReceivedCpsServeBcm;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.SubmissionStatus;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ApplicationsForDirectionsGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsDefendantOffences;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsOffences;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Defence;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Defendants;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.DisplayEquipmentYesGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.DynamicFormAnswers;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PendingLinesOfEnquiryYesGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PetFormData;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PointOfLawYesGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Prosecution;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionComplianceNoGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionComplianceYesGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutorGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutorServeEvidenceYesGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SlaveryOrExploitationYesGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.VariationStandardDirectionsProsecutorYesGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Witnesses;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class BaseCpsServeMaterialHandler {

    @Inject
    protected EventSource eventSource;

    @Inject
    protected AggregateService aggregateService;

    @Inject
    protected ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    protected JsonObjectToObjectConverter jsonObjectToObjectConverter;

    protected uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServeBcmProcessed convertToCpsServeBcmSubmitted(final ProcessReceivedCpsServeBcm processReceivedCpsServeBcm, final SubmissionStatus submissionStatus) {
        return uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServeBcmProcessed.receivedCpsServeBcmProcessed()
                .withProsecutionCaseSubject(convertProsecutionCaseSubject(processReceivedCpsServeBcm.getProsecutionCaseSubject()))
                .withSubmissionId(processReceivedCpsServeBcm.getSubmissionId())
                .withSubmissionStatus(convertSubmissionStatus(submissionStatus))
                .withCpsDefendantOffences(convertCpsDefendantOffences(processReceivedCpsServeBcm.getCpsDefendantOffences()))
                .build();
    }

    protected uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionCaseSubject convertProsecutionCaseSubject(final uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.ProsecutionCaseSubject prosecutionCaseSubject) {
        return uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionCaseSubject
                .prosecutionCaseSubject()
                .withUrn(prosecutionCaseSubject.getUrn())
                .withProsecutingAuthority(prosecutionCaseSubject.getProsecutingAuthority())
                .build();
    }

    protected uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionCaseSubject convertCpsProsecutionCaseSubject(final uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.ProsecutionCaseSubject prosecutionCaseSubject) {
        return uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionCaseSubject
                .prosecutionCaseSubject()
                .withUrn(prosecutionCaseSubject.getUrn())
                .withProsecutingAuthority(prosecutionCaseSubject.getProsecutingAuthority())
                .build();
    }

    protected List<CpsDefendantOffences> convertCpsDefendantOffences(final List<uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.CpsDefendantOffences> cpsDefendantOffences) {
        final List<uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsDefendantOffences> returnList = new ArrayList<>();
        cpsDefendantOffences.forEach(o ->
                returnList.add(uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsDefendantOffences
                        .cpsDefendantOffences()
                        .withProsecutorDefendantId(o.getProsecutorDefendantId())
                        .withCpsDefendantId(o.getCpsDefendantId())
                        .withAsn(o.getAsn())
                        .withTitle(o.getTitle())
                        .withForename(o.getForename())
                        .withForename2(o.getForename2())
                        .withForename3(o.getForename3())
                        .withSurname(o.getSurname())
                        .withDateOfBirth(o.getDateOfBirth())
                        .withOrganisationName(o.getOrganisationName())
                        .withCpsOffenceDetails(convertCpsOffence(o.getCpsOffenceDetails()))
                        .build())
        );
        return returnList;
    }

    protected List<uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsOffenceDetails> convertCpsOffence(final List<uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.CpsOffenceDetails> offences) {
        final List<uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsOffenceDetails> returnList = new ArrayList<>();
        offences.forEach(o ->
                returnList.add(uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsOffenceDetails
                        .cpsOffenceDetails()
                        .withCjsOffenceCode(o.getCjsOffenceCode())
                        .withOffenceDate(o.getOffenceDate())
                        .withOffenceWording(o.getOffenceWording())
                        .build())
        );
        return returnList;
    }

    protected uk.gov.moj.cps.prosecutioncasefile.domain.event.PetFormData convertPetFormData(final PetFormData petFormData) {
        return uk.gov.moj.cps.prosecutioncasefile.domain.event.PetFormData
                .petFormData()
                .withDefence(convertDefence(petFormData.getDefence()))
                .withProsecution(convertProsecution(petFormData.getProsecution()))
                .build();
    }

    protected uk.gov.moj.cps.prosecutioncasefile.domain.event.Defence convertDefence(final Defence defence) {
        return uk.gov.moj.cps.prosecutioncasefile.domain.event.Defence
                .defence()
                .withDefendants(convertDefandants(defence.getDefendants()))
                .build();
    }

    protected List<uk.gov.moj.cps.prosecutioncasefile.domain.event.Defendants> convertDefandants(final List<Defendants> defendants) {
        final List<uk.gov.moj.cps.prosecutioncasefile.domain.event.Defendants> returnList = new ArrayList<>();
        defendants.forEach(o ->
                returnList.add(uk.gov.moj.cps.prosecutioncasefile.domain.event.Defendants
                        .defendants()
                        .withId(o.getId())
                        .withProsecutorDefendantId(o.getProsecutorDefendantId())
                        .withCpsDefendantId(o.getCpsDefendantId())
                        .withCpsOffences(convertCpsOffences(o.getCpsOffences()))
                        .build()));
        return returnList;
    }

    protected List<uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsOffences> convertCpsOffences(final List<CpsOffences> cpsOffences) {
        final List<uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsOffences> returnList = new ArrayList<>();
        cpsOffences.forEach(o ->
                returnList.add(uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsOffences
                        .cpsOffences()
                        .withOffenceCode(o.getOffenceCode())
                        .withDate(o.getDate())
                        .withWording(o.getWording())
                        .build()));
        return returnList;
    }

    protected uk.gov.moj.cps.prosecutioncasefile.domain.event.Prosecution convertProsecution(final Prosecution prosecution) {
        return uk.gov.moj.cps.prosecutioncasefile.domain.event.Prosecution
                .prosecution()
                .withWitnesses(convertProsecutionWitness(prosecution.getWitnesses()))
                .withDynamicFormAnswers(convertDynamicAnswers(prosecution.getDynamicFormAnswers()))
                .build();
    }

    protected uk.gov.moj.cps.prosecutioncasefile.domain.event.DynamicFormAnswers convertDynamicAnswers(final DynamicFormAnswers dynamicAnswers) {
        return uk.gov.moj.cps.prosecutioncasefile.domain.event.DynamicFormAnswers
                .dynamicFormAnswers()
                .withProsecutorGroup(convertProsecutorGroup(dynamicAnswers.getProsecutorGroup()))
                .withApplicationsForDirectionsGroup(convertAppDirectGroup(dynamicAnswers.getApplicationsForDirectionsGroup()))
                .build();
    }

    protected uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutorGroup convertProsecutorGroup(final ProsecutorGroup prosecutorGroup) {
        return uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutorGroup
                .prosecutorGroup()
                .withDisplayEquipment(prosecutorGroup.getDisplayEquipment())
                .withDisplayEquipmentYesGroup(convertDisplayEquipmentYes(prosecutorGroup.getDisplayEquipmentYesGroup()))
                .withPendingLinesOfEnquiry(prosecutorGroup.getPendingLinesOfEnquiry())
                .withPendingLinesOfEnquiryYesGroup(convertPendingLinesOfEnquiryYes(prosecutorGroup.getPendingLinesOfEnquiryYesGroup()))
                .withPointOfLaw(prosecutorGroup.getPointOfLaw())
                .withPointOfLawYesGroup(convertPointofLawYes(prosecutorGroup.getPointOfLawYesGroup()))
                .withProsecutionCompliance(prosecutorGroup.getProsecutionCompliance())
                .withProsecutionComplianceNoGroup(convertProsecutionComplianceNo(prosecutorGroup.getProsecutionComplianceNoGroup()))
                .withProsecutionComplianceYesGroup(convertProsecutionComplianceYes(prosecutorGroup.getProsecutionComplianceYesGroup()))
                .withProsecutorServeEvidence(prosecutorGroup.getProsecutorServeEvidence())
                .withProsecutorServeEvidenceYesGroup(convertProsecutorServeEvidence(prosecutorGroup.getProsecutorServeEvidenceYesGroup()))
                .withRelyOn(prosecutorGroup.getRelyOn())
                .withSlaveryOrExploitation(prosecutorGroup.getSlaveryOrExploitation())
                .withSlaveryOrExploitationYesGroup(convertSlaveryOrExploitationYes(prosecutorGroup.getSlaveryOrExploitationYesGroup()))
                .build();
    }

    protected uk.gov.moj.cps.prosecutioncasefile.domain.event.ApplicationsForDirectionsGroup convertAppDirectGroup(final ApplicationsForDirectionsGroup appForDirectionsGroup) {
        return uk.gov.moj.cps.prosecutioncasefile.domain.event.ApplicationsForDirectionsGroup
                .applicationsForDirectionsGroup()
                .withGroundRulesQuestioning(appForDirectionsGroup.getGroundRulesQuestioning())
                .withVariationStandardDirectionsProsecutor(appForDirectionsGroup.getVariationStandardDirectionsProsecutor())
                .withVariationStandardDirectionsProsecutorYesGroup((convertVariationStdProsecutionYes(appForDirectionsGroup.getVariationStandardDirectionsProsecutorYesGroup())))
                .build();
    }

    protected List<uk.gov.moj.cps.prosecutioncasefile.domain.event.Witnesses> convertProsecutionWitness(final List<Witnesses> witnesses) {
        final List<uk.gov.moj.cps.prosecutioncasefile.domain.event.Witnesses> returnList = new ArrayList<>();
        witnesses.forEach(o ->
                returnList.add(uk.gov.moj.cps.prosecutioncasefile.domain.event.Witnesses
                        .witnesses()
                        .withFirstName(o.getFirstName())
                        .withLastName(o.getLastName())
                        .withAge(o.getAge())
                        .withInterpreterRequired(o.getInterpreterRequired())
                        .withLanguageAndDialect(o.getLanguageAndDialect())
                        .withSpecialOtherMeasuresRequired(o.getSpecialOtherMeasuresRequired())
                        .withMeasuresRequired(o.getMeasuresRequired())
                        .withProsecutionProposesWitnessAttendInPerson(o.getProsecutionProposesWitnessAttendInPerson())
                        .build())
        );
        return returnList;
    }

    protected uk.gov.moj.cps.prosecutioncasefile.domain.event.DisplayEquipmentYesGroup convertDisplayEquipmentYes(final DisplayEquipmentYesGroup displayEquipmentYesGroup) {
        return uk.gov.moj.cps.prosecutioncasefile.domain.event.DisplayEquipmentYesGroup
                .displayEquipmentYesGroup()
                .withDisplayEquipmentDetails(displayEquipmentYesGroup.getDisplayEquipmentDetails())
                .build();
    }

    protected uk.gov.moj.cps.prosecutioncasefile.domain.event.VariationStandardDirectionsProsecutorYesGroup convertVariationStdProsecutionYes(final VariationStandardDirectionsProsecutorYesGroup variationStandardsDirections) {
        return uk.gov.moj.cps.prosecutioncasefile.domain.event.VariationStandardDirectionsProsecutorYesGroup
                .variationStandardDirectionsProsecutorYesGroup()
                .withVariationStandardDirectionsProsecutorYesGroupDetails(variationStandardsDirections.getVariationStandardDirectionsProsecutorYesGroupDetails())
                .build();
    }

    protected uk.gov.moj.cps.prosecutioncasefile.domain.event.PendingLinesOfEnquiryYesGroup convertPendingLinesOfEnquiryYes(final PendingLinesOfEnquiryYesGroup pendingLinesOfEnquiry) {
        return uk.gov.moj.cps.prosecutioncasefile.domain.event.PendingLinesOfEnquiryYesGroup
                .pendingLinesOfEnquiryYesGroup()
                .withPendingLinesOfEnquiryYesGroup(pendingLinesOfEnquiry.getPendingLinesOfEnquiryYesGroup())
                .build();
    }

    protected uk.gov.moj.cps.prosecutioncasefile.domain.event.PointOfLawYesGroup convertPointofLawYes(final PointOfLawYesGroup pointOfLawYesGroup) {
        return uk.gov.moj.cps.prosecutioncasefile.domain.event.PointOfLawYesGroup
                .pointOfLawYesGroup()
                .withPointOfLawDetails(pointOfLawYesGroup.getPointOfLawDetails())
                .build();
    }

    protected uk.gov.moj.cps.prosecutioncasefile.domain.event.SlaveryOrExploitationYesGroup convertSlaveryOrExploitationYes(final SlaveryOrExploitationYesGroup slaveryOrExploitation) {
        return uk.gov.moj.cps.prosecutioncasefile.domain.event.SlaveryOrExploitationYesGroup
                .slaveryOrExploitationYesGroup()
                .withSlaveryOrExploitationDetails(slaveryOrExploitation.getSlaveryOrExploitationDetails())
                .build();
    }

    protected uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionComplianceNoGroup convertProsecutionComplianceNo(final ProsecutionComplianceNoGroup prosecutionCompliance) {
        return uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionComplianceNoGroup
                .prosecutionComplianceNoGroup()
                .withProsecutionComplianceDetailsNo(prosecutionCompliance.getProsecutionComplianceDetailsNo())
                .build();
    }

    protected uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionComplianceYesGroup convertProsecutionComplianceYes(final ProsecutionComplianceYesGroup prosecutionCompliance) {
        return uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionComplianceYesGroup
                .prosecutionComplianceYesGroup()
                .withProsecutionComplianceDetailsYes(prosecutionCompliance.getProsecutionComplianceDetailsYes())
                .build();
    }

    protected uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutorServeEvidenceYesGroup convertProsecutorServeEvidence(final ProsecutorServeEvidenceYesGroup prosecutorServeEvidence) {
        return uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutorServeEvidenceYesGroup
                .prosecutorServeEvidenceYesGroup()
                .withProsecutorServeEvidenceDetails(prosecutorServeEvidence.getProsecutorServeEvidenceDetails())
                .build();
    }

    protected uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmissionStatus convertSubmissionStatus(final SubmissionStatus submissionStatus) {
        return uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmissionStatus.valueOf(submissionStatus.name());
    }
}
