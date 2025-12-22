package uk.gov.moj.cpp.prosecution.casefile.query.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.CaseDefendantHearings;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.CaseDetail;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

public class ProgressionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionService.class);
    public static final String PROSECUTION_GET_CASE = "progression.query.case";
    public static final String PROGRESSION_SEARCH_CASES = "progression.query.search-cases";
    public static final String PROGRESSION_CASE_DEFENDANT_HEARINGS = "progression.query.case-defendant-hearings";
    public static final String CASE_ID = "caseId";
    public static final String DEFENDANT_ID = "defendantId";
    public static final String QUERY_PARAM = "q";
    public static final String SEARCH_RESULT = "searchResults";

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private CaseDetailConverter caseDetailConverter;

    private final Predicate<Defendant> defendantPersonWithPostcodePredicate = d -> nonNull(d.getPersonDefendant())
            && nonNull(d.getPersonDefendant().getPersonDetails())
            && nonNull(d.getPersonDefendant().getPersonDetails().getAddress())
            && nonNull(d.getPersonDefendant().getPersonDetails().getAddress().getPostcode());

    private final Predicate<Defendant> defendantLegalEntityWithPostcodePredicate = d -> nonNull(d.getLegalEntityDefendant())
            && nonNull(d.getLegalEntityDefendant().getOrganisation())
            && nonNull(d.getLegalEntityDefendant().getOrganisation().getAddress())
            && nonNull(d.getLegalEntityDefendant().getOrganisation().getAddress().getPostcode());

    public List<CaseDetail> getCaseDetailWithDefendantForPerson(final Envelope<?> originatingQuery, final Requester requester, final String caseId, final String postcode) {
        final ProsecutionCase prosecutionCase = getProsecutionCase(originatingQuery, requester, caseId);

        final List<Defendant> matchingDefendants = prosecutionCase.getDefendants().stream()
                .filter(defendantPersonWithPostcodePredicate)
                .filter(defendantWithPostCode -> postcode.equals(defendantWithPostCode.getPersonDefendant().getPersonDetails().getAddress().getPostcode()))
                .collect(Collectors.toList());

        return caseDetailConverter.convert(originatingQuery, requester, prosecutionCase, matchingDefendants);
    }

    public List<CaseDetail> getCaseDetailWithDefendantForLegalEntity(final Envelope<?> originatingQuery, final Requester requester, final String caseId, final String postcode) {
        final ProsecutionCase prosecutionCase = getProsecutionCase(originatingQuery, requester, caseId);

        final List<Defendant> matchingDefendants = prosecutionCase.getDefendants().stream()
                .filter(defendantLegalEntityWithPostcodePredicate)
                .filter(defendantLegalEntityWithPostCode -> postcode.equals(defendantLegalEntityWithPostCode.getLegalEntityDefendant().getOrganisation().getAddress().getPostcode()))
                .collect(Collectors.toList());

        return caseDetailConverter.convert(originatingQuery, requester, prosecutionCase, matchingDefendants);
    }

    public ProsecutionCase getProsecutionCase(final Envelope<?> originatingQuery, final Requester requester, final String caseId) {
        LOGGER.info(" Calling {} to get prosecutionCase for caseId={} ", PROSECUTION_GET_CASE, caseId);

        final Envelope<JsonObject> requestEnvelope = envelop(createObjectBuilder().add(CASE_ID, caseId).build())
                .withName(PROSECUTION_GET_CASE).withMetadataFrom(originatingQuery);
        final Envelope<JsonObject> responseEnvelope = requester.request(requestEnvelope, JsonObject.class);

        if (isNull(responseEnvelope.payload())) {
            throw new ProgressionServiceException(format("Failed to get prosecution case from progression for caseId %s", caseId));
        }

        return jsonObjectToObjectConverter.convert(responseEnvelope.payload().getJsonObject("prosecutionCase"), ProsecutionCase.class);
    }

    public CaseDefendantHearings getCaseDefendantHearings(final Envelope<?> originatingQuery, final Requester requester, final String caseId, final String defendantId) {
        LOGGER.info(" Calling {} to defendant hearings for caseId={} and defendantId={} ", PROGRESSION_CASE_DEFENDANT_HEARINGS, caseId, defendantId);

        final Envelope<JsonObject> requestEnvelope = envelop(createObjectBuilder()
                .add(CASE_ID, caseId).add(DEFENDANT_ID, defendantId).build())
                .withName(PROGRESSION_CASE_DEFENDANT_HEARINGS).withMetadataFrom(originatingQuery);
        final Envelope<JsonObject> responseEnvelope = requester.request(requestEnvelope, JsonObject.class);

        if (isNull(responseEnvelope.payload())) {
            throw new ProgressionServiceException(format("Failed to get defendant hearings from progression for caseId %s and defendantId %s", caseId, defendantId));
        }

        return jsonObjectToObjectConverter.convert(responseEnvelope.payload(), CaseDefendantHearings.class);
    }

    public Optional<String> getCaseId(final Envelope<?> originatingQuery, final Requester requester, final String caseUrn) {
        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(createObjectBuilder().add(QUERY_PARAM, caseUrn).build())
                .withName(PROGRESSION_SEARCH_CASES).withMetadataFrom(originatingQuery);

        final Envelope<JsonObject> responseEnvelope = requester.request(requestEnvelope, JsonObject.class);

        if (nonNull(responseEnvelope.payload()) && nonNull(responseEnvelope.payload().getJsonArray(SEARCH_RESULT))
                && !responseEnvelope.payload().getJsonArray(SEARCH_RESULT).isEmpty()) {

            return of(responseEnvelope.payload().getJsonArray(SEARCH_RESULT).getJsonObject(0).getString(CASE_ID));
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(format("Failed to find case by urn in progression for caseUrn %s", caseUrn));
        }
        return empty();
    }
}
