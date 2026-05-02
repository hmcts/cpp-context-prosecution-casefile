package uk.gov.moj.cpp.prosecution.casefile;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.time.LocalDate.parse;
import static java.util.Arrays.stream;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.moj.cpp.json.schemas.prosecutioncasefile.events.DefendantValidationPassed.defendantValidationPassed;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.MCC;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem.defendantProblem;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution.prosecution;
import static uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.InitiationCode.values;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_ON_CP;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DUPLICATE_DEFENDANT;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ValidationRuleExecutor.validate;
import static uk.gov.moj.cpp.prosecution.casefile.validation.provider.CcProsecutionValidationRuleProvider.getDefendantValidationRules;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.ASN;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.CPS_DEFENDANT_ID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.DATE_OF_BIRTH;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.FORENAME;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.FORENAME2;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.FORENAME3;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.ORGANISATION_NAME;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.PROSECUTOR_DEFENDANT_ID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.SURNAME;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.TITLE;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.DefendantIdpcAlreadyExists.defendantIdpcAlreadyExists;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.IdpcDefendantMatchPending.idpcDefendantMatchPending;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialAddedV2.materialAddedV2;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialPending.materialPending;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialPendingV2.materialPendingV2;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialRejectedV2.materialRejectedV2;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialRejectedWithWarnings.materialRejectedWithWarnings;

import uk.gov.justice.core.courts.FeeStatus;
import uk.gov.justice.core.courts.FeeType;
import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.event.DefendantValidationFailed;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.AlcoholRelatedOffence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CpsPersonDefendantDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantSubject;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Material;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorPersonDefendantDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.context.ReferenceDataValidationContext;
import uk.gov.moj.cpp.prosecution.casefile.validation.provider.CCMaterialsValidationRuleProviderV2;
import uk.gov.moj.cpp.prosecution.casefile.validation.provider.CcProsecutionWarningRuleProvider;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning.DefendantWarningsValidationRule;
import uk.gov.moj.cps.prosecutioncasefile.common.AddMaterialCommonV2;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialAddedV2;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialPending;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@SuppressWarnings({"squid:S1188", "java:S107"})
public class ProsecutionCaseFileHelper {

    public static final String FIELD_VALUE = "value";
    public static final String FIELD_NAME = "fieldName";
    public static final String ALCOHOL_LEVEL_AMOUNT = "alcoholLevelAmount";
    public static final String ALCOHOL_RELATED_OFFENCE = "alcoholRelatedOffence";
    public static final String OFFENCES = "offences";
    public static final String CASE_MARKERS = "caseMarkers";
    public static final String MARKER_TYPE_CODE = "markerTypeCode";

    private static final String DEFAULT_FEE_STATUS = "OUTSTANDING";
    private static final List<String> intValues = List.of("individual_personalInformation_observedEthnicity");

    private ProsecutionCaseFileHelper() {
    }

    public static Object markMaterialAsPending(final UUID caseId, final String prosecutingAuthority, final String prosecutorDefendantId, final Material material, final ZonedDateTime receivedDateTime, Boolean isCpsCase, final DocumentDetails documentDetails) {
        MaterialPending.Builder materialPendingBuilder = materialPending()
                .withCaseId(caseId)
                .withProsecutingAuthority(prosecutingAuthority)
                .withProsecutorDefendantId(prosecutorDefendantId)
                .withMaterial(material)
                .withIsCpsCase(isCpsCase);

        if (nonNull(documentDetails)) {
            materialPendingBuilder = materialPendingBuilder
                    .withCmsDocumentId(documentDetails.getCmsDocumentId())
                    .withMaterialType(documentDetails.getMaterialType())
                    .withSectionCode(documentDetails.getSectionCode())
                    .withIsCpsCase(isCpsCase);
        }

        materialPendingBuilder.withReceivedDateTime(receivedDateTime);

        return materialPendingBuilder.build();

    }

    public static Object markMaterialAsPendingV2(final AddMaterialCommonV2 addMaterialCommonV2) {
        return markMaterialAsPendingV2(addMaterialCommonV2, null, null);
    }

