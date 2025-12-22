package uk.gov.moj.cpp.prosecutioncasefile.query.view;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.getString;
import static uk.gov.justice.services.messaging.JsonObjects.getUUID;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination.PaginationParameterFactory.newPaginationParameter;

import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncasefile.mapping.FilterParameter;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination.PaginationParameter;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination.PaginationResult;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.response.BusinessValidationErrorView;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.response.CaseDetailsView;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.response.CountsCasesErrorsView;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.service.BusinessErrorDetailsService;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.service.CaseDetailsService;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.service.CountsCasesErrorsService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.persistence.NoResultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProsecutionCasefileQueryView {

    public static final String PROSECUTION_CASE_FILE_QUERY_CASES_ERRORS = "prosecutioncasefile.query.cases.errors";
    public static final String CASE_ID = "caseId";
    public static final String CASES = "cases";
    public static final String RESULTS = "results";
    public static final String PAGE_COUNT = "pageCount";
    public static final String FILTERS = "filters";
    public static final String COURT = "court";
    public static final String CASE_TYPE = "caseType";
    public static final String URN = "urn";
    public static final String REGION = "region";
    public static final String HEARING_DATE_FROM = "hearingDateFrom";
    public static final String HEARING_DATE_TO = "hearingDateTo";
    public static final String COURT_LOCATION = "courtLocation";
    public static final String PROSECUTION_CASE_FILE_QUERY_VIEW_CASES_ERRORS_COUNT = "prosecutioncasefile.query.counts-cases-errors";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCasefileQueryView.class);
    private static final String NO_CASE_FOUND_RESPONSE = "prosecutioncasefile.query.no-case-response";
    private static final String CASE_DETAILS_RESPONSE = "prosecutioncasefile.query.case";
    private static final String CASE_DETAILS_RESPONSE_BY_PROS_CASE_REF = "prosecutioncasefile.query.case-by-prosecutionCaseReference";

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private CaseDetailsService caseDetailsService;

    @Inject
    private ListToJsonArrayConverter listToJsonArrayConverter;

    @Inject
    private BusinessErrorDetailsService businessErrorDetailsService;

    @Inject
    private CountsCasesErrorsService countsCasesErrorsService;

    public JsonEnvelope getCaseDetails(final JsonEnvelope envelope) {
        final Optional<UUID> caseId = getUUID(envelope.payloadAsJsonObject(), CASE_ID);
        final CaseDetailsView caseDetails;

        caseDetails = caseDetailsService.findCase(caseId.orElse(null));

        return envelopeFrom(metadataFrom(envelope.metadata()).withName(CASE_DETAILS_RESPONSE), caseDetails != null ? objectToJsonObjectConverter.convert(caseDetails) : JsonValue.NULL);
    }

    public JsonEnvelope getCaseDetailsByProsecutionCaseReference(final JsonEnvelope envelope) {
        final Optional<String> prosecutionCaseReference = getString(envelope.payloadAsJsonObject(), "prosecutionCaseReference");
        final CaseDetailsView caseDetails;

        try {
            caseDetails = caseDetailsService.findCaseByProsecutionReferenceId(prosecutionCaseReference.orElse(null));
        } catch (final NoResultException nre) {
            LOGGER.info("No Case found for prosecutionCaseReference: " + prosecutionCaseReference, nre);
            return envelopeFrom(metadataFrom(envelope.metadata()).withName(NO_CASE_FOUND_RESPONSE), JsonValue.NULL);
        }

        return envelopeFrom(metadataFrom(envelope.metadata()).withName(CASE_DETAILS_RESPONSE_BY_PROS_CASE_REF), caseDetails != null ? objectToJsonObjectConverter.convert(caseDetails) : JsonValue.NULL);
    }

    public JsonEnvelope getErrorDetailsForCases(final JsonEnvelope envelope) {
        final Optional<UUID> caseId = getUUID(envelope.payloadAsJsonObject(), CASE_ID);
        final List<BusinessValidationErrorView> allBusinessValidationErrors;
        if (caseId.isPresent()) {
            allBusinessValidationErrors = businessErrorDetailsService.findAllErrorsByCaseId(caseId.orElse(null));
            return envelopeFrom(metadataFrom(envelope.metadata()).withName(PROSECUTION_CASE_FILE_QUERY_CASES_ERRORS), createObjectBuilder()
                    .add(CASES, (isEmpty(allBusinessValidationErrors)) ? createArrayBuilder().build() : listToJsonArrayConverter.convert(allBusinessValidationErrors))
                    .build());
        }
        return paginateBusinessValidationErrors(envelope);
    }

    public JsonEnvelope casesErrorsCount(final JsonEnvelope envelope) {
        final Optional<String> region = getString(envelope.payloadAsJsonObject(), REGION);
        final Optional<String> courtLocation = getString(envelope.payloadAsJsonObject(), COURT_LOCATION);
        final Optional<String> caseType = getString(envelope.payloadAsJsonObject(), CASE_TYPE);

        final CountsCasesErrorsView casesCount = countsCasesErrorsService.countsCasesErrors(region, courtLocation, caseType);

        return envelopeFrom(metadataFrom(envelope.metadata())
                        .withName(PROSECUTION_CASE_FILE_QUERY_VIEW_CASES_ERRORS_COUNT),
                casesCount != null ? objectToJsonObjectConverter.convert(casesCount) : JsonValue.NULL);
    }

    private JsonEnvelope paginateBusinessValidationErrors(final JsonEnvelope envelope) {
        final JsonObject queryFilters = envelope.payloadAsJsonObject();
        final PaginationParameter paginationParameter = newPaginationParameter(queryFilters);
        final FilterParameter filterParameter = newFilterParameter(queryFilters);
        final PaginationResult<BusinessValidationErrorView> allErrors = businessErrorDetailsService.findAllErrors(paginationParameter, filterParameter);
        return envelopeFrom(metadataFrom(envelope.metadata()).withName(PROSECUTION_CASE_FILE_QUERY_CASES_ERRORS), buildResponsePayload(allErrors, queryFilters));
    }

    private FilterParameter newFilterParameter(final JsonObject queryFilters) {
        final FilterParameter.FilterParameterBuilder filterBuilder = FilterParameter.filterParameterBuilder();
        getString(queryFilters, COURT).ifPresent(filterBuilder::withCourt);
        getString(queryFilters, CASE_TYPE).ifPresent(filterBuilder::withCaseType);
        getString(queryFilters, URN).ifPresent(filterBuilder::withUrn);
        getString(queryFilters, HEARING_DATE_FROM).ifPresent(filterBuilder::withHearingDateFrom);
        getString(queryFilters, HEARING_DATE_TO).ifPresent(filterBuilder::withHearingDateTo);

        return filterBuilder.build();
    }

    private JsonObject buildResponsePayload(final PaginationResult<BusinessValidationErrorView> paginationResult, final JsonObject filters) {
        final List<BusinessValidationErrorView> allBusinessValidationErrors = paginationResult.getResult();
        return createObjectBuilder()
                .add(RESULTS, paginationResult.getTotalResultCount())
                .add(PAGE_COUNT, paginationResult.getPageCount())
                .add(CASES, (isEmpty(allBusinessValidationErrors)) ? createArrayBuilder().build() : listToJsonArrayConverter.convert(allBusinessValidationErrors))
                .add(FILTERS, filters)
                .build();
    }

}
