package uk.gov.moj.cpp.prosecution.casefile.command.api;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AddMaterialApiTest {

    @Mock
    private Sender sender;

    @Mock
    private FileService fileService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private AddMaterialApi addMaterial;

    @Mock
    private Function<Object, JsonEnvelope> function;

    @Mock
    private JsonEnvelope commandEnvelope;

    @Captor
    private ArgumentCaptor<JsonEnvelope> captor;

    private final UUID fileStoreId = randomUUID();
    private final String fileType = randomAlphanumeric(10);
    private final JsonEnvelope addMaterialCommand = buildAddMaterialCommand(fileStoreId);
    private final JsonEnvelope addCpsMaterialCommand = buildAddCpsMaterialCommand(fileStoreId);
    private static final String IS_UNBUNDLED_DOCUMENT = "isUnbundledDocument";

    @Test
    public void shouldHandleProsecutionCaseFileCommandInitiateSjpProsecution() {
        assertThat(AddMaterialApi.class, isHandlerClass(COMMAND_API).with(method("addMaterial").thatHandles("prosecutioncasefile.add-material")));
    }

    @Test
    public void shouldHandleAddMaterialIdpcApi() {
        assertThat(AddMaterialApi.class, isHandlerClass(COMMAND_API).with(method("addIdpcMaterial").thatHandles("prosecutioncasefile.add-idpc-material")));
    }


    @Test
    public void testAddMaterialIdpcApi() throws FileServiceException {
        final JsonEnvelope command = mock(JsonEnvelope.class);
        final Metadata metadata = metadataBuilder()
                .withName("prosecutioncasefile.handler.add-idpc-material")
                .withId(randomUUID())
                .build();
        when(command.metadata()).thenReturn(metadata);
        addMaterial.addIdpcMaterial(command);
        verify(sender).send(captor.capture());

        final JsonEnvelope jsonEnvelope = captor.getValue();
        assertThat(jsonEnvelope.metadata().name(), Matchers.is("prosecutioncasefile.handler.add-idpc-material"));
    }

    @Test
    public void testAddMaterialV2Api() throws FileServiceException {
        final JsonEnvelope command = mock(JsonEnvelope.class);
        final Metadata metadata = metadataBuilder()
                .withName("prosecutioncasefile.add-material-v2")
                .withId(randomUUID())
                .build();
        when(command.metadata()).thenReturn(metadata);
        addMaterial.addMaterialV2(command);
        verify(sender).send(captor.capture());

        final JsonEnvelope jsonEnvelope = captor.getValue();
        assertThat(jsonEnvelope.metadata().name(), Matchers.is("prosecutioncasefile.command.add-material-v2"));
    }

    @Test
    public void testAddApplicationMaterialV2() {
        final JsonEnvelope command = mock(JsonEnvelope.class);
        final Metadata metadata = metadataBuilder()
                .withName("prosecutioncasefile.add-material-v2")
                .withId(randomUUID())
                .build();
        when(command.metadata()).thenReturn(metadata);
        addMaterial.addApplicationMaterialV2(command);
        verify(sender).send(captor.capture());

        final JsonEnvelope jsonEnvelope = captor.getValue();
        assertThat(jsonEnvelope.metadata().name(), Matchers.is("prosecutioncasefile.command.add-application-material-v2"));
    }

    @Test
    public void shouldAddMaterialWithFileType() throws Exception {
        givenFileMetadataWithFileTypeExists();
        whenAddMaterialCommandReceived();
        thenAddMaterialCommandSentWithFileType();
    }

    @Test
    public void shouldAddMaterialsWithFileType() throws Exception {
        givenFileMetadataWithFileTypeExists();
        final JsonEnvelope addMaterialsCommand = buildAddMaterialsCommand(fileStoreId);
        whenAddMaterialsCommandReceived(addMaterialsCommand);
        thenAddMaterialsCommandSentWithFileType(addMaterialsCommand, Optional.of(fileType));
    }

    @Test
    public void shouldAddMaterialsWithoutFileType() throws Exception {
        givenFileMetadataWithoutFileTypeExists();
        final JsonEnvelope addMaterialsCommand = buildAddMaterialsCommand(fileStoreId);
        whenAddMaterialsCommandReceived(addMaterialsCommand);
        thenAddMaterialsCommandSentWithFileType(addMaterialsCommand, Optional.empty());
    }

    @Test
    public void shouldAddCpsMaterialWithFileType() throws Exception {
        givenFileMetadataWithFileTypeExists();
        whenAddCpsMaterialCommandReceived();
        thenAddCpsMaterialCommandSentWithFileType();
    }

    @Test
    public void shouldAddMaterialWithoutFileType() throws Exception {
        givenFileMetadataWithoutFileTypeExists();
        whenAddMaterialCommandReceived();
        thenAddMaterialCommandSentWithoutFileType();
    }

    @Test
    public void shouldAddMaterialWithoutFileTypeWhenMetadataDoesNotExists() throws Exception {
        givenFileMetadataDoesNotExist();
        whenAddMaterialCommandReceived();
        thenAddMaterialCommandSentWithoutFileType();
    }

    private void givenFileMetadataWithFileTypeExists() throws Exception {
        final JsonObject fileMetadata = createObjectBuilder()
                .add("fileName", randomAlphanumeric(20))
                .add("mediaType", fileType)
                .build();
        when(fileService.retrieveMetadata(fileStoreId)).thenReturn(of(fileMetadata));
    }

    private void givenFileMetadataWithoutFileTypeExists() throws Exception {
        final JsonObject fileMetadata = createObjectBuilder()
                .add("fileName", randomAlphanumeric(20))
                .build();
        when(fileService.retrieveMetadata(fileStoreId)).thenReturn(of(fileMetadata));
    }

    private void givenFileMetadataDoesNotExist() throws Exception {
        when(fileService.retrieveMetadata(fileStoreId)).thenReturn(empty());
    }

    private void whenAddMaterialCommandReceived() throws Exception {
        addMaterial.addMaterial(addMaterialCommand);
    }

    private void whenAddMaterialsCommandReceived(final JsonEnvelope addMaterialsCommand) throws Exception {
        addMaterial.addMaterials(addMaterialsCommand);
    }

    private void whenAddCpsMaterialCommandReceived() throws Exception {
        addMaterial.addCpsMaterial(addCpsMaterialCommand);
    }

    private void thenAddMaterialCommandSentWithFileType() {
        verifyAddMaterialCommandSend(addMaterialCommand, withJsonPath("$.material.fileType", is(fileType)));
    }

    private void thenAddMaterialsCommandSentWithFileType(final JsonEnvelope addMaterialsCommand, final Optional<String> fileType) {
        verifyAddMaterialsCommandSend(addMaterialsCommand, fileType);
    }

    private void thenAddCpsMaterialCommandSentWithFileType() {
        verifyCpsCommandSend(addCpsMaterialCommand, withJsonPath("$.material.fileType", is(fileType)));
    }

    private void thenAddMaterialCommandSentWithoutFileType() {
        verifyAddMaterialCommandSend(addMaterialCommand, withoutJsonPath("$.material.fileType"));
    }

    private void verifyAddMaterialCommandSend(final JsonEnvelope addMaterialCommand, final Matcher fileTypeMatcher) {
        verify(sender).send(argThat(
                jsonEnvelope(
                        metadata().withName("prosecutioncasefile.command.add-material"),
                        payload().isJson(allOf(
                                withJsonPath("$.prosecutingAuthority", is(addMaterialCommand.payloadAsJsonObject().getString("prosecutingAuthority"))),
                                withJsonPath("$.prosecutorDefendantId", is(addMaterialCommand.payloadAsJsonObject().getString("prosecutorDefendantId"))),
                                withJsonPath("$.material.fileStoreId", is(addMaterialCommand.payloadAsJsonObject().getJsonObject("material").getString("fileStoreId"))),
                                withJsonPath("$.material.isUnbundledDocument", is(addMaterialCommand.payloadAsJsonObject().getJsonObject("material").getBoolean("isUnbundledDocument"))),
                                withJsonPath("$.material.documentType", is(addMaterialCommand.payloadAsJsonObject().getJsonObject("material").getString("documentType"))),
                                fileTypeMatcher
                        )))));
    }

    private void verifyAddMaterialsCommandSend(final JsonEnvelope addMaterialsCommand, final Optional<String> fileType) {
        List<Matcher<? super ReadContext>> materialsMatchers = new ArrayList<>();
        int idx = 0;
        for (JsonObject material : addMaterialsCommand.payloadAsJsonObject().getJsonArray("materials").getValuesAs(JsonObject.class)) {
            materialsMatchers.add(withJsonPath(format("$.materials[%d].fileStoreId", idx), is(material.getString("fileStoreId"))));
            materialsMatchers.add(withJsonPath(format("$.materials[%d].documentType", idx), is(material.getString("documentType"))));
            materialsMatchers.add(withJsonPath(format("$.materials[%d].documentType", idx), is(material.getString("documentType"))));
            if (fileType.isPresent()) {
                materialsMatchers.add(withJsonPath(format("$.materials[%d].fileType", idx), is(fileType.get())));
            } else {
                materialsMatchers.add(withoutJsonPath(format("$.materials[%d].fileType", idx)));
            }
            idx++;
        }
        materialsMatchers.add(withJsonPath("$.materials.length()", is(idx)));

        verify(sender).send(argThat(
                jsonEnvelope(
                        metadata().withName("prosecutioncasefile.command.add-materials"),
                        payload().isJson(allOf(
                                withJsonPath("$.prosecutingAuthority", is(addMaterialsCommand.payloadAsJsonObject().getString("prosecutingAuthority"))),
                                withJsonPath("$.prosecutorDefendantId", is(addMaterialsCommand.payloadAsJsonObject().getString("prosecutorDefendantId"))),
                                allOf(materialsMatchers)
                        )))));
    }

    private void verifyCpsCommandSend(final JsonEnvelope addCpsMaterialCommand, final Matcher fileTypeMatcher) {
        verify(sender).send(argThat(
                jsonEnvelope(
                        metadata().withName("prosecutioncasefile.command.handler.add-cps-material"),
                        payload().isJson(allOf(
                                withJsonPath("$.prosecutingAuthority", is(addCpsMaterialCommand.payloadAsJsonObject().getString("prosecutingAuthority"))),
                                withJsonPath("$.prosecutorDefendantId", is(addCpsMaterialCommand.payloadAsJsonObject().getString("prosecutorDefendantId"))),
                                withJsonPath("$.material.fileStoreId", is(addCpsMaterialCommand.payloadAsJsonObject().getJsonObject("material").getString("fileStoreId"))),
                                withJsonPath("$.material.documentType", is(addCpsMaterialCommand.payloadAsJsonObject().getJsonObject("material").getString("documentType"))),
                                withJsonPath("$.receivedDateTime", is(addCpsMaterialCommand.payloadAsJsonObject().getString("receivedDateTime"))),
                                fileTypeMatcher
                        )))));
    }

    private static JsonEnvelope buildAddMaterialCommand(final UUID fileStoreId) {
        return envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.add-material"),
                createObjectBuilder()
                        .add("caseId", UUID.randomUUID().toString())
                        .add("prosecutingAuthority", "00TFL")
                        .add("prosecutorDefendantId", randomAlphanumeric(36))
                        .add("material", createObjectBuilder()
                                .add("fileStoreId", fileStoreId.toString())
                                .add("documentType", "SJPN")
                                .add(IS_UNBUNDLED_DOCUMENT, Boolean.FALSE)
                        )
                        .add("isCpsCase", Boolean.FALSE)
        );
    }

    private static JsonEnvelope buildAddMaterialsCommand(final UUID fileStoreId) {
        final JsonArrayBuilder materials = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("fileStoreId", fileStoreId.toString())
                        .add("documentType", "SJPN")
                        .add(IS_UNBUNDLED_DOCUMENT, Boolean.FALSE))
                .add(createObjectBuilder()
                        .add("fileStoreId", fileStoreId.toString())
                        .add("documentType", "PLEA")
                        .add(IS_UNBUNDLED_DOCUMENT, Boolean.FALSE)
                );
        return envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.add-material"),
                createObjectBuilder()
                        .add("caseId", UUID.randomUUID().toString())
                        .add("prosecutingAuthority", "00TFL")
                        .add("prosecutorDefendantId", randomAlphanumeric(36))
                        .add("materials", materials)
                        .add("isCpsCase", Boolean.FALSE)
        );
    }

    private static JsonEnvelope buildAddCpsMaterialCommand(final UUID fileStoreId) {
        return envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.add-cps-material"),
                createObjectBuilder()
                        .add("caseId", UUID.randomUUID().toString())
                        .add("prosecutingAuthority", "00TFL")
                        .add("prosecutorDefendantId", randomAlphanumeric(36))
                        .add("material", createObjectBuilder()
                                .add("fileStoreId", fileStoreId.toString())
                                .add("documentType", "SJPN")
                                .add(IS_UNBUNDLED_DOCUMENT, Boolean.TRUE)
                        )
                        .add("receivedDateTime", "2020-02-04T05:27:17.210Z")
        );
    }
}
