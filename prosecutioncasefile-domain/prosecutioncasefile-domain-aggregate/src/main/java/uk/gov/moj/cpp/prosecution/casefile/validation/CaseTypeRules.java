package uk.gov.moj.cpp.prosecution.casefile.validation;

import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.prosecution.casefile.validation.CaseType.OTHER;

import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;

public class CaseTypeRules {

    private CaseTypeRules() {
    }

    /**
     * OTHER-type (civil, initiation code "O") case. Broad by design - covers all civil
     * submissions, not just Enforcement ones (Listing's own auto-allocation eligibility check
     * relies on this broad meaning). Do not use this alone to bypass field-level validation
     * rules - see {@link #isOtherCaseTypeDateRangeSubmission(DefendantWithReferenceData)}.
     */
    public static boolean isOtherCaseType(final DefendantWithReferenceData defendantWithReferenceData) {
        final CaseDetails caseDetails = defendantWithReferenceData.getCaseDetails();
        return nonNull(caseDetails) && OTHER.getCode().equalsIgnoreCase(caseDetails.getInitiationCode());
    }

    /**
     * Enforcement Auto's date-range submission (CIMD-3539, {@code hearingDateRangeDetails}) genuinely
     * has no real courtHearingLocation/dateOfHearing at this validation stage - the actual slot is
     * resolved later, downstream in Listing. {@code InitialHearing.endDate} is only ever populated
     * for this specific submission shape, so it's used here to distinguish it from an ordinary
     * OTHER-type/civil single-date submission ({@code hearingDetails}), which does carry real,
     * validatable values and must not have field-level validation bypassed.
     */
    public static boolean isOtherCaseTypeDateRangeSubmission(final DefendantWithReferenceData defendantWithReferenceData) {
        if (!isOtherCaseType(defendantWithReferenceData)) {
            return false;
        }
        final InitialHearing initialHearing = defendantWithReferenceData.getDefendant().getInitialHearing();
        return nonNull(initialHearing) && nonNull(initialHearing.getEndDate());
    }
}