package uk.gov.moj.cpp.prosecution.casefile.command.api;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(COMMAND_API)
public class AddMaterialApi {

    public static final String MATERIAL = "material";
    public static final String MATERIALS = "materials";
    public static final String FILE_TYPE = "fileType";
    @Inject
    private Sender sender;
    @Inject
    private Enveloper enveloper;
    @Inject
    private FileService fileService;

    @Handles("prosecutioncasefile.add-material")
    public void addMaterial(final JsonEnvelope addMaterialCommand) throws FileServiceException {

        final JsonObject addMaterialPayload = addMaterialCommand.payloadAsJsonObject();

        final JsonObject material = addMaterialPayload.getJsonObject(MATERIAL);

        final JsonObjectBuilder enrichedMaterialBuilder = createObjectBuilder(material);
        getFileType(material).ifPresent(fileType -> enrichedMaterialBuilder.add(FILE_TYPE, fileType));

        final JsonObject enrichedAddMaterialCommandPayload = createObjectBuilder(addMaterialPayload).add(MATERIAL, enrichedMaterialBuilder).build();

        final Metadata metadata = metadataFrom(addMaterialCommand.metadata())
                .withName("prosecutioncasefile.command.add-material")
                .build();
        sender.send(envelopeFrom(metadata, enrichedAddMaterialCommandPayload));

    }

    @Handles("prosecutioncasefile.add-materials")
    public void addMaterials(final JsonEnvelope addMaterialCommand) throws FileServiceException {
        final JsonObject addMaterialPayload = addMaterialCommand.payloadAsJsonObject();

        final JsonArray materials = addMaterialPayload.getJsonArray(MATERIALS);
        final JsonArrayBuilder enrichedMaterialsBuilder = Json.createArrayBuilder();
        for (final JsonObject material : materials.getValuesAs(JsonObject.class)) {
            final JsonObjectBuilder enrichedMaterialBuilder = createObjectBuilder(material);
            getFileType(material).ifPresent(fileType -> enrichedMaterialBuilder.add(FILE_TYPE, fileType));
            enrichedMaterialsBuilder.add(enrichedMaterialBuilder);
        }

        final JsonObject enrichedAddMaterialsCommandPayload = createObjectBuilder(addMaterialPayload).add(MATERIALS, enrichedMaterialsBuilder).build();
        final Metadata metadata = metadataFrom(addMaterialCommand.metadata())
                .withName("prosecutioncasefile.command.add-materials")
                .build();
        sender.send(envelopeFrom(metadata, enrichedAddMaterialsCommandPayload));
    }

    @Handles("prosecutioncasefile.add-material-v2")
    public void addMaterialV2(final JsonEnvelope addMaterialCommand) {
        final JsonObject addMaterialPayload = addMaterialCommand.payloadAsJsonObject();
         final Metadata metadata = metadataFrom(addMaterialCommand.metadata())
                .withName("prosecutioncasefile.command.add-material-v2")
                .build();
        sender.send(envelopeFrom(metadata, addMaterialPayload));
    }

    @Handles("prosecutioncasefile.add-application-material-v2")
    public void addApplicationMaterialV2(final JsonEnvelope addMaterialCommand) {
        final JsonObject addMaterialPayload = addMaterialCommand.payloadAsJsonObject();
        final Metadata metadata = metadataFrom(addMaterialCommand.metadata())
                .withName("prosecutioncasefile.command.add-application-material-v2")
                .build();
        sender.send(envelopeFrom(metadata, addMaterialPayload));
    }

    @Handles("prosecutioncasefile.add-idpc-material")
    public void addIdpcMaterial(final JsonEnvelope addMaterialCommand) {
        final JsonObject addMaterialPayload = addMaterialCommand.payloadAsJsonObject();
        final Metadata metadata = metadataFrom(addMaterialCommand.metadata())
                .withName("prosecutioncasefile.handler.add-idpc-material")
                .build();
        sender.send(envelopeFrom(metadata, addMaterialPayload));
    }

    @Handles("prosecutioncasefile.add-cps-material")
    public void addCpsMaterial(final JsonEnvelope addMaterialCommand) throws FileServiceException{
        final JsonObject addMaterialPayload = addMaterialCommand.payloadAsJsonObject();

        final JsonObject material = addMaterialPayload.getJsonObject(MATERIAL);

        final JsonObjectBuilder enrichedMaterialBuilder = createObjectBuilder(material);
        getFileType(material).ifPresent(fileType -> enrichedMaterialBuilder.add(FILE_TYPE, fileType));

        final JsonObject enrichedAddMaterialCommandPayload = createObjectBuilder(addMaterialPayload).add(MATERIAL, enrichedMaterialBuilder).build();
        final Metadata metadata = metadataFrom(addMaterialCommand.metadata())
                .withName("prosecutioncasefile.command.handler.add-cps-material")
                .build();
        sender.send(envelopeFrom(metadata, enrichedAddMaterialCommandPayload));
    }

    private Optional<String> getFileType(final JsonObject material) throws FileServiceException {
        return fileService.retrieveMetadata(UUID.fromString(material.getString("fileStoreId")))
                .flatMap(metadata -> ofNullable(metadata.getString("mediaType", null)));
    }

}