    public static Object markMaterialAsPendingV2(final AddMaterialCommonV2 addMaterialCommonV2, final List<Problem> warnings, final Boolean cpsFlag) {
        return materialPendingV2()
                .withCaseId(addMaterialCommonV2.getCaseId())
                .withCaseSubFolderName(addMaterialCommonV2.getCaseSubFolderName())
                .withCaseType(addMaterialCommonV2.getCaseType())
                .withDefendantId(addMaterialCommonV2.getDefendantId())
                .withDefendantName(addMaterialCommonV2.getDefendantName())
                .withDocumentCategory(addMaterialCommonV2.getDocumentCategory())
                .withDocumentType(addMaterialCommonV2.getDocumentType())
                .withDocumentTypeId(addMaterialCommonV2.getDocumentTypeId())
                .withExhibit(addMaterialCommonV2.getExhibit())
                .withFileName(addMaterialCommonV2.getFileName())
                .withIsCpsCase(addMaterialCommonV2.getIsCpsCase())
                .withMaterial(addMaterialCommonV2.getMaterial())
                .withMaterialContentType(addMaterialCommonV2.getMaterialContentType())
                .withMaterialName(addMaterialCommonV2.getMaterialName())
                .withMaterialType(addMaterialCommonV2.getMaterialType())
                .withProsecutionCaseSubject(addMaterialCommonV2.getProsecutionCaseSubject())
                .withReceivedDateTime(addMaterialCommonV2.getReceivedDateTime())
                .withSectionOrderSequence(addMaterialCommonV2.getSectionOrderSequence())
                .withSubmissionId(addMaterialCommonV2.getSubmissionId())
                .withTag(addMaterialCommonV2.getTag())
                .withWitnessStatement(addMaterialCommonV2.getWitnessStatement())
                .withWarnings(warnings)
                .withCpsFlag(cpsFlag)
                .build();

    }

    static JsonObject appendAlcoholRelatedOffenceJsonObject(final String property, JsonObject objectToUpdate) {
        final AlcoholRelatedOffence alcoholRelatedOffence = AlcoholRelatedOffence.alcoholRelatedOffence().build();
        objectToUpdate.add(property, new JsonParser().parse((new Gson()).toJson(alcoholRelatedOffence)).getAsJsonObject());
        objectToUpdate = objectToUpdate.getAsJsonObject(property);
        return objectToUpdate;
    }

    @SuppressWarnings("squid:S2259")
    public static void addJsonProperty(final JsonObject jsonObject, final String value, final String name, final String id) {
        if (name != null && value != null) {
            final String[] rootToNameNode = name.split("_");
            JsonObject objectToUpdate = jsonObject;
            for (int index = 0; index < (rootToNameNode.length - 1); index++) {
                objectToUpdate = getObjectToUpdate(jsonObject, name, id, rootToNameNode, objectToUpdate, index);
            }

            if (id != null && id.length() == 1) {
                objectToUpdate = jsonObject.getAsJsonArray(CASE_MARKERS).get(parseInt(id)).getAsJsonObject();
                objectToUpdate.addProperty(MARKER_TYPE_CODE, value);
            } else if (intValues.contains(name) || name.contains(ALCOHOL_LEVEL_AMOUNT)) {
                objectToUpdate.addProperty(rootToNameNode[rootToNameNode.length - 1], parseInt(value));
            } else {
                objectToUpdate.addProperty(rootToNameNode[rootToNameNode.length - 1], value);
            }
        }

    }

    private static JsonObject getObjectToUpdate(final JsonObject jsonObject, final String name, final String id, final String[] rootToNameNode, JsonObject objectToUpdate, final int index) {
        if (id != null && index == 0) {
            final JsonArray jsonArray = jsonObject.getAsJsonArray(OFFENCES);
            objectToUpdate = findJsonObjectWithId(id, jsonArray);
        } else {
            if (objectToUpdate.getAsJsonObject(rootToNameNode[index]) == null && name.contains(ALCOHOL_RELATED_OFFENCE)) {
                objectToUpdate = appendAlcoholRelatedOffenceJsonObject(rootToNameNode[index], objectToUpdate);
            } else {
                objectToUpdate = objectToUpdate.getAsJsonObject(rootToNameNode[index]);
            }
        }
        return objectToUpdate;
    }

    private static JsonObject findJsonObjectWithId(final String id, final JsonArray jsonArray) {
        for (final JsonElement jsonElement : jsonArray) {
            if (jsonElement.getAsJsonObject().get("offenceId").getAsString().equals(id)) {
                return jsonElement.getAsJsonObject();
            }
        }
        return null;
    }

