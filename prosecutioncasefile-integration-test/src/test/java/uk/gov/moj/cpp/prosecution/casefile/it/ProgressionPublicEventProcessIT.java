package uk.gov.moj.cpp.prosecution.casefile.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.bigDecimal;

import uk.gov.moj.cpp.prosecution.casefile.helper.AppplicationProceedingsEditedHelper;

public class ProgressionPublicEventProcessIT extends BaseIT {

    public static final String PUBLIC_PROGRESSION_EVENT_COURT_APPLICATION_CREATED = "public.progression.court-application-created";

    private UUID caseId;
    private UUID applicationId;
    private String prosecutorCost;
    private boolean summonsSuppressed;
    private boolean personalService;

    @BeforeEach
    public void setup() {
        caseId = randomUUID();
        prosecutorCost = format("£%s", bigDecimal(100, 10000, 2).next());
        summonsSuppressed = BOOLEAN.next();
        personalService = BOOLEAN.next();
        applicationId = randomUUID();
    }

    @Test
    public void shouldRaiseProgressionEventCourtApplicationCreated() {
        sendPublicEvent(PUBLIC_PROGRESSION_EVENT_COURT_APPLICATION_CREATED, "stub-data/public.progression.court-application-event-created.json",
                this.applicationId.toString(), this.caseId.toString(), this.prosecutorCost, String.valueOf(this.summonsSuppressed), String.valueOf(this.personalService));
    }
    @Test
    public void shouldRaiseProgressionEventCourtApplicationProceedingsEdited() {
        sendPublicEvent("public.progression.event.application-proceedings-edited", "stub-data/public.progression.event.application-proceedings-edited.json");

        AppplicationProceedingsEditedHelper appplicationProceedingsEditedHelper = new AppplicationProceedingsEditedHelper();
        assertThat(appplicationProceedingsEditedHelper.getPrivateEvent(), is(notNullValue()));
    }

}
