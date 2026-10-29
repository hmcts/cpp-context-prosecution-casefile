package uk.gov.moj.cpp.prosecution.casefile.domain;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.GroupProsecution;

import java.io.Serializable;

public class GroupProsecutionWithReferenceData implements Serializable {

    private static final long serialVersionUID = 1L;

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
