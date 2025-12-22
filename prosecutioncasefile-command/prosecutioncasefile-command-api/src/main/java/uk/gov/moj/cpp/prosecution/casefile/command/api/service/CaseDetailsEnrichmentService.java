package uk.gov.moj.cpp.prosecution.casefile.command.api.service;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;

import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;

public class CaseDetailsEnrichmentService {
    @Inject
    private IdGenerationService idGenerationService;

    public CaseDetails enrichCaseDetails(final CaseDetails caseDetails, Prosecutor prosecutorWithReferenceData) {
        final String prosecutorCaseReference = Optional.ofNullable(caseDetails.getProsecutorCaseReference())
                .orElseGet(() -> idGenerationService.generateCaseReference());
        final UUID caseId = Optional.ofNullable(caseDetails.getCaseId())
                .orElseGet(() -> idGenerationService.generateCaseId(prosecutorCaseReference));
        return enrichCaseDetailsWithCaseIdAndProsecutorCaseReference(caseId, prosecutorCaseReference, caseDetails, prosecutorWithReferenceData);
    }

    private CaseDetails enrichCaseDetailsWithCaseIdAndProsecutorCaseReference(final UUID caseId, final String prosecutorCaseReference, final CaseDetails caseDetails, final Prosecutor prosecutorWithReferenceData) {
        return CaseDetails.caseDetails()
                .withValuesFrom(caseDetails)
                .withCaseId(caseId)
                .withProsecutor(prosecutorWithReferenceData)
                .withProsecutorCaseReference(prosecutorCaseReference)
                .build();
    }
}