    public static List<DefendantProblem> validateDefendantErrors(final CaseDetails caseDetails,
                                                                 final Channel channel,
                                                                 final DefendantsWithReferenceData defendantsWithReferenceData,
                                                                 final ReferenceDataQueryService referenceDataQueryService,
                                                                 final Stream.Builder<Object> builder,
                                                                 final Boolean isGroupCase, final boolean isMCCWithListNewHearing, final boolean isInactiveMigratedCase,final Boolean isCivil) {
        final List<DefendantProblem> defendantErrors = new ArrayList<>();

        defendantsWithReferenceData.getDefendants().forEach(defendant -> {
            final DefendantWithReferenceData defendantWithReferenceData = new DefendantWithReferenceData(defendant, defendantsWithReferenceData.getReferenceDataVO(), defendantsWithReferenceData.getCaseDetails(), isMCCWithListNewHearing, MCC.equals(channel), isInactiveMigratedCase);
            final String defendantInitiationCode = defendant.getInitiationCode();
            final String initiationCode = defendantInitiationCode != null && isValidInitiationCode(defendantInitiationCode) ? defendant.getInitiationCode() : caseDetails.getInitiationCode();

            final List<Problem> defendantProblemList =
                    validate(defendantWithReferenceData, referenceDataQueryService, getDefendantValidationRules(initiationCode, channel,isCivil));

            if (!defendantProblemList.isEmpty()) {
                defendantErrors.add(defendantProblem()
                        .withProblems(defendantProblemList)
                        .withProsecutorDefendantReference(Strings.isNullOrEmpty(defendant.getProsecutorDefendantReference()) ?
                                defendant.getAsn() : defendant.getProsecutorDefendantReference())
                        .build()
                );
                if (channel == SPI) {
                    builder.add(new DefendantValidationFailed(defendant, defendantProblemList, caseDetails.getCaseId(), caseDetails.getProsecutorCaseReference(), caseDetails.getInitiationCode(), caseDetails.getPoliceSystemId()));
                }
            } else {
                if (channel == SPI) {
                    builder.add(defendantValidationPassed()
                            .withDefendantId(fromString(defendant.getId()))
                            .withCaseId(caseDetails.getCaseId())
                            .build());
                }
            }
        });

        return defendantErrors;
    }

    private static boolean isValidInitiationCode(final String initiationCode) {
        return stream(values())
                .anyMatch(code -> initiationCode.equalsIgnoreCase(String.valueOf(code)));
    }

    public static List<DefendantProblem> validateDefendantWarnings(final DefendantsWithReferenceData defendantsWithReferenceData, final String initiationCode) {

        final ReferenceDataValidationContext referenceDataValidationContext = ReferenceDataValidationContext.newInstance(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData(), defendantsWithReferenceData.getReferenceDataVO().getCountryNationalityReferenceData());

        final List<DefendantProblem> defendantProblems = new ArrayList<>();

        defendantsWithReferenceData.getDefendants().forEach(defendant -> {

            final List<Problem> validationWarnings = validate(
                    defendant,
                    referenceDataValidationContext,
                    CcProsecutionWarningRuleProvider.getWarningRules(initiationCode));
            if (!validationWarnings.isEmpty()) {
                defendantProblems.add(defendantProblem()
                        .withProblems(validationWarnings)
                        .withProsecutorDefendantReference(Strings.isNullOrEmpty(defendant.getProsecutorDefendantReference()) ?
                                defendant.getAsn() : defendant.getProsecutorDefendantReference())
                        .build());
            }
        });

        return defendantProblems;
    }

    public static DefendantsWithReferenceData buildDefendantWithReferenceData(final ProsecutionWithReferenceData prosecutionWithReferenceData, final List<DefendantRefDataEnricher> defendantRefDataEnrichers) {

        final DefendantsWithReferenceData defendantsWithReferenceData = prosecutionWithReferenceData.getReferenceDataVO().getProsecutorsReferenceData() == null ?
                new DefendantsWithReferenceData(prosecutionWithReferenceData.getProsecution().getDefendants()) :
                new DefendantsWithReferenceData(prosecutionWithReferenceData.getProsecution().getDefendants(), prosecutionWithReferenceData.getReferenceDataVO().getProsecutorsReferenceData().getShortName());

        defendantsWithReferenceData.setCaseDetails(prosecutionWithReferenceData.getProsecution().getCaseDetails());
        defendantsWithReferenceData.setReferenceDataVO(prosecutionWithReferenceData.getReferenceDataVO());
        if (nonNull(prosecutionWithReferenceData.getProsecution().getIsCivil())) {
            defendantsWithReferenceData.setCivil(prosecutionWithReferenceData.getProsecution().getIsCivil());
        }

        defendantRefDataEnrichers.forEach(x -> x.enrich(defendantsWithReferenceData));
        return defendantsWithReferenceData;
    }

