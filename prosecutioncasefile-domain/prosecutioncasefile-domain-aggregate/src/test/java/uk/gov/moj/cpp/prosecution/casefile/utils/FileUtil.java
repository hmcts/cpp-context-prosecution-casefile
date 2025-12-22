package uk.gov.moj.cpp.prosecution.casefile.utils;

import static com.google.common.io.Resources.getResource;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;


public class FileUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

    private FileUtil() {
    }

    public static <T> T readJson(final String jsonPath, final Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(getResource(jsonPath), clazz);
        } catch (IOException e) {
            throw new IllegalStateException("Resource " + jsonPath + " inaccessible: " + e.getMessage());
        }
    }

}
