package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.ALCOHOL_DRUG_LEVEL_METHOD_MISSING;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.OFFENCE_ALCOHOL_LEVEL_METHOD;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OffenceDrugLevelMethodValidationAndEnricherRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {


        if (defendantWithReferenceData.getDefendant() == null || defendantWithReferenceData.getDefendant().getOffences() == null
                || defendantWithReferenceData.getDefendant().getOffences().isEmpty()) {
            return VALID;
        }

        final List<ProblemValue> problemValues = defendantWithReferenceData.getDefendant().getOffences().stream().map(offence ->
                verifyAlcoholDrugLevelMethodRequired(offence, defendantWithReferenceData.getCaseDetails().getInitiationCode(), defendantWithReferenceData, referenceDataQueryService)).filter(Objects::nonNull).collect(Collectors.toList());

        if (null == problemValues || problemValues.isEmpty()) {
            return VALID;
        }

        return newValidationResult(of(newProblem(ALCOHOL_DRUG_LEVEL_METHOD_MISSING, problemValues.toArray(new ProblemValue[problemValues.size()]))));

    }

    private ProblemValue verifyAlcoholDrugLevelMethodRequired(final Offence offence, final String initiationCode, final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final ReferenceDataVO referenceDataVO = defendantWithReferenceData.getReferenceDataVO();
        final List<OffenceReferenceData> offenceReferenceDataListFromVO = referenceDataVO.getOffenceReferenceData().stream()
                .filter(rd -> rd.getCjsOffenceCode().equals(offence.getOffenceCode())).filter(Objects::nonNull).collect(Collectors.toList());

        if (offenceReferenceDataListFromVO != null && !offenceReferenceDataListFromVO.isEmpty()) {
            return validateAlcoholDrugLevelMethod(offence, offenceReferenceDataListFromVO);
        }

        final List<OffenceReferenceData> newOffenceReferenceDataList = referenceDataQueryService.retrieveOffenceData(offence, initiationCode).stream()
                .filter(rd -> rd.getCjsOffenceCode().equals(offence.getOffenceCode())).filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (newOffenceReferenceDataList != null && !newOffenceReferenceDataList.isEmpty()) {
            if (referenceDataVO.getOffenceReferenceData() != null) {
                referenceDataVO.getOffenceReferenceData().addAll(newOffenceReferenceDataList);
            } else {
                final List<OffenceReferenceData> offenceReferenceDataList = new ArrayList<>();
                offenceReferenceDataList.addAll(newOffenceReferenceDataList);
                referenceDataVO.setOffenceReferenceData(offenceReferenceDataList);
            }
            return null;
        }
        return null;

    }

    @SuppressWarnings("squid:S1155")
    private ProblemValue validateAlcoholDrugLevelMethod(final Offence offence, final List<OffenceReferenceData> offenceReferenceDataListFromVO) {
        if (offenceReferenceDataListFromVO.size() == 0) {
            return getProblemValueForAlcoholMethod(offence, offence.getAlcoholRelatedOffence() != null ? offence.getAlcoholRelatedOffence().getAlcoholLevelMethod() : null);
        }

        if (hasDrugsOrAlcoholRelatedFlag(offenceReferenceDataListFromVO) &&
                (offence.getAlcoholRelatedOffence() == null || (offence.getAlcoholRelatedOffence() != null && offence.getAlcoholRelatedOffence().getAlcoholLevelMethod() == null))) {
            return getProblemValueForAlcoholMethod(offence, null);
        }

        return null;
    }

    private boolean hasDrugsOrAlcoholRelatedFlag(final List<OffenceReferenceData> offenceReferenceData) {
        return offenceReferenceData.stream().map(OffenceReferenceData::getDrugsOrAlcoholRelated).filter(Objects::nonNull).map(String::trim)
                .map("Y"::equals).findAny()
                .orElse(false);
    }


    private ProblemValue getProblemValueForAlcoholMethod(final Offence offence, final String offenceAlcoholLevelMethod) {
        return new ProblemValue(offence.getOffenceId().toString(), OFFENCE_ALCOHOL_LEVEL_METHOD.getValue(), offenceAlcoholLevelMethod == null ? "" : offenceAlcoholLevelMethod);
    }

}