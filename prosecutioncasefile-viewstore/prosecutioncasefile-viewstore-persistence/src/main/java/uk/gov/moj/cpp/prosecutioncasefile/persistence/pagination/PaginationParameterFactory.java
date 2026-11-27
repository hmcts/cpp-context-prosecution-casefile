package uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.commons.lang3.EnumUtils;

public class PaginationParameterFactory {
    private static final String EMPTY = "";

    private PaginationParameterFactory() {
    }

    public static PaginationParameter newPaginationParameter(final JsonObject requestJsonObject) {
        final int pageSize = requestJsonObject.getInt(PaginationConstant.PAGE_SIZE, PaginationConstant.DEFAULT_PAGE_SIZE);
        final int pageNumber = requestJsonObject.getInt(PaginationConstant.PAGE_NUMBER, PaginationConstant.FIRST_PAGE_NUMBER);
        final SortOrder sortOrder = getEnum(requestJsonObject, PaginationConstant.SORT_ORDER, SortOrder.class).orElse(SortOrder.DESC);
        final OrderByField sortField = getEnum(requestJsonObject, PaginationConstant.SORT_FIELD, OrderByField.class).orElse(OrderByField.HEARING_DATE);
        return new PaginationParameter(pageSize, pageNumber, sortField, sortOrder);
    }

    private static boolean valueExists(final JsonObject jsonObject, final String key) {
        return jsonObject.containsKey(key) && !Objects.equals(jsonObject.get(key), JsonValue.NULL);
    }

    public static String getString(final JsonObject jsonObject, final String... keys) {
        return Arrays.stream(keys)
                .filter(key -> valueExists(jsonObject, key))
                .findFirst()
                .map(jsonObject::getString)
                .orElse(EMPTY);
    }

    public static <E extends Enum<E>> Optional<E> getEnum(final JsonObject jsonObject, final String key, Class<E> clazz) {
        final String enumAsString = getString(jsonObject, key);
        return Optional.ofNullable(getCaseInsensitiveEnum(enumAsString, clazz));
    }

    private static <E extends Enum<E>> E getCaseInsensitiveEnum(final String enumAsString, final Class<E> clazz) {
        return EnumUtils.getEnum(clazz, trimToEmpty(enumAsString).toUpperCase());
    }
}
