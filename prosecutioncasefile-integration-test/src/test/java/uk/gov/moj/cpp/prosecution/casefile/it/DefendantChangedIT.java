package uk.gov.moj.cpp.prosecution.casefile.it;

import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_CASE_VALIDATION_COMPLETED;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataOffencesStub.stubOffencesForOffenceCodeWithEitherWayModeOfTrial;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetCaseMarkersWithCode;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetOrganisationUnitsReturnsMag;
import static uk.gov.moj.cpp.prosecution.casefile.stub.TestUtils.readFile;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.prosecution.casefile.helper.InitiateCCProsecutionHelper;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefendantChangedIT extends BaseIT {

    private static final String CASE_MARKER_CODE = "ABC";
    private final JmsMessageConsumerClient stagingProsecutorsOperationalDetailsResponseReportedPrivateEventConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME)
            .withEventNames(EVENT_CASE_VALIDATION_COMPLETED)
            .getMessageConsumerClient();

    private String caseUrn;
    private String defendantId1;

    @BeforeAll
    public static void setup() {
        stubGetCaseMarkersWithCode(CASE_MARKER_CODE);
        stubGetOrganisationUnitsReturnsMag("B04NM04");
        stubGetOrganisationUnitsReturnsMag("B02NM03");
    }

    @BeforeEach
    public void setUp() {
        stubOffencesForOffenceCodeWithEitherWayModeOfTrial();
        caseUrn = randomAlphanumeric(10);
        defendantId1 = randomUUID().toString();
    }

    @Test
    public void shouldChangeDefendantPersonalDetails() {
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        final String expectedPayload = readFile("expected/initiate_cc_expected_output.json");
        final String ccPayLoad = replaceValues(staticPayLoad);
        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);
        initiateCCProsecutionHelper.thenProsecutionReceivedEventShouldBeRaised();
        initiateCCProsecutionHelper.verifyCourtProceedingsForCaseCreationHasBeenInitiated(caseUrn, expectedPayload);
        initiateCCProsecutionHelper.whenCaseDefendantChanged(defendantId1);
    }

    private String replaceValues(final String payload) {
        return payload
                .replace("CASE-ID", randomUUID().toString())
                .replace("CHANNEL", "SPI")
                .replace("CASE-URN", caseUrn)
                .replace("DEFENDANT_ID1", this.defendantId1)
                .replace("DEFENDANT_REFERENCE1", this.defendantId1)
                .replace("OFFENCE_ID1", randomUUID().toString())
                .replace("DEFENDANT_ID2", randomUUID().toString())
                .replace("DEFENDANT_REFERENCE2", randomUUID().toString())
                .replace("DEFENDANT_ID3", randomUUID().toString())
                .replace("DEFENDANT_REFERENCE3", randomUUID().toString())
                .replace("OFFENCE_ID2", randomUUID().toString())
                .replace("OFFENCE_ID3", randomUUID().toString())
                .replace("OFFENCE_ID4", randomUUID().toString())
                .replace("OFFENCE_ID5", randomUUID().toString())
                .replace("OFFENCE_ID6", randomUUID().toString())
                .replace("INITIATION_CODE", "C")
                .replace("CASE_MARKER", CASE_MARKER_CODE)
                .replace("DATE_RECEIVED", LocalDates.to(LocalDate.now()))
                .replace("EXTERNAL_ID", randomUUID().toString());
    }
}
