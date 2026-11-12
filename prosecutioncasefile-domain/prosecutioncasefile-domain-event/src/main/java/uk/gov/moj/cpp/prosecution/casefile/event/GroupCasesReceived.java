package uk.gov.moj.cpp.prosecution.casefile.event;

import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;

import java.io.Serializable;
import java.util.List;

@Event("prosecutioncasefile.events.group-cases-received")
@SuppressWarnings("squid:S2384")
public class GroupCasesReceived implements Serializable {

    @SuppressWarnings("squid:S1948")
    private final GroupProsecutionList groupProsecutionList;
    private final List<DefendantProblem> defendantWarnings;
    // set only when a civil group parked for summons approval is approved; carries the prosecutor email for the notification
    @SuppressWarnings("squid:S1948")
    private final SummonsApprovedOutcome summonsApprovedOutcome;

    private GroupCasesReceived(final GroupProsecutionList groupProsecutionList, final List<DefendantProblem> defendantWarnings, final SummonsApprovedOutcome summonsApprovedOutcome) {
        this.groupProsecutionList = groupProsecutionList;
        this.defendantWarnings = defendantWarnings;
        this.summonsApprovedOutcome = summonsApprovedOutcome;
    }

    public GroupProsecutionList getGroupProsecutionList() {
        return groupProsecutionList;
    }

    public List<DefendantProblem> getDefendantWarnings() {
        return defendantWarnings;
    }

    public SummonsApprovedOutcome getSummonsApprovedOutcome() {
        return summonsApprovedOutcome;
    }

    public static GroupCasesReceivedBuilder groupCasesReceived(){
        return new GroupCasesReceivedBuilder();
    }

    public static final class GroupCasesReceivedBuilder {
        private GroupProsecutionList groupProsecutionList;
        private List<DefendantProblem> defendantWarnings;
        private SummonsApprovedOutcome summonsApprovedOutcome;

        private GroupCasesReceivedBuilder() {
        }

        public GroupCasesReceivedBuilder withGroupProsecutionList(GroupProsecutionList groupProsecutionList) {
            this.groupProsecutionList = groupProsecutionList;
            return this;
        }

        public GroupCasesReceivedBuilder withDefendantWarnings(List<DefendantProblem> defendantWarnings) {
            this.defendantWarnings = defendantWarnings;
            return this;
        }

        public GroupCasesReceivedBuilder withSummonsApprovedOutcome(SummonsApprovedOutcome summonsApprovedOutcome) {
            this.summonsApprovedOutcome = summonsApprovedOutcome;
            return this;
        }

        public GroupCasesReceived build() {
            return new GroupCasesReceived(groupProsecutionList, defendantWarnings, summonsApprovedOutcome);
        }
    }
}