    public static ProsecutionWithReferenceData setCivilFees(ProsecutionWithReferenceData prosecutionWithReferenceData) {

        final Prosecution prosecution = prosecutionWithReferenceData.getProsecution();
        final Boolean isCivil = Optional.ofNullable(prosecution.getIsCivil()).orElse(false);
        if (Boolean.TRUE.equals(isCivil)) {
            final CaseDetails caseDetails = updateCaseDetailsWithCivilFee(prosecution.getCaseDetails());

            final Prosecution updatedProsecution = prosecution().withValuesFrom(prosecution).withCaseDetails(caseDetails).build();
            return new ProsecutionWithReferenceData(updatedProsecution, prosecutionWithReferenceData.getReferenceDataVO(), prosecutionWithReferenceData.getExternalId());
        }

        return prosecutionWithReferenceData;
    }

    private static CaseDetails updateCaseDetailsWithCivilFee(final CaseDetails caseDetails) {

        String paymentReference = caseDetails.getPaymentReference();
        String initialFeeStatus = getInitialFeeStatus(caseDetails.getFeeStatus(), paymentReference);

        return CaseDetails.caseDetails()
                .withValuesFrom(caseDetails)
                .withFeeId(randomUUID())
                .withFeeStatus(initialFeeStatus)
                .withFeeType(FeeType.INITIAL.name())
                .withPaymentReference(paymentReference)
                .withContestedFeeId(randomUUID())
                .withContestedFeeType(FeeType.CONTESTED.name())
                .withContestedFeeStatus(FeeStatus.NOT_APPLICABLE.name())
                .withContestedFeePaymentReference(caseDetails.getContestedFeePaymentReference())
                .build();
    }

    private static String getInitialFeeStatus(final String feeStatus, String paymentReference) {
        if (feeStatus != null) {
            return feeStatus;
        } else if (paymentReference != null) {
            return FeeStatus.SATISFIED.name();
        }
        return DEFAULT_FEE_STATUS;
    }

    public static String getDefendantId(final Optional<Defendant> associatedDefendant) {
        return associatedDefendant.map(Defendant::getId).orElse(null);
    }

    public static Object populateIdpcDefendantMatchPendingEvent(final UUID cmsCaseId, final String urn, final UUID fileServiceId, final String materialType, final uk.gov.moj.cps.prosecutioncasefile.domain.event.Defendant defendant) {
        return idpcDefendantMatchPending()
                .withCaseId(cmsCaseId)
                .withCaseUrn(urn)
                .withFileServiceId(fileServiceId)
                .withMaterialType(materialType)
                .withDefendant(defendant)
                .build();
    }

    public static Object populateDefendantIdpcAlreadyExistsEvent(final String defendantId, final UUID fileServiceId, final UUID caseId) {

        return defendantIdpcAlreadyExists()
                .withDefendantId(defendantId)
                .withFileServiceId(fileServiceId)
                .withCaseId(caseId)
                .build();
    }

