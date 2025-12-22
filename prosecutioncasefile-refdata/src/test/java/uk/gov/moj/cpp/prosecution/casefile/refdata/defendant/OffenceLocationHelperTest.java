package uk.gov.moj.cpp.prosecution.casefile.refdata.defendant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;

import org.junit.jupiter.api.Test;

public class OffenceLocationHelperTest {

    private static final String DVLA = "DVLA";

    @Test
    public void shouldGetDefaultOffenceLocationWhenProsecutionAuthorityIsDvlaAndOffenceLocationIsNull() {
        final Offence offence = Offence.offence().withOffenceLocation(null).build();

        final String offenceLocation = OffenceLocationHelper.getOffenceLocation(offence, DVLA);

        assertThat(offenceLocation, equalTo("No location provided"));
    }

    @Test
    public void shouldGetDefaultOffenceLocationWhenProsecutionAuthorityIsDvlaAndOffenceLocationIsBlank() {
        final Offence offence = Offence.offence().withOffenceLocation(" ").build();

        final String offenceLocation = OffenceLocationHelper.getOffenceLocation(offence, DVLA);

        assertThat(offenceLocation, equalTo("No location provided"));
    }

    @Test
    public void shouldGetOffenceLocationFromOffenceWhenProsecutionAuthorityIsNonDvla() {
        final Offence offence = Offence.offence().withOffenceLocation("My Location").build();

        final String offenceLocation = OffenceLocationHelper.getOffenceLocation(offence, "NON DVLA");

        assertThat(offenceLocation, equalTo("My Location"));
    }

    @Test
    public void shouldGetOffenceLocationFromOffenceWhenProsecutionAuthorityIsNull() {
        final Offence offence = Offence.offence().withOffenceLocation("Another location").build();

        final String offenceLocation = OffenceLocationHelper.getOffenceLocation(offence, null);

        assertThat(offenceLocation, equalTo("Another location"));
    }
}