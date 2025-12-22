package uk.gov.moj.cpp.prosecution.casefile.it;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PROSECUTIONCASEFILE_EVENTS_ONLINE_PLEA_PCQ_VISITED_SUBMITTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PROSECUTIONCASEFILE_EVENTS_ONLINE_PLEA_SUBMITTED;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.stubForPleadOnline;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.stubProgressionQueryService;
import static uk.gov.moj.cpp.prosecution.casefile.stub.SjpStub.stubForSjpPleadOnline;
import static uk.gov.moj.cpp.prosecution.casefile.stub.SjpStub.stubSjpQuery;
import static uk.gov.moj.cpp.prosecution.casefile.stub.TestUtils.readFile;

import uk.gov.moj.cpp.prosecution.casefile.helper.OnlinePleaHelper;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class OnlinePleaIT extends BaseIT {

    @BeforeAll
    public static void createCommonWiremockStubs() {
        stubForSjpPleadOnline();
        stubForPleadOnline();
    }

    @Test
    public void shouldRaiseOnlinePleaSubmittedForAValidJspCase() {

        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();
        stubSjpQuery(caseId, randomUUID());

        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.plead-online.json");
        final String onlinePleaPayLoad = replaceValues(staticPayLoad, caseId, defendantId, "J");
        final OnlinePleaHelper onlinePleaHelper = new OnlinePleaHelper(PROSECUTIONCASEFILE_EVENTS_ONLINE_PLEA_SUBMITTED);
        onlinePleaHelper.submitPlea(onlinePleaPayLoad, caseId, defendantId);
        onlinePleaHelper.thenOnlinePleaSubmittedPrivateEventShouldBeRaised(caseId);
        onlinePleaHelper.verifyPublicEventRaisedForSJPReceived(caseId, defendantId);
    }
    @Test
    public void shouldRaiseOnlinePleaSubmittedWithLegalEntityForAValidJspCase() {

        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();
        stubSjpQuery(caseId, randomUUID());
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.plead-online-legal-entity-defendant.json");
        final String onlinePleaPayLoad = replaceValues(staticPayLoad, caseId, defendantId, "J");
        final OnlinePleaHelper onlinePleaHelper = new OnlinePleaHelper(PROSECUTIONCASEFILE_EVENTS_ONLINE_PLEA_SUBMITTED);
        onlinePleaHelper.submitPlea(onlinePleaPayLoad, caseId, defendantId);
        onlinePleaHelper.thenOnlinePleaSubmittedPrivateEventShouldBeRaised(caseId);
        onlinePleaHelper.verifyPublicEventRaisedForSJPReceived(caseId, defendantId);
    }

    //CCT-1324
    @Test
    public void shouldRaiseOnlinePleaPcqVisitedSubmittedSjpCase() {

        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID pcqId = randomUUID();
        stubSjpQuery(caseId, randomUUID());
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.plead-online-pcq-visited.json");
        final String onlinePleaPcqVisitedPayLoad = replaceValuesForPCQ(staticPayLoad, caseId, defendantId, "TFL123456", pcqId);
        final OnlinePleaHelper onlinePleaHelper = new OnlinePleaHelper(PROSECUTIONCASEFILE_EVENTS_ONLINE_PLEA_PCQ_VISITED_SUBMITTED);
        onlinePleaHelper.submitPleaPcqVisited(onlinePleaPcqVisitedPayLoad, caseId, defendantId, pcqId);
        onlinePleaHelper.thenOnlinePleaPcqVisitedSubmittedPrivateEventShouldBeRaised(caseId, defendantId, pcqId, "SJP");
    }

    @Test
    public void shouldRaiseOnlinePleaSubmittedForAValidJspCC() {

        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();
        stubProgressionQueryService(caseId, readFile("stub-data/progression.query.prosecutioncase.json"));
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.plead-online.json");
        final String onlinePleaPayLoad = replaceValues(staticPayLoad, caseId, defendantId, "Q");
        final OnlinePleaHelper onlinePleaHelper = new OnlinePleaHelper(PROSECUTIONCASEFILE_EVENTS_ONLINE_PLEA_SUBMITTED);
        onlinePleaHelper.submitPlea(onlinePleaPayLoad, caseId, defendantId);
        onlinePleaHelper.thenOnlinePleaSubmittedPrivateEventShouldBeRaised(caseId);
    }


    private String replaceValues(final String payload, final UUID caseId, final UUID defendantId, final String initiationCode) {
        return payload
                .replaceAll("CASE_ID", caseId.toString())
                .replaceAll("DEFENDANT_ID", defendantId.toString())
                .replaceAll("INITIATION_CODE", initiationCode);
    }

    private String replaceValuesForPCQ(final String payload, final UUID caseId, final UUID defendantId, final String urn, final UUID pcqId) {
        return payload
                .replaceAll("CASE_ID", caseId.toString())
                .replaceAll("DEFENDANT_ID", defendantId.toString())
                .replaceAll("URN", urn)
                .replaceAll("PCQ_ID", pcqId.toString());
    }


}
