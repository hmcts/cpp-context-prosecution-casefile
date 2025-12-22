package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper.buildOffenceTitleReferenceDataOffences;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper.buildOffences;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper.buildOffencesWithNullResultCode;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper.buildPressRestrictableOffences;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseFileOffenceToSjpOffenceConverterTest {
    
    @InjectMocks
    private ProsecutionCaseFileOffenceToSjpOffenceConverter converter;

    @Test
    public void shouldConvertPressRestrictableValueWhengetReportRestrictResultCodeIsNull() {

        final List<Offence> offences = buildOffencesWithNullResultCode()    ;
        final List<uk.gov.justice.json.schemas.domains.sjp.commands.Offence> convertedOffences = converter.convert(offences);

        assertThat(convertedOffences.size(), equalTo(offences.size()));
        assertThat(convertedOffences.get(0).getPressRestrictable(), equalTo(false));
    }

    @Test
    public void shouldConvertPressRestrictableValueToFalse() {

        final List<Offence> offences = buildOffences();
        final List<uk.gov.justice.json.schemas.domains.sjp.commands.Offence> convertedOffences = converter.convert(offences);

        assertThat(convertedOffences.size(), equalTo(offences.size()));
        assertThat(convertedOffences.get(0).getPressRestrictable(), equalTo(false));
    }


    @Test
    public void shouldConvertPressRestrictableValueToTrue() {

        final List<Offence> offences = buildPressRestrictableOffences();
        final List<uk.gov.justice.json.schemas.domains.sjp.commands.Offence> convertedOffences = converter.convert(offences);

        assertThat(convertedOffences.size(), equalTo(offences.size()));
        assertThat(convertedOffences.get(0).getPressRestrictable(), equalTo(true));


    }

    @Test
    public void shouldConvertOffenceTitles() {

        final List<Offence> offences = buildOffenceTitleReferenceDataOffences();
        final List<uk.gov.justice.json.schemas.domains.sjp.commands.Offence> convertedOffences = converter.convert(offences);

        assertThat(convertedOffences.size(), equalTo(offences.size()));
        assertThat(convertedOffences.get(0).getOffenceTitle(), equalTo(offences.get(0).getReferenceData().getTitle()));
        assertThat(convertedOffences.get(0).getOffenceTitleWelsh(), equalTo(offences.get(0).getReferenceData().getDetails().getDocument().getWelsh().getWelshoffencetitle()));

    }

    @Test
    public void shouldHandleNullOffenceTitles() {

        final List<Offence> offences = buildOffences();
        final List<uk.gov.justice.json.schemas.domains.sjp.commands.Offence> convertedOffences = converter.convert(offences);

        assertThat(convertedOffences.size(), equalTo(offences.size()));
        assertThat(convertedOffences.get(0).getOffenceTitle(), is(nullValue()));
        assertThat(convertedOffences.get(0).getOffenceTitleWelsh(), is(nullValue()));

    }

    @Test
    public void shouldConvertProsecutorOfferedAOCPtoTrue() {

        final List<Offence> offences = buildOffences()    ;
        final List<uk.gov.justice.json.schemas.domains.sjp.commands.Offence> convertedOffences = converter.convert(offences);

        assertThat(convertedOffences.size(), equalTo(offences.size()));
        assertThat(convertedOffences.get(0).getProsecutorOfferAOCP(), equalTo(true));
    }
}

