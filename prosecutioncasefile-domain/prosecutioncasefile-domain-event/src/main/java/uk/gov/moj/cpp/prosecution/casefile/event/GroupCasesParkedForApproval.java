package uk.gov.moj.cpp.prosecution.casefile.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Event("prosecutioncasefile.events.group-cases-parked-for-approval")
@SuppressWarnings("squid:S2384")
public class GroupCasesParkedForApproval implements Serializable {

    private static final long serialVersionUID = -4951490385243539665L;

    private final UUID applicationId;
    @SuppressWarnings("squid:S1948")
    private final GroupProsecutionList groupProsecutionList;
    private final List<DefendantProblem> defendantWarnings;

    public GroupCasesParkedForApproval(final UUID applicationId, final GroupProsecutionList groupProsecutionList, final List<DefendantProblem> defendantWarnings) {
        this.applicationId = applicationId;
        this.groupProsecutionList = groupProsecutionList;
        this.defendantWarnings = defendantWarnings;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public GroupProsecutionList getGroupProsecutionList() {
        return groupProsecutionList;
    }

    public List<DefendantProblem> getDefendantWarnings() {
        return defendantWarnings;
    }

    public static GroupCasesParkedForApproval.Builder groupCasesParkedForApproval() {
        return new GroupCasesParkedForApproval.Builder();
    }

    public static class Builder {
        private UUID applicationId;
        private GroupProsecutionList groupProsecutionList;
        private List<DefendantProblem> defendantWarnings;

        public GroupCasesParkedForApproval.Builder withApplicationId(final UUID applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        public GroupCasesParkedForApproval.Builder withGroupProsecutionList(final GroupProsecutionList groupProsecutionList) {
            this.groupProsecutionList = groupProsecutionList;
            return this;
        }

        public GroupCasesParkedForApproval.Builder withDefendantWarnings(final List<DefendantProblem> defendantWarnings) {
            this.defendantWarnings = defendantWarnings;
            return this;
        }

        public GroupCasesParkedForApproval build() {
            return new GroupCasesParkedForApproval(applicationId, groupProsecutionList, defendantWarnings);
        }
    }
}
