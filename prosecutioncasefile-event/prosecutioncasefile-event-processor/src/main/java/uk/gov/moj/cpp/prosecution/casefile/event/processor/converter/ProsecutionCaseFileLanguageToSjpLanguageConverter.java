package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Language;

public class ProsecutionCaseFileLanguageToSjpLanguageConverter implements Converter<Language, uk.gov.justice.json.schemas.domains.sjp.Language> {
    @Override
    public uk.gov.justice.json.schemas.domains.sjp.Language convert(final Language source) {
        switch (source) {
            case W:
                return uk.gov.justice.json.schemas.domains.sjp.Language.W;
            case E:
                return uk.gov.justice.json.schemas.domains.sjp.Language.E;
            default:
                return null;
        }
    }
}