    public static Object validateMaterialWithDocumentDetailsV2(final AddMaterialCommonV2 addMaterialCommonV2, final ReferenceDataQueryService referenceDataQueryService, final CaseDocumentWithReferenceData caseDocumentWithReferenceData, CaseType caseType, final List<Defendant> defendants) {
        final List<Problem> materialRejections = validate(caseDocumentWithReferenceData, referenceDataQueryService, CCMaterialsValidationRuleProviderV2.getRejectionRules());

        final List<Problem> materialWarnings = materialRejections.stream()
                .filter(problem -> problem.getCode().equals(DUPLICATE_DEFENDANT.toString()))
                .flatMap(problem -> DefendantWarningsValidationRule.INSTANCE.validate(caseDocumentWithReferenceData, referenceDataQueryService).problems().stream())
                .collect(Collectors.toList());

        final MaterialAddedV2.Builder materialAddedV2builder = materialAddedV2();

        if (caseDocumentWithReferenceData.getDocumentTypeAccessReferenceData() != null) {
            materialAddedV2builder.withDocumentTypeId(caseDocumentWithReferenceData.getDocumentTypeAccessReferenceData().getId().toString());
            materialAddedV2builder.withDocumentCategory(caseDocumentWithReferenceData.getDocumentTypeAccessReferenceData().getDocumentCategory());
        }

        if (materialRejections.isEmpty()) {
            final List<Problem> materialPendingList = validate(caseDocumentWithReferenceData, referenceDataQueryService, CCMaterialsValidationRuleProviderV2.getPendingRules());
            if (materialPendingList.isEmpty()) {
                return materialAddedV2builder
                        .withSubmissionId(addMaterialCommonV2.getSubmissionId())
                        .withCaseId(addMaterialCommonV2.getCaseId())
                        .withCaseType(caseType.name())
                        .withMaterial(addMaterialCommonV2.getMaterial())
                        .withIsCpsCase(addMaterialCommonV2.getIsCpsCase())
                        .withReceivedDateTime(addMaterialCommonV2.getReceivedDateTime())
                        .withMaterialContentType(addMaterialCommonV2.getMaterialContentType())
                        .withMaterialType(caseDocumentWithReferenceData.getDocumentType())
                        .withMaterialName(addMaterialCommonV2.getMaterialName())
                        .withFileName(addMaterialCommonV2.getFileName())
                        .withDefendantId(caseDocumentWithReferenceData.getDefendantId())
                        .withDefendantName(getDefendantName(caseDocumentWithReferenceData.getDefendantId(), defendants))
                        .withSectionOrderSequence(addMaterialCommonV2.getSectionOrderSequence())
                        .withCaseSubFolderName(addMaterialCommonV2.getCaseSubFolderName())
                        .withExhibit(addMaterialCommonV2.getExhibit())
                        .withWitnessStatement(addMaterialCommonV2.getWitnessStatement())
                        .withTag(addMaterialCommonV2.getTag())
                        .withCourtApplicationSubject(addMaterialCommonV2.getCourtApplicationSubject())
                        .withProsecutionCaseSubject(addMaterialCommonV2.getProsecutionCaseSubject())
                        .withCpsFlag(caseDocumentWithReferenceData.isHeaderOuCodeCPS())
                        .build();
            } else {
                return ProsecutionCaseFileHelper.markMaterialAsPendingV2(addMaterialCommonV2, materialPendingList, caseDocumentWithReferenceData.isHeaderOuCodeCPS());
            }
        } else {
            if (materialWarnings.isEmpty()) {
                return materialRejectedV2()
                        .withSubmissionId(addMaterialCommonV2.getSubmissionId())
                        .withCaseId(addMaterialCommonV2.getCaseId())
                        .withMaterial(addMaterialCommonV2.getMaterial())
                        .withIsCpsCase(addMaterialCommonV2.getIsCpsCase())
                        .withReceivedDateTime(addMaterialCommonV2.getReceivedDateTime())
                        .withMaterialContentType(addMaterialCommonV2.getMaterialContentType())
                        .withMaterialType(caseDocumentWithReferenceData.getDocumentType())
                        .withMaterialName(addMaterialCommonV2.getMaterialName())
                        .withFileName(addMaterialCommonV2.getFileName())
                        .withSectionOrderSequence(addMaterialCommonV2.getSectionOrderSequence())
                        .withCaseSubFolderName(addMaterialCommonV2.getCaseSubFolderName())
                        .withExhibit(addMaterialCommonV2.getExhibit())
                        .withWitnessStatement(addMaterialCommonV2.getWitnessStatement())
                        .withTag(addMaterialCommonV2.getTag())
                        .withCourtApplicationSubject(addMaterialCommonV2.getCourtApplicationSubject())
                        .withProsecutionCaseSubject(addMaterialCommonV2.getProsecutionCaseSubject())
                        .withErrors(materialRejections)
                        .build();
            } else {
                return materialRejectedWithWarnings()
                        .withSubmissionId(addMaterialCommonV2.getSubmissionId())
                        .withCaseId(addMaterialCommonV2.getCaseId())
                        .withMaterial(addMaterialCommonV2.getMaterial())
                        .withIsCpsCase(addMaterialCommonV2.getIsCpsCase())
                        .withReceivedDateTime(addMaterialCommonV2.getReceivedDateTime())
                        .withMaterialContentType(addMaterialCommonV2.getMaterialContentType())
                        .withMaterialType(caseDocumentWithReferenceData.getDocumentType())
                        .withMaterialName(addMaterialCommonV2.getMaterialName())
                        .withFileName(addMaterialCommonV2.getFileName())
                        .withSectionOrderSequence(addMaterialCommonV2.getSectionOrderSequence())
                        .withCaseSubFolderName(addMaterialCommonV2.getCaseSubFolderName())
                        .withExhibit(addMaterialCommonV2.getExhibit())
                        .withWitnessStatement(addMaterialCommonV2.getWitnessStatement())
                        .withTag(addMaterialCommonV2.getTag())
                        .withCourtApplicationSubject(addMaterialCommonV2.getCourtApplicationSubject())
                        .withProsecutionCaseSubject(addMaterialCommonV2.getProsecutionCaseSubject())
                        .withWarnings(materialWarnings)
                        .withErrors(materialRejections)
                        .build();
            }
        }
    }

