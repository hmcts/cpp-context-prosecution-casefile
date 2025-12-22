package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper.buildOffences;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseFileOffenceToDefenceOffenceConverterTest {

    @InjectMocks
    ProsecutionCaseFileOffenceToDefenceOffenceConverter prosecutionCaseFileOffenceToDefenceOffenceConverter;


    @Test
    public void convert() {

        final List<Offence> offences = buildOffences();
        final List<uk.gov.justice.cps.prosecutioncasefile.Offence> convertedOffences = prosecutionCaseFileOffenceToDefenceOffenceConverter.convert(offences);

        assertThat(convertedOffences.size(), equalTo(offences.size()));

        final uk.gov.justice.cps.prosecutioncasefile.Offence convertedOffence = convertedOffences.get(0);
        final Offence offence = offences.get(0);

        assertThat(convertedOffence.getCjsCode(), equalTo(offence.getOffenceCode()));
        assertThat(convertedOffence.getStartDate(), equalTo(offence.getOffenceCommittedDate().toString()));
    }
}