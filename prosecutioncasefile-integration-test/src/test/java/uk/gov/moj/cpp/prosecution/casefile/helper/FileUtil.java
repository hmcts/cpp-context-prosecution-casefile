package uk.gov.moj.cpp.prosecution.casefile.helper;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static javax.json.Json.createReader;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.Map;

import javax.json.JsonObject;
import javax.json.JsonReader;

import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

    public static String resourceToString(final String path, final Object... placeholders) {
        try (final InputStream systemResourceAsStream = getSystemResourceAsStream(path)) {
            assertThat(systemResourceAsStream, is(notNullValue()));
            return format(IOUtils.toString(systemResourceAsStream), placeholders);
        } catch (final IOException e) {
            LOGGER.error("Error consuming file from location {}", path, e);
            fail("Error consuming file from location " + path);
            throw new UncheckedIOException(e);
        }
    }

    public static JsonObject readJsonResource(final String filePath, final Object... placeholders) {
        return readJson(resourceToString(filePath, placeholders));
    }

    public static JsonObject readJsonResource(final String filePath, final Map<String, Object> namedPlaceholders) {
        return readJson(new StrSubstitutor(namedPlaceholders).replace(resourceToString(filePath)));
    }

    public static JsonObject readJson(final String payload) {
        try (final JsonReader reader = createReader(new StringReader(payload))) {
            return reader.readObject();
        }
    }

    public static String getStringFromResource(final String path) {
        String request = null;
        try {
            request = Resources.toString(Resources.getResource(path), Charset.defaultCharset());
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        return request;
    }

}
