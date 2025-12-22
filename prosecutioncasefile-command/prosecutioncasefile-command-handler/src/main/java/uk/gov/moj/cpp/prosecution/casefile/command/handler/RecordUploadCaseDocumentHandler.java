package uk.gov.moj.cpp.prosecution.casefile.command.handler;


import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.RecordUploadCaseDocument;

import java.util.UUID;

@ServiceComponent(COMMAND_HANDLER)
public class RecordUploadCaseDocumentHandler extends BaseProsecutionCaseFileHandler {

    @Handles("prosecutioncasefile.command.record-upload-case-document")
    public void recordUploadCaseDocument(final Envelope<RecordUploadCaseDocument> command) throws EventStreamException {

        final RecordUploadCaseDocument recordUploadCaseDocument = command.payload();

        final UUID caseId = recordUploadCaseDocument.getCaseId();
        final UUID documentId = recordUploadCaseDocument.getDocumentId();

        appendEventsToStream(caseId, command, prosecutionCaseFile ->
                prosecutionCaseFile.recordUploadCaseDocument(caseId, documentId));

    }
}

