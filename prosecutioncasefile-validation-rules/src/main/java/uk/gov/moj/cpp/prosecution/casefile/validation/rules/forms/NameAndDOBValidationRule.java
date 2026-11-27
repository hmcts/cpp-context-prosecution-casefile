package uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.MatchedDefendant.matchedDefendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem.problem;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue.problemValue;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError.INVALID_DEFENDANTS_PROVIDED;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.CPS_DEFENDANT_ID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.DATE_OF_BIRTH;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.DEFENDANTS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.DEFENDANT_ID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.FIRST_NAME;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.FORENAME;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.LAST_NAME;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.MATCHING_ID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.ORGANISATION_NAME;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.PERSONAL_INFORMATION;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.SELF_DEFINED_INFORMATION;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.SURNAME;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.MatchedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NameAndDOBValidationRule implements ValidationRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(NameAndDOBValidationRule.class);


    @Override
    public MatchedDefendant validate(final JsonObject cpsDefendant, final JsonValue ruleInput) {

        LOGGER.info("Executing Rule: Name and DOB matching rule for CPS Defendant");
        final Map<String, String> defendantIdCpsDefendantIdMap = new HashMap<>();
        final List<DefendantInfo> defendantInfoList = new ArrayList<>();

        ((JsonObject) ruleInput).getJsonArray("cpsDefendantIdList")
                .getValuesAs(JsonObject.class)
                .forEach(pair -> defendantIdCpsDefendantIdMap.put(pair.getString(DEFENDANT_ID), pair.getString(CPS_DEFENDANT_ID)));

        final JsonObject prosecutionCase = ((JsonObject) ruleInput).getJsonObject("prosecutionCase");

        prosecutionCase
                .getJsonArray(DEFENDANTS)
                .getValuesAs(JsonObject.class)
                .forEach(defendant -> extractAllDefendantDataFromProsecutionCase(defendant, defendantInfoList));

        final MatchedDefendant.Builder matchedDefendantBuilder = matchedDefendant()
                .withMatchingId(fromString(cpsDefendant.getString(MATCHING_ID)));

        if (cpsDefendant.containsKey(ORGANISATION_NAME)) {
            LOGGER.info("defendant matching using organisation name");
            matchingOrganisationName(cpsDefendant, matchedDefendantBuilder, defendantIdCpsDefendantIdMap, defendantInfoList);
        } else {
            LOGGER.info("defendant matching using name,surname, dob");
            matchingFirstNameLastNameAndDob(cpsDefendant, matchedDefendantBuilder, defendantIdCpsDefendantIdMap, defendantInfoList);
        }

        return matchedDefendantBuilder.build();

    }

    private void matchingFirstNameLastNameAndDob(final JsonObject cpsDefendant, final MatchedDefendant.Builder matchedDefendantBuilder, final Map<String, String> defendantIdCpsDefendantIdMap,
                                                 final List<DefendantInfo> defendantInfoList) {

        final List<JsonObject> prosecutionCaseDefendantData = defendantInfoList.stream()
                .filter(caseDefendant -> matchDefendant(caseDefendant, cpsDefendant))
                .map(DefendantInfo::getDefendantData)
                .collect(toList());

        final boolean isUniqueMatch = isNotEmpty(prosecutionCaseDefendantData) && prosecutionCaseDefendantData.size() == 1;

        if (isUniqueMatch) {
            buildNameMatchedDefendant(cpsDefendant, matchedDefendantBuilder, defendantIdCpsDefendantIdMap, prosecutionCaseDefendantData);
        } else {
            final Problem.Builder problemBuilder = problem()
                    .withCode(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode());
            final List<ProblemValue> problemValueList = getProblemValues(cpsDefendant, defendantInfoList);
            problemBuilder.withValues(problemValueList);
            matchedDefendantBuilder.withProblems(asList(problemBuilder.build()));
        }
    }

    private void buildNameMatchedDefendant(final JsonObject cpsDefendant, final MatchedDefendant.Builder matchedDefendantBuilder, final Map<String, String> defendantIdCpsDefendantIdMap, final List<JsonObject> prosecutionCaseDefendantData) {
        final UUID defendantId = fromString(prosecutionCaseDefendantData.get(0).getString(DEFENDANT_ID));
        LOGGER.info("unique defendant found for defendant name,surname and dob with defendantId : {}", defendantId);

        matchedDefendantBuilder.withDefendantId(defendantId);

        if (cpsDefendant.containsKey(CPS_DEFENDANT_ID) && defendantIdCpsDefendantIdMap.containsKey(defendantId.toString())
                && !cpsDefendant.getString(CPS_DEFENDANT_ID).contentEquals(defendantIdCpsDefendantIdMap.get(defendantId.toString()))) {
            final String caseDefendantCpsDefendantId = defendantIdCpsDefendantIdMap.get(defendantId.toString());
            final String cpsDefendantId = cpsDefendant.getString(CPS_DEFENDANT_ID);
            LOGGER.info("defendant matched with name,surname,DOB but cpsDefendantId from cps : {} mismatched with defendant from case with cpsDefendantId : {}", cpsDefendantId, caseDefendantCpsDefendantId);
            matchedDefendantBuilder
                    .withProblems(asList(problem()
                            .withCode(INVALID_DEFENDANTS_PROVIDED.getCode())
                            .withValues(asList(problemValue()
                                    .withKey(CPS_DEFENDANT_ID)
                                    .withValue(cpsDefendantId)
                                    .build()))
                            .build()))
                    .build();
            matchedDefendantBuilder.withDefendantId(null);
        } else if (cpsDefendant.containsKey(CPS_DEFENDANT_ID) && !defendantIdCpsDefendantIdMap.containsKey(defendantId.toString())) {
            matchedDefendantBuilder.withCpsDefendantId(cpsDefendant.getString(CPS_DEFENDANT_ID));
            LOGGER.info("Defendant matched with defendantId : {} will be updated with cpsDefendantId : {}", defendantId, cpsDefendant.getString(CPS_DEFENDANT_ID));
        }
    }

    private List<ProblemValue> getProblemValues(final JsonObject cpsDefendant, final List<DefendantInfo> defendantInfoList) {
        final List<ProblemValue> problemValueList = new ArrayList<>();

        if (!isCpsNamesAndDobPresent(defendantInfoList, cpsDefendant.getString(FORENAME, ""), FORENAME)) {
            problemValueList.add(problemValue()
                    .withKey(FORENAME)
                    .withValue(cpsDefendant.getString(FORENAME, ""))
                    .build());
        }

        if (!isCpsNamesAndDobPresent(defendantInfoList, cpsDefendant.getString(SURNAME, ""), SURNAME)) {
            problemValueList.add(problemValue()
                    .withKey(SURNAME)
                    .withValue(cpsDefendant.getString(SURNAME, ""))
                    .build());
        }

        if (!isCpsNamesAndDobPresent(defendantInfoList, cpsDefendant.getString(DATE_OF_BIRTH, ""), DATE_OF_BIRTH)) {
            problemValueList.add(problemValue()
                    .withKey(DATE_OF_BIRTH)
                    .withValue(cpsDefendant.getString(DATE_OF_BIRTH, ""))
                    .build());
        }
        return problemValueList;
    }

    private boolean matchDefendant(final DefendantInfo caseDefendant, final JsonObject cpsDefendant) {
        return isNotEmpty(cpsDefendant.getString(FORENAME, "")) && cpsDefendant.getString(FORENAME).equalsIgnoreCase(caseDefendant.getFirstName())
                && isNotEmpty(cpsDefendant.getString(SURNAME, "")) && cpsDefendant.getString(SURNAME).equalsIgnoreCase(caseDefendant.getLastName())
                && isNotEmpty(cpsDefendant.getString(DATE_OF_BIRTH, "")) && cpsDefendant.getString(DATE_OF_BIRTH).equals(caseDefendant.getDob());
    }

    private void matchingOrganisationName(final JsonObject cpsDefendant, final MatchedDefendant.Builder matchedDefendantBuilder, final Map<String, String> defendantIdCpsDefendantIdMap
            , final List<DefendantInfo> defendantInfoList) {
        final String organisationName = cpsDefendant.getString(ORGANISATION_NAME, "");
        final List<JsonObject> prosecutionCaseDefendantData = defendantInfoList.stream()
                .filter(caseDefendant -> isNotEmpty(organisationName) && organisationName.equalsIgnoreCase(caseDefendant.getOrganisationName()))
                .map(DefendantInfo::getDefendantData)
                .collect(toList());

        final boolean isUniqueMatch = isNotEmpty(prosecutionCaseDefendantData) && prosecutionCaseDefendantData.size() == 1;
        if (isUniqueMatch) {
            buildOrganisationMatchedDefendant(cpsDefendant, matchedDefendantBuilder, defendantIdCpsDefendantIdMap, prosecutionCaseDefendantData);
        } else {
            matchedDefendantBuilder
                    .withProblems(asList(Problem.problem()
                            .withCode(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode())
                            .withValues(asList(problemValue()
                                    .withKey(ORGANISATION_NAME)
                                    .withValue(organisationName)
                                    .build()))
                            .build()))
                    .build();
        }

    }

    private void buildOrganisationMatchedDefendant(final JsonObject cpsDefendant, final MatchedDefendant.Builder matchedDefendantBuilder, final Map<String, String> defendantIdCpsDefendantIdMap, final List<JsonObject> prosecutionCaseDefendantData) {
        final UUID defendantId = fromString(prosecutionCaseDefendantData.get(0).getString(DEFENDANT_ID));
        LOGGER.info("unique defendant found for organisation with defendantId : {}", defendantId);

        matchedDefendantBuilder.withDefendantId(defendantId);

        if (cpsDefendant.containsKey(CPS_DEFENDANT_ID) && defendantIdCpsDefendantIdMap.containsKey(defendantId.toString())
        && !cpsDefendant.getString(CPS_DEFENDANT_ID).contentEquals(defendantIdCpsDefendantIdMap.get(defendantId.toString()))){
            final String caseDefendantCpsDefendantId = defendantIdCpsDefendantIdMap.get(defendantId.toString());
            final String cpsDefendantId = cpsDefendant.getString(CPS_DEFENDANT_ID);
            LOGGER.info("defendant matched with organisation but cpsDefendantId from cps : {} mismatched with defendant from case with cpsDefendantId : {}", cpsDefendantId, caseDefendantCpsDefendantId);
            matchedDefendantBuilder
                    .withProblems(asList(problem()
                            .withCode(INVALID_DEFENDANTS_PROVIDED.getCode())
                            .withValues(asList(problemValue()
                                    .withKey(CPS_DEFENDANT_ID)
                                    .withValue(cpsDefendantId)
                                    .build()))
                            .build()))
                    .build();
            matchedDefendantBuilder.withDefendantId(null);
        }else if (cpsDefendant.containsKey(CPS_DEFENDANT_ID) && !defendantIdCpsDefendantIdMap.containsKey(defendantId.toString())){
            matchedDefendantBuilder.withCpsDefendantId(cpsDefendant.getString(CPS_DEFENDANT_ID));
            LOGGER.info("Defendant matched with defendantId : {} will be updated with cpsDefendantId : {}", defendantId, cpsDefendant.getString(CPS_DEFENDANT_ID));
        }
    }

    private void extractAllDefendantDataFromProsecutionCase(final JsonObject defendant, final List<DefendantInfo> defendantInfoList) {

        final DefendantInfo defendantInfo = new DefendantInfo();

        if (defendant.containsKey(ORGANISATION_NAME) && isNotEmpty(defendant.getString(ORGANISATION_NAME))) {
            defendantInfo.setOrganisationName(defendant.getString(ORGANISATION_NAME));
            defendantInfo.setDefendantData(defendant);
            defendantInfoList.add(defendantInfo);
            return;
        }
        if (defendant.containsKey(PERSONAL_INFORMATION)) {
            final JsonObject personalInformation = defendant.getJsonObject(PERSONAL_INFORMATION);
            if (personalInformation.containsKey(FIRST_NAME)) {
                defendantInfo.setFirstName(personalInformation.getString(FIRST_NAME));
            }
            if (personalInformation.containsKey(LAST_NAME)) {
                defendantInfo.setLastName(personalInformation.getString(LAST_NAME));
            }
        }

        if (defendant.containsKey(SELF_DEFINED_INFORMATION)) {
            final JsonObject selfDefinedInformation = defendant.getJsonObject(SELF_DEFINED_INFORMATION);
            if (selfDefinedInformation.containsKey(DATE_OF_BIRTH)) {
                defendantInfo.setDob(selfDefinedInformation.getString(DATE_OF_BIRTH));
            }
        }
        defendantInfo.setDefendantData(defendant);
        defendantInfoList.add(defendantInfo);
    }

    private boolean isCpsNamesAndDobPresent(final List<DefendantInfo> defendantInfoList, final String defendantData, final String param) {

        boolean matchFlag = true;
        if (FORENAME.equals(param)) {
            matchFlag = !isEmpty(defendantData) && nonNull(defendantInfoList) && defendantInfoList.stream().anyMatch(defendantInfo ->
                    (nonNull(defendantInfo.getFirstName()) && defendantInfo.getFirstName().equalsIgnoreCase(defendantData)));
        }
        if (SURNAME.equals(param)) {
            matchFlag = !isEmpty(defendantData) && nonNull(defendantInfoList) && defendantInfoList.stream().anyMatch(defendantInfo ->
                    (nonNull(defendantInfo.getLastName()) && defendantInfo.getLastName().equalsIgnoreCase(defendantData)));
        }
        if (DATE_OF_BIRTH.equals(param)) {
            matchFlag = !isEmpty(defendantData) && nonNull(defendantInfoList) && defendantInfoList.stream().anyMatch(defendantInfo ->
                    (nonNull(defendantInfo.getDob()) && defendantInfo.getDob().equals(defendantData)));
        }

        return matchFlag;
    }

}
