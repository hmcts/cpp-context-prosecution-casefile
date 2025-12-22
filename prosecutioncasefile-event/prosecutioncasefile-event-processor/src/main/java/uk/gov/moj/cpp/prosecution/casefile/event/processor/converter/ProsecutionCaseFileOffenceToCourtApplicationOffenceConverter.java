package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;


import uk.gov.moj.cpp.prosecution.casefile.domain.ParamsVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;

import java.util.List;

import javax.inject.Inject;

public class ProsecutionCaseFileOffenceToCourtApplicationOffenceConverter implements ParameterisedConverter<List<Offence>, List<uk.gov.justice.core.courts.Offence>, ParamsVO> {

    @Inject
    private ProsecutionCaseFileOffenceToCourtsOffenceConverter prosecutionCaseFileOffenceToCourtsOffenceConverter;

    @Override
    public List<uk.gov.justice.core.courts.Offence> convert(final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence> source, final ParamsVO param) {
        return prosecutionCaseFileOffenceToCourtsOffenceConverter.convert(source, param);
    }
}
