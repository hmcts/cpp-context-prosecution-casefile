package uk.gov.moj.cpp.prosecution.casefile.helper;

import static java.lang.String.format;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.moj.cpp.prosecution.casefile.helper.AbstractTestHelper.USER_ID;
import static uk.gov.moj.cpp.prosecution.casefile.helper.AbstractTestHelper.getReadUrl;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;

public class DefaultRequests {
    public static final String GET_CASE_DEFENDANTS = "application/vnd.prosecutioncasefile.query.defendants+json";
    public static final String GET_CASE_DETAILS = "application/vnd.prosecutioncasefile.query.case+json";
    public static final String GET_CASE_ERROR_DETAILS = "application/vnd.prosecutioncasefile.query.cases.errors+json";
    public static final String GET_CASE_DETAILS_BY_PROSECUTION_CASE_REF = "application/vnd.prosecutioncasefile.query.case-by-prosecutionCaseReference+json";

    public static RequestParamsBuilder getDefendantsByCaseIdBuilder(final String caseId) {
        return requestParams(getReadUrl(format("/cases/%s/defendants", caseId)), GET_CASE_DEFENDANTS)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
    }

    public static RequestParamsBuilder getCaseDetailsBuilder(final String caseId) {
        return requestParams(getReadUrl(format("/cases/%s", caseId)), GET_CASE_DETAILS)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
    }

    public static RequestParamsBuilder getCaseDetailsByProsecutionReferenceIdBuilder(final String prosecutionCaseReference) {
        return requestParams(getReadUrl(format("/cases?prosecutionCaseReference=" + prosecutionCaseReference)), GET_CASE_DETAILS_BY_PROSECUTION_CASE_REF)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
    }

    public static RequestParamsBuilder getAllCaseErrorDetails() {
        return requestParams(getReadUrl(format("/cases")), GET_CASE_ERROR_DETAILS)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
    }

    public static RequestParamsBuilder getCaseErrorDetails(final String caseId) {
        return requestParams(getReadUrl(format("/cases/?caseId=" + caseId)), GET_CASE_ERROR_DETAILS)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
    }
}
