package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

public interface ParameterisedConverter<S, T, P> {

    T convert(final S source, final P param);

}
