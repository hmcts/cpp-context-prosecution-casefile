package uk.gov.moj.cpp.prosecution.casefile.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CASE_RECEIVED_WITH_DUPLICATE_DEFENDANTS;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CC_PROSECUTION_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CC_PROSECUTION_REJECTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_EVENT_SELECTOR_PROSECUTION_REJECTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CPPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.MCC;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetOrganisationUnits;
import static uk.gov.moj.cpp.prosecution.casefile.stub.TestUtils.readFile;

import uk.gov.moj.cpp.prosecution.casefile.helper.InitiateCCProsecutionHelper;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;

import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InitiateSummonsProsecutionIT extends BaseIT {

    private static final String COMMAND_PAYLOAD_FOR_INITIATE_SUMMONS_FOR_MCC_OR_CPPI = "command-json/prosecutioncasefile.command.initiate-channel-parametric-summons-prosecution.json";
    private static final String COMMAND_PAYLOAD_FOR_INITIATE_SUMMONS_FOR_MCC = "command-json/prosecutioncasefile.command.initiate-channel-mcc-summons-prosecution.json";
    private static final String COMMAND_PAYLOAD_FOR_SUBSEQUENT_INITIATE_SUMMONS_FOR_MCC_OR_CPPI = "command-json/prosecutioncasefile.command.subsequent_initiate-channel-parametric-summons-prosecution.json";
    private static final String COMMAND_PAYLOAD_FOR_SUBSEQUENT_INITIATE_SUMMONS_FOR_MCC = "command-json/prosecutioncasefile.command.subsequent_initiate-channel-parametric-summons-prosecution.json";
    private static final String EXPECTED_INITIATE_SUMMONS_APPLICATION_FOR_MCC_OR_CPPI = "expected/initiate-summons-application-for-mcc-or-cppi.json";
    private static final String EXPECTED_INITIATE_SUMMONS_APPLICATION_FOR_MCC = "expected/initiate-summons-application-for-mcc.json";
    private static final String EXPECTED_SUBSEQUENT_INITIATE_SUMMONS_APPLICATION_FOR_MCC_OR_CPPI = "expected/subsequent_initiate-summons-application-for-mcc-or-cppi.json";
    private static final String COMMAND_PAYLOAD_FOR_INITIATE_SUMMONS_FOR_SPI = "command-json/prosecutioncasefile.command.initiate-spi-summons-prosecution.json";
    private static final String EXPECTED_INITIATE_SUMMONS_APPLICATION_FOR_SPI = "expected/initiate-summons-application-for-spi.json";
    private static final String COMMAND_PAYLOAD_FOR_SUBSEQUENT_INITIATE_SUMMONS_FOR_SPI = "command-json/prosecutioncasefile.command.subsequent_initiate-spi-summons-prosecution.json";
    private static final String EXPECTED_SUBSEQUENT_INITIATE_SUMMONS_APPLICATION_FOR_SPI = "expected/subsequent_initiate-summons-application-for-spi.json";

    public static Stream<Arguments> channelToPayloadMappingForSummonsInitiation() {
        return Stream.of(
                Arguments.of(MCC, COMMAND_PAYLOAD_FOR_INITIATE_SUMMONS_FOR_MCC_OR_CPPI, EXPECTED_INITIATE_SUMMONS_APPLICATION_FOR_MCC_OR_CPPI),
                Arguments.of(CPPI, COMMAND_PAYLOAD_FOR_INITIATE_SUMMONS_FOR_MCC_OR_CPPI, EXPECTED_INITIATE_SUMMONS_APPLICATION_FOR_MCC_OR_CPPI),
                Arguments.of(SPI, COMMAND_PAYLOAD_FOR_INITIATE_SUMMONS_FOR_SPI, EXPECTED_INITIATE_SUMMONS_APPLICATION_FOR_SPI)
        );
    }

    @BeforeAll
    public static void setup() {
        stubGetOrganisationUnits();
    }

    @ParameterizedTest
    @MethodSource("channelToPayloadMappingForSummonsInitiation")
    public void shouldNotInitiateCourtProceedingsWhenNewSummonsCaseIsCreated(final Channel channel, final String payloadPath, final String expectedPayloadPath) {
        final InitiateCCProsecutionHelper helper = new InitiateCCProsecutionHelper();
        helper.initiateSummonsCaseForChannelAndVerifyApplicationCreatedInstead(channel, payloadPath, expectedPayloadPath);

        helper.verifyCourtProceedingsForSummonsApplicationHasBeenInitiated(expectedPayloadPath);
    }

    @Test
    public void shouldRaiseNewSummonsApplicationForSameDefendantWhenEarlierApplicationWasRejectedForSameCaseReceivedViaCPPIChannel() {
        final InitiateCCProsecutionHelper helper = new InitiateCCProsecutionHelper();
        helper.initiateSummonsCaseForChannelAndVerifyApplicationCreatedInstead(CPPI, COMMAND_PAYLOAD_FOR_INITIATE_SUMMONS_FOR_MCC_OR_CPPI, EXPECTED_INITIATE_SUMMONS_APPLICATION_FOR_MCC_OR_CPPI);

        helper.whenSummonsApplicationIsRejectedForDefendants();

        helper.thenEventsShouldBeRaised(new String[]{EVENT_SELECTOR_CC_PROSECUTION_REJECTED});
        helper.thenEventsShouldBeRaised(new String[]{PUBLIC_EVENT_SELECTOR_PROSECUTION_REJECTED});

        helper.initiateSubsequentSummonsCaseForChannelAndVerifyApplicationCreatedInstead(CPPI, COMMAND_PAYLOAD_FOR_SUBSEQUENT_INITIATE_SUMMONS_FOR_MCC_OR_CPPI, EXPECTED_SUBSEQUENT_INITIATE_SUMMONS_APPLICATION_FOR_MCC_OR_CPPI);
    }

    @Test
    public void shouldRaiseNewSummonsApplicationForSameDefendantWhenEarlierApplicationWasRejectedForSameCaseReceivedViaMCCChannel() {
        final InitiateCCProsecutionHelper helper = new InitiateCCProsecutionHelper();
        helper.initiateSummonsCaseForChannelAndVerifyApplicationCreatedInstead(MCC, COMMAND_PAYLOAD_FOR_INITIATE_SUMMONS_FOR_MCC_OR_CPPI, EXPECTED_INITIATE_SUMMONS_APPLICATION_FOR_MCC_OR_CPPI);
        helper.verifyPublicEventRaisedForManualCaseReceivedForMCCChannel();

        helper.whenSummonsApplicationIsRejectedForDefendants();

        helper.thenEventsShouldBeRaised(new String[]{EVENT_SELECTOR_CC_PROSECUTION_REJECTED});
        helper.thenEventsShouldBeRaised(new String[]{PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED});

        helper.initiateSubsequentSummonsCaseForChannelAndVerifyApplicationCreatedInstead(MCC, COMMAND_PAYLOAD_FOR_SUBSEQUENT_INITIATE_SUMMONS_FOR_MCC_OR_CPPI, EXPECTED_SUBSEQUENT_INITIATE_SUMMONS_APPLICATION_FOR_MCC_OR_CPPI);

    }

    @Test
    public void shouldRaiseNewSummonsApplicationForSameDefendantWhenEarlierApplicationWasRejectedForSameCaseReceivedViaSPIChannel() {
        final InitiateCCProsecutionHelper helper = new InitiateCCProsecutionHelper();
        helper.initiateSummonsCaseForChannelAndVerifyApplicationCreatedInstead(SPI, COMMAND_PAYLOAD_FOR_INITIATE_SUMMONS_FOR_SPI, EXPECTED_INITIATE_SUMMONS_APPLICATION_FOR_SPI);

        helper.whenSummonsApplicationIsRejectedForDefendants();

        helper.initiateSubsequentSummonsCaseForChannelAndVerifyApplicationCreatedInstead(SPI, COMMAND_PAYLOAD_FOR_SUBSEQUENT_INITIATE_SUMMONS_FOR_SPI, EXPECTED_SUBSEQUENT_INITIATE_SUMMONS_APPLICATION_FOR_SPI);
    }

    @Test
    public void shouldRejectCaseWhenSameDefendantIsReceivedAndSummonsApplicationForTheDefendantHasAlreadyBeenApprovedViaCPPIChannel() {
        final InitiateCCProsecutionHelper helper = new InitiateCCProsecutionHelper();
        helper.initiateSummonsCaseForChannelAndVerifyApplicationCreatedInstead(CPPI, COMMAND_PAYLOAD_FOR_INITIATE_SUMMONS_FOR_MCC_OR_CPPI, EXPECTED_INITIATE_SUMMONS_APPLICATION_FOR_MCC_OR_CPPI);

        helper.whenSummonsApplicationIsApprovedForDefendants();

        helper.thenEventsShouldBeRaised(new String[]{EVENT_SELECTOR_CC_PROSECUTION_RECEIVED});
        helper.thenEventsShouldBeRaised(new String[]{PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED});
        final String expectedPayload = readFile("expected/initiate_cc_expected_output_cppi_summons.json");
        helper.verifyCourtProceedingsForCaseCreationHasBeenInitiated(helper.getCaseUrn(), expectedPayload);

        helper.whenInitiateSummonsCaseIsRaisedByChannel(CPPI, COMMAND_PAYLOAD_FOR_SUBSEQUENT_INITIATE_SUMMONS_FOR_MCC_OR_CPPI);

        helper.thenEventsShouldBeRaised(new String[]{EVENT_SELECTOR_CC_PROSECUTION_REJECTED});
    }

    @Disabled
    @Test
    public void shouldRejectCaseWhenSameDefendantIsReceivedAndSummonsApplicationForTheDefendantHasAlreadyBeenApprovedViaMCCChannel() {
        final InitiateCCProsecutionHelper helper = new InitiateCCProsecutionHelper();
        helper.initiateSummonsCaseForChannelAndVerifyApplicationCreatedInstead(MCC, COMMAND_PAYLOAD_FOR_INITIATE_SUMMONS_FOR_MCC_OR_CPPI, EXPECTED_INITIATE_SUMMONS_APPLICATION_FOR_MCC_OR_CPPI);

        helper.whenSummonsApplicationIsApprovedForDefendants();

        helper.thenEventsShouldBeRaised(new String[]{EVENT_SELECTOR_CC_PROSECUTION_RECEIVED});

        helper.thenPublicEventsShouldBeRaised(PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED, isJson(Matchers.allOf(
                withJsonPath("$.caseDetails.caseId", CoreMatchers.is(helper.getCaseId().toString())))));

        helper.thenPublicEventsShouldBeRaised(PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED, isJson(Matchers.allOf(
                withJsonPath("$.caseId", CoreMatchers.is(helper.getCaseId().toString())))));

        final String expectedPayload = readFile("expected/initiate_cc_expected_output_mcc_summons.json");
        helper.verifyCourtProceedingsForCaseCreationHasBeenInitiated(helper.getCaseUrn(), expectedPayload);

        helper.whenInitiateSummonsCaseIsRaisedByChannel(MCC, COMMAND_PAYLOAD_FOR_SUBSEQUENT_INITIATE_SUMMONS_FOR_MCC_OR_CPPI);

        helper.thenEventsShouldBeRaised(new String[]{EVENT_SELECTOR_CC_PROSECUTION_REJECTED});
    }

    @Test
    public void shouldIgnoreAsDuplicateDefendantWhenSameDefendantIsReceivedAndSummonsApplicationForTheDefendantHasAlreadyBeenApprovedViaSPIChannel() {
        final InitiateCCProsecutionHelper helper = new InitiateCCProsecutionHelper();
        helper.initiateSummonsCaseForChannelAndVerifyApplicationCreatedInstead(SPI, COMMAND_PAYLOAD_FOR_INITIATE_SUMMONS_FOR_SPI, EXPECTED_INITIATE_SUMMONS_APPLICATION_FOR_SPI);

        helper.whenSummonsApplicationIsApprovedForDefendants();

        helper.thenEventsShouldBeRaised(new String[]{EVENT_SELECTOR_CC_PROSECUTION_RECEIVED});
        helper.thenEventsShouldBeRaised(new String[]{PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED});
        final String expectedPayload = readFile("expected/initiate_cc_expected_output_spi_summons.json");
        helper.verifyCourtProceedingsForCaseCreationHasBeenInitiated(helper.getCaseUrn(), expectedPayload);

        helper.whenInitiateSummonsCaseIsRaisedByChannel(SPI, COMMAND_PAYLOAD_FOR_SUBSEQUENT_INITIATE_SUMMONS_FOR_SPI);
        helper.thenEventsShouldBeRaised(new String[]{EVENT_SELECTOR_CASE_RECEIVED_WITH_DUPLICATE_DEFENDANTS});
    }

    @Test
    public void shouldInitiateCaseWhenSummonsApplicationForTheDefendantHasAlreadyBeenApprovedViaMCCChannel() {
        final InitiateCCProsecutionHelper helper = new InitiateCCProsecutionHelper();
        helper.initiateSummonsCaseForChannelAndVerifyApplicationCreatedInstead(MCC, COMMAND_PAYLOAD_FOR_INITIATE_SUMMONS_FOR_MCC, EXPECTED_INITIATE_SUMMONS_APPLICATION_FOR_MCC);

        helper.whenSummonsApplicationIsApprovedForDefendants();

        helper.thenEventsShouldBeRaised(new String[]{EVENT_SELECTOR_CC_PROSECUTION_RECEIVED});

        helper.thenPublicEventsShouldBeRaised(PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED, isJson(Matchers.allOf(
                withJsonPath("$.caseDetails.caseId", CoreMatchers.is(helper.getCaseId().toString())))));

        helper.thenPublicEventsShouldBeRaised(PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED, isJson(Matchers.allOf(
                withJsonPath("$.caseId", CoreMatchers.is(helper.getCaseId().toString())))));

        final String expectedPayload = readFile("expected/initiate_cc_expected_output_mcc_one_defendent_summons.json");
        helper.verifyCourtProceedingsForCaseCreationHasBeenInitiatedForMcc(helper.getCaseUrn(), expectedPayload);

        helper.whenInitiateSummonsCaseIsRaisedByChannel(MCC, COMMAND_PAYLOAD_FOR_SUBSEQUENT_INITIATE_SUMMONS_FOR_MCC);

    }
}
