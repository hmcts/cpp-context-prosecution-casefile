package uk.gov.moj.cpp.prosecution.casefile.domain;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.GroupProsecution;

public class GroupProsecutionWithReferenceData {

    private final GroupProsecution groupProsecution;

    private ReferenceDataVO referenceDataVO = new ReferenceDataVO();


    public GroupProsecutionWithReferenceData(final GroupProsecution groupProsecution) {
        this.groupProsecution = groupProsecution;
    }

    public GroupProsecution getGroupProsecution() {
        return groupProsecution;
    }

    public ReferenceDataVO getReferenceDataVO() {
        return referenceDataVO;
    }

    public void setReferenceDataVO(ReferenceDataVO referenceDataVO) {
        this.referenceDataVO = referenceDataVO;
    }
}
