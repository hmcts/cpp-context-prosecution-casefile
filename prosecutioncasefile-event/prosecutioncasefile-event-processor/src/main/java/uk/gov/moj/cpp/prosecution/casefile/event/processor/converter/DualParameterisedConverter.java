package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

public interface DualParameterisedConverter<S, T, P, M> {

    T convert(final S source, final P param1, final M param2);

}