    public static List<Problem> getProblemFromDefendants(final List<Defendant> defendants, final DefendantSubject defendantSubject) {

        return defendants.stream().map(defendant -> getProblemFromDefendants(defendant, defendantSubject)).collect(Collectors.toList());

    }

    public static Problem getProblemFromDefendants(final Defendant defendant, final DefendantSubject defendantSubject) {
        final List<ProblemValue> problemValues = new ArrayList<>();
        if (defendant.getIndividual() != null) {
            final PersonalInformation personalInformation = defendant.getIndividual().getPersonalInformation();
            ofNullable(personalInformation.getTitle()).ifPresent(title -> problemValues.add(new ProblemValue(defendant.getId(), TITLE.getValue(), title)));
            ofNullable(personalInformation.getFirstName()).ifPresent(firstName -> problemValues.add(new ProblemValue(defendant.getId(), FORENAME.getValue(), firstName)));
            ofNullable(personalInformation.getGivenName2()).ifPresent(givenName -> problemValues.add(new ProblemValue(defendant.getId(), FORENAME2.getValue(), givenName)));
            ofNullable(personalInformation.getGivenName3()).ifPresent(givenName -> problemValues.add(new ProblemValue(defendant.getId(), FORENAME3.getValue(), givenName)));
            ofNullable(personalInformation.getLastName()).ifPresent(lastName -> problemValues.add(new ProblemValue(defendant.getId(), SURNAME.getValue(), lastName)));
            ofNullable(defendant.getIndividual()).map(Individual::getSelfDefinedInformation).map(SelfDefinedInformation::getDateOfBirth)
                    .ifPresent(dateOfBith -> problemValues.add(new ProblemValue(defendant.getId(), DATE_OF_BIRTH.getValue(), dateOfBith.toString())));
        }
        ofNullable(defendant.getAsn()).ifPresent(asn -> problemValues.add(new ProblemValue(defendant.getId(), ASN.getValue(), asn)));
        final String givenDefendantId;
        FieldName defendantFieldName = null;
        if (defendantSubject.getCpsPersonDefendantDetails() != null) {
            final CpsPersonDefendantDetails cpsPersonDefendantDetails = defendantSubject.getCpsPersonDefendantDetails();
            givenDefendantId = cpsPersonDefendantDetails.getCpsDefendantId();
            defendantFieldName = CPS_DEFENDANT_ID;
        } else if (defendantSubject.getProsecutorPersonDefendantDetails() != null) {
            final ProsecutorPersonDefendantDetails prosecutorPersonDefendantDetails = defendantSubject.getProsecutorPersonDefendantDetails();
            givenDefendantId = prosecutorPersonDefendantDetails.getProsecutorDefendantId();
            defendantFieldName = PROSECUTOR_DEFENDANT_ID;
        } else {
            givenDefendantId = null;
        }
        ofNullable(defendantFieldName).ifPresent(fieldName -> problemValues.add(new ProblemValue(defendant.getId(), fieldName.getValue(), givenDefendantId)));

        return newProblem(DEFENDANT_ON_CP, problemValues);
    }

