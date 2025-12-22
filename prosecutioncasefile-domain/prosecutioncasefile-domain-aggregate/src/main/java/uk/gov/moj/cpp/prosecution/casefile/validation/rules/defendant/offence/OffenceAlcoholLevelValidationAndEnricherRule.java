package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence;

import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.ALCOHOL_DRUG_LEVEL_METHOD_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.OFFENCE_ALCOHOL_LEVEL_METHOD;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.AlcoholLevelMethodReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OffenceAlcoholLevelValidationAndEnricherRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {


        if (defendantWithReferenceData.getDefendant() == null || defendantWithReferenceData.getDefendant().getOffences() == null
                || defendantWithReferenceData.getDefendant().getOffences().isEmpty()) {
            return VALID;
        }

        final List<ProblemValue> problemValues = defendantWithReferenceData.getDefendant().getOffences().stream().map(offence ->
                validateAlcoholLevelMethodInOffence(offence, defendantWithReferenceData.getReferenceDataVO().getAlcoholLevelMethodReferenceData())).filter(Objects::nonNull).collect(Collectors.toList());

        if (null == problemValues || problemValues.isEmpty()) {
            return VALID;
        }

        return newValidationResult(of(newProblem(ALCOHOL_DRUG_LEVEL_METHOD_INVALID, problemValues.toArray(new ProblemValue[problemValues.size()]))));

    }

    private ProblemValue validateAlcoholLevelMethodInOffence(final Offence offence, final List<AlcoholLevelMethodReferenceData> alcoholLevelMethodReferenceData) {

        if (alcoholLevelMethodReferenceData == null) {
            return null;
        }

        final String alcoholLevelMethod = offence.getAlcoholRelatedOffence() != null ? offence.getAlcoholRelatedOffence().getAlcoholLevelMethod() : null;

        if (isEmpty(alcoholLevelMethod) || alcoholLevelMethodReferenceData.stream().anyMatch(s -> s.getMethodCode().equals(alcoholLevelMethod))) {
            return null;
        } else {

            return new ProblemValue(offence.getOffenceId().toString(), OFFENCE_ALCOHOL_LEVEL_METHOD.getValue(), alcoholLevelMethod);
        }

    }

}

