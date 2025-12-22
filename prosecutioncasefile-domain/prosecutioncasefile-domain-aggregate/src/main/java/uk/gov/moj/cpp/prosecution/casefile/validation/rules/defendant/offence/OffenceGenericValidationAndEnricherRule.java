package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.OFFENCE_CODE_IS_GENERIC;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.OFFENCE_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.OFFENCE_SEQUENCE_NO;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OffenceGenericValidationAndEnricherRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    private static final String GENERIC_OFFENCE_CODE = "998";

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {

        if (defendantWithReferenceData.getDefendant() == null || isEmpty(defendantWithReferenceData.getDefendant().getOffences())) {
            return VALID;
        }
        final List<Problem> problems = defendantWithReferenceData.getDefendant().getOffences().stream()
                .map(offence -> verifyGenericOffenceCode(offence, defendantWithReferenceData.getCaseDetails().getInitiationCode(), defendantWithReferenceData, referenceDataQueryService))
                .filter(Objects::nonNull).collect(Collectors.toList());
        if (isEmpty(problems)) {
            return VALID;
        }
        return newValidationResult(problems);
    }

    private Problem verifyGenericOffenceCode(final Offence offence, final String initiationCode, final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final ReferenceDataVO referenceDataVO = defendantWithReferenceData.getReferenceDataVO();

        List<OffenceReferenceData> offenceReferenceDataListFromVO = referenceDataVO.getOffenceReferenceData().stream()
                .filter(rd -> rd.getCjsOffenceCode().equals(offence.getOffenceCode())).collect(Collectors.toList());

        if (offenceReferenceDataListFromVO.isEmpty()) {

            final List<OffenceReferenceData> newOffenceReferenceDataList = referenceDataQueryService.retrieveOffenceData(offence, initiationCode).stream()
                    .filter(rd -> rd.getCjsOffenceCode().equals(offence.getOffenceCode()))
                    .collect(Collectors.toList());

            if (!isEmpty(newOffenceReferenceDataList)) {
                if (referenceDataVO.getOffenceReferenceData() != null) {
                    referenceDataVO.getOffenceReferenceData().addAll(newOffenceReferenceDataList);
                } else {
                    referenceDataVO.setOffenceReferenceData(newOffenceReferenceDataList);
                }

                offenceReferenceDataListFromVO = referenceDataVO.getOffenceReferenceData().stream()
                        .filter(rd -> rd.getCjsOffenceCode().equals(offence.getOffenceCode())).collect(Collectors.toList());
            }
        }

        if (!isEmpty(offenceReferenceDataListFromVO)) {
            return validateOffenceCodeGeneric(offence, offenceReferenceDataListFromVO);
        }
        return null;
    }

    private Problem validateOffenceCodeGeneric(final Offence offence, final List<OffenceReferenceData> offenceReferenceDataListFromVO) {
        if (hasGenericOffenceCode(offenceReferenceDataListFromVO) || offence.getOffenceCode().equalsIgnoreCase(GENERIC_OFFENCE_CODE)) {
            return getProblemForOffence(offence);
        }
        return null;
    }

    private boolean hasGenericOffenceCode(final List<OffenceReferenceData> offenceReferenceData) {
        return offenceReferenceData.stream().map(OffenceReferenceData::getCjsOffenceCode)
                .map(GENERIC_OFFENCE_CODE::equals).findAny()
                .orElse(false);
    }

    private Problem getProblemForOffence(final Offence offence) {
        return newProblem(
                OFFENCE_CODE_IS_GENERIC,
                new ProblemValue(offence.getOffenceId().toString(), OFFENCE_CODE.getValue(), offence.getOffenceCode()),
                new ProblemValue(offence.getOffenceId().toString(), OFFENCE_SEQUENCE_NO.getValue(), String.valueOf(offence.getOffenceSequenceNumber())));
    }
}