package uk.gov.moj.cpp.prosecution.casefile.domain;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@SuppressWarnings("squid:S2384")
public class ProsecutionWithReferenceData {

    private final Prosecution prosecution;

    private ReferenceDataVO referenceDataVO = new ReferenceDataVO();

    private UUID externalId;

    @JsonCreator
    public ProsecutionWithReferenceData(final Prosecution prosecution) {
        this.prosecution = prosecution;
    }

    public ProsecutionWithReferenceData(final Prosecution prosecution, final ReferenceDataVO referenceDataVO) {
        this.prosecution = prosecution;
        this.referenceDataVO = referenceDataVO;
    }

    public ProsecutionWithReferenceData(final Prosecution prosecution, final ReferenceDataVO referenceDataVO, final UUID externalId) {
        this.prosecution = prosecution;
        this.referenceDataVO = referenceDataVO;
        this.externalId = externalId;
    }

    public Prosecution getProsecution() {
        return prosecution;
    }

    public ReferenceDataVO getReferenceDataVO() {
        return referenceDataVO;
    }

    public void setReferenceDataVO(final ReferenceDataVO referenceDataVO) {
        this.referenceDataVO = referenceDataVO;
    }

    public UUID getExternalId() {
        return externalId;
    }

    public void setExternalId(final UUID externalId) {
        this.externalId = externalId;
    }
}
