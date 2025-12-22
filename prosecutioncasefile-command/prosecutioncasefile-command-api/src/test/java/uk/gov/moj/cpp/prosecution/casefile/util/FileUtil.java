package uk.gov.moj.cpp.prosecution.casefile.util;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.apache.commons.io.IOUtils;

public final class FileUtil {

    public static String resourceToString(final String path, final Object... placeholders) {
        try (final InputStream systemResourceAsStream = getSystemResourceAsStream(path)) {
            return format(IOUtils.toString(systemResourceAsStream), placeholders);
        } catch (final IOException e) {
            fail("Error consuming file from location " + path);
            throw new UncheckedIOException(e);
        }
    }
}
