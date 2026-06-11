package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_CUSTODY_STATUS_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.DEFENDANT_CUSTODY_STATUS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.BailStatusReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.Objects;
import java.util.Optional;

public class CustodyStatusValidationAndEnricherRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    public static final String CHARGE_CASE_TYPE = "C";
    public static final String O_CASE_TYPE = "O";

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {

        boolean isDefendantOrganisation = Objects.nonNull(defendantWithReferenceData.getDefendant().getOrganisationName()) ;
        boolean isChargeCase = CHARGE_CASE_TYPE.equals(defendantWithReferenceData.getCaseDetails().getInitiationCode());
        boolean isMCCAndCaseTypeO= O_CASE_TYPE.equals(defendantWithReferenceData.getCaseDetails().getInitiationCode()) && defendantWithReferenceData.isMCC();

        if(isDefendantOrganisation){
            return VALID;
        }

        if(isChargeCase || isMCCAndCaseTypeO ) {
            if(defendantWithReferenceData.getDefendant().getCustodyStatus() == null) {
                return newValidationResult(of(newProblem(DEFENDANT_CUSTODY_STATUS_INVALID, new ProblemValue(null,DEFENDANT_CUSTODY_STATUS.getValue(), ""))));
            }
            final String custodyStatus = defendantWithReferenceData.getDefendant().getCustodyStatus();
            final ReferenceDataVO referenceDataVO = defendantWithReferenceData.getReferenceDataVO();

            Optional<BailStatusReferenceData> bailStatusReferenceDataOptional = referenceDataVO.getBailStatusReferenceData().stream().filter(custodyStatusReferenceData -> custodyStatus.equals(custodyStatusReferenceData.getStatusCode())).findAny();
            if (bailStatusReferenceDataOptional.isPresent()) {
                return VALID;
            }

            bailStatusReferenceDataOptional = referenceDataQueryService.retrieveBailStatuses().stream().filter(custodyStatusReferenceData -> custodyStatus.equals(custodyStatusReferenceData.getStatusCode())).findAny();
            if (bailStatusReferenceDataOptional.isPresent()) {
                defendantWithReferenceData.getReferenceDataVO().addBailStatusReferenceData(bailStatusReferenceDataOptional.get());
                return VALID;
            } else {
                return newValidationResult(of(newProblem(DEFENDANT_CUSTODY_STATUS_INVALID, new ProblemValue(null,DEFENDANT_CUSTODY_STATUS.getValue(), custodyStatus))));

            }
        } else {
            return VALID;
        }
    }
}
