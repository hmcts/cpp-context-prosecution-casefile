package uk.gov.moj.cpp.prosecution.casefile.helper;

import static java.lang.String.format;
import static uk.gov.moj.cpp.prosecution.casefile.helper.QueryHelper.pollForResponse;

import uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher;

public class CaseFileQueryHelper extends AbstractTestHelper {

    public static String getCaseForCitizenWithNoMandatoryQueryParams(final String userId, final ResponseStatusMatcher responseStatusMatcher) {
        return pollForResponse("/cases-for-citizen", "application/vnd.prosecutioncasefile.query.case-for-citizen+json", userId, responseStatusMatcher);
    }

    public static String getCaseForCitizenWithMandatoryQueryParams(final String userId, final String caseUrn, final String postcode,
                                                                   final String defendantType, final String dob, final ResponseStatusMatcher responseStatusMatcher) {
        final String path = format("/cases-for-citizen?urn=%s&postcode=%s&defendantType=%s&dob=%s", caseUrn, postcode, defendantType, dob);
        return pollForResponse(path, "application/vnd.prosecutioncasefile.query.case-for-citizen+json", userId, responseStatusMatcher);
    }
}
