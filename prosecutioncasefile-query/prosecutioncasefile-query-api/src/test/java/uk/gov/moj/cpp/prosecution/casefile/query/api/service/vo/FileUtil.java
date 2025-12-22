package uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo;

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

import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.io.IOUtils;
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

    public static JsonObject readJson(final String payload) {
        try (final JsonReader reader = createReader(new StringReader(payload))) {
            return reader.readObject();
        }
    }

}
