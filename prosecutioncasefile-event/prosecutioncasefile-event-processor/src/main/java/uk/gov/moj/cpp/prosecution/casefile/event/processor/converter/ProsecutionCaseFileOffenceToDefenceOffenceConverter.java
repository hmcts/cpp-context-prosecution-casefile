package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.stream.Collectors.toList;
import static uk.gov.justice.cps.prosecutioncasefile.Offence.offence;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;

import java.util.List;

public class ProsecutionCaseFileOffenceToDefenceOffenceConverter implements Converter<List<Offence>, List<uk.gov.justice.cps.prosecutioncasefile.Offence>> {


    @Override
    public List<uk.gov.justice.cps.prosecutioncasefile.Offence> convert(final List<Offence> source) {

        return source.stream()
                .map(offence ->
                        offence()
                                .withCjsCode(offence.getOffenceCode())
                                .withStartDate(offence.getOffenceCommittedDate().toString())
                                .withId(offence.getOffenceId().toString())
                                .build()
                )
                .collect(toList());
    }
}
