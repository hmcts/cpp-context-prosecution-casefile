package uk.gov.moj.cpp.prosecution.casefile.query.api;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.CaseForCitizenService;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.DefendantType;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.ProsecutionCasefileQueryView;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static java.time.LocalDate.parse;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.CaseForCitizenService.DOB_DATE_PATTERN;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.DefendantType.PERSON;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.DefendantType.valueOf;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.SjpService.POSTCODE;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.SjpService.URN;

@ServiceComponent(Component.QUERY_API)
public class CasefileQueryApi {

    static final String DEFENDANT_TYPE = "defendantType";
    static final String DATE_OF_BIRTH = "dob";

    @Inject
    private ProsecutionCasefileQueryView casefileQueryView;

    @Inject
    private CaseForCitizenService caseForCitizenService;

    @Inject
    private Requester requester;

    @Handles("prosecutioncasefile.query.case")
    public JsonEnvelope getCaseDetails(final JsonEnvelope query) {
        return casefileQueryView.getCaseDetails(query);
    }

    @Handles("prosecutioncasefile.query.case-by-prosecutionCaseReference")
    public JsonEnvelope getCaseByProsecutionCaseReference(final JsonEnvelope query) {
        return casefileQueryView.getCaseDetailsByProsecutionCaseReference(query);
    }

    @Handles("prosecutioncasefile.query.cases.errors")
    public JsonEnvelope getErrorDetailsForCases(final JsonEnvelope query) {
        return casefileQueryView.getErrorDetailsForCases(query);
    }

    @Handles("prosecutioncasefile.query.counts-cases-errors")
    public JsonEnvelope casesErrorsCount(final JsonEnvelope query) {
        return casefileQueryView.casesErrorsCount(query);
    }

    @Handles("prosecutioncasefile.query.case-for-citizen")
    public JsonEnvelope findCaseForCitizen(final JsonEnvelope jsonEnvelope) {

        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        final String caseUrn = payload.getString(URN);
        final String postcode = payload.getString(POSTCODE);
        final DefendantType defendantType = valueOf(payload.getString(DEFENDANT_TYPE).toUpperCase());
        final LocalDate dob = ofNullable(payload.get(DATE_OF_BIRTH)).map(v -> parseDate(payload.getString(DATE_OF_BIRTH))).orElse(null);

        if (defendantType == PERSON && isNull(dob)) {
            throw new BadRequestException("Error!! query param 'dob' is mandatory when defendantType==person");
        }

        return JsonEnvelope.envelopeFrom(jsonEnvelope.metadata(),
                caseForCitizenService.getCaseWithDefendant(jsonEnvelope, requester, defendantType, caseUrn, postcode, dob));
    }

    private LocalDate parseDate(final String dateQueryStr) {
        try {
            return parse(dateQueryStr, ofPattern(DOB_DATE_PATTERN));
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid dateFormat of dob pram, allowed format=" + DOB_DATE_PATTERN, e);
        }
    }
}
