package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.CASE_INITIATION_CODE_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.CASE_LIST_NEW_LISTING_HEARING;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.CASE_INITIATION_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.LIST_DEFENDANT_REQUESTS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.List;


public class CaseNewhearingListDefendantsValidationRule implements ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService> {


    @Override
    public ValidationResult validate(final ProsecutionWithReferenceData prosecutionWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {

        final Prosecution prosecution = prosecutionWithReferenceData.getProsecution();
        final Channel channel = prosecution.getChannel();
        final boolean isMCCWithNewHearingList = Channel.MCC == channel && (nonNull(prosecution.getListNewHearing()));

        if (isMCCWithNewHearingList) {
            final List<?> listDefendantRequests = prosecution.getListNewHearing().getListDefendantRequests();
            final boolean listDefendantRequestsMatchDefendants =
                    nonNull(listDefendantRequests) && prosecution.getDefendants().size() == listDefendantRequests.size();
            return listDefendantRequestsMatchDefendants ? VALID :
                    newValidationResult(of(newProblem(CASE_LIST_NEW_LISTING_HEARING, new ProblemValue(null, LIST_DEFENDANT_REQUESTS.getValue(), ""))));
        }

        return VALID;

    }
}
