package uk.gov.moj.cpp.prosecution.casefile.domain;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class GroupProsecutionList {
    private List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList;

    private UUID externalId;

    private Channel channel;

    public GroupProsecutionList(final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList) {
        if(groupProsecutionWithReferenceDataList != null) {
            this.groupProsecutionWithReferenceDataList = Collections.synchronizedList(groupProsecutionWithReferenceDataList);
        } else {
            this.groupProsecutionWithReferenceDataList = new ArrayList<>();
        }

    }

    public GroupProsecutionList(final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList, final UUID externalId) {
        this.groupProsecutionWithReferenceDataList = Collections.synchronizedList(groupProsecutionWithReferenceDataList);
        this.externalId = externalId;
    }

    public GroupProsecutionList(final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList, final UUID externalId, final Channel channel) {
        this.groupProsecutionWithReferenceDataList = Collections.synchronizedList(groupProsecutionWithReferenceDataList);
        this.externalId = externalId;
        this.channel = channel;
    }

    public List<GroupProsecutionWithReferenceData> getGroupProsecutionWithReferenceDataList() {
        return new ArrayList<>(groupProsecutionWithReferenceDataList);
    }

    public void setGroupProsecutionWithReferenceDataList(final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList) {
        this.groupProsecutionWithReferenceDataList = new ArrayList<>(groupProsecutionWithReferenceDataList);
    }

    public UUID getExternalId() {
        return externalId;
    }

    public void setExternalId(final UUID externalId) {
        this.externalId = externalId;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(final Channel channel) {
        this.channel = channel;
    }
}
