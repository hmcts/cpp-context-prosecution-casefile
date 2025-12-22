package uk.gov.moj.cpp.prosecution.casefile.event;

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

    private GroupCasesReceived(final GroupProsecutionList groupProsecutionList, final List<DefendantProblem> defendantWarnings) {
        this.groupProsecutionList = groupProsecutionList;
        this.defendantWarnings = defendantWarnings;
    }

    public GroupProsecutionList getGroupProsecutionList() {
        return groupProsecutionList;
    }

    public List<DefendantProblem> getDefendantWarnings() {
        return defendantWarnings;
    }

    public static GroupCasesReceivedBuilder groupCasesReceived(){
        return new GroupCasesReceivedBuilder();
    }

    public static final class GroupCasesReceivedBuilder {
        private GroupProsecutionList groupProsecutionList;
        private List<DefendantProblem> defendantWarnings;

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

        public GroupCasesReceived build() {
            return new GroupCasesReceived(groupProsecutionList, defendantWarnings);
        }
    }
}
