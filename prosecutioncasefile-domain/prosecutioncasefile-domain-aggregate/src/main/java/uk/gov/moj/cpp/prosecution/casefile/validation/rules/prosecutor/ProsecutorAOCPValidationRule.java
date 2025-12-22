package uk.gov.moj.cpp.prosecution.casefile.validation.rules.prosecutor;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.PROSECUTING_AUTHORITY;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

public class ProsecutorAOCPValidationRule
        implements ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(ProsecutionWithReferenceData prosecution, ReferenceDataQueryService query) {

        final Prosecutor prosecutor = prosecution.getProsecution().getCaseDetails().getProsecutor();
        if (isValidIfProsecutorIsAOCPApproved(prosecution.getProsecution() )){
            return VALID;
        } else {
            return newValidationResult(of(newProblem(ProblemCode.PROSECUTOR_NOT_AOCP_APPROVED,
                    new ProblemValue(null,
                            PROSECUTING_AUTHORITY.getValue(),
                            prosecutor.getProsecutingAuthority()))));
        }
    }

    private boolean isValidIfProsecutorIsAOCPApproved(final Prosecution prosecution) {
        final Prosecutor prosecutor = prosecution.getCaseDetails().getProsecutor();
        final boolean isAnyOffenceHasAOCPOffer = prosecution.getDefendants().stream()
                .flatMap(defendant -> defendant.getOffences().stream())
                .anyMatch(offence -> offence.getProsecutorOfferAOCP() != null && offence.getProsecutorOfferAOCP());

        if (!isAnyOffenceHasAOCPOffer) {
            return true;
        } else {
            return prosecutor.getReferenceData() != null && prosecutor.getReferenceData().getAocpApproved();
        }
    }
}



