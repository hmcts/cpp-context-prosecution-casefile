package uk.gov.moj.cpp.prosecution.casefile.query.api.utils;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.stream.Stream;

import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OffenceHelperTest {

    @InjectMocks
    private OffenceHelper offenceHelper;

    public static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("CA03013","SNONIMP",false),
                Arguments.of("RR84227","STRAFF",false),
                Arguments.of("RT88584B","SIMP",false),
                Arguments.of("CD98070","EWAY",true)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldVerifyOffenceIsSummaryType(final String offenceCode, final String modeOfTrial, final boolean expectedSummaryOffence) {
        final JsonObjectBuilder offenceDefinition = createObjectBuilder();
        offenceDefinition.add("modeOfTrial", modeOfTrial);

        final boolean actualSummaryOffence = offenceHelper.isOffenceSummaryType(offenceDefinition.build());
        assertThat(actualSummaryOffence, is(expectedSummaryOffence));
    }
}