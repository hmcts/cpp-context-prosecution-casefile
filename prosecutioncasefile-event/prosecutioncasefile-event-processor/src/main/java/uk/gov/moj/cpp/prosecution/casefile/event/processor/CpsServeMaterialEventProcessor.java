package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.nameUUIDFromBytes;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.validator.util.ValidatorUtils.getValueAsString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue.problemValue;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError.CASE_URN_NOT_FOUND;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsServeBcmSubmitted.cpsServeBcmSubmitted;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsServePtphSubmitted.cpsServePtphSubmitted;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmissionStatus.PENDING;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmissionStatus.SUCCESS;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmissionStatus.SUCCESS_WITH_WARNINGS;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.PendingCpsServeBcmExpiration;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.PendingCpsServePetExpiration;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.service.ProsecutionCaseQueryService;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.DateUtil;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ProgressionService;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.AdvAtPthp;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.AdvForTrial;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.AdvocateAtPTPH;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.AdvocateForTrial;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.BcmDefendants;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseProgressionOfficer;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Contacts;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsOffice;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsServeBcmSubmitted;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsServeCotrSubmitted;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsServeMaterialStatusUpdated;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsServePtphSubmitted;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsUpdateCotrSubmitted;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.FormData;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.IntermediaryForWitness;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.OfficerInCase;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsCaseProgOfficer;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutorOffences;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PtphFormData;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PtphHeader;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PtphWitnesses;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServeBcmProcessed;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServeCotrProcessed;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServePetProcessed;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServePtphProcessed;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsUpdateCotrProcessed;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ReviewingLawyer;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmissionStatus;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3776", "squid:MethodCyclomaticComplexity"})
@ServiceComponent(EVENT_PROCESSOR)
public class CpsServeMaterialEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CpsServeMaterialEventProcessor.class);

    private static final String VALIDATION_DATA = "validationData";

    private static final String PTPH_VALIDATION_DATA = "ptphValidationData";
    private static final String STATUS_UPDATED_PUBLIC_EVENT = "public.prosecutioncasefile.cps-serve-material-status-updated";
    private static final String SUBMISSION_ID = "submissionId";
    private static final String CASE_ID = "caseId";
    private static final String PET_DEFENDANTS = "petDefendants";
    private static final String PET_FORM_DATA = "petFormData";
    private static final String USER_NAME = "userName";
    private static final String SUBMISSION_STATUS = "submissionStatus";
    private static final String PROSECUTION_CASE_SUBJECT = "prosecutionCaseSubject";
    private static final String CPS_DEFENDANT_OFFENCES = "cpsDefendantOffences";
    private static final String REVIEWING_LAWYER = "reviewingLawyer";
    private static final String PROSECUTION_CASE_PROGRESSION_OFFICER = "prosecutionCaseProgressionOfficer";
    private static final String CJS_OFFENCE_CODE = "cjsOffenceCode";
    private static final String CPS_OFFENCE_DETAILS = "cpsOffenceDetails";
    private static final String CPS_DEFENDANT_ID = "cpsDefendantId";
    private static final String CASE_URN = "urn";
    private static final String ID = "id";
    private static final String DEFENDANTS = "defendants";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String DEFENDANT_IDS = "defendantIds";
    private static final String PROSECUTION_CASE = "prosecutionCase";
    private static final String DEFENDANT_SUBJECT = "defendantSubject";
    public static final String ADDITIONAL_INFORMATION = "additionalInformation";
    private static final String IS_YOUTH = "isYouth";

    private static final List<SubmissionStatus> ERROR_SUBMISSION_LIST = asList(SubmissionStatus.REJECTED, SubmissionStatus.EXPIRED, SubmissionStatus.FAILED);
    private static final String TAG = "tag";
    private static final String COTR_ID = "cotrId";
    private static final String EVIDENCE_PRE_PTPH = "evidencePrePTPH";
    private static final String EVIDENCE_POST_PTPH = "evidencePostPTPH";
    private static final String OTHER_INFORMATION = "otherInformation";
    private static final String OTHER_AREAS_BEFORE_PTPH = "otherAreasBeforePtph";
    private static final String OTHER_AREAS_AFTER_PTPH = "otherAreasAfterPtph";
    private static final String ANY_OTHER = "anyOther";
    private static final String PROSECUTOR_OFFENCES = "prosecutorOffences";
    private static final String OFFENCE_CODE = "offenceCode";
    private static final String WORDING = "wording";
    private static final String DATE = "date";
    public static final String PTPH_HEADER = "ptphHeader";
    public static final String PRINCIPAL_CHARGES = "principalCharges";
    public static final String CASE_PROGRESSION_OFFICER = "caseProgressionOfficer";
    public static final String ADVOCATE_AT_PTPH = "advocateAtPTPH";
    public static final String ADVOCATE_FOR_TRIAL = "advocateForTrial";
    public static final String CONTACTS = "contacts";
    public static final String PROSECUTION_INFORMATION = "prosecutionInformation";
    public static final String WITNESSES = "witnesses";
    public static final String ADV_AT_PTHP = "advAtPthp";
    public static final String ADV_FOR_TRIAL = "advForTrial";
    public static final String PROS_CASE_PROG_OFFICER = "prosCaseProgOfficer";
    public static final String CPS_OFFICE = "cpsOffice";
    public static final String OFFICER_IN_CASE = "officerInCase";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String INTERMEDIARY_FIRST_NAME = "intermediaryFirstName";
    public static final String INTERMEDIARY_LAST_NAME = "intermediaryLastName";
    public static final String COLLAR_NUMBER = "collarNumber";
    public static final String RANK = "rank";
    public static final String CPS_DEFENDANT = "cpsDefendant";
    public static final String PTPH_ADVOCATE = "ptphAdvocate";
    public static final String TRIAL_ADVOCATE = "trialAdvocate";
    public static final String OFFICER_IN_THE_CASE = "officerInTheCase";
    public static final String DRAFT_INDICTMENT = "draftIndictment";
    public static final String DRAFT_INDICTMENT_NOTES = "draftIndictmentNotes";
    public static final String SUMMARY_OF_CIRCUMSTANCES = "summaryOfCircumstances";
    public static final String SUMMARY_OF_CIRCUMSTANCES_NOTES = "summaryOfCircumstancesNotes";
    public static final String STATEMENTS_FOR_P_AND_I_CM = "statementsForPAndICm";
    public static final String STATEMENTS_FOR_P_AND_I_CM_NOTES = "statementsForPAndICmNotes";
    public static final String EXHIBITS_FOR_P_AND_I_CM = "exhibitsForPAndICm";
    public static final String EXHIBITS_FOR_P_AND_I_CM_NOTES = "exhibitsForPAndICmNotes";
    public static final String CCTV = "cctv";
    public static final String CCTV_NOTES = "cctvNotes";
    public static final String STREAMLINED_FORENSIC_REPORT = "streamlinedForensicReport";
    public static final String STREAMLINED_FORENSIC_REPORT_NOTES = "streamlinedForensicReportNotes";
    public static final String MEDICAL_EVIDENCE = "medicalEvidence";
    public static final String MEDICAL_EVIDENCE_NOTES = "medicalEvidenceNotes";
    public static final String EXPERT_EVIDENCE = "expertEvidence";
    public static final String EXPERT_EVIDENCE_NOTES = "expertEvidenceNotes";
    public static final String BAD_CHARACTER = "badCharacter";
    public static final String BAD_CHARACTER_NOTES = "badCharacterNotes";
    public static final String HEARSAY = "hearsay";
    public static final String HEARSAY_NOTES = "hearsayNotes";
    public static final String SPECIAL_MEASURES = "specialMeasures";
    public static final String SPECIAL_MEASURES_NOTES = "specialMeasuresNotes";
    public static final String CRIMINAL_RECORD = "criminalRecord";
    public static final String CRIMINAL_RECORD_NOTES = "criminalRecordNotes";
    public static final String VICTIM_PERSONAL_STATEMENT = "victimPersonalStatement";
    public static final String VICTIM_PERSONAL_STATEMENT_NOTES = "victimPersonalStatementNotes";
    public static final String DISCLOSURE_MANAGEMENT_DOC = "disclosureManagementDoc";
    public static final String DISCLOSURE_MANAGEMENT_DOC_NOTES = "disclosureManagementDocNotes";
    public static final String THIRD_PARTY = "thirdParty";
    public static final String THIRD_PARTY_NOTES = "thirdPartyNotes";
    public static final String REVIEW_DISCLOSABLE_MATERIAL = "reviewDisclosableMaterial";
    public static final String REVIEW_DISCLOSABLE_MATERIAL_NOTES = "reviewDisclosableMaterialNotes";
    public static final String PARTICULARS_OF_ANY_RELATED_CRIMINAL_PROCEEDINGS = "particularsOfAnyRelatedCriminalProceedings";
    public static final String PARTICULARS_OF_ANY_FAMILY = "particularsOfAnyFamily";
    public static final String RELEVANT_DISPUTED_ISSUE = "relevantDisputedIssue";
    public static final String DEFENCE_INFORMATION = "defenceInformation";

    private static final String TRIAL_DATE = "trialDate";
    private static final String LAST_RECORDED_TIME_ESTIMATE = "lastRecordedTimeEstimate";
    private static final String HAS_ALL_EVIDENCE_TOBE_RELIED_ON_BEEN_SERVED = "hasAllEvidenceToBeReliedOnBeenServed";
    private static final String HAS_ALL_EVIDENCE_TOBE_RELIED_ON_BEEN_SERVED_DETAILS = "hasAllEvidenceToBeReliedOnBeenServedDetails";
    private static final String HAS_ALL_DISCLOSURE_BEEN_PROVIDED = "hasAllDisclosureBeenProvided";
    private static final String HAS_ALL_DISCLOSURE_BEEN_PROVIDED_DETAILS = "hasAllDisclosureBeenProvidedDetails";
    private static final String HAVE_OTHER_DIRECTIONS_BEEN_COMPLIED_WITH = "haveOtherDirectionsBeenCompliedWith";
    private static final String HAVE_OTHER_DIRECTIONS_BEEN_COMPLIED_WITH_DETAILS = "haveOtherDirectionsBeenCompliedWithDetails";
    private static final String PROSECUTION_WITNESSES = "haveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttend";
    private static final String PROSECUTION_WITNESSES_DETAILS = "haveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttendDetails";
    private static final String WITNESSES_SUMMONSES = "haveAnyWitnessSummonsesRequiredBeenReceivedAndServed";
    private static final String WITNESSES_SUMMONSES_DETAILS = "haveAnyWitnessSummonsesRequiredBeenReceivedAndServedDetails";
    private static final String SPECIAL_MEASURES_FOR_WITNESSES = "haveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolved";
    private static final String SPECIAL_MEASURES_FOR_WITNESSES_DETAILS = "haveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolvedDetails";
    private static final String INTERPRETERS_FOR_WITNESSES = "haveInterpretersForWitnessesBeenArranged";
    private static final String INTERPRETERS_FOR_WITNESSES_DETAILS = "haveInterpretersForWitnessesBeenArrangedDetails";
    private static final String INTERVIEWS = "haveEditedAbeInterviewsBeenPreparedAndAgreed";
    private static final String INTERVIEWS_DETAILS = "haveEditedAbeInterviewsBeenPreparedAndAgreedDetails";
    private static final String STATEMENT_OF_POINTS = "haveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreement";
    private static final String STATEMENT_OF_POINTS_DETAILS = "haveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreementDetails";
    private static final String CASE_READY = "isTheCaseReadyToProceedWithoutDelayBeforeTheJury";
    private static final String CASE_READY_DETAILS = "isTheCaseReadyToProceedWithoutDelayBeforeTheJuryDetails";
    private static final String TIME_ESTIMATE = "isTheTimeEstimateCorrect";
    private static final String TIME_ESTIMATE_DETAILS = "isTheTimeEstimateCorrectDetails";
    private static final String FURTHER_INFORMATION = "furtherInformationToAssistTheCourt";
    private static final String PROSECUTOR_TRIAL_READY = "certifyThatTheProsecutionIsTrialReady";
    private static final String PROSECUTOR_TRIAL_READY_DETAILS = "certifyThatTheProsecutionIsTrialReadyDetails";
    private static final String PTR_VACATED = "applyForThePtrToBeVacated";
    private static final String PTR_VACATED_DETAILS = "applyForThePtrToBeVacatedDetails";
    private static final String PROSECUTION_BY_FORM = "formCompletedOnBehalfOfTheProsecutionBy";
    private static final String CERTIFICATION_DATE = "certificationDate";
    private static final String SOW_REF_VALUE = "MoJ";

    @Inject
    private Sender sender;

    @Inject
    private PendingCpsServePetExpiration pendingCpsServePetExpiration;

    @Inject
    private PendingCpsServeBcmExpiration pendingCpsServeBcmExpiration;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Inject
    private ProsecutionCaseQueryService prosecutionCaseQueryService;

    @Inject
    private ProgressionService progressionService;

    private static final List<String> NOTES_LIST = Lists.newArrayList(DRAFT_INDICTMENT,
            DRAFT_INDICTMENT_NOTES,
            SUMMARY_OF_CIRCUMSTANCES,
            SUMMARY_OF_CIRCUMSTANCES_NOTES,
            STATEMENTS_FOR_P_AND_I_CM,
            STATEMENTS_FOR_P_AND_I_CM_NOTES,
            EXHIBITS_FOR_P_AND_I_CM,
            EXHIBITS_FOR_P_AND_I_CM_NOTES,
            CCTV,
            CCTV_NOTES,
            STREAMLINED_FORENSIC_REPORT,
            STREAMLINED_FORENSIC_REPORT_NOTES,
            MEDICAL_EVIDENCE,
            MEDICAL_EVIDENCE_NOTES,
            EXPERT_EVIDENCE,
            EXPERT_EVIDENCE_NOTES,
            BAD_CHARACTER,
            BAD_CHARACTER_NOTES,
            HEARSAY,
            HEARSAY_NOTES,
            SPECIAL_MEASURES,
            SPECIAL_MEASURES_NOTES,
            CRIMINAL_RECORD,
            CRIMINAL_RECORD_NOTES,
            VICTIM_PERSONAL_STATEMENT,
            VICTIM_PERSONAL_STATEMENT_NOTES,
            DISCLOSURE_MANAGEMENT_DOC,
            DISCLOSURE_MANAGEMENT_DOC_NOTES,
            THIRD_PARTY,
            THIRD_PARTY_NOTES,
            REVIEW_DISCLOSABLE_MATERIAL,
            REVIEW_DISCLOSABLE_MATERIAL_NOTES,
            PARTICULARS_OF_ANY_RELATED_CRIMINAL_PROCEEDINGS,
            PARTICULARS_OF_ANY_FAMILY);

    @Handles("public.stagingprosecutors.cps-serve-pet-received")
    public void handleServePetReceivedPublicEvent(final JsonEnvelope envelope) {
        this.sender.send(envelop(buildProsecutionCaseFileCommandProcessReceivedCpsServePet(envelope))
                .withName("prosecutioncasefile.command.process-received-cps-serve-pet")
                .withMetadataFrom(envelope));
    }

    private JsonObject buildProsecutionCaseFileCommandProcessReceivedCpsServePet(final JsonEnvelope envelope) {
        final JsonObject publicEventCpsServePet = envelope.payloadAsJsonObject();

        final String caseUrn = publicEventCpsServePet.getJsonObject(PROSECUTION_CASE_SUBJECT).getString(CASE_URN);
        final Optional<JsonObject> prosecutionCase = Optional.ofNullable(prosecutionCaseQueryService.getProsecutionCaseByCaseUrn(envelopeFrom(envelope.metadata(), NULL), caseUrn));
        final Optional<String> sowRef = getSowRef(prosecutionCase);
        final List<OffenceReferenceData> validOffences = retrieveOffencesFromReferenceData(publicEventCpsServePet.getJsonArray(CPS_DEFENDANT_OFFENCES), sowRef);

        final UUID caseId = prosecutionCase.map(jsonObject -> fromString((jsonObject.getString(CASE_ID)))).orElse(null);

        final JsonArray cpsDefendantIds = retrieveAndBuildCpsDefendantIdsList(caseId);
        final JsonObjectBuilder commandBuilder = createObjectBuilder()
                .add(SUBMISSION_ID, publicEventCpsServePet.getString(SUBMISSION_ID))
                .add(SUBMISSION_STATUS, publicEventCpsServePet.getString(SUBMISSION_STATUS))
                .add(PROSECUTION_CASE_SUBJECT, publicEventCpsServePet.getJsonObject(PROSECUTION_CASE_SUBJECT))
                .add(CPS_DEFENDANT_OFFENCES, publicEventCpsServePet.getJsonArray(CPS_DEFENDANT_OFFENCES))
                .add(PET_FORM_DATA, publicEventCpsServePet.getJsonObject(PET_FORM_DATA))
                .add(VALIDATION_DATA, buildValidationData(validOffences, cpsDefendantIds))
                .add(IS_YOUTH, publicEventCpsServePet.getBoolean(IS_YOUTH));

        if (publicEventCpsServePet.containsKey(REVIEWING_LAWYER)) {
            commandBuilder.add(REVIEWING_LAWYER, publicEventCpsServePet.getJsonObject(REVIEWING_LAWYER));
        } else {
            commandBuilder.add(PROSECUTION_CASE_PROGRESSION_OFFICER, publicEventCpsServePet.getJsonObject(PROSECUTION_CASE_PROGRESSION_OFFICER));
        }

        if(publicEventCpsServePet.containsKey(ADDITIONAL_INFORMATION)){
            commandBuilder.add(ADDITIONAL_INFORMATION,publicEventCpsServePet.getString(ADDITIONAL_INFORMATION));
        }

        return commandBuilder.build();
    }

    private JsonObject buildProsecutionCaseFileCommandProcessReceivedCpsServeBcm(final JsonEnvelope envelope) {
        final JsonObject publicEventCpsServeBcm = envelope.payloadAsJsonObject();

        final String caseUrn = publicEventCpsServeBcm.getJsonObject(PROSECUTION_CASE_SUBJECT).getString(CASE_URN);
        final Optional<JsonObject> prosecutionCase = Optional.ofNullable(prosecutionCaseQueryService.getProsecutionCaseByCaseUrn(envelopeFrom(envelope.metadata(), NULL), caseUrn));
        final Optional<String> sowRef = getSowRef(prosecutionCase);
        final List<OffenceReferenceData> validOffences = retrieveOffencesFromReferenceData(publicEventCpsServeBcm.getJsonArray(CPS_DEFENDANT_OFFENCES), sowRef);

        final UUID caseId = prosecutionCase.map(jsonObject -> fromString((jsonObject.getString(CASE_ID)))).orElse(null);

        final JsonArray cpsDefendantIds = retrieveAndBuildCpsDefendantIdsList(caseId);
        final JsonObjectBuilder commandBuilder = createObjectBuilder()
                .add(SUBMISSION_ID, publicEventCpsServeBcm.getString(SUBMISSION_ID))
                .add(SUBMISSION_STATUS, publicEventCpsServeBcm.getString(SUBMISSION_STATUS))
                .add(PROSECUTION_CASE_SUBJECT, publicEventCpsServeBcm.getJsonObject(PROSECUTION_CASE_SUBJECT))
                .add(CPS_DEFENDANT_OFFENCES, publicEventCpsServeBcm.getJsonArray(CPS_DEFENDANT_OFFENCES))
                .add(VALIDATION_DATA, buildValidationData(validOffences, cpsDefendantIds));

        addIfSourceItemExist(publicEventCpsServeBcm, TAG, commandBuilder, TAG);
        addIfSourceItemExist(publicEventCpsServeBcm, EVIDENCE_PRE_PTPH, commandBuilder, EVIDENCE_PRE_PTPH);
        addIfSourceItemExist(publicEventCpsServeBcm, EVIDENCE_POST_PTPH, commandBuilder, EVIDENCE_POST_PTPH);
        addIfSourceItemExist(publicEventCpsServeBcm, OTHER_INFORMATION, commandBuilder, OTHER_INFORMATION);

        return commandBuilder.build();
    }

    private JsonObject buildProsecutionCaseFileCommandProcessReceivedCpsServePtph(final JsonEnvelope envelope) {
        final JsonObject publicEventCpsServePtph = envelope.payloadAsJsonObject();

        final String caseUrn = publicEventCpsServePtph.getJsonObject(PROSECUTION_CASE_SUBJECT).getString(CASE_URN);
        final Optional<JsonObject> prosecutionCase = Optional.ofNullable(prosecutionCaseQueryService.getProsecutionCaseByCaseUrn(envelopeFrom(envelope.metadata(), NULL), caseUrn));

        final UUID caseId = prosecutionCase.map(jsonObject -> fromString((jsonObject.getString(CASE_ID)))).orElse(null);

        final JsonArray cpsDefendantIds = retrieveAndBuildCpsDefendantIdsList(caseId);
        final JsonObjectBuilder commandBuilder = createObjectBuilder()
                .add(SUBMISSION_ID, publicEventCpsServePtph.getString(SUBMISSION_ID))
                .add(SUBMISSION_STATUS, publicEventCpsServePtph.getString(SUBMISSION_STATUS))
                .add(PROSECUTION_CASE_SUBJECT, publicEventCpsServePtph.getJsonObject(PROSECUTION_CASE_SUBJECT))
                .add(CPS_DEFENDANT, publicEventCpsServePtph.getJsonArray(CPS_DEFENDANT))
                .add(PTPH_ADVOCATE, publicEventCpsServePtph.getJsonObject(PTPH_ADVOCATE))
                .add(REVIEWING_LAWYER, publicEventCpsServePtph.getJsonObject(REVIEWING_LAWYER))
                .add(PROSECUTION_CASE_PROGRESSION_OFFICER, publicEventCpsServePtph.getJsonObject(PROSECUTION_CASE_PROGRESSION_OFFICER))
                .add(OFFICER_IN_THE_CASE, publicEventCpsServePtph.getJsonObject(OFFICER_IN_THE_CASE))
                .add(CPS_OFFICE, publicEventCpsServePtph.getString(CPS_OFFICE));
        NOTES_LIST.forEach(note -> addNotesType(publicEventCpsServePtph, commandBuilder, note));

        if (nonNull(publicEventCpsServePtph.getJsonObject(TRIAL_ADVOCATE))) {
            commandBuilder.add(TRIAL_ADVOCATE, publicEventCpsServePtph.getJsonObject(TRIAL_ADVOCATE));
        }

        if (isNotEmpty(publicEventCpsServePtph.getJsonArray(WITNESSES))) {
            commandBuilder.add(WITNESSES, publicEventCpsServePtph.getJsonArray(WITNESSES));
        }
        commandBuilder.add(PTPH_VALIDATION_DATA, buildValidationDataForPtph(cpsDefendantIds));

        addIfSourceItemExist(publicEventCpsServePtph, TAG, commandBuilder, TAG);

        return commandBuilder.build();
    }

    private void addNotesType(final JsonObject publicEventCpsServePtph, final JsonObjectBuilder commandBuilder, String key) {
        if (isNotEmpty(getValueAsString(publicEventCpsServePtph, key))) {
            commandBuilder.add(key, publicEventCpsServePtph.getString(key));
        }
    }


    private void addIfSourceItemExist(final JsonObject source, final String sourceKey, final JsonObjectBuilder destination, final String destKey) {

        if (source.containsKey(sourceKey)) {
            destination.add(destKey, source.getString(sourceKey));
        }
    }


    @Handles("prosecutioncasefile.events.received-cps-serve-pet-processed")
    public void handleCpsServePetReceivedPrivateEvent(final Envelope<ReceivedCpsServePetProcessed> envelope) {
        final ReceivedCpsServePetProcessed servePetReceived = envelope.payload();
        final SubmissionStatus submissionStatus = servePetReceived.getSubmissionStatus();

        if (nonNull(submissionStatus)) {
            final String caseUrn = servePetReceived.getProsecutionCaseSubject().getUrn();
            final UUID timerUUID = nameUUIDFromBytes(caseUrn.getBytes(UTF_8));
            if (submissionStatus.equals(PENDING)) {
                pendingCpsServePetExpiration.startCpsServePetTimer(timerUUID, envelope.metadata());
            } else {
                pendingCpsServePetExpiration.cancelCpsServePetTimer(timerUUID);
                if (submissionStatus.equals(SUCCESS)
                        || submissionStatus.equals(SUCCESS_WITH_WARNINGS)) {

                    sendCpsServePetSubmittedPublicEvent(envelope);
                }
                sendCpsServePetStatusUpdatedPublicEvent(envelope);
            }
        }
    }

    @Handles("prosecutioncasefile.events.received-cps-serve-bcm-processed")
    public void handleCpsServeBcmReceivedPrivateEvent(final Envelope<ReceivedCpsServeBcmProcessed> envelope) {
        final ReceivedCpsServeBcmProcessed receivedCpsServeBcmProcessed = envelope.payload();
        final SubmissionStatus submissionStatus = receivedCpsServeBcmProcessed.getSubmissionStatus();

        if (nonNull(submissionStatus)) {
            final String caseUrn = receivedCpsServeBcmProcessed.getProsecutionCaseSubject().getUrn();
            final UUID timerUUID = nameUUIDFromBytes(caseUrn.getBytes(UTF_8));
            if (submissionStatus.equals(PENDING)) {
                pendingCpsServeBcmExpiration.startCpsServeBcmTimer(timerUUID, envelope.metadata());
            } else {
                pendingCpsServeBcmExpiration.cancelCpsServeBcmTimer(timerUUID);
                if (submissionStatus.equals(SUCCESS)
                        || submissionStatus.equals(SUCCESS_WITH_WARNINGS)) {
                    sendCpsServeBcmSubmittedPublicEvent(envelope);
                }
                sendCpsServeBcmStatusUpdatedPublicEvent(envelope);
            }
        }
    }

    @Handles("prosecutioncasefile.events.received-cps-serve-ptph-processed")
    public void handleCpsServePtphReceivedPrivateEvent(final Envelope<ReceivedCpsServePtphProcessed> envelope) {
        final ReceivedCpsServePtphProcessed receivedCpsServePtphProcessed = envelope.payload();
        final SubmissionStatus submissionStatus = receivedCpsServePtphProcessed.getSubmissionStatus();

        if (nonNull(submissionStatus) && (submissionStatus.equals(SUCCESS) || submissionStatus.equals(SUCCESS_WITH_WARNINGS))) {
            sendCpsServePtphSubmittedPublicEvent(envelope);
        }
        final CpsServeMaterialStatusUpdated.Builder statusPublicEventBuilder = CpsServeMaterialStatusUpdated.cpsServeMaterialStatusUpdated()
                .withSubmissionId(receivedCpsServePtphProcessed.getSubmissionId())
                .withSubmissionStatus(receivedCpsServePtphProcessed.getSubmissionStatus())
                .withErrors(receivedCpsServePtphProcessed.getErrors());
        updateCpsServeMaterialStatusUpdatedWithErrorsAndWarnings(statusPublicEventBuilder, submissionStatus, receivedCpsServePtphProcessed.getErrors());
        sendCpsServePtphStatusUpdatedPublicEvent(statusPublicEventBuilder, envelope);
    }

    private void sendCpsServePtphStatusUpdatedPublicEvent(final CpsServeMaterialStatusUpdated.Builder statusPublicEventBuilder, final Envelope<ReceivedCpsServePtphProcessed> envelope) {
        final Metadata metadata = JsonEnvelope.metadataFrom(envelope.metadata())
                .withName(STATUS_UPDATED_PUBLIC_EVENT)
                .build();


        sender.send(Envelope.envelopeFrom(metadata, statusPublicEventBuilder.build()));
    }

    private void sendCpsServePtphSubmittedPublicEvent(final Envelope<ReceivedCpsServePtphProcessed> envelope) {
        final ReceivedCpsServePtphProcessed receivedCpsServePtphProcessed = envelope.payload();

        final Metadata metadata = JsonEnvelope.metadataFrom(envelope.metadata())
                .withName("public.prosecutioncasefile.cps-serve-ptph-submitted")
                .build();
        sender.send(Envelope.envelopeFrom(metadata, buildCpsServePtphSubmitted(receivedCpsServePtphProcessed)));
    }

    private CpsServePtphSubmitted buildCpsServePtphSubmitted(final ReceivedCpsServePtphProcessed receivedCpsServePtphProcessed) {

        String userName = null;
        final Contacts contacts = receivedCpsServePtphProcessed.getPtphFormData().getContacts();
        if (nonNull(contacts.getReviewingLawyer()) &&
                isNotEmpty(contacts.getReviewingLawyer().getFirstName())) {
            userName = contacts.getReviewingLawyer().getFirstName();
            if (isNotEmpty(contacts.getReviewingLawyer().getLastName())) {
                userName = userName + StringUtils.SPACE + contacts.getReviewingLawyer().getLastName();
            }
        }
        return cpsServePtphSubmitted()
                .withSubmissionId(receivedCpsServePtphProcessed.getSubmissionId())
                .withCaseId(receivedCpsServePtphProcessed.getCaseId())
                .withFormDefendants(receivedCpsServePtphProcessed.getFormDefendants())
                .withFormData(createStringFromFormDataForPtph(receivedCpsServePtphProcessed.getPtphFormData()))
                .withUserName(userName)
                .build();
    }

    private String createStringFromFormDataForPtph(PtphFormData ptphFormData) {


        //ptphHeader
        final PtphHeader ptphHeader = ptphFormData.getPtphHeader();
        final JsonObjectBuilder ptphHeaderJson = createObjectBuilder();
        ptphHeaderJson.add(CASE_URN, ptphHeader.getUrn());

        //defendants
        final JsonArrayBuilder defendantArrayBuilder = JsonObjects.createArrayBuilder();
        ptphFormData.getPtphFormdefendants().forEach(ptphFormdefendants -> {
            final JsonObjectBuilder defendant = createObjectBuilder();
            defendant.add(ID, ptphFormdefendants.getId());
            if (ptphFormdefendants.getPrincipalCharges() != null) {
                defendant.add(PRINCIPAL_CHARGES, ptphFormdefendants.getPrincipalCharges());
            }
            //Case progression officer
            final CaseProgressionOfficer caseProgressionOfficer = ptphFormdefendants.getCaseProgressionOfficer();
            if (caseProgressionOfficer != null) {
                defendant.add(CASE_PROGRESSION_OFFICER, objectToJsonObjectConverter.convert(caseProgressionOfficer));
            }
            //advocate At PTPH
            final AdvocateAtPTPH advocatePtph = ptphFormdefendants.getAdvocateAtPTPH();
            if (advocatePtph != null) {
                defendant.add(ADVOCATE_AT_PTPH, objectToJsonObjectConverter.convert(advocatePtph));
            }
            //Advocate for Trial
            final AdvocateForTrial advocateForTrial = ptphFormdefendants.getAdvocateForTrial();
            if (advocateForTrial != null) {
                defendant.add(ADVOCATE_FOR_TRIAL, objectToJsonObjectConverter.convert(advocateForTrial));
            }

            defendantArrayBuilder.add(defendant);

        });
        //Contacts
        final JsonObjectBuilder contacts = constructContacts(ptphFormData.getContacts());

        final JsonArrayBuilder defenceInformationArrayBuilder = JsonObjects.createArrayBuilder();
        if (isNotEmpty(ptphFormData.getDefenceInformation())) {
            ptphFormData.getDefenceInformation().forEach(defenceInformation -> defenceInformationArrayBuilder.add(objectToJsonObjectConverter.convert(defenceInformation)));
        }

        return createObjectBuilder()
                .add(PTPH_HEADER, ptphHeaderJson)
                .add(DEFENDANTS, defendantArrayBuilder.build())
                .add(CONTACTS, contacts)
                .add(PROSECUTION_INFORMATION, objectToJsonObjectConverter.convert(ptphFormData.getProsecutionInformation()))
                .add(DEFENCE_INFORMATION, defenceInformationArrayBuilder.build())
                .add(WITNESSES, constructWitnesses(ptphFormData.getPtphWitnesses()))
                .build()
                .toString();
    }

    private JsonObjectBuilder constructContacts(Contacts contacts) {
        final JsonObjectBuilder formContacts = createObjectBuilder();
        //advAtPthp
        final AdvAtPthp advAtpthp = contacts.getAdvAtPthp();
        if (advAtpthp != null) {
            formContacts.add(ADV_AT_PTHP, objectToJsonObjectConverter.convert(advAtpthp));
        }

        //advForTrial
        final AdvForTrial advForTrial = contacts.getAdvForTrial();
        if (advForTrial != null) {
            formContacts.add(ADV_FOR_TRIAL, objectToJsonObjectConverter.convert(advForTrial));
        }
        //reviewingLawyer
        final ReviewingLawyer reviewingLawyer = contacts.getReviewingLawyer();
        if (reviewingLawyer != null) {
            formContacts.add(REVIEWING_LAWYER, objectToJsonObjectConverter.convert(reviewingLawyer));
        }
        //prosCaseProgOfficer
        final ProsCaseProgOfficer caseProgOfficer = contacts.getProsCaseProgOfficer();
        if (caseProgOfficer != null) {
            formContacts.add(PROS_CASE_PROG_OFFICER, objectToJsonObjectConverter.convert(caseProgOfficer));
        }
        //cpsOffice
        final CpsOffice cpsOffice = contacts.getCpsOffice();
        if (cpsOffice != null) {
            formContacts.add(CPS_OFFICE, objectToJsonObjectConverter.convert(cpsOffice));
        }
        //officerInCase
        final OfficerInCase officerInCase = contacts.getOfficerInCase();
        if (officerInCase != null) {
            formContacts.add(OFFICER_IN_CASE, objectToJsonObjectConverter.convert(officerInCase));
        }
        return formContacts;

    }

    private JsonArrayBuilder constructWitnesses(final List<PtphWitnesses> witnessPtphs) {
        final JsonArrayBuilder witnessArrayBuilder = createArrayBuilder();
        witnessPtphs.forEach(witnessPtph -> populateWitnessArrayBuilder(witnessArrayBuilder, witnessPtph));
        return witnessArrayBuilder;
    }

    private void populateWitnessArrayBuilder(final JsonArrayBuilder witnessArrayBuilder, final PtphWitnesses witnessPtph) {
        final JsonObjectBuilder witness = createObjectBuilder();

        if(nonNull(witnessPtph.getId())){
            witness.add(ID, witnessPtph.getId().toString());
        }
        if (nonNull(witnessPtph.getWitnessFirstName())) {
            witness.add(FIRST_NAME, witnessPtph.getWitnessFirstName());
        }
        if (nonNull(witnessPtph.getWitnessLastName())) {
            witness.add(LAST_NAME, witnessPtph.getWitnessLastName());
        }
        final JsonArrayBuilder detailsArrayBuilder = createArrayBuilder();
        if (IntermediaryForWitness.Y.equals(witnessPtph.getIntermediaryForWitness())) {
            if (nonNull(witnessPtph.getFirstNameOfIntermediaryKnownAtPtph())) {
                witness.add(INTERMEDIARY_FIRST_NAME, witnessPtph.getFirstNameOfIntermediaryKnownAtPtph());
            }
            if (nonNull(witnessPtph.getLastNameOfIntermediaryKnownAtPtph())) {
                witness.add(INTERMEDIARY_LAST_NAME, witnessPtph.getLastNameOfIntermediaryKnownAtPtph());
            }
            detailsArrayBuilder.add("INTERMEDIARY");
        }
        if (witnessPtph.getPoliceOfficerSubject() != null) {
            if (isNotEmpty(witnessPtph.getPoliceOfficerSubject().getOfficeCollarNumber())) {
                witness.add(COLLAR_NUMBER, witnessPtph.getPoliceOfficerSubject().getOfficeCollarNumber());
            }
            if (nonNull(witnessPtph.getPoliceOfficerSubject().getOfficerInCharge()) && witnessPtph.getPoliceOfficerSubject().getOfficerInCharge()){
                detailsArrayBuilder.add("OIC");
            }
            witness.add(RANK, witnessPtph.getPoliceOfficerSubject().getOfficerRank());
            detailsArrayBuilder.add("POLICE_OFFICER");
        }

        if (nonNull(witnessPtph.getRelevantDisputedIssue())) {
            witness.add(RELEVANT_DISPUTED_ISSUE, witnessPtph.getRelevantDisputedIssue());
        }
        witness.add("details", detailsArrayBuilder);
        witnessArrayBuilder.add(witness);
    }

    @Handles("public.stagingprosecutors.cps-serve-bcm-received")
    public void handleServeBcmReceivedPublicEvent(final JsonEnvelope envelope) {
        this.sender.send(envelop(buildProsecutionCaseFileCommandProcessReceivedCpsServeBcm(envelope))
                .withName("prosecutioncasefile.command.process-received-cps-serve-bcm")
                .withMetadataFrom(envelope));
    }

    @Handles("public.stagingprosecutors.cps-serve-ptph-received")
    public void handleServePtphReceivedPublicEvent(final JsonEnvelope envelope) {
        this.sender.send(envelop(buildProsecutionCaseFileCommandProcessReceivedCpsServePtph(envelope))
                .withName("prosecutioncasefile.command.process-received-cps-serve-ptph")
                .withMetadataFrom(envelope));
    }

    private void sendCpsServePetSubmittedPublicEvent(final Envelope<ReceivedCpsServePetProcessed> envelope) {
        final ReceivedCpsServePetProcessed servePetReceived = envelope.payload();
        final Metadata metadata = JsonEnvelope.metadataFrom(envelope.metadata())
                .withName("public.prosecutioncasefile.cps-serve-pet-submitted")
                .build();
        final JsonArrayBuilder petDefendantsArrayBuilder = JsonObjects.createArrayBuilder();
        if (nonNull(servePetReceived.getPetDefendants())) {
            servePetReceived
                    .getPetDefendants()
                    .forEach(petDefendant -> {
                        final JsonObjectBuilder builder = createObjectBuilder().add(DEFENDANT_ID, petDefendant.getDefendantId().toString());
                        if (Objects.nonNull(petDefendant.getCpsDefendantId())) {
                            builder.add(CPS_DEFENDANT_ID, petDefendant.getCpsDefendantId());
                        }
                        petDefendantsArrayBuilder.add(builder.build());
                    });
        }

        String userName = null;
        if (nonNull(servePetReceived.getProsecutionCaseProgressionOfficer()) &&
                isNotEmpty(servePetReceived.getProsecutionCaseProgressionOfficer().getName())) {
            userName = servePetReceived.getProsecutionCaseProgressionOfficer().getName();
        } else if (nonNull(servePetReceived.getReviewingLawyer())) {
            userName = servePetReceived.getReviewingLawyer().getName();
        }

        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add(SUBMISSION_ID, String.valueOf(servePetReceived.getSubmissionId()))
                .add(CASE_ID, String.valueOf(servePetReceived.getCaseId()))
                .add(PET_DEFENDANTS, petDefendantsArrayBuilder.build())
                .add(USER_NAME, userName)
                .add(IS_YOUTH, servePetReceived.getIsYouth());

        if (nonNull(servePetReceived.getPetFormData())) {
            payloadBuilder.add(PET_FORM_DATA, objectToJsonObjectConverter.convert(servePetReceived.getPetFormData()).toString());
        }

        final JsonObject payload = payloadBuilder.build();
        LOGGER.info("public.prosecutioncasefile.cps-serve-pet-submitted : {}", payload.getJsonString(SUBMISSION_ID));
        sender.send(Envelope.envelopeFrom(metadata, payload));
    }

    private void sendCpsServeBcmSubmittedPublicEvent(final Envelope<ReceivedCpsServeBcmProcessed> envelope) {
        final ReceivedCpsServeBcmProcessed receivedCpsServeBcmProcessed = envelope.payload();

        final Metadata metadata = JsonEnvelope.metadataFrom(envelope.metadata())
                .withName("public.prosecutioncasefile.cps-serve-bcm-submitted")
                .build();
        sender.send(Envelope.envelopeFrom(metadata, buildCpsServeBcmSubmitted(receivedCpsServeBcmProcessed)));
    }

    private CpsServeBcmSubmitted buildCpsServeBcmSubmitted(final ReceivedCpsServeBcmProcessed receivedCpsServeBcmProcessed) {
        return cpsServeBcmSubmitted()
                .withSubmissionId(receivedCpsServeBcmProcessed.getSubmissionId())
                .withCaseId(receivedCpsServeBcmProcessed.getCaseId())
                .withFormDefendants(receivedCpsServeBcmProcessed.getFormDefendants())
                .withFormData(createStringFromFormDataForBcm(receivedCpsServeBcmProcessed.getFormData()))
                .build();
    }

    private String createStringFromFormDataForBcm(FormData bcmFormData) {
        final JsonArrayBuilder defendantArrayBuilder = JsonObjects.createArrayBuilder();
        bcmFormData.getBcmDefendants().forEach(bcmDefendants -> defendantArrayBuilder.add(createJsonObjectFromBcmDefendants(bcmDefendants)));
        return createObjectBuilder()
                .add(DEFENDANTS, defendantArrayBuilder.build())
                .build()
                .toString();
    }

    private JsonObject createJsonObjectFromBcmDefendants(BcmDefendants bcmDefendants) {
        final JsonObjectBuilder builder = createObjectBuilder();
        if(nonNull(bcmDefendants.getProsecutorOffences())) {
            builder.add(PROSECUTOR_OFFENCES, createProsecutorOffences(bcmDefendants.getProsecutorOffences()));
        }
        if(nonNull(bcmDefendants.getId())){
            builder.add(ID, bcmDefendants.getId().toString());
        }
        if (nonNull(bcmDefendants.getAnyOther())) {
            builder.add(ANY_OTHER, bcmDefendants.getAnyOther());
        }
        if (nonNull(bcmDefendants.getOtherAreasAfterPtph())) {
            builder.add(OTHER_AREAS_AFTER_PTPH, bcmDefendants.getOtherAreasAfterPtph());
        }
        if (nonNull(bcmDefendants.getOtherAreasBeforePtph())) {
            builder.add(OTHER_AREAS_BEFORE_PTPH, bcmDefendants.getOtherAreasBeforePtph());
        }
        return builder.build();
    }

    private JsonArray createProsecutorOffences(List<ProsecutorOffences> prosecutorOffencesList) {
        final JsonArrayBuilder offencesArrayBuilder = JsonObjects.createArrayBuilder();
        prosecutorOffencesList.forEach(prosecutorOffences -> offencesArrayBuilder.add(createObjectBuilder()
                .add(DATE, DateUtil.convertToStringFromLocalDate(prosecutorOffences.getDate()))
                .add(OFFENCE_CODE, prosecutorOffences.getOffenceCode())
                .add(WORDING, prosecutorOffences.getWording())
        ));
        return offencesArrayBuilder.build();
    }

    private void sendCpsServePetStatusUpdatedPublicEvent(final Envelope<ReceivedCpsServePetProcessed> envelope) {
        final ReceivedCpsServePetProcessed servePetReceived = envelope.payload();
        final Metadata metadata = JsonEnvelope.metadataFrom(envelope.metadata())
                .withName(STATUS_UPDATED_PUBLIC_EVENT)
                .build();
        final CpsServeMaterialStatusUpdated.Builder statusPublicEventBuilder = CpsServeMaterialStatusUpdated.cpsServeMaterialStatusUpdated()
                .withSubmissionId(servePetReceived.getSubmissionId())
                .withSubmissionStatus(servePetReceived.getSubmissionStatus());

        updateCpsServeMaterialStatusUpdatedWithErrorsAndWarnings(statusPublicEventBuilder, servePetReceived.getSubmissionStatus(), servePetReceived.getErrors());

        sender.send(Envelope.envelopeFrom(metadata, statusPublicEventBuilder.build()));
    }

    private void updateCpsServeMaterialStatusUpdatedWithErrorsAndWarnings(final CpsServeMaterialStatusUpdated.Builder builder, final SubmissionStatus submissionStatus, final List<Problem> errors) {
        if (ERROR_SUBMISSION_LIST.contains(submissionStatus)) {
            builder.withErrors(errors);
        } else {
            builder.withWarnings(errors);
        }
    }

    private void sendCpsServeBcmStatusUpdatedPublicEvent(final Envelope<ReceivedCpsServeBcmProcessed> envelope) {
        final ReceivedCpsServeBcmProcessed receivedCpsServeBcmProcessed = envelope.payload();
        final Metadata metadata = JsonEnvelope.metadataFrom(envelope.metadata())
                .withName(STATUS_UPDATED_PUBLIC_EVENT)
                .build();

        final CpsServeMaterialStatusUpdated.Builder statusPublicEventBuilder = CpsServeMaterialStatusUpdated.cpsServeMaterialStatusUpdated()
                .withSubmissionId(receivedCpsServeBcmProcessed.getSubmissionId())
                .withSubmissionStatus(receivedCpsServeBcmProcessed.getSubmissionStatus());

        updateCpsServeMaterialStatusUpdatedWithErrorsAndWarnings(statusPublicEventBuilder, receivedCpsServeBcmProcessed.getSubmissionStatus(), receivedCpsServeBcmProcessed.getErrors());

        sender.send(Envelope.envelopeFrom(metadata, statusPublicEventBuilder.build()));
    }

    private JsonObject buildValidationData(final List<OffenceReferenceData> cjsOffences, final JsonArray defendantIds) {
        final JsonArrayBuilder validOffencesArray = createArrayBuilder();

        cjsOffences.forEach(offence -> validOffencesArray.add(offence.getCjsOffenceCode()));

        final JsonObjectBuilder validationDataBuilder = createObjectBuilder()
                .add("validOffences", validOffencesArray.build());

        if (!defendantIds.isEmpty()) {
            validationDataBuilder.add(DEFENDANT_IDS, defendantIds);
        }

        return validationDataBuilder.build();
    }

    private JsonObject buildValidationDataForPtph(final JsonArray defendantIds) {

        final JsonObjectBuilder validationDataBuilder = createObjectBuilder();

        if (!defendantIds.isEmpty()) {
            validationDataBuilder.add(DEFENDANT_IDS, defendantIds);
        }

        return validationDataBuilder.build();
    }

    private List<OffenceReferenceData> retrieveOffencesFromReferenceData(final JsonArray defendantsAsJsonArray, final Optional<String> sowRef) {
        final List<String> cjsOffenceCodeList = defendantsAsJsonArray.getValuesAs(JsonObject.class).stream()
                .flatMap(cpsDefendant -> cpsDefendant.getJsonArray(CPS_OFFENCE_DETAILS).getValuesAs(JsonObject.class).stream().
                        map(cpsOffence -> cpsOffence.getString(CJS_OFFENCE_CODE)))
                .collect(toList());

        return referenceDataQueryService.retrieveOffenceDataList(cjsOffenceCodeList, sowRef);
    }

    private JsonArray retrieveAndBuildCpsDefendantIdsList(final UUID caseId) {
        final JsonArrayBuilder cpsDefendantsBuilder = createArrayBuilder();
        LOGGER.info("retrieveAndBuildCpsDefendantIdsList for caseId : {}", caseId);

        if (caseId != null) {
            final JsonObject prosecutionCaseAsJsonObject = progressionService.getProsecutionCase(caseId);

            final JsonArray defendants = prosecutionCaseAsJsonObject.getJsonObject(PROSECUTION_CASE).getJsonArray(DEFENDANTS);

            for (final JsonValue defendant : defendants) {
                final JsonObject defendantAsJsonObject = (JsonObject) defendant;
                final String cpsDefendantId = defendantAsJsonObject.getString(CPS_DEFENDANT_ID, null);
                if (cpsDefendantId != null) {
                    final String defendantId = defendantAsJsonObject.getString(ID);
                    LOGGER.info("Building Cps defendant id: {} and defendant id: {} validation pair", cpsDefendantId, defendantId);
                    cpsDefendantsBuilder.add(createObjectBuilder()
                            .add(CPS_DEFENDANT_ID, cpsDefendantId)
                            .add(DEFENDANT_ID, defendantId)
                            .build());
                }
            }
        }
        return cpsDefendantsBuilder.build();
    }

    @Handles("public.stagingprosecutors.cps-serve-cotr-received")
    public void handleServeCotrReceivedPublicEvent(final JsonEnvelope envelope) {
        final JsonObject publicEventCpsServeCotr = envelope.payloadAsJsonObject();
        LOGGER.info("public.stagingprosecutors.cps-serve-cotr-received : {}", envelope.toObfuscatedDebugString());

        final String caseUrn = publicEventCpsServeCotr.getJsonObject(PROSECUTION_CASE_SUBJECT).getString(CASE_URN);
        final Optional<JsonObject> prosecutionCase = Optional.ofNullable(prosecutionCaseQueryService.getProsecutionCaseByCaseUrn(envelopeFrom(envelope.metadata(), NULL), caseUrn));

        if (prosecutionCase.isPresent()) {
            final UUID caseId = prosecutionCase.map(jsonObject -> fromString((jsonObject.getString(CASE_ID)))).orElse(null);

            LOGGER.info("Raising private command prosecutioncasefile.command.process-received-cps-serve-cotr");
            this.sender.send(envelop(buildProsecutionCaseFileCommandProcessReceivedCpsServeCotr(publicEventCpsServeCotr, caseId))
                    .withName("prosecutioncasefile.command.process-received-cps-serve-cotr")
                    .withMetadataFrom(envelope));
        } else {
            materialRejectWhenNoCaseUrnFound(envelope, publicEventCpsServeCotr, caseUrn);
        }
    }


    @Handles("public.stagingprosecutors.cps-update-cotr-received")
    public void handleUpdateCotrReceivedPublicEvent(final JsonEnvelope envelope) {

        final JsonObject publicEventCpsUpdateCotr = envelope.payloadAsJsonObject();
        LOGGER.info("public.stagingprosecutors.cps-update-cotr-received : {}", envelope.toObfuscatedDebugString());

        final String caseUrn = publicEventCpsUpdateCotr.getJsonObject(PROSECUTION_CASE_SUBJECT).getString(CASE_URN);
        final Optional<JsonObject> prosecutionCase = Optional.ofNullable(prosecutionCaseQueryService.getProsecutionCaseByCaseUrn(envelopeFrom(envelope.metadata(), NULL), caseUrn));
        if (prosecutionCase.isPresent()) {
            final UUID caseId = prosecutionCase.map(jsonObject -> fromString((jsonObject.getString(CASE_ID)))).orElse(null);

            LOGGER.info("Raising private command prosecutioncasefile.command.process-received-cps-update-cotr");
            this.sender.send(envelop(buildProsecutionCaseFileCommandProcessReceivedCpsUpdateCotr(publicEventCpsUpdateCotr, caseId))
                    .withName("prosecutioncasefile.command.process-received-cps-update-cotr")
                    .withMetadataFrom(envelope));
        }
        else {
            materialRejectWhenNoCaseUrnFound(envelope, publicEventCpsUpdateCotr, caseUrn);
        }

    }

    private JsonObject buildProsecutionCaseFileCommandProcessReceivedCpsUpdateCotr(final JsonObject publicEventCpsUpdateCotr, final UUID caseId) {

        final JsonArray cpsDefendantIds = retrieveAndBuildCpsDefendantIdsList(caseId);
        final JsonObjectBuilder commandBuilder = buildCotrCommand(publicEventCpsUpdateCotr);
        if (!cpsDefendantIds.isEmpty()) {
            commandBuilder.add(VALIDATION_DATA, createObjectBuilder().add(DEFENDANT_IDS, cpsDefendantIds));
        }
        addIfSourceItemExist(publicEventCpsUpdateCotr, TAG, commandBuilder, TAG);
        addIfSourceItemExist(publicEventCpsUpdateCotr, TRIAL_DATE, commandBuilder, TRIAL_DATE);
        addIfSourceItemExist(publicEventCpsUpdateCotr, PROSECUTOR_TRIAL_READY, commandBuilder, PROSECUTOR_TRIAL_READY);
        addIfSourceItemExist(publicEventCpsUpdateCotr, "formCompletedOnBehalfOfProsecutionBy", commandBuilder, "formCompletedOnBehalfOfProsecutionBy");
        addIfSourceItemExist(publicEventCpsUpdateCotr, "furtherProsecutionInformationProvidedAfterCertification", commandBuilder, "furtherProsecutionInformationProvidedAfterCertification");
        addIfSourceItemExist(publicEventCpsUpdateCotr, "date", commandBuilder, CERTIFICATION_DATE);
        addIfSourceItemExist(publicEventCpsUpdateCotr, COTR_ID, commandBuilder, COTR_ID);
        return commandBuilder.build();
    }


    private JsonObject buildProsecutionCaseFileCommandProcessReceivedCpsServeCotr(final JsonObject publicEventCpsServeCotr, final UUID caseId) {

        final JsonArray cpsDefendantIds = retrieveAndBuildCpsDefendantIdsList(caseId);

        final JsonObjectBuilder commandBuilder = buildCotrCommand(publicEventCpsServeCotr);

        commandBuilder.add(VALIDATION_DATA, createObjectBuilder().add(DEFENDANT_IDS, cpsDefendantIds));

        addIfSourceItemExist(publicEventCpsServeCotr, TAG, commandBuilder, TAG);
        addIfSourceItemExist(publicEventCpsServeCotr, TRIAL_DATE, commandBuilder, TRIAL_DATE);
        addIfSourceItemExist(publicEventCpsServeCotr, HAS_ALL_EVIDENCE_TOBE_RELIED_ON_BEEN_SERVED, commandBuilder, HAS_ALL_EVIDENCE_TOBE_RELIED_ON_BEEN_SERVED);
        addIfSourceItemExist(publicEventCpsServeCotr, HAS_ALL_EVIDENCE_TOBE_RELIED_ON_BEEN_SERVED_DETAILS, commandBuilder, HAS_ALL_EVIDENCE_TOBE_RELIED_ON_BEEN_SERVED_DETAILS);
        addIfSourceItemExist(publicEventCpsServeCotr, HAS_ALL_DISCLOSURE_BEEN_PROVIDED, commandBuilder, HAS_ALL_DISCLOSURE_BEEN_PROVIDED);
        addIfSourceItemExist(publicEventCpsServeCotr, HAS_ALL_DISCLOSURE_BEEN_PROVIDED_DETAILS, commandBuilder, HAS_ALL_DISCLOSURE_BEEN_PROVIDED_DETAILS);
        addIfSourceItemExist(publicEventCpsServeCotr, HAVE_OTHER_DIRECTIONS_BEEN_COMPLIED_WITH, commandBuilder, HAVE_OTHER_DIRECTIONS_BEEN_COMPLIED_WITH);
        addIfSourceItemExist(publicEventCpsServeCotr, HAVE_OTHER_DIRECTIONS_BEEN_COMPLIED_WITH_DETAILS, commandBuilder, HAVE_OTHER_DIRECTIONS_BEEN_COMPLIED_WITH_DETAILS);
        addIfSourceItemExist(publicEventCpsServeCotr, PROSECUTION_WITNESSES, commandBuilder, PROSECUTION_WITNESSES);
        addIfSourceItemExist(publicEventCpsServeCotr, PROSECUTION_WITNESSES_DETAILS, commandBuilder, PROSECUTION_WITNESSES_DETAILS);
        addIfSourceItemExist(publicEventCpsServeCotr, WITNESSES_SUMMONSES, commandBuilder, WITNESSES_SUMMONSES);
        addIfSourceItemExist(publicEventCpsServeCotr, WITNESSES_SUMMONSES_DETAILS, commandBuilder, WITNESSES_SUMMONSES_DETAILS);
        addIfSourceItemExist(publicEventCpsServeCotr, SPECIAL_MEASURES_FOR_WITNESSES, commandBuilder, SPECIAL_MEASURES_FOR_WITNESSES);
        addIfSourceItemExist(publicEventCpsServeCotr, SPECIAL_MEASURES_FOR_WITNESSES_DETAILS, commandBuilder, SPECIAL_MEASURES_FOR_WITNESSES_DETAILS);
        addIfSourceItemExist(publicEventCpsServeCotr, INTERPRETERS_FOR_WITNESSES, commandBuilder, INTERPRETERS_FOR_WITNESSES);
        addIfSourceItemExist(publicEventCpsServeCotr, INTERPRETERS_FOR_WITNESSES_DETAILS, commandBuilder, INTERPRETERS_FOR_WITNESSES_DETAILS);
        addIfSourceItemExist(publicEventCpsServeCotr, INTERVIEWS, commandBuilder, INTERVIEWS);
        addIfSourceItemExist(publicEventCpsServeCotr, INTERVIEWS_DETAILS, commandBuilder, INTERVIEWS_DETAILS);
        addIfSourceItemExist(publicEventCpsServeCotr, STATEMENT_OF_POINTS, commandBuilder, STATEMENT_OF_POINTS);
        addIfSourceItemExist(publicEventCpsServeCotr, STATEMENT_OF_POINTS_DETAILS, commandBuilder, STATEMENT_OF_POINTS_DETAILS);
        addIfSourceItemExist(publicEventCpsServeCotr, CASE_READY, commandBuilder, CASE_READY);
        addIfSourceItemExist(publicEventCpsServeCotr, CASE_READY_DETAILS, commandBuilder, CASE_READY_DETAILS);
        addIfSourceItemExist(publicEventCpsServeCotr, TIME_ESTIMATE, commandBuilder, TIME_ESTIMATE);
        addIfSourceItemExist(publicEventCpsServeCotr, TIME_ESTIMATE_DETAILS, commandBuilder, TIME_ESTIMATE_DETAILS);
        addIfSourceItemExist(publicEventCpsServeCotr, FURTHER_INFORMATION, commandBuilder, FURTHER_INFORMATION);
        addIfSourceItemExist(publicEventCpsServeCotr, PROSECUTOR_TRIAL_READY, commandBuilder, PROSECUTOR_TRIAL_READY);
        addIfSourceItemExist(publicEventCpsServeCotr, PROSECUTOR_TRIAL_READY_DETAILS, commandBuilder, PROSECUTOR_TRIAL_READY_DETAILS);
        addIfSourceItemExist(publicEventCpsServeCotr, PTR_VACATED, commandBuilder, PTR_VACATED);
        addIfSourceItemExist(publicEventCpsServeCotr, PTR_VACATED_DETAILS, commandBuilder, PTR_VACATED_DETAILS);
        addIfSourceItemExist(publicEventCpsServeCotr, PROSECUTION_BY_FORM, commandBuilder, PROSECUTION_BY_FORM);
        addIfSourceItemExist(publicEventCpsServeCotr, CERTIFICATION_DATE, commandBuilder, CERTIFICATION_DATE);

        if (publicEventCpsServeCotr.containsKey(LAST_RECORDED_TIME_ESTIMATE)) {
            commandBuilder.add(LAST_RECORDED_TIME_ESTIMATE, publicEventCpsServeCotr.getInt(LAST_RECORDED_TIME_ESTIMATE));
        }

        return commandBuilder.build();
    }

    @Handles("prosecutioncasefile.events.received-cps-serve-cotr-processed")
    public void handleCpsServeCotrReceivedEvent(final Envelope<ReceivedCpsServeCotrProcessed> envelope) {
        final ReceivedCpsServeCotrProcessed receivedCpsServeCotrProcessed = envelope.payload();
        LOGGER.info("prosecutioncasefile.events.received-cps-serve-cotr-processed for SubmissionId: {}", receivedCpsServeCotrProcessed.getSubmissionId());

        final SubmissionStatus submissionStatus = receivedCpsServeCotrProcessed.getSubmissionStatus();

        if (nonNull(submissionStatus)) {
            if (submissionStatus.equals(SUCCESS)
                    || submissionStatus.equals(SUCCESS_WITH_WARNINGS)) {
                sendCpsServeCotrSubmittedPublicEvent(envelope);
            }
            sendCpsServeCotrStatusUpdatedPublicEvent(envelope);
        }
    }

    @Handles("prosecutioncasefile.events.received-cps-update-cotr-processed")
    public void handleCpsUpdateCotrReceivedEvent(final Envelope<ReceivedCpsUpdateCotrProcessed> envelope) {
        final ReceivedCpsUpdateCotrProcessed receivedCpsUpdateCotrProcessed = envelope.payload();
        LOGGER.info("prosecutioncasefile.events.received-cps-update-cotr-processed for CotrId: {}", receivedCpsUpdateCotrProcessed.getCotrId());

        final SubmissionStatus submissionStatus = receivedCpsUpdateCotrProcessed.getSubmissionStatus();

        if (nonNull(submissionStatus)) {
            if (submissionStatus.equals(SUCCESS)
                    || submissionStatus.equals(SUCCESS_WITH_WARNINGS)) {
                sendCpsUpdateCotrSubmittedPublicEvent(envelope);
            }
            sendCpsUpdateCotrStatusUpdatedPublicEvent(envelope);
        }
    }

    private static Optional<String> getSowRef(final Optional<JsonObject> prosecutionCase) {
        return prosecutionCase.flatMap(caseObj -> {
            boolean isCivil = caseObj.getBoolean("isCivil", false);
            return isCivil
                    ? Optional.of(SOW_REF_VALUE)
                    : Optional.empty();
        });
    }

    private void sendCpsServeCotrSubmittedPublicEvent(final Envelope<ReceivedCpsServeCotrProcessed> envelope) {
        final ReceivedCpsServeCotrProcessed receivedCpsServeCotrProcessed = envelope.payload();

        final Metadata metadata = JsonEnvelope.metadataFrom(envelope.metadata())
                .withName("public.prosecutioncasefile.cps-serve-cotr-submitted")
                .build();
        sender.send(Envelope.envelopeFrom(metadata, buildCpsServeCotrSubmitted(receivedCpsServeCotrProcessed)));
    }

    private void sendCpsUpdateCotrSubmittedPublicEvent(final Envelope<ReceivedCpsUpdateCotrProcessed> envelope) {
        final ReceivedCpsUpdateCotrProcessed receivedCpsUpdateCotrProcessed = envelope.payload();

        final Metadata metadata = JsonEnvelope.metadataFrom(envelope.metadata())
                .withName("public.prosecutioncasefile.cps-update-cotr-submitted")
                .build();
        sender.send(Envelope.envelopeFrom(metadata, buildCpsUpdateCotrSubmitted(receivedCpsUpdateCotrProcessed)));
    }

    private CpsUpdateCotrSubmitted buildCpsUpdateCotrSubmitted(final ReceivedCpsUpdateCotrProcessed receivedCpsUpdateCotrProcessed) {
        return CpsUpdateCotrSubmitted.cpsUpdateCotrSubmitted()
                .withSubmissionId(receivedCpsUpdateCotrProcessed.getSubmissionId())
                .withCotrId(receivedCpsUpdateCotrProcessed.getCotrId())
                .withCaseId(receivedCpsUpdateCotrProcessed.getCaseId())
                .withFormDefendants(receivedCpsUpdateCotrProcessed.getFormDefendants())
                .withCertificationDate(receivedCpsUpdateCotrProcessed.getCertificationDate())
                .withCertifyThatTheProsecutionIsTrialReady(receivedCpsUpdateCotrProcessed.getCertifyThatTheProsecutionIsTrialReady())
                .withDefendantSubject(receivedCpsUpdateCotrProcessed.getDefendantSubject())
                .withTag(receivedCpsUpdateCotrProcessed.getTag())
                .withTrialDate(receivedCpsUpdateCotrProcessed.getTrialDate())
                .withFormCompletedOnBehalfOfProsecutionBy(receivedCpsUpdateCotrProcessed.getFormCompletedOnBehalfOfProsecutionBy())
                .withFurtherProsecutionInformationProvidedAfterCertification(receivedCpsUpdateCotrProcessed.getFurtherProsecutionInformationProvidedAfterCertification())
                .withCertifyThatTheProsecutionIsTrialReady(receivedCpsUpdateCotrProcessed.getCertifyThatTheProsecutionIsTrialReady())
                .build();
    }


    private CpsServeCotrSubmitted buildCpsServeCotrSubmitted(final ReceivedCpsServeCotrProcessed receivedCpsServeCotrProcessed) {
        return CpsServeCotrSubmitted.cpsServeCotrSubmitted()
                .withSubmissionId(receivedCpsServeCotrProcessed.getSubmissionId())
                .withCaseId(receivedCpsServeCotrProcessed.getCaseId())
                .withFormDefendants(receivedCpsServeCotrProcessed.getFormDefendants())
                .withApplyForThePtrToBeVacated(receivedCpsServeCotrProcessed.getApplyForThePtrToBeVacated())
                .withApplyForThePtrToBeVacatedDetails(receivedCpsServeCotrProcessed.getApplyForThePtrToBeVacatedDetails())
                .withCertificationDate(receivedCpsServeCotrProcessed.getCertificationDate())
                .withCertifyThatTheProsecutionIsTrialReady(receivedCpsServeCotrProcessed.getCertifyThatTheProsecutionIsTrialReady())
                .withCertifyThatTheProsecutionIsTrialReadyDetails(receivedCpsServeCotrProcessed.getCertifyThatTheProsecutionIsTrialReadyDetails())
                .withDefendantSubject(receivedCpsServeCotrProcessed.getDefendantSubject())
                .withFormCompletedOnBehalfOfTheProsecutionBy(receivedCpsServeCotrProcessed.getFormCompletedOnBehalfOfTheProsecutionBy())
                .withFurtherInformationToAssistTheCourt(receivedCpsServeCotrProcessed.getFurtherInformationToAssistTheCourt())
                .withHasAllDisclosureBeenProvided(receivedCpsServeCotrProcessed.getHasAllDisclosureBeenProvided())
                .withHasAllDisclosureBeenProvidedDetails(receivedCpsServeCotrProcessed.getHasAllDisclosureBeenProvidedDetails())
                .withHasAllEvidenceToBeReliedOnBeenServed(receivedCpsServeCotrProcessed.getHasAllEvidenceToBeReliedOnBeenServed())
                .withHasAllEvidenceToBeReliedOnBeenServedDetails(receivedCpsServeCotrProcessed.getHasAllEvidenceToBeReliedOnBeenServedDetails())
                .withHaveAnyWitnessSummonsesRequiredBeenReceivedAndServed(receivedCpsServeCotrProcessed.getHaveAnyWitnessSummonsesRequiredBeenReceivedAndServed())
                .withHaveAnyWitnessSummonsesRequiredBeenReceivedAndServedDetails(receivedCpsServeCotrProcessed.getHaveAnyWitnessSummonsesRequiredBeenReceivedAndServedDetails())
                .withHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreement(receivedCpsServeCotrProcessed.getHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreement())
                .withHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreementDetails(receivedCpsServeCotrProcessed.getHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreementDetails())
                .withHaveEditedAbeInterviewsBeenPreparedAndAgreed(receivedCpsServeCotrProcessed.getHaveEditedAbeInterviewsBeenPreparedAndAgreed())
                .withHaveEditedAbeInterviewsBeenPreparedAndAgreedDetails(receivedCpsServeCotrProcessed.getHaveEditedAbeInterviewsBeenPreparedAndAgreedDetails())
                .withHaveInterpretersForWitnessesBeenArranged(receivedCpsServeCotrProcessed.getHaveInterpretersForWitnessesBeenArranged())
                .withHaveInterpretersForWitnessesBeenArrangedDetails(receivedCpsServeCotrProcessed.getHaveInterpretersForWitnessesBeenArrangedDetails())
                .withHaveOtherDirectionsBeenCompliedWith(receivedCpsServeCotrProcessed.getHaveOtherDirectionsBeenCompliedWith())
                .withHaveOtherDirectionsBeenCompliedWithDetails(receivedCpsServeCotrProcessed.getHaveOtherDirectionsBeenCompliedWithDetails())
                .withHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolved(receivedCpsServeCotrProcessed.getHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolved())
                .withHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolvedDetails(receivedCpsServeCotrProcessed.getHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolvedDetails())
                .withHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttend(receivedCpsServeCotrProcessed.getHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttend())
                .withHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttendDetails(receivedCpsServeCotrProcessed.getHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttendDetails())
                .withIsTheCaseReadyToProceedWithoutDelayBeforeTheJury(receivedCpsServeCotrProcessed.getIsTheCaseReadyToProceedWithoutDelayBeforeTheJury())
                .withIsTheCaseReadyToProceedWithoutDelayBeforeTheJuryDetails(receivedCpsServeCotrProcessed.getIsTheCaseReadyToProceedWithoutDelayBeforeTheJuryDetails())
                .withIsTheTimeEstimateCorrect(receivedCpsServeCotrProcessed.getIsTheTimeEstimateCorrect())
                .withIsTheTimeEstimateCorrectDetails(receivedCpsServeCotrProcessed.getIsTheTimeEstimateCorrectDetails())
                .withLastRecordedTimeEstimate(receivedCpsServeCotrProcessed.getLastRecordedTimeEstimate())
                .withTag(receivedCpsServeCotrProcessed.getTag())
                .withTrialDate(receivedCpsServeCotrProcessed.getTrialDate())
                .withCaseUrn(receivedCpsServeCotrProcessed.getProsecutionCaseSubject().getUrn())
                .build();
    }

    private void sendCpsServeCotrStatusUpdatedPublicEvent(final Envelope<ReceivedCpsServeCotrProcessed> envelope) {
        final ReceivedCpsServeCotrProcessed receivedCpsServeCotrProcessed = envelope.payload();
        final Metadata metadata = JsonEnvelope.metadataFrom(envelope.metadata())
                .withName(STATUS_UPDATED_PUBLIC_EVENT)
                .build();

        final CpsServeMaterialStatusUpdated.Builder statusPublicEventBuilder = CpsServeMaterialStatusUpdated.cpsServeMaterialStatusUpdated()
                .withSubmissionId(receivedCpsServeCotrProcessed.getSubmissionId())
                .withSubmissionStatus(receivedCpsServeCotrProcessed.getSubmissionStatus());

        updateCpsServeMaterialStatusUpdatedWithErrorsAndWarnings(statusPublicEventBuilder, receivedCpsServeCotrProcessed.getSubmissionStatus(), receivedCpsServeCotrProcessed.getErrors());

        sender.send(Envelope.envelopeFrom(metadata, statusPublicEventBuilder.build()));
    }

    private void sendCpsUpdateCotrStatusUpdatedPublicEvent(final Envelope<ReceivedCpsUpdateCotrProcessed> envelope) {
        final ReceivedCpsUpdateCotrProcessed receivedCpsUpdateCotrProcessed = envelope.payload();
        final Metadata metadata = JsonEnvelope.metadataFrom(envelope.metadata())
                .withName(STATUS_UPDATED_PUBLIC_EVENT)
                .build();

        final CpsServeMaterialStatusUpdated.Builder statusPublicEventBuilder = CpsServeMaterialStatusUpdated.cpsServeMaterialStatusUpdated()
                .withSubmissionId(receivedCpsUpdateCotrProcessed.getSubmissionId())
                .withSubmissionStatus(receivedCpsUpdateCotrProcessed.getSubmissionStatus());

        updateCpsServeMaterialStatusUpdatedWithErrorsAndWarnings(statusPublicEventBuilder, receivedCpsUpdateCotrProcessed.getSubmissionStatus(), receivedCpsUpdateCotrProcessed.getErrors());

        sender.send(Envelope.envelopeFrom(metadata, statusPublicEventBuilder.build()));
    }

    private JsonObjectBuilder buildCotrCommand(JsonObject publicEventCpsServeCotr) {
        return createObjectBuilder()
                .add(SUBMISSION_ID, publicEventCpsServeCotr.getString(SUBMISSION_ID))
                .add(SUBMISSION_STATUS, publicEventCpsServeCotr.getString(SUBMISSION_STATUS))
                .add(PROSECUTION_CASE_SUBJECT, publicEventCpsServeCotr.getJsonObject(PROSECUTION_CASE_SUBJECT))
                .add(DEFENDANT_SUBJECT, publicEventCpsServeCotr.getJsonArray(DEFENDANT_SUBJECT));
    }


    private void materialRejectWhenNoCaseUrnFound(JsonEnvelope envelope, JsonObject publicEventCpsUpdateCotr, String caseUrn) {
        LOGGER.info("CASE_URN_NOT_FOUND not found");
        final CpsServeMaterialStatusUpdated.Builder statusPublicEventBuilder = CpsServeMaterialStatusUpdated.cpsServeMaterialStatusUpdated()
                .withSubmissionId(UUID.fromString(publicEventCpsUpdateCotr.getString(SUBMISSION_ID)))
                .withSubmissionStatus(SubmissionStatus.REJECTED)
                .withErrors(asList(Problem.problem()
                        .withCode(CASE_URN_NOT_FOUND.getCode())
                        .withValues(asList(problemValue()
                                .withKey(CASE_URN)
                                .withValue(caseUrn)
                                .build()))
                        .build()));

        final Metadata metadata = JsonEnvelope.metadataFrom(envelope.metadata())
                .withName(STATUS_UPDATED_PUBLIC_EVENT)
                .build();

        LOGGER.info("Raising public event public.prosecutioncasefile.cps-serve-material-status-updated with Rejected status");
        this.sender.send(Envelope.envelopeFrom(metadata, statusPublicEventBuilder.build()));
    }
}
