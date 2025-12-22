package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.CASE_MARKER_IS_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.CASE_MARKERS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseMarker;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.ArrayList;
import java.util.List;

public class CaseMarkersValidationAndEnricherRule implements ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService> {


    @Override
    public ValidationResult validate(final ProsecutionWithReferenceData prosecutionWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final List<CaseMarker> caseMarkers = prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseMarkers();

        if (caseMarkers == null || caseMarkers.isEmpty()) {
            return VALID;
        }

        final List<String> caseMarkerTypeCodes = caseMarkers.stream().map(CaseMarker::getMarkerTypeCode).collect(toList());

        final List<CaseMarker> caseMarkersRefData = referenceDataQueryService.getCaseMarkerDetails().stream()
                .filter(caseMarker -> caseMarkerTypeCodes.stream().anyMatch(caseMarker.getMarkerTypeCode()::equalsIgnoreCase))
                .collect(toList());
        prosecutionWithReferenceData.getReferenceDataVO().setCaseMarkers(caseMarkersRefData);

        final List<ProblemValue> problemValues = new ArrayList<>();
        for (int i = 0; i < caseMarkers.size(); i++) {
            final CaseMarker curCaseMarker = caseMarkers.get(i);
            final boolean isValidCaseMarker = caseMarkersRefData.stream().anyMatch(caseMarkerRefData -> caseMarkerRefData.getMarkerTypeCode().equalsIgnoreCase(curCaseMarker.getMarkerTypeCode()));
            if (!isValidCaseMarker) {
                problemValues.add(new ProblemValue(Integer.toString(i), CASE_MARKERS.getValue(), curCaseMarker.getMarkerTypeCode()));
            }
        }


        if (problemValues.isEmpty()) {
            return VALID;
        }

        return newValidationResult(of(newProblem(CASE_MARKER_IS_INVALID, problemValues.toArray(new ProblemValue[problemValues.size()]))));
    }
}
