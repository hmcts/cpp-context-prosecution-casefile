package uk.gov.moj.cpp.prosecution.casefile.command.api;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCivilApplication;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Applicant;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplicationCase;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Respondent;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.response.CaseDetailsView;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.service.CaseDetailsService;
import uk.gov.moj.cps.prosecutioncasefile.command.api.SubmitApplication;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;

@ServiceComponent(COMMAND_API)
public class SubmitCCApplicationApi {
    private static final Logger LOGGER = getLogger(SubmitCCApplicationApi.class);

    @Inject
    private Sender sender;

    @Inject
    private Requester requester;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Inject
    CaseDetailsService caseDetailsService;
    public static final String CASE_ID = "caseId";
    private static final String PROGRESSION_QUERY_PROSECUTION_CASE = "progression.query.prosecutioncase";

    @Handles("prosecutioncasefile.command.submit-application")
    public void submitCCApplication(final JsonEnvelope envelope) {

        List<CaseDetailsView> prosecutionReferenceIds = Collections.emptyList();

        final SubmitApplication incomingSubmitApplication = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), SubmitApplication.class);

        final List<String> caseURNs = ofNullable(incomingSubmitApplication.getCourtApplication().getCourtApplicationCases())
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(Objects::nonNull)
                .map(CourtApplicationCase::getCaseURN)
                .filter(Objects::nonNull)
                .collect(toList());

        if(!caseURNs.isEmpty()) {
            prosecutionReferenceIds = caseDetailsService.findAllCaseByProsecutionReferenceIds(caseURNs);
        }


        final List<ProsecutionCase> prosecutionCases = ofNullable(prosecutionReferenceIds)
                .orElseGet(Collections::emptyList).stream()
                .map(caseDetailsView -> getProsecutionCase(envelope, caseDetailsView.getCaseId()))
                .filter(Objects::nonNull)
                .collect(toList());

        final List<CourtApplicationType> courtApplicationTypes = referenceDataQueryService.retrieveApplicationTypes();

        final Optional<OrganisationUnitWithCourtroomReferenceData> courtroomReferenceData = referenceDataQueryService.retrieveCourtCentreDetails(incomingSubmitApplication.getBoxHearingRequest().getCourtCentre().getName());
        final boolean isCivil = CollectionUtils.isNotEmpty(prosecutionCases) && prosecutionCases.get(0).getIsCivil();

        final SubmitApplication submitApplicationCommand = SubmitApplication.submitApplication()
                .withCourtApplication(buildCourtApplication(incomingSubmitApplication.getCourtApplication(), isCivil))
                .withBoxHearingRequest(incomingSubmitApplication.getBoxHearingRequest())
                .withProsecutionCases(isEmpty(prosecutionCases) ? null : prosecutionCases)
                .withApplicationTypes(isEmpty(courtApplicationTypes) ? null : courtApplicationTypes)
                .withCourtroomReferenceData(courtroomReferenceData.orElse(null))
                .withPocaFileId(incomingSubmitApplication.getPocaFileId())
                .withSenderEmail(incomingSubmitApplication.getSenderEmail())
                .withEmailSubject(incomingSubmitApplication.getEmailSubject())
                .build();

        this.sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata()).withName("prosecutioncasefile.command.submit-application").build(),
                submitApplicationCommand));
    }

    private CourtApplication buildCourtApplication(final CourtApplication courtApplication, final Boolean isCivil) {
        return CourtApplication.courtApplication()
                .withValuesFrom(courtApplication)
                .withApplicant(generateId(courtApplication.getApplicant()))
                .withRespondents(generateId(courtApplication.getRespondents()))
                .withCourtCivilApplication(CourtCivilApplication.courtCivilApplication()
                        .withIsCivil(isCivil)
                        .build())
                .build();
    }

    private Applicant generateId(final Applicant applicant) {
        if (isNull(applicant)) {
            return null;
        }
        return Applicant.applicant()
                .withValuesFrom(applicant)
                .withId(randomUUID())
                .build();
    }

    @SuppressWarnings("squid:S1168")
    private List<Respondent> generateId(final List<Respondent> respondents) {
        if (isNull(respondents)) {
            return null;
        }
        return respondents.stream().map(this::generateId).collect(toList());
    }

    private Respondent generateId(final Respondent respondent) {
        if (isNull(respondent)) {
            return null;
        }
        return Respondent.respondent()
                .withValuesFrom(respondent)
                .withId(randomUUID())
                .build();
    }


    public ProsecutionCase getProsecutionCase(final JsonEnvelope envelope, final UUID caseId) {
        LOGGER.info("calling progression prosecution case with case id : {}", caseId);

        final Envelope<JsonObject> queryEnvelope = envelopeFrom(metadataFrom(envelope.metadata())
                        .withName(PROGRESSION_QUERY_PROSECUTION_CASE)
                        .build(),
                createObjectBuilder().add(CASE_ID, caseId.toString()).build());

        final JsonObject response = requester.requestAsAdmin(queryEnvelope, JsonObject.class).payload();
        ProsecutionCase prosecutionCase = null;
        if (nonNull(response) && response.containsKey("prosecutionCase")) {
            prosecutionCase = jsonObjectToObjectConverter.convert(response.getJsonObject("prosecutionCase"), ProsecutionCase.class);
        }
        return prosecutionCase;
    }
}
