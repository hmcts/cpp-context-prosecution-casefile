package uk.gov.moj.cpp.prosecution.casefile.event.listener;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.time.ZonedDateTime.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static uk.gov.justice.services.test.utils.core.matchers.JsonSchemaValidationMatcher.isValidJsonEnvelopeForSchema;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.schema.catalog.CatalogObjectFactory;
import uk.gov.justice.schema.catalog.RawCatalog;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PCF-05 parity probe (J17 golden vs J25). Jakarta stand-in for MI's
 * {@code ProsecutionCaseFileSchemaContractValidationTest} (mireportdata-prosecutioncasefile-event-listener
 * tests jar, compiled against javax.json and excluded on J25).
 *
 * <p>Same check: each MI sample payload, in an envelope named after the event, must validate against this
 * context's event schema on the test classpath ({@code json/schema}, {@code raml/json/schema},
 * {@code yaml/json/schema}). The J17 copy of this class differs only in the {@code javax.json} imports.
 *
 * <p>MI payloads come from {@code -Dmi.contract.payload.dir}, else the MI repo checkout next to this
 * workspace, else the classpath {@code /schema.validation/} (present on J17 via the unpack). With none of
 * them the test is skipped, not passed.
 */
public class MiSchemaContractParityTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MiSchemaContractParityTest.class);
    private static final String PAYLOAD_DIR_PROPERTY = "mi.contract.payload.dir";
    private static final Path MI_REPO_PAYLOAD_DIR = Paths.get("../../../../../cpp-context-mi-reportdata/mireportdata-event/"
            + "mireportdata-prosecutioncasefile-event-listener/src/test/resources/schema.validation");
    private static final String[] SCHEMA_LOCATIONS = {"json/schema/%s.json", "raml/json/schema/%s.json", "yaml/json/schema/%s.json"};
    private static final String[] MI_EVENTS = {
            "prosecutioncasefile.events.cc-case-received",
            "prosecutioncasefile.events.cc-case-received-with-warnings",
            "prosecutioncasefile.events.sjp-prosecution-received",
            "prosecutioncasefile.events.sjp-prosecution-received-with-warnings",
            "prosecutioncasefile.events.case-validation-failed",
            "prosecutioncasefile.events.sjp-validation-failed",
            "prosecutioncasefile.events.defendant-validation-failed",
            "prosecutioncasefile.events.case-filtered"};
    private static final Pattern REF = Pattern.compile("\"\\$ref\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ID = Pattern.compile("\"\\$?id\"\\s*:\\s*\"(https?://[^\"]+)\"");

    @Test
    public void miSamplePayloadShouldValidateAgainstPcfSchema_ProsecutionCaseReceived() throws IOException {
        shouldValidateMiSamplePayload("prosecutioncasefile.events.cc-case-received");
    }

    @Test
    public void miSamplePayloadShouldValidateAgainstPcfSchema_ProsecutionCaseReceivedWithWarnings() throws IOException {
        shouldValidateMiSamplePayload("prosecutioncasefile.events.cc-case-received-with-warnings");
    }

    @Test
    public void miSamplePayloadShouldValidateAgainstPcfSchema_SjpProsecutionReceived() throws IOException {
        shouldValidateMiSamplePayload("prosecutioncasefile.events.sjp-prosecution-received");
    }

    @Test
    public void miSamplePayloadShouldValidateAgainstPcfSchema_SjpProsecutionReceivedWithWarnings() throws IOException {
        shouldValidateMiSamplePayload("prosecutioncasefile.events.sjp-prosecution-received-with-warnings");
    }

    @Test
    public void miSamplePayloadShouldValidateAgainstPcfSchema_CaseValidationFailed() throws IOException {
        shouldValidateMiSamplePayload("prosecutioncasefile.events.case-validation-failed");
    }

    @Test
    public void miSamplePayloadShouldValidateAgainstPcfSchema_SjpValidationFailed() throws IOException {
        shouldValidateMiSamplePayload("prosecutioncasefile.events.sjp-validation-failed");
    }

    @Test
    public void miSamplePayloadShouldValidateAgainstPcfSchema_DefendantValidationFailed() throws IOException {
        shouldValidateMiSamplePayload("prosecutioncasefile.events.defendant-validation-failed");
    }

    @Test
    public void miSamplePayloadShouldValidateAgainstPcfSchema_CaseFiltered() throws IOException {
        shouldValidateMiSamplePayload("prosecutioncasefile.events.case-filtered");
    }

    private void shouldValidateMiSamplePayload(final String eventName) throws IOException {
        final JsonEnvelope envelope = envelope(eventName, miPayload(eventName));

        assertThat("MI schema contract validation has failed for event " + eventName, envelope, isValidJsonEnvelopeForSchema());
    }

    @Test
    public void schemaCheckShouldRejectPayloadMissingRequiredField() throws IOException {
        final JsonObject withoutRequired = copyExcept(miPayload("prosecutioncasefile.events.cc-case-received"), "prosecutionWithReferenceData").build();

        assertThat(envelope("prosecutioncasefile.events.cc-case-received", withoutRequired), not(isValidJsonEnvelopeForSchema()));
    }

    @Test
    public void schemaCheckShouldResolveExternalUuidDefinition() throws IOException {
        // caseId is "$ref": http://justice.gov.uk/domain/core/common/definitions.json#/definitions/uuid
        final JsonObject badCaseId = copyExcept(miPayload("prosecutioncasefile.events.case-filtered"), "caseId")
                .add("caseId", "not-a-uuid")
                .build();

        assertThat(envelope("prosecutioncasefile.events.case-filtered", badCaseId), not(isValidJsonEnvelopeForSchema()));
    }

    /**
     * Diagnostic: writes the resolved schema closure of every MI event (root schema by classpath location,
     * then every absolute $ref by catalog id) to target/mi-contract-closure and prints a SHA-256 per file,
     * so the J17 and J25 closures can be compared with diff -r.
     */
    @Test
    public void dumpResolvedSchemaClosureForCrossVersionDiff() throws Exception {
        final RawCatalog rawCatalog = new CatalogObjectFactory().rawCatalog();
        final Path out = Paths.get("target", "mi-contract-closure");
        Files.createDirectories(out);
        final Map<String, String> closure = new TreeMap<>();
        final Deque<String[]> todo = new ArrayDeque<>();

        for (final String eventName : MI_EVENTS) {
            final String[] root = rootSchema(eventName);
            closure.put("root:" + root[0], root[1]);
            todo.push(new String[]{idOf(root[1]), root[1]});
        }

        final List<String> unresolved = new ArrayList<>();
        while (!todo.isEmpty()) {
            final String[] current = todo.pop();
            final Matcher matcher = REF.matcher(current[1]);
            while (matcher.find()) {
                final String ref = matcher.group(1);
                if (ref.startsWith("#")) {
                    continue;
                }
                final String target = stripFragment(current[0] == null ? ref : URI.create(current[0]).resolve(ref).toString());
                if (closure.containsKey(target)) {
                    continue;
                }
                final Optional<String> raw = rawCatalog.getRawJsonSchema(target);
                if (raw.isPresent()) {
                    closure.put(target, raw.get());
                    todo.push(new String[]{target, raw.get()});
                } else {
                    closure.put(target, "<UNRESOLVED>");
                    unresolved.add(target);
                }
            }
        }

        final StringBuilder index = new StringBuilder();
        for (final Map.Entry<String, String> entry : closure.entrySet()) {
            final String fileName = entry.getKey().replaceAll("[^A-Za-z0-9._-]", "_") + ".json";
            Files.write(out.resolve(fileName), entry.getValue().getBytes(UTF_8));
            index.append(sha256(entry.getValue())).append("  ").append(entry.getKey()).append('\n');
        }
        Files.write(out.resolve("_index.txt"), index.toString().getBytes(UTF_8));
        LOGGER.info("MI contract schema closure ({} schemas, unresolved={}):\n{}", closure.size(), unresolved, index);

        assertFalse(closure.isEmpty());
    }

    private static JsonEnvelope envelope(final String eventName, final JsonObject payload) {
        return JsonEnvelope.envelopeFrom(metadataWithRandomUUID(eventName).createdAt(now()), payload);
    }

    private static JsonObject miPayload(final String eventName) throws IOException {
        final String fileName = eventName + ".json";
        final String json;
        final String source;
        final String configured = System.getProperty(PAYLOAD_DIR_PROPERTY);
        if (configured != null && Files.isRegularFile(Paths.get(configured, fileName))) {
            source = Paths.get(configured, fileName).toAbsolutePath().normalize().toString();
            json = new String(Files.readAllBytes(Paths.get(configured, fileName)), UTF_8);
        } else if (Files.isRegularFile(MI_REPO_PAYLOAD_DIR.resolve(fileName))) {
            source = MI_REPO_PAYLOAD_DIR.resolve(fileName).toAbsolutePath().normalize().toString();
            json = new String(Files.readAllBytes(MI_REPO_PAYLOAD_DIR.resolve(fileName)), UTF_8);
        } else {
            final URL resource = MiSchemaContractParityTest.class.getResource("/schema.validation/" + fileName);
            Assumptions.assumeTrue(resource != null, "No MI payload for " + eventName + " (no -D" + PAYLOAD_DIR_PROPERTY
                    + ", no MI repo checkout, no unpacked MI tests jar)");
            source = resource.toString();
            try (InputStream in = resource.openStream()) {
                json = new String(in.readAllBytes(), UTF_8);
            }
        }
        LOGGER.info("MI payload {} <- {} sha256={}", eventName, source, sha256(json));
        final Path sources = Paths.get("target", "mi-contract-closure", "_payload-sources.txt");
        Files.createDirectories(sources.getParent());
        Files.write(sources, (sha256(json) + "  " + eventName + "  " + source + "\n").getBytes(UTF_8),
                CREATE, APPEND);
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }

    private static JsonObjectBuilder copyExcept(final JsonObject source, final String key) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        source.forEach((name, value) -> {
            if (!name.equals(key)) {
                builder.add(name, value);
            }
        });
        return builder;
    }

    private static String[] rootSchema(final String eventName) throws IOException {
        for (final String location : SCHEMA_LOCATIONS) {
            final String path = String.format(location, eventName);
            final URL url = MiSchemaContractParityTest.class.getClassLoader().getResource(path);
            if (url != null) {
                try (InputStream in = url.openStream()) {
                    return new String[]{path, new String(in.readAllBytes(), UTF_8)};
                }
            }
        }
        throw new IllegalStateException("No schema on classpath for " + eventName);
    }

    private static String idOf(final String rawSchema) {
        final Matcher matcher = ID.matcher(rawSchema);
        return matcher.find() ? stripFragment(matcher.group(1)) : null;
    }

    private static String stripFragment(final String uri) {
        final int hash = uri.indexOf('#');
        return hash < 0 ? uri : uri.substring(0, hash);
    }

    private static String sha256(final String text) {
        try {
            final StringBuilder hex = new StringBuilder();
            for (final byte b : MessageDigest.getInstance("SHA-256").digest(text.getBytes(UTF_8))) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