    public static String getDefendantName(final UUID defendantId, final List<Defendant> defendants) {
        if (isNull(defendantId)) {
            return null;
        }
        if (isNull(defendants)) {
            return null;
        }

        return defendants.stream()
                .filter(defendant -> defendant.getId().equals(defendantId.toString()))
                .map(defendant -> {
                    final Individual individual = defendant.getIndividual();
                    final String organisationName = defendant.getOrganisationName();
                    String fullName = null;

                    if (nonNull(individual) && nonNull(individual.getPersonalInformation())) {
                        final PersonalInformation personalInformation = individual.getPersonalInformation();
                        fullName = format("%s %s", personalInformation.getFirstName(), personalInformation.getLastName());
                    } else if (nonNull(organisationName) && !organisationName.isEmpty()) {
                        fullName = defendant.getOrganisationName();
                    }
                    return fullName;
                }).findFirst().orElse(null);
    }

    public static List<ProblemValue> matchDefendants(final CaseDocumentWithReferenceData caseDocumentWithReferenceData, final DefendantSubject defendantSubject, final List<Defendant> matchedDefendants) {
        final List<ProblemValue> problemValues = new ArrayList<>();
        List<ProblemValue> problemValuesForDefendant = new ArrayList<>();
        for (final Defendant defendant : caseDocumentWithReferenceData.getDefendants()) {
            problemValuesForDefendant.clear();

            if (defendant.getIndividual() != null) {
                if (defendantSubject.getCpsPersonDefendantDetails() != null) {
                    problemValuesForDefendant = checkPersonalData(defendantSubject.getCpsPersonDefendantDetails(), defendant);
                }
                if (defendantSubject.getProsecutorPersonDefendantDetails() != null) {
                    problemValuesForDefendant = checkPersonalData(defendantSubject.getProsecutorPersonDefendantDetails(), defendant);
                }
            }
            if (problemValuesForDefendant.isEmpty() && defendant.getIndividual() != null) {
                problemValues.clear();
                matchedDefendants.add(defendant);
            } else {
                problemValues.addAll(problemValuesForDefendant);
            }
        }
        return problemValues;
    }

    public static List<ProblemValue> matchOrganisationsInDefendants(final CaseDocumentWithReferenceData caseDocumentWithReferenceData, final DefendantSubject defendantSubject, final List<Defendant> matchedDefendants) {
        final List<ProblemValue> problemValues = new ArrayList<>();
        final List<ProblemValue> problemValuesForDefendant = new ArrayList<>();
        for (final Defendant defendant : caseDocumentWithReferenceData.getDefendants()) {
            problemValuesForDefendant.clear();
            if (defendant.getIndividual() == null) {
                if (defendantSubject.getCpsOrganisationDefendantDetails() != null) {
                    validateField(defendant.getId(), defendant.getOrganisationName(), defendantSubject.getCpsOrganisationDefendantDetails().getOrganisationName(), ORGANISATION_NAME).ifPresent(problemValuesForDefendant::add);
                }
                if (defendantSubject.getProsecutorOrganisationDefendantDetails() != null) {
                    validateField(defendant.getId(), defendant.getOrganisationName(), defendantSubject.getProsecutorOrganisationDefendantDetails().getOrganisationName(), ORGANISATION_NAME).ifPresent(problemValuesForDefendant::add);
                }
            }
            if (problemValuesForDefendant.isEmpty() && defendant.getIndividual() == null) {
                problemValues.clear();
                matchedDefendants.add(defendant);
            } else {
                problemValues.addAll(problemValuesForDefendant);
            }
        }
        return problemValues;
    }

    private static List<ProblemValue> checkPersonalData(final CpsPersonDefendantDetails cpsPersonDefendantDetails, final Defendant defendant) {
        final List<ProblemValue> problemValues = new ArrayList<>();
        final PersonalInformation personalInformation = defendant.getIndividual().getPersonalInformation();
        validateField(defendant.getId(), personalInformation.getTitle(), cpsPersonDefendantDetails.getTitle(), TITLE).ifPresent(problemValues::add);
        validateField(defendant.getId(), personalInformation.getFirstName(), cpsPersonDefendantDetails.getForename(), FORENAME).ifPresent(problemValues::add);
        validateField(defendant.getId(), personalInformation.getGivenName2(), cpsPersonDefendantDetails.getForename2(), FORENAME2).ifPresent(problemValues::add);
        validateField(defendant.getId(), personalInformation.getGivenName3(), cpsPersonDefendantDetails.getForename3(), FORENAME3).ifPresent(problemValues::add);
        validateField(defendant.getId(), personalInformation.getLastName(), cpsPersonDefendantDetails.getSurname(), SURNAME).ifPresent(problemValues::add);
        validateDateOfBirth(defendant.getId(), defendant, cpsPersonDefendantDetails.getDateOfBirth()).ifPresent(problemValues::add);
        return problemValues;
    }

