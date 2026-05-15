package uk.gov.moj.cpp.prosecution.casefile.command.handler.builder;


import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.prosecution.casefile.command.handler.util.DefaultTestData.CASE_DOCUMENT_ID;
import static uk.gov.moj.cpp.prosecution.casefile.command.handler.util.DefaultTestData.CASE_DOCUMENT_MATERIAL_ID;
import static uk.gov.moj.cpp.prosecution.casefile.command.handler.util.DefaultTestData.CASE_DOCUMENT_TYPE_IDPC;
import static uk.gov.moj.cpp.prosecution.casefile.command.handler.util.DefaultTestData.CASE_ID;
import static uk.gov.moj.cpp.prosecution.casefile.command.handler.util.DefaultTestData.DEFENDANT_ID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.json.JsonObjectBuilder;

public class AddDefendantIdpcCommandBuilder {

    private final UUID caseId = CASE_ID;
    private final UUID id = CASE_DOCUMENT_ID;
    private final UUID materialId = CASE_DOCUMENT_MATERIAL_ID;
    private final UUID defendantId = DEFENDANT_ID;
    private String documentType;

    private AddDefendantIdpcCommandBuilder() {
    }

    public static AddDefendantIdpcCommandBuilder anAddDefendantIdpcCommand() {
        return new AddDefendantIdpcCommandBuilder().withDocumentType(CASE_DOCUMENT_TYPE_IDPC);
    }

    public AddDefendantIdpcCommandBuilder withDocumentType(final String documentType) {
        this.documentType = documentType;
        return this;
    }

    public JsonEnvelope build() {
        if (caseId == null || id == null || materialId == null || defendantId == null) {
            throw new RuntimeException("CaseId, id, materialId and defendantId required by the AddDefendantIdpcMaterial command.");
        }

        final JsonObjectBuilder victim = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("id", id.toString())
                .add("materialId", materialId.toString())
                .add("defendantId", defendantId.toString());

        if (this.documentType != null) {
            victim.add("documentType", documentType);
        }

        Metadata metadata = metadataBuilder()
                .withName("prosecutioncasefile.command.add-defendant-idpc")
                .withId(UUID.randomUUID()).build();
        return JsonEnvelope.envelopeFrom(metadata, victim.build());
    }
}
