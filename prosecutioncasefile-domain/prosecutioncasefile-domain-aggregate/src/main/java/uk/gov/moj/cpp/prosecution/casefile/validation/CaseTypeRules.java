package uk.gov.moj.cpp.prosecution.casefile.validation;

import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.prosecution.casefile.validation.CaseType.OTHER;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;

public class CaseTypeRules {

    private CaseTypeRules() {
    }

    /**
     * OTHER-type (civil, initiation code "O") cases must never be hard-rejected by these
     * validation rules. A separate upstream API layer already validates these cases before they
     * reach this system, and any failure that still gets through should let the case proceed to
     * downstream listing/allocation, where it will naturally end up "Unallocated" rather than
     * allocated.
     */
    public static boolean isOtherCaseType(final DefendantWithReferenceData defendantWithReferenceData) {
        final CaseDetails caseDetails = defendantWithReferenceData.getCaseDetails();
        return nonNull(caseDetails) && OTHER.getCode().equalsIgnoreCase(caseDetails.getInitiationCode());
    }
}