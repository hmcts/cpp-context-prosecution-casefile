package uk.gov.moj.cpp.prosecution.casefile.validation;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.prosecution.casefile.domain.FormValidationResult.formValidationResult;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.MatchedDefendant.matchedDefendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem.problem;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue.problemValue;
import static uk.gov.moj.cpp.prosecution.casefile.validation.helper.ValidationRuleHelper.getValueAsString;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.ADDRESS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.ADDRESS1;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.ADDRESS2;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.ADDRESS3;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.ADDRESS4;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.ADDRESS5;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.ANY_OTHER;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.ASN;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.ASSOCIATED_PERSON;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.AUTHORITY;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.AUTHORITY_DETAILS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.BCM_DEFENDANTS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.CJS_OFFENCE_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.CPS_DEFENDANT;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.CPS_DEFENDANT_ID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.CPS_DEFENDANT_OFFENCES;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.CPS_OFFENCES;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.CPS_OFFENCE_DETAILS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.DATE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.DATE_OF_BIRTH;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.DEFENCE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.DEFENDANTS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.DEFENDANT_ID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.EMAIL;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.EVIDENCE_POST_PTPH;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.EVIDENCE_PRE_PTPH;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.FORENAME;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.FORENAME2;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.FORENAME3;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.FORM_DEFENDANTS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.GUARDIAN_DETAILS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.ID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.IS_LOOKED_AFTER_CHILD;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.LOCAL_AUTHORITY_DETAILS_FOR_YOUTH_DEFENDANTS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.MATCHING_ID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.NAME;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.OFFENCE_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.OFFENCE_DATE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.OFFENCE_WORDING;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.ORGANISATION_NAME;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.OTHER_AREAS_AFTER_PTPH;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.OTHER_AREAS_BEFORE_PTPH;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.OTHER_INFORMATION;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.PARENT_GUARDIAN_FOR_YOUTH_DEFENDANTS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.PET_DEFENDANTS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.PET_FORM_DATA;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.PHONE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.POSTCODE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.PRINCIPAL_CHARGES;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.PROSECUTION;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.PROSECUTION_CASE_PROGRESSION_OFFICER;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.PROSECUTOR_DEFENDANT_ID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.PROSECUTOR_OFFENCES;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.PTPH_ADVOCATE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.PTPH_DEFENDANTS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.REF;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.REFERENCE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.RELATIONSHIP;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.RELATIONSHIP_TO_DEFENDANT;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.RESPONSIBLE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.SURNAME;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.TRIAL_ADVOCATE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.WORDING;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.prosecution.casefile.domain.FormValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.MatchedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.AsnValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.CpsDefendantIdValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.NameAndDOBValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.ValidationRule;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.AdvocateAtPTPH;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.AdvocateForTrial;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseProgressionOfficer;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmissionStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S3776")
public class CpsFormValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CpsFormValidator.class);
    private static final String DEFENDANT_ASN = "asn";
    private static final String DEFENDANT_CPS_DEFENDANT_ID = "cpsDefendantId";
    private static final String DEFENDANT_NAME_AND_DOB = "nameAndDob";


    @Inject
    Instance<ValidationRule> validatorRules;

    public FormValidationResult validateAndRebuildingFormDataPtph(final JsonObject processReceivedCpsServePtph,
                                                                  final JsonObject prosecutionCase,
                                                                  final JsonArray defendantIds,
                                                                  final ObjectToJsonObjectConverter objectToJsonObjectConverter) {
        final FormValidationResult.Builder formBuilder = FormValidationResult.formValidationResult();
        SubmissionStatus submissionStatus;
        final Set<MatchedDefendant> matchedDefendantsSet = new HashSet<>();

        final Map<String, ValidationRule> validationRuleMap = new HashMap<>();
        final List<MatchedDefendant> validateResults = new ArrayList<>();
        final List<Problem> errorList = new ArrayList<>();
        stream(validatorRules.spliterator(), false)
                .forEach(rule -> {
                    if (rule instanceof AsnValidationRule) {
                        validationRuleMap.put(DEFENDANT_ASN, rule);
                    }
                    if (rule instanceof CpsDefendantIdValidationRule) {
                        validationRuleMap.put(DEFENDANT_CPS_DEFENDANT_ID, rule);
                    }
                    if (rule instanceof NameAndDOBValidationRule) {
                        validationRuleMap.put(DEFENDANT_NAME_AND_DOB, rule);
                    }
                });

        processReceivedCpsServePtph.getJsonArray(CPS_DEFENDANT).getValuesAs(JsonObject.class)
                .forEach(cpsDefendant -> matchDefendant(prosecutionCase, defendantIds, validationRuleMap, validateResults, cpsDefendant));

        for (final MatchedDefendant matchedDefendant : validateResults) {
            if (nonNull(matchedDefendant.getDefendantId())) {
                matchedDefendantsSet.add(matchedDefendant);
            }
            if (isNotEmpty(matchedDefendant.getProblems())) {
                errorList.addAll(matchedDefendant.getProblems());
            }
        }

        //case when none of defendants matched
        if (matchedDefendantsSet.isEmpty()) {
            submissionStatus = SubmissionStatus.REJECTED;
            return formBuilder
                    .withSubmissionStatus(submissionStatus)
                    .withErrorList(errorList)
                    .build();
        }

        final JsonObjectBuilder newBcmFormData = createObjectBuilder();
        final JsonArrayBuilder newBcmFormDefendantsArrayBuilder = Json.createArrayBuilder();
        final Set<MatchedDefendant> finalMatchedDefendantSet = matchedDefendantsSet;

        processReceivedCpsServePtph
                .getJsonArray(CPS_DEFENDANT)
                .getValuesAs(JsonObject.class)
                .stream()
                .filter(defendantOffenceFromEvent -> finalMatchedDefendantSet.stream()
                        .anyMatch(matchedDefendant -> matchedDefendant.getMatchingId().toString().equalsIgnoreCase(defendantOffenceFromEvent.getString(MATCHING_ID))))
                .map(defendantFromEventPayload -> buildPtphDefendantWithDefendantId(objectToJsonObjectConverter, finalMatchedDefendantSet, defendantFromEventPayload, processReceivedCpsServePtph))
                .forEach(newBcmFormDefendantsArrayBuilder::add);

        final JsonArrayBuilder newFormDefendantArrayBuilder = Json.createArrayBuilder();
        matchedDefendantsSet
                .stream()
                .map(this::buildFormDefendant)
                .forEach(newFormDefendantArrayBuilder::add);

        return formValidationResult()
                .withFormData(newBcmFormData
                        .add(PTPH_DEFENDANTS, newBcmFormDefendantsArrayBuilder.build())
                        .build())
                .withFormDefendants(Json
                        .createObjectBuilder()
                        .add(FORM_DEFENDANTS, newFormDefendantArrayBuilder.build())
                        .build())
                .withErrorList(errorList)
                .withSubmissionStatus(errorList.isEmpty() ? SubmissionStatus.SUCCESS : SubmissionStatus.REJECTED)
                .build();
    }

    private void matchDefendant(final JsonObject prosecutionCase, final JsonArray defendantIds, final Map<String, ValidationRule> validationRuleMap, final List<MatchedDefendant> validateResults, final JsonObject cpsDefendant) {
        MatchedDefendant matchedDefendant;

        if (cpsDefendant.containsKey(DEFENDANT_ASN)) {
            matchedDefendant = validationRuleMap.get(DEFENDANT_ASN).validate(cpsDefendant, prosecutionCase);
        } else {
            matchedDefendant = processWhenCpsDidNotProvidedAsnButAllCaseDefendantHasAsn(cpsDefendant, prosecutionCase);
        }


        if (cpsDefendant.containsKey(DEFENDANT_CPS_DEFENDANT_ID) && matchedDefendant.getIsContinueMatching()) {

            final List<JsonObject> caseDefendantList = prosecutionCase
                    .getJsonArray(DEFENDANTS)
                    .getValuesAs(JsonObject.class);

            final boolean areAllDefendantsHaveCpsIds = defendantIds.size() == caseDefendantList.size();
            final JsonObject cpsDefendantIdRuleInput = Json.createObjectBuilder()
                    .add("cpsDefendantIdList", defendantIds)
                    .add("areAllDefendantsHaveCpsIds", areAllDefendantsHaveCpsIds)
                    .build();
            matchedDefendant = validationRuleMap.get(DEFENDANT_CPS_DEFENDANT_ID).validate(cpsDefendant, cpsDefendantIdRuleInput);
        }

        if (matchedDefendant.getIsContinueMatching()) {
            final JsonObject cpsDefendantDetailRuleInput = Json.createObjectBuilder()
                    .add("cpsDefendantIdList", defendantIds)
                    .add("prosecutionCase", prosecutionCase)
                    .build();
            matchedDefendant = validationRuleMap.get(DEFENDANT_NAME_AND_DOB).validate(cpsDefendant, cpsDefendantDetailRuleInput);
        }

        validateResults.add(matchedDefendant);
    }

    private MatchedDefendant processWhenCpsDidNotProvidedAsnButAllCaseDefendantHasAsn(final JsonObject cpsDefendant, final JsonValue pcfDefendant) {
        final List<JsonObject> caseDefendantList = ((JsonObject) pcfDefendant)
                .getJsonArray(DEFENDANTS)
                .getValuesAs(JsonObject.class);

        final List<String> matchedObjectAsn = caseDefendantList
                .stream()
                .filter(asn -> nonNull(getValueAsString(asn, ASN)))
                .map(o -> getValueAsString(o, ASN))
                .collect(toList());

        final MatchedDefendant.Builder matchedDefendantBuilder = matchedDefendant()
                .withMatchingId(fromString(cpsDefendant.getString(MATCHING_ID)));
        if (caseDefendantList.size() == matchedObjectAsn.size()) {
            matchedDefendantBuilder.withIsContinueMatching(false);

            final Problem.Builder problemBuilder = problem()
                    .withCode(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode());

            final List<ProblemValue> problemValueList = populateProblemValue(cpsDefendant);

            problemBuilder.withValues(problemValueList);
            matchedDefendantBuilder.withProblems(asList(problemBuilder.build()));
        } else {
            matchedDefendantBuilder.withIsContinueMatching(true);
        }

        return matchedDefendantBuilder.build();
    }

    private List<ProblemValue> populateProblemValue(final JsonObject cpsDefendant) {
        final List<ProblemValue> problemValueList = new ArrayList<>();
        if (cpsDefendant.containsKey(FORENAME)) {
            problemValueList.add(problemValue()
                    .withKey(FORENAME)
                    .withValue(cpsDefendant.getString(FORENAME, ""))
                    .build());
        }

        if (cpsDefendant.containsKey(SURNAME)) {
            problemValueList.add(problemValue()
                    .withKey(SURNAME)
                    .withValue(cpsDefendant.getString(SURNAME, ""))
                    .build());
        }

        if (cpsDefendant.containsKey(DATE_OF_BIRTH)) {
            problemValueList.add(problemValue()
                    .withKey(DATE_OF_BIRTH)
                    .withValue(cpsDefendant.getString(DATE_OF_BIRTH, ""))
                    .build());
        }

        if (cpsDefendant.containsKey(DEFENDANT_CPS_DEFENDANT_ID)) {
            problemValueList.add(problemValue()
                    .withKey(DEFENDANT_CPS_DEFENDANT_ID)
                    .withValue(cpsDefendant.getString(DEFENDANT_CPS_DEFENDANT_ID, ""))
                    .build());
        }

        if (cpsDefendant.containsKey(ORGANISATION_NAME)) {
            problemValueList.add(problemValue()
                    .withKey(ORGANISATION_NAME)
                    .withValue(cpsDefendant.getString(ORGANISATION_NAME, ""))
                    .build());
        }
        return problemValueList;
    }


    public FormValidationResult validateAndRebuildingFormDataBcm(final JsonObject processReceivedCpsServeBcm, final JsonObject prosecutionCase, final List<String> validOffences, final JsonArray defendantIds) {
        final FormValidationResult.Builder formBuilder = FormValidationResult.formValidationResult();
        SubmissionStatus submissionStatus;
        final Set<MatchedDefendant> matchedDefendantsSet = new HashSet<>();

        final Map<String, ValidationRule> validationRuleMap = new HashMap<>();
        final List<MatchedDefendant> validateResults = new ArrayList<>();
        final List<Problem> errorList = new ArrayList<>();
        stream(validatorRules.spliterator(), false)
                .forEach(rule -> {
                    if (rule instanceof AsnValidationRule) {
                        validationRuleMap.put(DEFENDANT_ASN, rule);
                    }
                    if (rule instanceof CpsDefendantIdValidationRule) {
                        validationRuleMap.put(DEFENDANT_CPS_DEFENDANT_ID, rule);
                    }
                    if (rule instanceof NameAndDOBValidationRule) {
                        validationRuleMap.put(DEFENDANT_NAME_AND_DOB, rule);
                    }
                });

        processReceivedCpsServeBcm.getJsonArray(CPS_DEFENDANT_OFFENCES).getValuesAs(JsonObject.class)
                .forEach(cpsDefendant -> matchDefendant(prosecutionCase, defendantIds, validationRuleMap, validateResults, cpsDefendant));

        for (final MatchedDefendant matchedDefendant : validateResults) {
            if (nonNull(matchedDefendant.getDefendantId())) {
                matchedDefendantsSet.add(matchedDefendant);
            }
            if (isNotEmpty(matchedDefendant.getProblems())) {
                errorList.addAll(matchedDefendant.getProblems());
            }
        }

        //case when none of defendants matched
        if (matchedDefendantsSet.isEmpty()) {
            submissionStatus = SubmissionStatus.REJECTED;
            return formBuilder
                    .withSubmissionStatus(submissionStatus)
                    .withErrorList(errorList)
                    .build();
        }

        //case when none of offences are valid.
        if (matchedDefendantsSet.isEmpty()) {
            submissionStatus = SubmissionStatus.REJECTED;
            return formBuilder
                    .withSubmissionStatus(submissionStatus)
                    .withErrorList(errorList)
                    .build();
        }
        final JsonObjectBuilder newBcmFormData = createObjectBuilder();
        final JsonArrayBuilder newBcmFormDefendantsArrayBuilder = Json.createArrayBuilder();
        final Set<MatchedDefendant> finalMatchedDefendantSet = matchedDefendantsSet;

        processReceivedCpsServeBcm
                .getJsonArray(CPS_DEFENDANT_OFFENCES)
                .getValuesAs(JsonObject.class)
                .stream()
                .filter(defendantOffenceFromEvent -> finalMatchedDefendantSet.stream()
                        .anyMatch(matchedDefendant -> matchedDefendant.getMatchingId().toString().equalsIgnoreCase(defendantOffenceFromEvent.getString(MATCHING_ID))))
                .map(defendantFromEventPayload -> buildBcmDefendantWithDefendantId(finalMatchedDefendantSet, validOffences, defendantFromEventPayload, processReceivedCpsServeBcm))
                .forEach(newBcmFormDefendantsArrayBuilder::add);

        final JsonArrayBuilder newFormDefendantArrayBuilder = Json.createArrayBuilder();
        matchedDefendantsSet
                .stream()
                .map(this::buildFormDefendant)
                .forEach(newFormDefendantArrayBuilder::add);

        return formValidationResult()
                .withFormData(newBcmFormData
                        .add(BCM_DEFENDANTS, newBcmFormDefendantsArrayBuilder.build())
                        .build())
                .withFormDefendants(Json
                        .createObjectBuilder()
                        .add(FORM_DEFENDANTS, newFormDefendantArrayBuilder.build())
                        .build())
                .withErrorList(errorList)
                .withSubmissionStatus(errorList.isEmpty() ? SubmissionStatus.SUCCESS : SubmissionStatus.REJECTED)
                .build();
    }

    private JsonObject buildBcmDefendantWithDefendantId(final Set<MatchedDefendant> finalMatchedDefendantSet, final List<String> validOffences, final JsonObject defendantJson, final JsonObject processReceivedCpsServeBcm) {
        LOGGER.info("number of valid offences : {}", validOffences.size());
        final JsonArrayBuilder newCpsOffencesArrayBuilder = createArrayBuilder();
        defendantJson
                .getJsonArray(CPS_OFFENCE_DETAILS)
                .getValuesAs(JsonObject.class)
                .stream()
                .forEach(offenceDetails -> newCpsOffencesArrayBuilder.add(
                        createObjectBuilder()
                                .add(OFFENCE_CODE, offenceDetails.getJsonString(CJS_OFFENCE_CODE))
                                .add(WORDING, offenceDetails.getJsonString(OFFENCE_WORDING))
                                .add(DATE, offenceDetails.getJsonString(OFFENCE_DATE))
                                .build()
                ));


        final JsonObjectBuilder defendantBuilder = createObjectBuilder()
                .add(PROSECUTOR_OFFENCES, newCpsOffencesArrayBuilder.build());

        final MatchedDefendant matchedDefendant = findMatchedDefendantFromMatchingId(finalMatchedDefendantSet, defendantJson.getString(MATCHING_ID));
        if (nonNull(matchedDefendant)) {
            defendantBuilder.add(ID, matchedDefendant.getDefendantId().toString());
        }

        if (processReceivedCpsServeBcm.containsKey(EVIDENCE_PRE_PTPH)) {
            defendantBuilder.add(OTHER_AREAS_BEFORE_PTPH, processReceivedCpsServeBcm.getString(EVIDENCE_PRE_PTPH));
        }

        if (processReceivedCpsServeBcm.containsKey(EVIDENCE_POST_PTPH)) {
            defendantBuilder.add(OTHER_AREAS_AFTER_PTPH, processReceivedCpsServeBcm.getString(EVIDENCE_POST_PTPH));
        }

        if (processReceivedCpsServeBcm.containsKey(OTHER_INFORMATION)) {
            defendantBuilder.add(ANY_OTHER, processReceivedCpsServeBcm.getString(OTHER_INFORMATION));
        }
        return defendantBuilder.build();
    }

    private JsonObject buildPtphDefendantWithDefendantId(final ObjectToJsonObjectConverter objectToJsonObjectConverter, final Set<MatchedDefendant> finalMatchedDefendantSet, final JsonObject defendantJson, final JsonObject processReceivedCpsServePtph) {

        final JsonObjectBuilder defendantBuilder = createObjectBuilder();

        final MatchedDefendant matchedDefendant = findMatchedDefendantFromMatchingId(finalMatchedDefendantSet, defendantJson.getString(MATCHING_ID));
        if (nonNull(matchedDefendant)) {
            defendantBuilder.add(ID, matchedDefendant.getDefendantId().toString());
            constructPtphDefendant(objectToJsonObjectConverter, defendantBuilder, defendantJson, processReceivedCpsServePtph);
        }

        return defendantBuilder.build();
    }

    private void constructPtphDefendant(final ObjectToJsonObjectConverter objectToJsonObjectConverter, final JsonObjectBuilder defendantBuilder, final JsonObject defendant, final JsonObject processReceivedCpsServePtph) {
        final AdvocateAtPTPH advocateAtPTPH = convertToAdvocateAtPtph(processReceivedCpsServePtph);
        if (advocateAtPTPH != null) {
            defendantBuilder.add(PTPH_ADVOCATE, objectToJsonObjectConverter.convert(advocateAtPTPH));
        }
        final AdvocateForTrial advocateForTrial = convertToAdvocateForTrial(processReceivedCpsServePtph);
        if (advocateForTrial != null) {
            defendantBuilder.add(TRIAL_ADVOCATE, objectToJsonObjectConverter.convert(advocateForTrial));
        }
        final CaseProgressionOfficer caseProgressionOfficer = convertToProgressionOfficer(processReceivedCpsServePtph);
        if (caseProgressionOfficer != null) {
            defendantBuilder.add(PROSECUTION_CASE_PROGRESSION_OFFICER, objectToJsonObjectConverter.convert(caseProgressionOfficer));
        }

        final String principalChargesForPtph = getPrincipalChargesForPtph(defendant);
        if (principalChargesForPtph != null) {
            defendantBuilder.add(PRINCIPAL_CHARGES, principalChargesForPtph);
        }
    }

    private String getPrincipalChargesForPtph(final JsonObject defendant) {
        return getValueAsString(defendant, PRINCIPAL_CHARGES);
    }

    private JsonObject getValueAsJsonObject(final JsonObject jsonObject, final String key) {
        if (isNull(jsonObject)) {
            return null;
        }

        return jsonObject.containsKey(key) ? jsonObject.getJsonObject(key) : null;
    }

    private CaseProgressionOfficer convertToProgressionOfficer(final JsonObject defendant) {
        final JsonObject contact = getValueAsJsonObject(defendant, PROSECUTION_CASE_PROGRESSION_OFFICER);

        if (isNull(contact)) {
            return null;
        }
        CaseProgressionOfficer caseProgressionOfficer = null;
        final String name = getValueAsString(contact, NAME);
        if (StringUtils.isNotEmpty(name)) {
            if (name.contains(StringUtils.SPACE)) {
                final String firstName = name.substring(0, name.indexOf(' '));
                final String lastName = name.substring(name.lastIndexOf(' ') + 1, name.length());

                caseProgressionOfficer = CaseProgressionOfficer.caseProgressionOfficer().withEmail(getValueAsString(contact, EMAIL))
                        .withFirstName(firstName)
                        .withLastName(lastName)
                        .withPhone(getValueAsString(contact, PHONE))
                        .build();
            } else {
                caseProgressionOfficer = CaseProgressionOfficer.caseProgressionOfficer().withEmail(getValueAsString(contact, EMAIL))
                        .withFirstName(name)
                        .withPhone(getValueAsString(contact, PHONE))
                        .build();
            }
        }

        return caseProgressionOfficer;
    }

    private AdvocateForTrial convertToAdvocateForTrial(final JsonObject defendant) {
        final JsonObject contact = getValueAsJsonObject(defendant, TRIAL_ADVOCATE);

        if (isNull(contact)) {
            return null;
        }
        AdvocateForTrial advocateForTrial = null;
        final String name = getValueAsString(contact, NAME);
        if (StringUtils.isNotEmpty(name)) {
            if (name.contains(StringUtils.SPACE)) {
                final String firstName = name.substring(0, name.indexOf(' '));
                final String lastName = name.substring(name.lastIndexOf(' ') + 1, name.length());

                advocateForTrial = AdvocateForTrial.advocateForTrial().withEmail(getValueAsString(contact, EMAIL))
                        .withFirstName(firstName)
                        .withLastName(lastName)
                        .withPhone(getValueAsString(contact, PHONE))
                        .build();
            } else {
                advocateForTrial = AdvocateForTrial.advocateForTrial().withEmail(getValueAsString(contact, EMAIL))
                        .withFirstName(name)
                        .withPhone(getValueAsString(contact, PHONE))
                        .build();
            }

        }

        return advocateForTrial;
    }

    private AdvocateAtPTPH convertToAdvocateAtPtph(final JsonObject defendant) {
        final JsonObject contact = getValueAsJsonObject(defendant, PTPH_ADVOCATE);

        if (isNull(contact)) {
            return null;
        }
        AdvocateAtPTPH advocateAtPTPH = null;
        final String name = getValueAsString(contact, NAME);
        if (StringUtils.isNotEmpty(name)) {
            if (name.contains(StringUtils.SPACE)) {
                final String firstName = name.substring(0, name.indexOf(' '));
                final String lastName = name.substring(name.lastIndexOf(' ') + 1, name.length());

                advocateAtPTPH = AdvocateAtPTPH.advocateAtPTPH().withEmail(getValueAsString(contact, EMAIL))
                        .withFirstName(firstName)
                        .withLastName(lastName)
                        .withPhone(getValueAsString(contact, PHONE))
                        .build();
            } else {
                advocateAtPTPH = AdvocateAtPTPH.advocateAtPTPH().withEmail(getValueAsString(contact, EMAIL))
                        .withFirstName(name)
                        .withPhone(getValueAsString(contact, PHONE))
                        .build();
            }
        }

        return advocateAtPTPH;
    }

    public FormValidationResult validateAndRebuildingFormData(final JsonObject processReceivedCpsServePet, final JsonObject prosecutionCase, final List<String> validOffences, final JsonArray defendantIds) {
        LOGGER.info("number of valid offences : {}", validOffences.size());
        final FormValidationResult.Builder formBuilder = FormValidationResult.formValidationResult();
        SubmissionStatus submissionStatus;
        final Set<MatchedDefendant> matchedDefendantsSet = new HashSet<>();

        final Map<String, ValidationRule> validationRuleMap = new HashMap<>();
        final List<MatchedDefendant> validateResults = new ArrayList<>();
        final List<Problem> errorList = new ArrayList<>();
        stream(validatorRules.spliterator(), false)
                .forEach(rule -> {
                    if (rule instanceof AsnValidationRule) {
                        validationRuleMap.put(DEFENDANT_ASN, rule);
                    }
                    if (rule instanceof CpsDefendantIdValidationRule) {
                        validationRuleMap.put(DEFENDANT_CPS_DEFENDANT_ID, rule);
                    }
                    if (rule instanceof NameAndDOBValidationRule) {
                        validationRuleMap.put(DEFENDANT_NAME_AND_DOB, rule);
                    }
                });

        processReceivedCpsServePet.getJsonArray(CPS_DEFENDANT_OFFENCES).getValuesAs(JsonObject.class)
                .forEach(cpsDefendant -> matchDefendant(prosecutionCase, defendantIds, validationRuleMap, validateResults, cpsDefendant));

        for (final MatchedDefendant matchedDefendant : validateResults) {
            if (nonNull(matchedDefendant.getDefendantId())) {
                matchedDefendantsSet.add(matchedDefendant);
            }
            if (isNotEmpty(matchedDefendant.getProblems())) {
                errorList.addAll(matchedDefendant.getProblems());
            }
        }

        final JsonObject petFormData = processReceivedCpsServePet.getJsonObject(PET_FORM_DATA);
        final JsonObject petDefendants = processReceivedCpsServePet.getJsonObject(PET_DEFENDANTS);

        //case when none of defendants matched
        if (matchedDefendantsSet.isEmpty()) {
            submissionStatus = SubmissionStatus.REJECTED;
            return formBuilder
                    .withSubmissionStatus(submissionStatus)
                    .withErrorList(errorList)
                    .withPetFormData(petFormData)
                    .withPetDefendants(petDefendants)
                    .build();
        }

        //case when none of offences are valid.
        if (matchedDefendantsSet.isEmpty()) {
            submissionStatus = SubmissionStatus.REJECTED;
            return formBuilder
                    .withSubmissionStatus(submissionStatus)
                    .withErrorList(errorList)
                    .withPetFormData(petFormData)
                    .withPetDefendants(petDefendants)
                    .build();
        }

        final JsonObjectBuilder newPetFormData = createObjectBuilder();
        final JsonArrayBuilder newPetFormDefenceDefendantsArrayBuilder = Json.createArrayBuilder();
        final Set<MatchedDefendant> finalMatchedDefendantSet = matchedDefendantsSet;

        processReceivedCpsServePet
                .getJsonArray(CPS_DEFENDANT_OFFENCES)
                .getValuesAs(JsonObject.class)
                .stream()
                .filter(defendantOffenceFromEvent -> finalMatchedDefendantSet.stream()
                        .anyMatch(matchedDefendant -> matchedDefendant.getMatchingId().toString().equalsIgnoreCase(defendantOffenceFromEvent.getString(MATCHING_ID))))
                .map(defendantFromEventPayload -> buildDefendantWithDefendantId(finalMatchedDefendantSet, validOffences, defendantFromEventPayload))
                .forEach(newPetFormDefenceDefendantsArrayBuilder::add);

        final JsonArrayBuilder newDefenceArrayBuilder = Json.createArrayBuilder();
        matchedDefendantsSet
                .stream()
                .map(this::buildFormDefendant)
                .forEach(newDefenceArrayBuilder::add);

        return formValidationResult()
                .withPetFormData(newPetFormData
                        .add(DEFENCE, createObjectBuilder()
                                .add(DEFENDANTS, newPetFormDefenceDefendantsArrayBuilder.build()))
                        .add(PROSECUTION, petFormData.getJsonObject(PROSECUTION))
                        .build())
                .withPetDefendants(Json
                        .createObjectBuilder()
                        .add(PET_DEFENDANTS, newDefenceArrayBuilder.build())
                        .build())
                .withErrorList(errorList)
                .withSubmissionStatus(errorList.isEmpty() ? SubmissionStatus.SUCCESS : SubmissionStatus.REJECTED)
                .build();
    }

    private JsonObject buildDefendantWithDefendantId(final Set<MatchedDefendant> matchedDefendantsSet, final List<String> validOffences, final JsonObject defendantJson) {
        LOGGER.info("number of valid offences: {}", validOffences.size());
        final JsonArrayBuilder newCpsOffencesArrayBuilder = createArrayBuilder();
        defendantJson
                .getJsonArray(CPS_OFFENCE_DETAILS)
                .getValuesAs(JsonObject.class)
                .stream()
                .forEach(offenceDetails -> newCpsOffencesArrayBuilder.add(
                        createObjectBuilder()
                                .add(OFFENCE_CODE, offenceDetails.getJsonString(CJS_OFFENCE_CODE))
                                .add(WORDING, offenceDetails.getJsonString(OFFENCE_WORDING))
                                .add(DATE, offenceDetails.getJsonString(OFFENCE_DATE))
                                .build()
                ));


        final JsonObjectBuilder defendantBuilder = createObjectBuilder()
                .add(CPS_OFFENCES, newCpsOffencesArrayBuilder.build());

        final MatchedDefendant matchedDefendant = findMatchedDefendantFromMatchingId(matchedDefendantsSet, defendantJson.getString(MATCHING_ID));
        if (nonNull(matchedDefendant)) {
            defendantBuilder.add(ID, matchedDefendant.getDefendantId().toString());
        }

        if (defendantJson.containsKey(CPS_DEFENDANT_ID)) {
            defendantBuilder.add(CPS_DEFENDANT_ID, defendantJson.getString(CPS_DEFENDANT_ID));
        }
        if (defendantJson.containsKey(PROSECUTOR_DEFENDANT_ID)) {
            defendantBuilder.add(PROSECUTOR_DEFENDANT_ID, defendantJson.getString(PROSECUTOR_DEFENDANT_ID));
        }

        buildAssociatedPerson(defendantJson,defendantBuilder);

        return defendantBuilder.build();
    }

    private MatchedDefendant findMatchedDefendantFromMatchingId(Set<MatchedDefendant> matchedDefendantSet, final String matchingId) {
        return matchedDefendantSet.stream()
                .filter(matchedDefendant -> matchedDefendant.getMatchingId().toString().equalsIgnoreCase(matchingId))
                .findFirst().orElse(null);

    }

    private JsonObject buildFormDefendant(final MatchedDefendant matchedDefendant) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(DEFENDANT_ID, matchedDefendant.getDefendantId().toString());
        if (StringUtils.isNotEmpty(matchedDefendant.getCpsDefendantId())) {
            builder.add(CPS_DEFENDANT_ID, matchedDefendant.getCpsDefendantId());
        }
        return builder.build();
    }

    public FormValidationResult validateCotr(final JsonObject processReceivedCpsServeCotr, final JsonObject prosecutionCase, final JsonArray defendantIds) {
        final FormValidationResult.Builder formBuilder = FormValidationResult.formValidationResult();
        SubmissionStatus submissionStatus;
        final Set<MatchedDefendant> matchedDefendantsSet = new HashSet<>();

        final Map<String, ValidationRule> validationRuleMap = new HashMap<>();
        final List<MatchedDefendant> validateResults = new ArrayList<>();
        final List<Problem> errorList = new ArrayList<>();
        stream(validatorRules.spliterator(), false)
                .forEach(rule -> {
                    if (rule instanceof AsnValidationRule) {
                        validationRuleMap.put(DEFENDANT_ASN, rule);
                    }
                    if (rule instanceof CpsDefendantIdValidationRule) {
                        validationRuleMap.put(DEFENDANT_CPS_DEFENDANT_ID, rule);
                    }
                    if (rule instanceof NameAndDOBValidationRule) {
                        validationRuleMap.put(DEFENDANT_NAME_AND_DOB, rule);
                    }
                });

        processReceivedCpsServeCotr.getJsonArray("defendantSubject").getValuesAs(JsonObject.class)
                .forEach(cpsDefendant -> matchDefendant(prosecutionCase, defendantIds, validationRuleMap, validateResults, cpsDefendant));

        for (final MatchedDefendant matchedDefendant : validateResults) {
            if (nonNull(matchedDefendant.getDefendantId())) {
                matchedDefendantsSet.add(matchedDefendant);
            }
            if (isNotEmpty(matchedDefendant.getProblems())) {
                errorList.addAll(matchedDefendant.getProblems());
            }
        }

        //case when none of defendants matched
        if (matchedDefendantsSet.isEmpty()) {
            submissionStatus = SubmissionStatus.REJECTED;
            return formBuilder
                    .withSubmissionStatus(submissionStatus)
                    .withErrorList(errorList)
                    .build();
        }

        final JsonArrayBuilder newFormDefendantArrayBuilder = Json.createArrayBuilder();
        matchedDefendantsSet
                .stream()
                .map(this::buildFormDefendant)
                .forEach(newFormDefendantArrayBuilder::add);

        return formValidationResult()
                .withFormDefendants(Json
                        .createObjectBuilder()
                        .add(FORM_DEFENDANTS, newFormDefendantArrayBuilder.build())
                        .build())
                .withErrorList(errorList)
                .withSubmissionStatus(errorList.isEmpty() ? SubmissionStatus.SUCCESS : SubmissionStatus.SUCCESS_WITH_WARNINGS)
                .build();
    }

    private void buildAssociatedPerson(final JsonObject defendantJson, final JsonObjectBuilder defendantBuilder) {
        final JsonObjectBuilder associatedPersonBuilder = Json.createObjectBuilder();

        if (defendantJson.containsKey(LOCAL_AUTHORITY_DETAILS_FOR_YOUTH_DEFENDANTS)) {
            final JsonObject localAuthorityObject = defendantJson.getJsonObject(LOCAL_AUTHORITY_DETAILS_FOR_YOUTH_DEFENDANTS);
            final JsonObject localAuthority = Json.createObjectBuilder()
                    .add(NAME, convertName(localAuthorityObject))
                    .add(ADDRESS, convertAddress(localAuthorityObject))
                    .add(EMAIL,localAuthorityObject.getString(EMAIL))
                    .add(PHONE, localAuthorityObject.getString(PHONE))
                    .add(REF, localAuthorityObject.getString(REFERENCE))
                    .add(RESPONSIBLE, localAuthorityObject.getString(AUTHORITY))
                    .add(IS_LOOKED_AFTER_CHILD, localAuthorityObject.getBoolean(IS_LOOKED_AFTER_CHILD))
                    .build();
            associatedPersonBuilder.add(AUTHORITY_DETAILS, localAuthority);
        }


        if (defendantJson.containsKey(PARENT_GUARDIAN_FOR_YOUTH_DEFENDANTS)) {
            final JsonObject guardianObject = defendantJson.getJsonObject(PARENT_GUARDIAN_FOR_YOUTH_DEFENDANTS);
            final JsonObject guardian = Json.createObjectBuilder()
                    .add(NAME, convertName(guardianObject))
                    .add(ADDRESS, convertAddress(guardianObject))
                    .add(EMAIL, guardianObject.getString(EMAIL))
                    .add(PHONE, guardianObject.getString(PHONE))
                    .add(RELATIONSHIP, guardianObject.getString(RELATIONSHIP_TO_DEFENDANT))
                    .build();

            associatedPersonBuilder.add(GUARDIAN_DETAILS, guardian);
        }

        defendantBuilder.add(ASSOCIATED_PERSON, associatedPersonBuilder.build());
    }

    private String convertName(JsonObject jsonObject) {
        String name = StringUtils.EMPTY;
        if (jsonObject.containsKey(FORENAME)) {
            name = name.concat(jsonObject.getString(FORENAME));
        }

        if (jsonObject.containsKey(FORENAME2)) {
            name = concatString(name);
            name = name.concat(jsonObject.getString(FORENAME2));
        }

        if (jsonObject.containsKey(FORENAME3)) {
            name = concatString(name);
            name = name.concat(jsonObject.getString(FORENAME3));
        }

        if (jsonObject.containsKey(SURNAME)) {
            name = concatString(name);
            name = name.concat(jsonObject.getString(SURNAME));
        }
        return name;
    }

    private String concatString(String value) {
        if (value.length() > 0) {
            value = value.concat(StringUtils.SPACE);
        }
        return value;
    }

    private String convertAddress(final JsonObject jsonObject) {
        String address = StringUtils.EMPTY;

        if (jsonObject.containsKey(ADDRESS1)) {
            address = address.concat(jsonObject.getString(ADDRESS1));
        }

        if (jsonObject.containsKey(ADDRESS2)) {
            address = concatString(address);
            address = address.concat(jsonObject.getString(ADDRESS2));
        }

        if (jsonObject.containsKey(ADDRESS3)) {
            address = concatString(address);
            address = address.concat(jsonObject.getString(ADDRESS3));
        }

        if (jsonObject.containsKey(ADDRESS4)) {
            address = concatString(address);
            address = address.concat(jsonObject.getString(ADDRESS4));
        }

        if (jsonObject.containsKey(ADDRESS5)) {
            address = concatString(address);
            address = address.concat(jsonObject.getString(ADDRESS5));
        }

        if (jsonObject.containsKey(POSTCODE)) {
            address = concatString(address);
            address = address.concat(jsonObject.getString(POSTCODE));
        }

        return address;
    }
}