    private static List<ProblemValue> checkPersonalData(final ProsecutorPersonDefendantDetails prosecutorPersonDefendantDetails, final Defendant defendant) {
        final List<ProblemValue> problemValues = new ArrayList<>();
        final PersonalInformation personalInformation = defendant.getIndividual().getPersonalInformation();
        validateField(defendant.getId(), personalInformation.getTitle(), prosecutorPersonDefendantDetails.getTitle(), TITLE).ifPresent(problemValues::add);
        validateField(defendant.getId(), personalInformation.getFirstName(), prosecutorPersonDefendantDetails.getForename(), FORENAME).ifPresent(problemValues::add);
        validateField(defendant.getId(), personalInformation.getGivenName2(), prosecutorPersonDefendantDetails.getForename2(), FORENAME2).ifPresent(problemValues::add);
        validateField(defendant.getId(), personalInformation.getGivenName3(), prosecutorPersonDefendantDetails.getForename3(), FORENAME3).ifPresent(problemValues::add);
        validateField(defendant.getId(), personalInformation.getLastName(), prosecutorPersonDefendantDetails.getSurname(), SURNAME).ifPresent(problemValues::add);
        validateDateOfBirth(defendant.getId(), defendant, prosecutorPersonDefendantDetails.getDateOfBirth()).ifPresent(problemValues::add);
        return problemValues;
    }

    private static Optional<ProblemValue> validateField(final String defendantId, final String defendantValue, final String givenValue, final FieldName fieldName) {
        if (isNotBlank(givenValue) && !givenValue.equalsIgnoreCase(defendantValue)) {
            return of(new ProblemValue(defendantId, fieldName.getValue(), givenValue));
        }
        return empty();
    }

    private static Optional<ProblemValue> validateDateOfBirth(final String defendantId, final Defendant defendant, final String givenBirthDate) {
        if (isBlank(givenBirthDate) || defendant.getIndividual() == null || defendant.getIndividual().getSelfDefinedInformation() == null || defendant.getIndividual().getSelfDefinedInformation().getDateOfBirth() == null) {
            return empty();
        }

        if (!parse(givenBirthDate).equals(defendant.getIndividual().getSelfDefinedInformation().getDateOfBirth())) {
            return of(new ProblemValue(defendantId, DATE_OF_BIRTH.getValue(), givenBirthDate));
        }
        return empty();
    }

    public static String getDefendantId(final DefendantSubject defendantSubject) {
        return getProsecutorDefendantId(defendantSubject).orElse(getCpsDefendantId(defendantSubject).orElse(""));
    }

    public static Optional<String> getProsecutorDefendantId(final DefendantSubject defendantSubject) {
        if (!isEmpty(defendantSubject.getProsecutorDefendantId())) {
            return ofNullable(defendantSubject.getProsecutorDefendantId());
        } else if (defendantSubject.getProsecutorPersonDefendantDetails() != null && !isEmpty(defendantSubject.getProsecutorPersonDefendantDetails().getProsecutorDefendantId())) {
            return ofNullable(defendantSubject.getProsecutorPersonDefendantDetails().getProsecutorDefendantId());
        } else if (defendantSubject.getProsecutorOrganisationDefendantDetails() != null && !isEmpty(defendantSubject.getProsecutorOrganisationDefendantDetails().getProsecutorDefendantId())) {
            return ofNullable(defendantSubject.getProsecutorOrganisationDefendantDetails().getProsecutorDefendantId());
        }
        return empty();
    }

    public static Optional<String> getCpsDefendantId(final DefendantSubject defendantSubject) {
        if (!isEmpty(defendantSubject.getCpsDefendantId())) {
            return ofNullable(defendantSubject.getCpsDefendantId());
        } else if (defendantSubject.getCpsPersonDefendantDetails() != null && !isEmpty(defendantSubject.getCpsPersonDefendantDetails().getCpsDefendantId())) {
            return ofNullable(defendantSubject.getCpsPersonDefendantDetails().getCpsDefendantId());
        } else if (defendantSubject.getCpsOrganisationDefendantDetails() != null && !isEmpty(defendantSubject.getCpsOrganisationDefendantDetails().getCpsDefendantId())) {
            return ofNullable(defendantSubject.getCpsOrganisationDefendantDetails().getCpsDefendantId());
        }
        return empty();
    }
}
