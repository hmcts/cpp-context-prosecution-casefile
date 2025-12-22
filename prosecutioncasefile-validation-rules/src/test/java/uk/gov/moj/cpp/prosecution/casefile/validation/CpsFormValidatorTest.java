package uk.gov.moj.cpp.prosecution.casefile.validation;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.ASSOCIATED_PERSON;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.AUTHORITY_DETAILS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.BCM_DEFENDANTS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.CPS_OFFENCES;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.DEFENCE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.DEFENDANTS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.FORM_DEFENDANTS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.GUARDIAN_DETAILS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.ID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.NAME;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.OFFENCE_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.PET_DEFENDANTS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.PROSECUTION;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.PROSECUTOR_OFFENCES;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.PTPH_DEFENDANTS;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.prosecution.casefile.domain.FormValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.AsnValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.CpsDefendantIdValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.NameAndDOBValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.utils.FileUtil;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmissionStatus;

import java.util.List;

import javax.enterprise.inject.Instance;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CpsFormValidatorTest {

    public static final String CPS_DEFENDANT_ID = "cpsDefendantId";
    public static final String DEFENDANT_ID = "defendantId";

    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Mock
    private Instance<ValidationRule> validatorRules;

    @InjectMocks
    private final AsnValidationRule cpsFormAsnValidationRule = new AsnValidationRule();

    @InjectMocks
    private final CpsDefendantIdValidationRule cpsDefendantIdValidationRule = new CpsDefendantIdValidationRule();

    @InjectMocks
    private final NameAndDOBValidationRule nameAndDOBValidationRule = new NameAndDOBValidationRule();

    @InjectMocks
    private CpsFormValidator cpsFormValidator;

    @BeforeEach
    public void setup() {
        List<ValidationRule> validationRules = asList(cpsFormAsnValidationRule, cpsDefendantIdValidationRule, nameAndDOBValidationRule);
        when(validatorRules.spliterator()).thenReturn(validationRules.spliterator());

    }

    @Test
    public void shouldReturnJsonWhen_AllDefendantAsnMatched_AllValidOffences() {
        List<String> validOffences = asList("OFFENCE_CODE_1", "OFFENCE_CODE_11", "OFFENCE_CODE_111", "OFFENCE_CODE_2", "OFFENCE_CODE_22");
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-pet.json");
        final JsonObject processReceivedPpsServePet = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormData(processReceivedPpsServePet, prosecutionCaseFileCase, validOffences, null);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.SUCCESS));
        assertThat(formValidationResult.getErrorList(), hasSize(0));

        assertThat(formValidationResult.getPetDefendants().getJsonArray(PET_DEFENDANTS), hasSize(2));
        final List<JsonObject> petDefendants = formValidationResult.getPetDefendants().getJsonArray(PET_DEFENDANTS)
                .getValuesAs(JsonObject.class);
        assertThat(petDefendants.get(0).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));
        assertThat(petDefendants.get(1).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));

        assertThat(formValidationResult.getPetFormData(), notNullValue());
        assertThat(formValidationResult.getPetFormData().getJsonObject(PROSECUTION), notNullValue());

        assertThat(formValidationResult.getPetFormData().getJsonObject(DEFENCE), notNullValue());
        final JsonObject defenceFromPetForm = formValidationResult.getPetFormData().getJsonObject(DEFENCE);
        assertThat(defenceFromPetForm.getJsonArray(DEFENDANTS), hasSize(2));
        final List<JsonObject> defendantListFromPetForm = defenceFromPetForm.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class);
        assertThat(defendantListFromPetForm.get(0).getString(ID), is(defendantIdOne));
        assertThat(defendantListFromPetForm.get(0).getJsonArray(CPS_OFFENCES), hasSize(3));
        final List<JsonObject> cpsOffencesListFromPetFormDefendantOne = defendantListFromPetForm.get(0).getJsonArray(CPS_OFFENCES).getValuesAs(JsonObject.class);
        assertThat(cpsOffencesListFromPetFormDefendantOne.get(0).getString(OFFENCE_CODE), is("OFFENCE_CODE_1"));
        assertThat(cpsOffencesListFromPetFormDefendantOne.get(1).getString(OFFENCE_CODE), is("OFFENCE_CODE_11"));
        assertThat(cpsOffencesListFromPetFormDefendantOne.get(2).getString(OFFENCE_CODE), is("OFFENCE_CODE_111"));
        assertThat(defendantListFromPetForm.get(1).getString(ID), is(defendantIdTwo));

        assertThat(defendantListFromPetForm.get(1).getJsonArray(CPS_OFFENCES), hasSize(2));
        final List<JsonObject> cpsOffencesListFromPetFormDefendantTwo = defendantListFromPetForm.get(1).getJsonArray(CPS_OFFENCES).getValuesAs(JsonObject.class);
        assertThat(cpsOffencesListFromPetFormDefendantTwo.get(0).getString(OFFENCE_CODE), is("OFFENCE_CODE_2"));
        assertThat(cpsOffencesListFromPetFormDefendantTwo.get(1).getString(OFFENCE_CODE), is("OFFENCE_CODE_22"));

        assertThat(defendantListFromPetForm.get(0).getJsonObject(ASSOCIATED_PERSON), notNullValue());
        assertThat(defendantListFromPetForm.get(0).getJsonObject(ASSOCIATED_PERSON).getJsonObject(AUTHORITY_DETAILS), notNullValue());
        assertThat(defendantListFromPetForm.get(0).getJsonObject(ASSOCIATED_PERSON).getJsonObject(GUARDIAN_DETAILS), notNullValue());
        assertThat(defendantListFromPetForm.get(0).getJsonObject(ASSOCIATED_PERSON).getJsonObject(AUTHORITY_DETAILS).getString(NAME), is("forename surname"));
        assertThat(defendantListFromPetForm.get(0).getJsonObject(ASSOCIATED_PERSON).getJsonObject(GUARDIAN_DETAILS).getString(NAME), is("parentForename surname"));
    }

    @Test
    public void shouldReturnJsonWhen_AllDefendantAsnMatched_AllValidOffencesBcm() {
        List<String> validOffences = asList("OFFENCE_CODE_1", "OFFENCE_CODE_11", "OFFENCE_CODE_111", "OFFENCE_CODE_2", "OFFENCE_CODE_22");
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-bcm.json");
        final JsonObject processReceivedPpsServeBcm = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefileBcm.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormDataBcm(processReceivedPpsServeBcm, prosecutionCaseFileCase, validOffences, null);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.SUCCESS));
        assertThat(formValidationResult.getErrorList(), hasSize(0));

        assertThat(formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS), hasSize(2));
        final List<JsonObject> bcmDefendants = formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS)
                .getValuesAs(JsonObject.class);
        assertThat(bcmDefendants.get(0).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));
        assertThat(bcmDefendants.get(1).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));

        assertThat(formValidationResult.getFormData(), notNullValue());

        assertThat(formValidationResult.getFormData(), notNullValue());
        final JsonObject defenceFromBcmForm = formValidationResult.getFormData();
        assertThat(defenceFromBcmForm.getJsonArray(BCM_DEFENDANTS), hasSize(2));
        final List<JsonObject> defendantListFromBcmForm = defenceFromBcmForm.getJsonArray(BCM_DEFENDANTS).getValuesAs(JsonObject.class);
        assertThat(defendantListFromBcmForm.get(0).getString(ID), is(defendantIdOne));
        assertThat(defendantListFromBcmForm.get(0).getJsonArray(PROSECUTOR_OFFENCES), hasSize(3));
        final List<JsonObject> prosecutorOffencesListFromPetFormDefendantOne = defendantListFromBcmForm.get(0).getJsonArray(PROSECUTOR_OFFENCES).getValuesAs(JsonObject.class);
        assertThat(prosecutorOffencesListFromPetFormDefendantOne.get(0).getString(OFFENCE_CODE), is("OFFENCE_CODE_1"));
        assertThat(prosecutorOffencesListFromPetFormDefendantOne.get(1).getString(OFFENCE_CODE), is("OFFENCE_CODE_11"));
        assertThat(prosecutorOffencesListFromPetFormDefendantOne.get(2).getString(OFFENCE_CODE), is("OFFENCE_CODE_111"));
        assertThat(defendantListFromBcmForm.get(1).getString(ID), is(defendantIdTwo));

        assertThat(defendantListFromBcmForm.get(1).getJsonArray(PROSECUTOR_OFFENCES), hasSize(2));
        final List<JsonObject> cpsOffencesListFromBcmFormDefendantTwo = defendantListFromBcmForm.get(1).getJsonArray(PROSECUTOR_OFFENCES).getValuesAs(JsonObject.class);
        assertThat(cpsOffencesListFromBcmFormDefendantTwo.get(0).getString(OFFENCE_CODE), is("OFFENCE_CODE_2"));
        assertThat(cpsOffencesListFromBcmFormDefendantTwo.get(1).getString(OFFENCE_CODE), is("OFFENCE_CODE_22"));
    }

    @Test
    public void shouldReturnJsonWhen_AllDefendantAsnMatched_Ptph() {
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-ptph.json");
        final JsonObject processReceivedPpsServePtph = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefilePtph.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormDataPtph(processReceivedPpsServePtph, prosecutionCaseFileCase, null, objectToJsonObjectConverter);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.SUCCESS));
        assertThat(formValidationResult.getErrorList(), hasSize(0));

        assertThat(formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS), hasSize(2));
        final List<JsonObject> ptphDefendants = formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS)
                .getValuesAs(JsonObject.class);
        assertThat(ptphDefendants.get(0).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));
        assertThat(ptphDefendants.get(1).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));

        assertThat(formValidationResult.getFormData(), notNullValue());

        assertThat(formValidationResult.getFormData(), notNullValue());
        final JsonObject defenceFromPtphForm = formValidationResult.getFormData();
        assertThat(defenceFromPtphForm.getJsonArray(PTPH_DEFENDANTS), hasSize(2));
        final List<JsonObject> defendantListFromPtphForm = defenceFromPtphForm.getJsonArray(PTPH_DEFENDANTS).getValuesAs(JsonObject.class);
        assertThat(defendantListFromPtphForm.get(0).getString(ID), is(defendantIdOne));
        assertThat(defendantListFromPtphForm.get(1).getString(ID), is(defendantIdTwo));

    }

    @Test
    public void shouldReturnJsonWhen_AllDefendantAsnMatched_ptph() {
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-ptph.json");
        final JsonObject processReceivedPpsServePtph = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefilePtph.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormDataPtph(processReceivedPpsServePtph, prosecutionCaseFileCase, null, objectToJsonObjectConverter);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.SUCCESS));

        assertThat(formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS), hasSize(2));
        final List<JsonObject> ptphDefendants = formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS)
                .getValuesAs(JsonObject.class);
        assertThat(ptphDefendants.get(0).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));
        assertThat(ptphDefendants.get(1).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));

        assertThat(formValidationResult.getFormData(), notNullValue());
        final JsonObject defenceFromPtphForm = formValidationResult.getFormData();
        assertThat(defenceFromPtphForm.getJsonArray(PTPH_DEFENDANTS), hasSize(2));
        final List<JsonObject> defendantListFromPtphForm = defenceFromPtphForm.getJsonArray(PTPH_DEFENDANTS).getValuesAs(JsonObject.class);
        assertThat(defendantListFromPtphForm.get(0).getString(ID), is(defendantIdOne));
        assertThat(defendantListFromPtphForm.get(1).getString(ID), is(defendantIdTwo));
    }

    @Test
    public void shouldReturnJsonWhen_OnlySomeDefendantAsnMatched_AllValidOffences() {
        List<String> validOffences = asList("OFFENCE_CODE_1", "OFFENCE_CODE_11", "OFFENCE_CODE_111", "OFFENCE_CODE_2", "OFFENCE_CODE_22");
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-pet.json")
                .replaceAll("ASN_1", "invalidAsn");

        final JsonObject processReceivedPpsServePet = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final JsonArray defendantIdsArray = Json.createArrayBuilder().add(createObjectBuilder()
                .add(DEFENDANT_ID, defendantIdOne)
                .add(CPS_DEFENDANT_ID, randomUUID().toString())
                .build())
                .build();

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormData(processReceivedPpsServePet, prosecutionCaseFileCase, validOffences, defendantIdsArray);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.REJECTED));
        assertThat(formValidationResult.getErrorList(), hasSize(1));
        List<Problem> errorList = formValidationResult.getErrorList();
        assertThat(errorList.get(0).getCode(), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(errorList.get(0).getValues().get(0).getKey(), is(FormConstant.FORENAME));
        assertThat(errorList.get(0).getValues().get(0).getValue(), is("John"));

        assertThat(formValidationResult.getPetDefendants().getJsonArray(PET_DEFENDANTS), hasSize(1));
        final List<JsonObject> petDefendants = formValidationResult.getPetDefendants().getJsonArray(PET_DEFENDANTS)
                .getValuesAs(JsonObject.class);
        assertThat(petDefendants.get(0).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));

        assertThat(formValidationResult.getPetFormData(), notNullValue());
        assertThat(formValidationResult.getPetFormData().getJsonObject(PROSECUTION), notNullValue());

        assertThat(formValidationResult.getPetFormData().getJsonObject(DEFENCE), notNullValue());
        final JsonObject defenceFromPetForm = formValidationResult.getPetFormData().getJsonObject(DEFENCE);
        assertThat(defenceFromPetForm.getJsonArray(DEFENDANTS), hasSize(1));
        final List<JsonObject> defendantListFromPetForm = defenceFromPetForm.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class);
        assertThat(defendantListFromPetForm.get(0).getString(ID), is(defendantIdTwo));
        assertThat(defendantListFromPetForm.get(0).getJsonArray(CPS_OFFENCES), hasSize(2));
        final List<JsonObject> cpsOffencesListFromPetFormDefendantTwo = defendantListFromPetForm.get(0).getJsonArray(CPS_OFFENCES).getValuesAs(JsonObject.class);
        assertThat(cpsOffencesListFromPetFormDefendantTwo.get(0).getString(OFFENCE_CODE), is("OFFENCE_CODE_2"));
        assertThat(cpsOffencesListFromPetFormDefendantTwo.get(1).getString(OFFENCE_CODE), is("OFFENCE_CODE_22"));

    }

    @Test
    public void shouldReturnJsonWhen_OnlySomeDefendantAsnMatched_NoCpsDefendantIdInCase_MatchingOnNameOnly_AllValidOffencesBcm() {
        List<String> validOffences = asList("OFFENCE_CODE_1", "OFFENCE_CODE_11", "OFFENCE_CODE_111", "OFFENCE_CODE_2", "OFFENCE_CODE_22");
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-bcm.json")
                .replaceAll("ASN_1", "invalidAsn");

        final JsonObject processReceivedPpsServeBcm = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefileBcm.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final JsonArray defendantIdsArray = Json.createArrayBuilder().add(createObjectBuilder()
                .add(DEFENDANT_ID, defendantIdOne)
                .add(CPS_DEFENDANT_ID, randomUUID().toString())
                .build())
                .build();

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormDataBcm(processReceivedPpsServeBcm, prosecutionCaseFileCase, validOffences, defendantIdsArray);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.REJECTED));
        assertThat(formValidationResult.getErrorList(), hasSize(1));
        List<Problem> errorList = formValidationResult.getErrorList();
        assertThat(errorList.get(0).getCode(), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(errorList.get(0).getValues().get(0).getKey(), is(FormConstant.FORENAME));
        assertThat(errorList.get(0).getValues().get(0).getValue(), is("John"));

        assertThat(formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS), hasSize(1));
        final List<JsonObject> bcmDefendants = formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS)
                .getValuesAs(JsonObject.class);
        assertThat(bcmDefendants.get(0).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));

        assertThat(formValidationResult.getFormData(), notNullValue());


        assertThat(formValidationResult.getFormData(), notNullValue());
        final JsonObject defenceFromBcmForm = formValidationResult.getFormData();
        assertThat(defenceFromBcmForm.getJsonArray(BCM_DEFENDANTS), hasSize(1));
        final List<JsonObject> defendantListFromBcmForm = defenceFromBcmForm.getJsonArray(BCM_DEFENDANTS).getValuesAs(JsonObject.class);
        assertThat(defendantListFromBcmForm.get(0).getString(ID), is(defendantIdTwo));
        assertThat(defendantListFromBcmForm.get(0).getJsonArray(PROSECUTOR_OFFENCES), hasSize(2));
        final List<JsonObject> cpsOffencesListFromBcmFormDefendantTwo = defendantListFromBcmForm.get(0).getJsonArray(PROSECUTOR_OFFENCES).getValuesAs(JsonObject.class);
        assertThat(cpsOffencesListFromBcmFormDefendantTwo.get(0).getString(OFFENCE_CODE), is("OFFENCE_CODE_2"));
        assertThat(cpsOffencesListFromBcmFormDefendantTwo.get(1).getString(OFFENCE_CODE), is("OFFENCE_CODE_22"));

    }

    @Test
    public void shouldReturnJsonWhen_OnlySomeDefendantAsnMatched_NoCpsDefendantIdsPresentInCase_MatchOnNameDob_ptph() {
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-ptph.json")
                .replaceAll("ASN_1", "invalidAsn");

        final JsonObject processReceivedPpsServePtph = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefilePtph.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final JsonArray defendantIdsArray = Json.createArrayBuilder().add(createObjectBuilder()
                .add(DEFENDANT_ID, defendantIdOne)
                .add(CPS_DEFENDANT_ID, randomUUID().toString())
                .build())
                .build();

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormDataPtph(processReceivedPpsServePtph, prosecutionCaseFileCase, defendantIdsArray, objectToJsonObjectConverter);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.REJECTED));
        assertThat(formValidationResult.getErrorList(), hasSize(1));
        List<Problem> errorList = formValidationResult.getErrorList();
        assertThat(errorList.get(0).getCode(), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(errorList.get(0).getValues().get(0).getKey(), is(FormConstant.FORENAME));
        assertThat(errorList.get(0).getValues().get(0).getValue(), is("John"));

        assertThat(formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS), hasSize(1));
        final List<JsonObject> bcmDefendants = formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS)
                .getValuesAs(JsonObject.class);
        assertThat(bcmDefendants.get(0).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));

        assertThat(formValidationResult.getFormData(), notNullValue());


        assertThat(formValidationResult.getFormData(), notNullValue());
        final JsonObject defenceFromPtphForm = formValidationResult.getFormData();
        assertThat(defenceFromPtphForm.getJsonArray(PTPH_DEFENDANTS), hasSize(1));
        final List<JsonObject> defendantListFromBcmForm = defenceFromPtphForm.getJsonArray(PTPH_DEFENDANTS).getValuesAs(JsonObject.class);
        assertThat(defendantListFromBcmForm.get(0).getString(ID), is(defendantIdTwo));
    }

    @Test
    public void shouldReturnJsonWhen_NoneOfDefendantAsnMatched_NoCpsDefendantIdInCase_MatchingOnNameOnly_AllValidOffences() {
        List<String> validOffences = asList("OFFENCE_CODE_1", "OFFENCE_CODE_11", "OFFENCE_CODE_111", "OFFENCE_CODE_2", "OFFENCE_CODE_22");
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-pet.json")
                .replaceAll("ASN_1", "invalidAsn1")
                .replaceAll("ASN_2", "invalidAsn2");

        final JsonObject processReceivedPpsServePet = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final JsonArray defendantIdsArray = Json.createArrayBuilder().add(createObjectBuilder()
                .add(DEFENDANT_ID, defendantIdOne)
                .add(CPS_DEFENDANT_ID, randomUUID().toString())
                .build())
                .build();

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormData(processReceivedPpsServePet, prosecutionCaseFileCase, validOffences, defendantIdsArray);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.REJECTED));
        assertThat(formValidationResult.getErrorList(), hasSize(2));
        List<Problem> errorList = formValidationResult.getErrorList();
        assertThat(errorList.get(0).getCode(), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(errorList.get(0).getValues().get(0).getKey(), is(FormConstant.FORENAME));
        assertThat(errorList.get(0).getValues().get(0).getValue(), is("John"));

        assertThat(errorList.get(1).getCode(), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(errorList.get(1).getValues().get(0).getKey(), is(FormConstant.ORGANISATION_NAME));
        assertThat(errorList.get(1).getValues().get(0).getValue(), is("XYZ Limited"));

        assertThat(formValidationResult.getPetDefendants(), nullValue());

        assertThat(formValidationResult.getPetFormData(), notNullValue());
        assertThat(formValidationResult.getPetFormData().getJsonObject(PROSECUTION), notNullValue());

        assertThat(formValidationResult.getPetFormData(), notNullValue());
        assertThat(formValidationResult.getPetFormData().getJsonObject(PROSECUTION), notNullValue());
        assertThat(formValidationResult.getPetFormData().getJsonObject(DEFENCE), notNullValue());
    }

    @Test
    public void shouldReturnJsonWhen_NoneOfDefendantAsnMatched_NoCpsDefendantIdInCase_MatchingOnNameOnly_AllValidOffencesBcm() {
        List<String> validOffences = asList("OFFENCE_CODE_1", "OFFENCE_CODE_11", "OFFENCE_CODE_111", "OFFENCE_CODE_2", "OFFENCE_CODE_22");
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-bcm.json")
                .replaceAll("ASN_1", "invalidAsn1")
                .replaceAll("ASN_2", "invalidAsn2");

        final JsonObject processReceivedPpsServeBcm = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefileBcm.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final JsonArray defendantIdsArray = Json.createArrayBuilder().add(createObjectBuilder()
                .add(DEFENDANT_ID, defendantIdOne)
                .add(CPS_DEFENDANT_ID, randomUUID().toString())
                .build())
                .build();

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormDataBcm(processReceivedPpsServeBcm, prosecutionCaseFileCase, validOffences, defendantIdsArray);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.REJECTED));
        assertThat(formValidationResult.getErrorList(), hasSize(2));
        List<Problem> errorList = formValidationResult.getErrorList();
        assertThat(errorList.get(0).getCode(), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(errorList.get(0).getValues().get(0).getKey(), is(FormConstant.FORENAME));
        assertThat(errorList.get(0).getValues().get(0).getValue(), is("John"));

        assertThat(errorList.get(1).getCode(), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(errorList.get(1).getValues().get(0).getKey(), is(FormConstant.ORGANISATION_NAME));
        assertThat(errorList.get(1).getValues().get(0).getValue(), is("XYZ Limited"));

        assertThat(formValidationResult.getFormDefendants(), nullValue());

    }

    @Test
    public void shouldReturnJsonWhen_NoneOfDefendantAsnMatched_NoCpsDefendantIdInCase_MatchingOnNameOnly_ptph() {
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-ptph.json")
                .replaceAll("ASN_1", "invalidAsn1")
                .replaceAll("ASN_2", "invalidAsn2");

        final JsonObject processReceivedPpsServePtph = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefilePtph.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final JsonArray defendantIdsArray = Json.createArrayBuilder().add(createObjectBuilder()
                .add(DEFENDANT_ID, defendantIdOne)
                .add(CPS_DEFENDANT_ID, randomUUID().toString())
                .build())
                .build();

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormDataPtph(processReceivedPpsServePtph, prosecutionCaseFileCase, defendantIdsArray, objectToJsonObjectConverter);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.REJECTED));
        assertThat(formValidationResult.getErrorList(), hasSize(2));
        List<Problem> errorList = formValidationResult.getErrorList();
        assertThat(errorList.get(0).getCode(), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(errorList.get(0).getValues().get(0).getKey(), is(FormConstant.FORENAME));
        assertThat(errorList.get(0).getValues().get(0).getValue(), is("John"));

        assertThat(errorList.get(1).getCode(), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(errorList.get(1).getValues().get(0).getKey(), is(FormConstant.ORGANISATION_NAME));
        assertThat(errorList.get(1).getValues().get(0).getValue(), is("XYZ Limited"));

        assertThat(formValidationResult.getFormDefendants(), nullValue());

    }

    @Test
    public void shouldReturnJsonWhen_OnlySomeDefendantAsnMatched_NoCpsDefendantIdInCase_MatchingOnNameOnly_ptphResultingInSuccessWithWarnings() {
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-ptph.json")
                .replaceAll("ASN_1", "invalidAsn");
        final JsonObject processReceivedPpsServePtph = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefilePtph.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final JsonArray defendantIdsArray = Json.createArrayBuilder().add(createObjectBuilder()
                .add(DEFENDANT_ID, defendantIdOne)
                .add(CPS_DEFENDANT_ID, randomUUID().toString())
                .build())
                .build();

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormDataPtph(processReceivedPpsServePtph, prosecutionCaseFileCase, defendantIdsArray, objectToJsonObjectConverter);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.REJECTED));
        assertThat(formValidationResult.getErrorList(), hasSize(1));
        List<Problem> errorList = formValidationResult.getErrorList();
        assertThat(errorList.get(0).getCode(), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(errorList.get(0).getValues().get(0).getKey(), is(FormConstant.FORENAME));
        assertThat(errorList.get(0).getValues().get(0).getValue(), is("John"));


        assertThat(formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS), hasSize(1));
        final List<JsonObject> ptphDefendants = formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS)
                .getValuesAs(JsonObject.class);
        assertThat(ptphDefendants.get(0).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));

        assertThat(formValidationResult.getFormData(), notNullValue());


        assertThat(formValidationResult.getFormData(), notNullValue());
        final JsonObject defenceFromBcmForm = formValidationResult.getFormData();
        assertThat(defenceFromBcmForm.getJsonArray(PTPH_DEFENDANTS), hasSize(1));
        final List<JsonObject> defendantListFromPtphForm = defenceFromBcmForm.getJsonArray(PTPH_DEFENDANTS).getValuesAs(JsonObject.class);
        assertThat(defendantListFromPtphForm.get(0).getString(ID), is(defendantIdTwo));

    }

    @Test
    public void shouldReturnRejectWhen_NoCpsDefendantHaveAsn_AllValidOffences_AllCaseDefendantsHaveAsn() {
        List<String> validOffences = asList("OFFENCE_CODE_1", "OFFENCE_CODE_11", "OFFENCE_CODE_111", "OFFENCE_CODE_2", "OFFENCE_CODE_22");
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String validForName = "John1";
        final String validSurName = "Lewis1";
        final String validDob = "03-06-1986";
        final String validOrgName = "organisationName";
        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-pet-all-defendant-data.json")
                .replaceAll("FOR_NAME", validForName)
                .replaceAll("SUR_NAME", validSurName)
                .replaceAll("DATE_OF_BIRTH", validDob)
                .replaceAll("ORGANISATION_NAME", validOrgName);
        final JsonObject processReceivedCpsServePet = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.case_withalldefendantshaveasn.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder().build();

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormData(processReceivedCpsServePet, prosecutionCaseFileCase, validOffences, defendantIdsAsJsonArrayFromProsecutionCase);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.REJECTED));
        assertThat(formValidationResult.getErrorList(), hasSize(2));

    }

    @Test
    public void shouldReturnJsonWhen_NoCpsDefendantHaveAsn_AllDefendantDataMatched_AllValidOffences_NotAllCaseDefendantsHaveAsn() {
        List<String> validOffences = asList("OFFENCE_CODE_1", "OFFENCE_CODE_11", "OFFENCE_CODE_111", "OFFENCE_CODE_2", "OFFENCE_CODE_22");
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String validForName = "John1";
        final String validSurName = "Lewis1";
        final String validDob = "03-06-1986";
        final String validOrgName = "organisationName";
        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-pet-all-defendant-data.json")
                .replaceAll("FOR_NAME", validForName)
                .replaceAll("SUR_NAME", validSurName)
                .replaceAll("DATE_OF_BIRTH", validDob)
                .replaceAll("ORGANISATION_NAME", validOrgName);
        final JsonObject processReceivedCpsServePet = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.case_withnoasn_defendants.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantIdOne.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsACpsDefendantId")
                        .build())
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantIdTwo.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsAnotherCpsDefendantId")
                        .build())
                .build();

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormData(processReceivedCpsServePet, prosecutionCaseFileCase, validOffences, defendantIdsAsJsonArrayFromProsecutionCase);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.SUCCESS));
        assertThat(formValidationResult.getErrorList(), hasSize(0));

        assertThat(formValidationResult.getPetDefendants().getJsonArray(PET_DEFENDANTS), hasSize(2));
        final List<JsonObject> petDefendants = formValidationResult.getPetDefendants().getJsonArray(PET_DEFENDANTS)
                .getValuesAs(JsonObject.class);
        assertThat(petDefendants.get(0).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));
        assertThat(petDefendants.get(1).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));

        assertThat(formValidationResult.getPetFormData(), notNullValue());
        assertThat(formValidationResult.getPetFormData().getJsonObject(PROSECUTION), notNullValue());

        assertThat(formValidationResult.getPetFormData().getJsonObject(DEFENCE), notNullValue());
        final JsonObject defenceFromPetForm = formValidationResult.getPetFormData().getJsonObject(DEFENCE);
        assertThat(defenceFromPetForm.getJsonArray(DEFENDANTS), hasSize(2));
        final List<JsonObject> defendantListFromPetForm = defenceFromPetForm.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class);
        assertThat(defendantListFromPetForm.get(0).getString(ID), is(defendantIdOne));
        assertThat(defendantListFromPetForm.get(0).getJsonArray(CPS_OFFENCES), hasSize(3));
        final List<JsonObject> cpsOffencesListFromPetFormDefendantOne = defendantListFromPetForm.get(0).getJsonArray(CPS_OFFENCES).getValuesAs(JsonObject.class);
        assertThat(cpsOffencesListFromPetFormDefendantOne.get(0).getString(OFFENCE_CODE), is("OFFENCE_CODE_1"));
        assertThat(cpsOffencesListFromPetFormDefendantOne.get(1).getString(OFFENCE_CODE), is("OFFENCE_CODE_11"));
        assertThat(cpsOffencesListFromPetFormDefendantOne.get(2).getString(OFFENCE_CODE), is("OFFENCE_CODE_111"));
        assertThat(defendantListFromPetForm.get(1).getString(ID), is(defendantIdTwo));

        assertThat(defendantListFromPetForm.get(1).getJsonArray(CPS_OFFENCES), hasSize(2));
        final List<JsonObject> cpsOffencesListFromPetFormDefendantTwo = defendantListFromPetForm.get(1).getJsonArray(CPS_OFFENCES).getValuesAs(JsonObject.class);
        assertThat(cpsOffencesListFromPetFormDefendantTwo.get(0).getString(OFFENCE_CODE), is("OFFENCE_CODE_2"));
        assertThat(cpsOffencesListFromPetFormDefendantTwo.get(1).getString(OFFENCE_CODE), is("OFFENCE_CODE_22"));

    }

    @Test
    public void shouldReturnJsonWhen_AllDefendantDataMatched_AllValidOffencesBcm() {
        List<String> validOffences = asList("OFFENCE_CODE_1", "OFFENCE_CODE_11", "OFFENCE_CODE_111", "OFFENCE_CODE_2", "OFFENCE_CODE_22");
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String validForName = "John1";
        final String validSurName = "Lewis1";
        final String validDob = "03-06-1986";
        final String validOrgName = "organisationName";
        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-bcm-all-defendant-data.json")
                .replaceAll("FOR_NAME", validForName)
                .replaceAll("SUR_NAME", validSurName)
                .replaceAll("DATE_OF_BIRTH", validDob)
                .replaceAll("ORGANISATION_NAME", validOrgName);
        final JsonObject processReceivedPpsServeBcm = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefileBcm.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantIdOne.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsACpsDefendantId")
                        .build())
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantIdTwo.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsAnotherCpsDefendantId")
                        .build())
                .build();

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormDataBcm(processReceivedPpsServeBcm, prosecutionCaseFileCase, validOffences, defendantIdsAsJsonArrayFromProsecutionCase);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.SUCCESS));
        assertThat(formValidationResult.getErrorList(), hasSize(0));

        assertThat(formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS), hasSize(2));
        final List<JsonObject> bcmDefendants = formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS)
                .getValuesAs(JsonObject.class);
        assertThat(bcmDefendants.get(0).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));
        assertThat(bcmDefendants.get(1).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));

        assertThat(formValidationResult.getFormData(), notNullValue());

        assertThat(formValidationResult.getFormData(), notNullValue());
        final JsonObject defenceFromBcmForm = formValidationResult.getFormData();
        assertThat(defenceFromBcmForm.getJsonArray(BCM_DEFENDANTS), hasSize(2));
        final List<JsonObject> defendantListFromBcmForm = defenceFromBcmForm.getJsonArray(BCM_DEFENDANTS).getValuesAs(JsonObject.class);
        assertThat(defendantListFromBcmForm.get(0).getString(ID), is(defendantIdOne));
        assertThat(defendantListFromBcmForm.get(0).getJsonArray(PROSECUTOR_OFFENCES), hasSize(3));
        final List<JsonObject> cpsOffencesListFromBcmFormDefendantOne = defendantListFromBcmForm.get(0).getJsonArray(PROSECUTOR_OFFENCES).getValuesAs(JsonObject.class);
        assertThat(cpsOffencesListFromBcmFormDefendantOne.get(0).getString(OFFENCE_CODE), is("OFFENCE_CODE_1"));
        assertThat(cpsOffencesListFromBcmFormDefendantOne.get(1).getString(OFFENCE_CODE), is("OFFENCE_CODE_11"));
        assertThat(cpsOffencesListFromBcmFormDefendantOne.get(2).getString(OFFENCE_CODE), is("OFFENCE_CODE_111"));
        assertThat(defendantListFromBcmForm.get(1).getString(ID), is(defendantIdTwo));

        assertThat(defendantListFromBcmForm.get(1).getJsonArray(PROSECUTOR_OFFENCES), hasSize(2));
        final List<JsonObject> cpsOffencesListFromPetFormDefendantTwo = defendantListFromBcmForm.get(1).getJsonArray(PROSECUTOR_OFFENCES).getValuesAs(JsonObject.class);
        assertThat(cpsOffencesListFromPetFormDefendantTwo.get(0).getString(OFFENCE_CODE), is("OFFENCE_CODE_2"));
        assertThat(cpsOffencesListFromPetFormDefendantTwo.get(1).getString(OFFENCE_CODE), is("OFFENCE_CODE_22"));

    }

    @Test
    public void shouldReturnJsonWhen_AllDefendantDataMatched_ptph() {
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String validForName = "John1";
        final String validSurName = "Lewis1";
        final String validDob = "03-06-1986";
        final String validOrgName = "organisationName";
        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-ptph-all-defendant-data.json")
                .replaceAll("FOR_NAME", validForName)
                .replaceAll("SUR_NAME", validSurName)
                .replaceAll("DATE_OF_BIRTH", validDob)
                .replaceAll("ORGANISATION_NAME", validOrgName);
        final JsonObject processReceivedPpsServePtph = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefilePtph.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder().build();

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormDataPtph(processReceivedPpsServePtph, prosecutionCaseFileCase, defendantIdsAsJsonArrayFromProsecutionCase, objectToJsonObjectConverter);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.SUCCESS));
        assertThat(formValidationResult.getErrorList(), hasSize(0));

        assertThat(formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS), hasSize(2));
        final List<JsonObject> bcmDefendants = formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS)
                .getValuesAs(JsonObject.class);
        assertThat(bcmDefendants.get(0).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));
        assertThat(bcmDefendants.get(1).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));

        assertThat(formValidationResult.getFormData(), notNullValue());

        assertThat(formValidationResult.getFormData(), notNullValue());
        final JsonObject defenceFromPtphForm = formValidationResult.getFormData();
        assertThat(defenceFromPtphForm.getJsonArray(PTPH_DEFENDANTS), hasSize(2));
        final List<JsonObject> defendantListFromPtphForm = defenceFromPtphForm.getJsonArray(PTPH_DEFENDANTS).getValuesAs(JsonObject.class);
        assertThat(defendantListFromPtphForm.get(0).getString(ID), is(defendantIdOne));
        assertThat(defendantListFromPtphForm.get(1).getString(ID), is(defendantIdTwo));

    }

    @Test
    public void shouldReturnJsonWhen_OrgNameDefendantDataNotMatched_AllValidOffences() {
        List<String> validOffences = asList("OFFENCE_CODE_1", "OFFENCE_CODE_11", "OFFENCE_CODE_111", "OFFENCE_CODE_2", "OFFENCE_CODE_22");
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String validForName = "John1";
        final String validSurName = "Lewis1";
        final String validDob = "03-06-1986";
        final String validOrgName = "invalidOrgName";
        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-pet-all-defendant-data.json")
                .replaceAll("FOR_NAME", validForName)
                .replaceAll("SUR_NAME", validSurName)
                .replaceAll("DATE_OF_BIRTH", validDob)
                .replaceAll("ORGANISATION_NAME", validOrgName);
        final JsonObject processReceivedPpsServePet = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder().build();

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormData(processReceivedPpsServePet, prosecutionCaseFileCase, validOffences, defendantIdsAsJsonArrayFromProsecutionCase);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.REJECTED));
        assertThat(formValidationResult.getErrorList(), hasSize(1));

        assertThat(formValidationResult.getPetDefendants().getJsonArray(PET_DEFENDANTS), hasSize(1));
        final List<JsonObject> petDefendants = formValidationResult.getPetDefendants().getJsonArray(PET_DEFENDANTS)
                .getValuesAs(JsonObject.class);
        assertThat(petDefendants.get(0).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));

        assertThat(formValidationResult.getPetFormData(), notNullValue());
        assertThat(formValidationResult.getPetFormData().getJsonObject(PROSECUTION), notNullValue());

        assertThat(formValidationResult.getPetFormData().getJsonObject(DEFENCE), notNullValue());
        final JsonObject defenceFromPetForm = formValidationResult.getPetFormData().getJsonObject(DEFENCE);
        assertThat(defenceFromPetForm.getJsonArray(DEFENDANTS), hasSize(1));
        final List<JsonObject> defendantListFromPetForm = defenceFromPetForm.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class);
        assertThat(defendantListFromPetForm.get(0).getString(ID), is(defendantIdOne));
        assertThat(defendantListFromPetForm.get(0).getJsonArray(CPS_OFFENCES), hasSize(3));
        final List<JsonObject> cpsOffencesListFromPetFormDefendantOne = defendantListFromPetForm.get(0).getJsonArray(CPS_OFFENCES).getValuesAs(JsonObject.class);
        assertThat(cpsOffencesListFromPetFormDefendantOne.get(0).getString(OFFENCE_CODE), is("OFFENCE_CODE_1"));
        assertThat(cpsOffencesListFromPetFormDefendantOne.get(1).getString(OFFENCE_CODE), is("OFFENCE_CODE_11"));
        assertThat(cpsOffencesListFromPetFormDefendantOne.get(2).getString(OFFENCE_CODE), is("OFFENCE_CODE_111"));
    }

    @Test
    public void shouldReturnJsonWhen_OrgNameDefendantDataNotMatched_AllValidOffencesBcm() {
        List<String> validOffences = asList("OFFENCE_CODE_1", "OFFENCE_CODE_11", "OFFENCE_CODE_111", "OFFENCE_CODE_2", "OFFENCE_CODE_22");
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String validForName = "John1";
        final String validSurName = "Lewis1";
        final String validDob = "03-06-1986";
        final String validOrgName = "invalidOrgName";
        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-bcm-all-defendant-data.json")
                .replaceAll("FOR_NAME", validForName)
                .replaceAll("SUR_NAME", validSurName)
                .replaceAll("DATE_OF_BIRTH", validDob)
                .replaceAll("ORGANISATION_NAME", validOrgName);
        final JsonObject processReceivedPpsServeBcm = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefileBcm.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder().build();

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormDataBcm(processReceivedPpsServeBcm, prosecutionCaseFileCase, validOffences, defendantIdsAsJsonArrayFromProsecutionCase);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.REJECTED));
        assertThat(formValidationResult.getErrorList(), hasSize(1));

        assertThat(formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS), hasSize(1));
        final List<JsonObject> bcmDefendants = formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS)
                .getValuesAs(JsonObject.class);
        assertThat(bcmDefendants.get(0).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));

        assertThat(formValidationResult.getFormData(), notNullValue());

        assertThat(formValidationResult.getFormData(), notNullValue());
        final JsonObject defenceFromBcmForm = formValidationResult.getFormData();
        assertThat(defenceFromBcmForm.getJsonArray(BCM_DEFENDANTS), hasSize(1));
        final List<JsonObject> defendantListFromBcmForm = defenceFromBcmForm.getJsonArray(BCM_DEFENDANTS).getValuesAs(JsonObject.class);
        assertThat(defendantListFromBcmForm.get(0).getString(ID), is(defendantIdOne));
        assertThat(defendantListFromBcmForm.get(0).getJsonArray(PROSECUTOR_OFFENCES), hasSize(3));
        final List<JsonObject> cpsOffencesListFromBcmFormDefendantOne = defendantListFromBcmForm.get(0).getJsonArray(PROSECUTOR_OFFENCES).getValuesAs(JsonObject.class);
        assertThat(cpsOffencesListFromBcmFormDefendantOne.get(0).getString(OFFENCE_CODE), is("OFFENCE_CODE_1"));
        assertThat(cpsOffencesListFromBcmFormDefendantOne.get(1).getString(OFFENCE_CODE), is("OFFENCE_CODE_11"));
        assertThat(cpsOffencesListFromBcmFormDefendantOne.get(2).getString(OFFENCE_CODE), is("OFFENCE_CODE_111"));
    }

    @Test
    public void shouldReturnJsonWhen_OrgNameDefendantDataNotMatched_ptph() {
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String validForName = "John1";
        final String validSurName = "Lewis1";
        final String validDob = "03-06-1986";
        final String validOrgName = "invalidOrgName";
        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-ptph-all-defendant-data.json")
                .replaceAll("FOR_NAME", validForName)
                .replaceAll("SUR_NAME", validSurName)
                .replaceAll("DATE_OF_BIRTH", validDob)
                .replaceAll("ORGANISATION_NAME", validOrgName);
        final JsonObject processReceivedPpsServePtph = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefilePtph.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder().build();

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormDataPtph(processReceivedPpsServePtph, prosecutionCaseFileCase, defendantIdsAsJsonArrayFromProsecutionCase, objectToJsonObjectConverter);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.REJECTED));
        assertThat(formValidationResult.getErrorList(), hasSize(1));

        assertThat(formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS), hasSize(1));
        final List<JsonObject> bcmDefendants = formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS)
                .getValuesAs(JsonObject.class);
        assertThat(bcmDefendants.get(0).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));

        assertThat(formValidationResult.getFormData(), notNullValue());

        assertThat(formValidationResult.getFormData(), notNullValue());
        final JsonObject defenceFromBcmForm = formValidationResult.getFormData();
        assertThat(defenceFromBcmForm.getJsonArray(PTPH_DEFENDANTS), hasSize(1));
        final List<JsonObject> defendantListFromPtphForm = defenceFromBcmForm.getJsonArray(PTPH_DEFENDANTS).getValuesAs(JsonObject.class);
        assertThat(defendantListFromPtphForm.get(0).getString(ID), is(defendantIdOne));

    }

    @Test
    public void shouldReturnJsonWhen_DefendantDataNotMatched_AllValidOffences() {
        List<String> validOffences = asList("OFFENCE_CODE_1", "OFFENCE_CODE_11", "OFFENCE_CODE_111", "OFFENCE_CODE_2", "OFFENCE_CODE_22");
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String validForName = "invalidForName";
        final String validSurName = "invalidSurName";
        final String validDob = "InvalidDob";
        final String validOrgName = "organisationName";
        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-pet-all-defendant-data.json")
                .replaceAll("FOR_NAME", validForName)
                .replaceAll("SUR_NAME", validSurName)
                .replaceAll("DATE_OF_BIRTH", validDob)
                .replaceAll("ORGANISATION_NAME", validOrgName);
        final JsonObject processReceivedPpsServePet = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder().build();

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormData(processReceivedPpsServePet, prosecutionCaseFileCase, validOffences, defendantIdsAsJsonArrayFromProsecutionCase);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.REJECTED));
        assertThat(formValidationResult.getErrorList(), hasSize(1));

        assertThat(formValidationResult.getPetDefendants().getJsonArray(PET_DEFENDANTS), hasSize(1));
        final List<JsonObject> petDefendants = formValidationResult.getPetDefendants().getJsonArray(PET_DEFENDANTS)
                .getValuesAs(JsonObject.class);
        assertThat(petDefendants.get(0).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));


        assertThat(formValidationResult.getPetFormData(), notNullValue());
        assertThat(formValidationResult.getPetFormData().getJsonObject(PROSECUTION), notNullValue());

        assertThat(formValidationResult.getPetFormData().getJsonObject(DEFENCE), notNullValue());
        final JsonObject defenceFromPetForm = formValidationResult.getPetFormData().getJsonObject(DEFENCE);
        assertThat(defenceFromPetForm.getJsonArray(DEFENDANTS), hasSize(1));
        final List<JsonObject> defendantListFromPetForm = defenceFromPetForm.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class);

        assertThat(defendantListFromPetForm.get(0).getString(ID), is(defendantIdTwo));

        assertThat(defendantListFromPetForm.get(0).getJsonArray(CPS_OFFENCES), hasSize(2));
        final List<JsonObject> cpsOffencesListFromPetFormDefendantTwo = defendantListFromPetForm.get(0).getJsonArray(CPS_OFFENCES).getValuesAs(JsonObject.class);
        assertThat(cpsOffencesListFromPetFormDefendantTwo.get(0).getString(OFFENCE_CODE), is("OFFENCE_CODE_2"));
        assertThat(cpsOffencesListFromPetFormDefendantTwo.get(1).getString(OFFENCE_CODE), is("OFFENCE_CODE_22"));

    }

    @Test
    public void shouldReturnJsonWhen_DefendantDataNotMatched_AllValidOffencesBcm() {
        List<String> validOffences = asList("OFFENCE_CODE_1", "OFFENCE_CODE_11", "OFFENCE_CODE_111", "OFFENCE_CODE_2", "OFFENCE_CODE_22");
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String validForName = "invalidForName";
        final String validSurName = "invalidSurName";
        final String validDob = "InvalidDob";
        final String validOrgName = "organisationName";
        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-bcm-all-defendant-data.json")
                .replaceAll("FOR_NAME", validForName)
                .replaceAll("SUR_NAME", validSurName)
                .replaceAll("DATE_OF_BIRTH", validDob)
                .replaceAll("ORGANISATION_NAME", validOrgName);
        final JsonObject processReceivedPpsServeBcm = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefileBcm.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder().build();

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormDataBcm(processReceivedPpsServeBcm, prosecutionCaseFileCase, validOffences, defendantIdsAsJsonArrayFromProsecutionCase);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.REJECTED));
        assertThat(formValidationResult.getErrorList(), hasSize(1));

        assertThat(formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS), hasSize(1));
        final List<JsonObject> bcmDefendants = formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS)
                .getValuesAs(JsonObject.class);
        assertThat(bcmDefendants.get(0).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));


        assertThat(formValidationResult.getFormData(), notNullValue());

        assertThat(formValidationResult.getFormData(), notNullValue());
        final JsonObject defenceFromBcmForm = formValidationResult.getFormData();
        assertThat(defenceFromBcmForm.getJsonArray(BCM_DEFENDANTS), hasSize(1));
        final List<JsonObject> defendantListFromBcmForm = defenceFromBcmForm.getJsonArray(BCM_DEFENDANTS).getValuesAs(JsonObject.class);

        assertThat(defendantListFromBcmForm.get(0).getString(ID), is(defendantIdTwo));

        assertThat(defendantListFromBcmForm.get(0).getJsonArray(PROSECUTOR_OFFENCES), hasSize(2));
        final List<JsonObject> cpsOffencesListFromBcmFormDefendantTwo = defendantListFromBcmForm.get(0).getJsonArray(PROSECUTOR_OFFENCES).getValuesAs(JsonObject.class);
        assertThat(cpsOffencesListFromBcmFormDefendantTwo.get(0).getString(OFFENCE_CODE), is("OFFENCE_CODE_2"));
        assertThat(cpsOffencesListFromBcmFormDefendantTwo.get(1).getString(OFFENCE_CODE), is("OFFENCE_CODE_22"));

    }

    @Test
    public void shouldReturnJsonWhen_DefendantDataNotMatched_ptph() {
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String validForName = "invalidForName";
        final String validSurName = "invalidSurName";
        final String validDob = "InvalidDob";
        final String validOrgName = "organisationName";
        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-ptph-all-defendant-data.json")
                .replaceAll("FOR_NAME", validForName)
                .replaceAll("SUR_NAME", validSurName)
                .replaceAll("DATE_OF_BIRTH", validDob)
                .replaceAll("ORGANISATION_NAME", validOrgName);
        final JsonObject processReceivedPpsServePtph = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefilePtph.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder().build();

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormDataPtph(processReceivedPpsServePtph, prosecutionCaseFileCase, defendantIdsAsJsonArrayFromProsecutionCase, objectToJsonObjectConverter);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.REJECTED));
        assertThat(formValidationResult.getErrorList(), hasSize(1));

        assertThat(formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS), hasSize(1));
        final List<JsonObject> ptphDefendants = formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS)
                .getValuesAs(JsonObject.class);
        assertThat(ptphDefendants.get(0).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));


        assertThat(formValidationResult.getFormData(), notNullValue());

        assertThat(formValidationResult.getFormData(), notNullValue());
        final JsonObject defenceFromPtphForm = formValidationResult.getFormData();
        assertThat(defenceFromPtphForm.getJsonArray(PTPH_DEFENDANTS), hasSize(1));
        final List<JsonObject> defendantListFromBcmForm = defenceFromPtphForm.getJsonArray(PTPH_DEFENDANTS).getValuesAs(JsonObject.class);

        assertThat(defendantListFromBcmForm.get(0).getString(ID), is(defendantIdTwo));

    }

    @Test
    public void shouldReturnJsonWhen_PartialDefendantDataMatched_AllValidOffences() {
        List<String> validOffences = asList("OFFENCE_CODE_1", "OFFENCE_CODE_11", "OFFENCE_CODE_111", "OFFENCE_CODE_2", "OFFENCE_CODE_22");
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String validForName = "John1";
        final String validSurName = "InvalidSurName";
        final String validDob = "03-06-1986";
        final String validOrgName = "organisationName";
        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-pet-all-defendant-data.json")
                .replaceAll("FOR_NAME", validForName)
                .replaceAll("SUR_NAME", validSurName)
                .replaceAll("DATE_OF_BIRTH", validDob)
                .replaceAll("ORGANISATION_NAME", validOrgName);
        final JsonObject processReceivedPpsServePet = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder().build();

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormData(processReceivedPpsServePet, prosecutionCaseFileCase, validOffences, defendantIdsAsJsonArrayFromProsecutionCase);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.REJECTED));
        assertThat(formValidationResult.getErrorList(), hasSize(1));

        assertThat(formValidationResult.getPetDefendants().getJsonArray(PET_DEFENDANTS), hasSize(1));
        final List<JsonObject> petDefendants = formValidationResult.getPetDefendants().getJsonArray(PET_DEFENDANTS)
                .getValuesAs(JsonObject.class);
        assertThat(petDefendants.get(0).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));

        assertThat(formValidationResult.getPetFormData(), notNullValue());
        assertThat(formValidationResult.getPetFormData().getJsonObject(PROSECUTION), notNullValue());

        assertThat(formValidationResult.getPetFormData().getJsonObject(DEFENCE), notNullValue());
        final JsonObject defenceFromPetForm = formValidationResult.getPetFormData().getJsonObject(DEFENCE);
        assertThat(defenceFromPetForm.getJsonArray(DEFENDANTS), hasSize(1));
        final List<JsonObject> defendantListFromPetForm = defenceFromPetForm.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class);

        assertThat(defendantListFromPetForm.get(0).getString(ID), is(defendantIdTwo));

        assertThat(defendantListFromPetForm.get(0).getJsonArray(CPS_OFFENCES), hasSize(2));
        final List<JsonObject> cpsOffencesListFromPetFormDefendantTwo = defendantListFromPetForm.get(0).getJsonArray(CPS_OFFENCES).getValuesAs(JsonObject.class);
        assertThat(cpsOffencesListFromPetFormDefendantTwo.get(0).getString(OFFENCE_CODE), is("OFFENCE_CODE_2"));
        assertThat(cpsOffencesListFromPetFormDefendantTwo.get(1).getString(OFFENCE_CODE), is("OFFENCE_CODE_22"));

    }

    @Test
    public void shouldReturnJsonWhen_PartialDefendantDataMatched_AllValidOffencesBcm() {
        List<String> validOffences = asList("OFFENCE_CODE_1", "OFFENCE_CODE_11", "OFFENCE_CODE_111", "OFFENCE_CODE_2", "OFFENCE_CODE_22");
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String validForName = "John1";
        final String validSurName = "InvalidSurName";
        final String validDob = "03-06-1986";
        final String validOrgName = "organisationName";
        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-bcm-all-defendant-data.json")
                .replaceAll("FOR_NAME", validForName)
                .replaceAll("SUR_NAME", validSurName)
                .replaceAll("DATE_OF_BIRTH", validDob)
                .replaceAll("ORGANISATION_NAME", validOrgName);
        final JsonObject processReceivedPpsServeBcm = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefileBcm.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder().build();

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormDataBcm(processReceivedPpsServeBcm, prosecutionCaseFileCase, validOffences, defendantIdsAsJsonArrayFromProsecutionCase);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.REJECTED));
        assertThat(formValidationResult.getErrorList(), hasSize(1));

        assertThat(formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS), hasSize(1));
        final List<JsonObject> bcmDefendants = formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS)
                .getValuesAs(JsonObject.class);
        assertThat(bcmDefendants.get(0).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));

        assertThat(formValidationResult.getFormData(), notNullValue());

        assertThat(formValidationResult.getFormData(), notNullValue());
        final JsonObject defenceFromBcmForm = formValidationResult.getFormData();
        assertThat(defenceFromBcmForm.getJsonArray(BCM_DEFENDANTS), hasSize(1));
        final List<JsonObject> defendantListFromBcmForm = defenceFromBcmForm.getJsonArray(BCM_DEFENDANTS).getValuesAs(JsonObject.class);

        assertThat(defendantListFromBcmForm.get(0).getString(ID), is(defendantIdTwo));

        assertThat(defendantListFromBcmForm.get(0).getJsonArray(PROSECUTOR_OFFENCES), hasSize(2));
        final List<JsonObject> cpsOffencesListFromPetFormDefendantTwo = defendantListFromBcmForm.get(0).getJsonArray(PROSECUTOR_OFFENCES).getValuesAs(JsonObject.class);
        assertThat(cpsOffencesListFromPetFormDefendantTwo.get(0).getString(OFFENCE_CODE), is("OFFENCE_CODE_2"));
        assertThat(cpsOffencesListFromPetFormDefendantTwo.get(1).getString(OFFENCE_CODE), is("OFFENCE_CODE_22"));

    }

    @Test
    public void shouldReturnJsonWhen_PartialDefendantDataMatched_ptph() {
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String validForName = "John1";
        final String validSurName = "InvalidSurName";
        final String validDob = "03-06-1986";
        final String validOrgName = "organisationName";
        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-ptph-all-defendant-data.json")
                .replaceAll("FOR_NAME", validForName)
                .replaceAll("SUR_NAME", validSurName)
                .replaceAll("DATE_OF_BIRTH", validDob)
                .replaceAll("ORGANISATION_NAME", validOrgName);
        final JsonObject processReceivedPpsServePtph = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefilePtph.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder().build();

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormDataPtph(processReceivedPpsServePtph, prosecutionCaseFileCase, defendantIdsAsJsonArrayFromProsecutionCase, objectToJsonObjectConverter);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.REJECTED));
        assertThat(formValidationResult.getErrorList(), hasSize(1));

        assertThat(formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS), hasSize(1));
        final List<JsonObject> bcmDefendants = formValidationResult.getFormDefendants().getJsonArray(FORM_DEFENDANTS)
                .getValuesAs(JsonObject.class);
        assertThat(bcmDefendants.get(0).getString(DEFENDANT_ID), anyOf(is(defendantIdOne), is(defendantIdTwo)));

        assertThat(formValidationResult.getFormData(), notNullValue());

        assertThat(formValidationResult.getFormData(), notNullValue());
        final JsonObject defenceFromBcmForm = formValidationResult.getFormData();
        assertThat(defenceFromBcmForm.getJsonArray(PTPH_DEFENDANTS), hasSize(1));
        final List<JsonObject> defendantListFromPtphForm = defenceFromBcmForm.getJsonArray(PTPH_DEFENDANTS).getValuesAs(JsonObject.class);

        assertThat(defendantListFromPtphForm.get(0).getString(ID), is(defendantIdTwo));
    }

    @Test
    public void shouldReturnJsonWhen_NoDefendantDataMatched_AllValidOffences() {
        List<String> validOffences = asList("OFFENCE_CODE_1", "OFFENCE_CODE_11", "OFFENCE_CODE_111", "OFFENCE_CODE_2", "OFFENCE_CODE_22");
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String validForName = "invalidForName";
        final String validSurName = "invalidSurName";
        final String validDob = "InvalidDob";
        final String validOrgName = "invalidOrgName";
        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-pet-all-defendant-data.json")
                .replaceAll("FOR_NAME", validForName)
                .replaceAll("SUR_NAME", validSurName)
                .replaceAll("DATE_OF_BIRTH", validDob)
                .replaceAll("ORGANISATION_NAME", validOrgName);
        final JsonObject processReceivedPpsServePet = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder().build();

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormData(processReceivedPpsServePet, prosecutionCaseFileCase, validOffences, defendantIdsAsJsonArrayFromProsecutionCase);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.REJECTED));
        assertThat(formValidationResult.getErrorList(), hasSize(2));

        assertThat(formValidationResult.getPetDefendants(), nullValue());

        assertThat(formValidationResult.getPetFormData(), notNullValue());
        assertThat(formValidationResult.getPetFormData().getJsonObject(PROSECUTION), notNullValue());
    }

    @Test
    public void shouldReturnJsonWhen_NoDefendantDataMatched_AllValidOffencesBcm() {
        List<String> validOffences = asList("OFFENCE_CODE_1", "OFFENCE_CODE_11", "OFFENCE_CODE_111", "OFFENCE_CODE_2", "OFFENCE_CODE_22");
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String validForName = "invalidForName";
        final String validSurName = "invalidSurName";
        final String validDob = "InvalidDob";
        final String validOrgName = "invalidOrgName";
        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-bcm-all-defendant-data.json")
                .replaceAll("FOR_NAME", validForName)
                .replaceAll("SUR_NAME", validSurName)
                .replaceAll("DATE_OF_BIRTH", validDob)
                .replaceAll("ORGANISATION_NAME", validOrgName);
        final JsonObject processReceivedPpsServePet = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefileBcm.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder().build();

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormDataBcm(processReceivedPpsServePet, prosecutionCaseFileCase, validOffences, defendantIdsAsJsonArrayFromProsecutionCase);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.REJECTED));
        assertThat(formValidationResult.getErrorList(), hasSize(2));

        assertThat(formValidationResult.getFormDefendants(), nullValue());
    }

    @Test
    public void shouldReturnJsonWhen_NoDefendantDataMatched_ptph() {
        final String defendantIdOne = randomUUID().toString();
        final String defendantIdTwo = randomUUID().toString();

        final String validForName = "invalidForName";
        final String validSurName = "invalidSurName";
        final String validDob = "InvalidDob";
        final String validOrgName = "invalidOrgName";
        final String commandPayloadString = FileUtil.getPayload("stub-data/prosecutioncasefile.command.process-received-cps-serve-ptph-all-defendant-data.json")
                .replaceAll("FOR_NAME", validForName)
                .replaceAll("SUR_NAME", validSurName)
                .replaceAll("DATE_OF_BIRTH", validDob)
                .replaceAll("ORGANISATION_NAME", validOrgName);
        final JsonObject processReceivedPpsServePtph = FileUtil.jsonFromString(commandPayloadString);

        final String prosecutionCasePayloadString = FileUtil.getPayload("stub-data/prosecutioncasefilePtph.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantIdOne)
                .replaceAll("DEFENDANT_ID_2", defendantIdTwo);
        final JsonObject prosecutionCaseFileCase = FileUtil.jsonFromString(prosecutionCasePayloadString);

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder().build();

        final FormValidationResult formValidationResult = cpsFormValidator.validateAndRebuildingFormDataPtph(processReceivedPpsServePtph, prosecutionCaseFileCase, defendantIdsAsJsonArrayFromProsecutionCase, objectToJsonObjectConverter);

        assertThat(formValidationResult.getSubmissionStatus(), is(SubmissionStatus.REJECTED));
        assertThat(formValidationResult.getErrorList(), hasSize(2));

        assertThat(formValidationResult.getFormDefendants(), nullValue());
    }

}
