package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.CaseRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.UUID;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_HANDLER)
public class ResolveErrorsHandler extends BaseProsecutionCaseFileHandler {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Inject
    private Instance<DefendantRefDataEnricher> defendantRefDataEnrichers;

    @Inject
    private Instance<CaseRefDataEnricher> caseRefDataEnrichers;


    @Handles("prosecutioncasefile.command.handler.resolve-errors")
    public void resolveErrors(final JsonEnvelope envelope) throws EventStreamException {
        final JsonObject errorsJsonObject =  envelope.asJsonObject();
        final UUID caseId = fromString(errorsJsonObject.getString("caseId"));
        appendEventsToStream(caseId, envelope, caseFile -> caseFile.receiveErrorCorrections(errorsJsonObject, objectToJsonObjectConverter, jsonObjectToObjectConverter, newArrayList(caseRefDataEnrichers.iterator()), newArrayList(defendantRefDataEnrichers.iterator()), referenceDataQueryService));
    }
}
