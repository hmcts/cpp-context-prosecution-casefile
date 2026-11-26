package uk.gov.moj.cpp.prosecution.casefile.event.processor.listener.external.sjp;


import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(EVENT_PROCESSOR)
public class CaseDocumentUploadedListener {

    @Inject
    private Sender sender;

    @Handles("public.sjp.case-document-uploaded")
    public void handleSjpCaseDocumentUploaded(final JsonEnvelope event) {
        this.sender.send(envelop(event.payloadAsJsonObject())
                .withName("prosecutioncasefile.command.record-upload-case-document")
                .withMetadataFrom(event));
    }

}