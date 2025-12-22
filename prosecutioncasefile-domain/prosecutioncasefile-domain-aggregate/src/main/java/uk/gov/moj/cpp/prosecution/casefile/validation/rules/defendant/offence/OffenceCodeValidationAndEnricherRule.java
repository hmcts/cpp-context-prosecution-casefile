package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence;

import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.OFFENCE_CODE_IS_INVALID;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OffenceCodeValidationAndEnricherRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {


    private static final String GENERIC_ALTERED_OFFENCE_CODE = "998A";

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {

        String feeStatus = defendantWithReferenceData.getCaseDetails().getFeeStatus();

        if(feeStatus != null) {
            return VALID;
        }

        if (defendantWithReferenceData.getDefendant() == null || defendantWithReferenceData.getDefendant().getOffences() == null
                || defendantWithReferenceData.getDefendant().getOffences().isEmpty()) {
            return VALID;
        }

        final List<Problem> problems = defendantWithReferenceData.getDefendant().getOffences().stream()
                .map(offence -> verifyOffenceCode(offence, defendantWithReferenceData.getCaseDetails().getInitiationCode(), defendantWithReferenceData, referenceDataQueryService))
                .filter(Objects::nonNull).collect(Collectors.toList());

        if (null == problems || problems.isEmpty()) {
            return VALID;
        }

        return newValidationResult(problems);

    }

    private Problem verifyOffenceCode(final Offence offence, final String initiationCode, final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final ReferenceDataVO referenceDataVO = defendantWithReferenceData.getReferenceDataVO();
        final List<OffenceReferenceData> offenceReferenceDataListFromVO = referenceDataVO.getOffenceReferenceData().stream()
                .filter(rd -> rd.getCjsOffenceCode().equals(offence.getOffenceCode())).filter(Objects::nonNull).collect(Collectors.toList());

        if (offence.getOffenceCode().equalsIgnoreCase(GENERIC_ALTERED_OFFENCE_CODE)) {
            return null;
        }

        if (offenceReferenceDataListFromVO != null && !offenceReferenceDataListFromVO.isEmpty()) {
            return null;
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
        } else {
            return newProblem(OFFENCE_CODE_IS_INVALID,
                    new ProblemValue(offence.getOffenceId().toString(), OFFENCE_CODE.getValue(), offence.getOffenceCode()),
                    new ProblemValue(offence.getOffenceId().toString(), OFFENCE_SEQUENCE_NO.getValue(), offence.getOffenceSequenceNumber().toString())
            );
        }
    }

}