package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Gender;

public class IntegerGenderToSjpGenderConverter implements Converter<Gender, uk.gov.justice.json.schemas.domains.sjp.Gender> {
    @Override
    public uk.gov.justice.json.schemas.domains.sjp.Gender convert(final Gender source) {
        switch(source) {
            case MALE:
                return uk.gov.justice.json.schemas.domains.sjp.Gender.MALE;
            case FEMALE:
                return uk.gov.justice.json.schemas.domains.sjp.Gender.FEMALE;
            case NOT_KNOWN:
                return uk.gov.justice.json.schemas.domains.sjp.Gender.NOT_SPECIFIED;
            case NOT_SPECIFIED:
                return uk.gov.justice.json.schemas.domains.sjp.Gender.NOT_SPECIFIED;
            default:
                throw new IllegalArgumentException("SJP doesn't support other gender values " + source.name());
        }
    }
}
