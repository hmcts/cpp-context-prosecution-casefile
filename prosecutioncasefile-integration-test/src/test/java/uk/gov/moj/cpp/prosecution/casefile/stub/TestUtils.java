package uk.gov.moj.cpp.prosecution.casefile.stub;

import static java.nio.charset.Charset.defaultCharset;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class);

    public static String readFile(String filePath) {
        String request = null;
        try (final InputStream inputStream = TestUtils.class.getClassLoader().getResourceAsStream(filePath)) {
            assertThat(inputStream, notNullValue());
            request = IOUtils.toString(inputStream, defaultCharset());
        } catch (Exception e) {
            LOGGER.error("Error consuming file from location {}", filePath);
            fail("Error consuming file from location " + filePath);
        }
        return request;
    }

    public static String readFile(final String path, final Object... placeholders) {
        return String.format(readFile(path), placeholders);
    }

}
