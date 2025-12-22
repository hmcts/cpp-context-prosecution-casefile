package uk.gov.moj.cpp.prosecution.casefile.refdata;

import java.util.Collections;
import java.util.List;

public interface RefDataEnricher<T> {

    default void enrich(final T prosecutionWithReferenceData) {
        enrich(Collections.singletonList(prosecutionWithReferenceData));
    }

    void enrich(final List<T> prosecutionWithReferenceDataList);
}
