package uk.gov.moj.cpp.prosecution.casefile.domain;

import uk.gov.justice.core.courts.MigrationSourceSystem;
import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingRequest;

import java.time.LocalDate;
import java.util.UUID;

public class ParamsVO {
    private ReferenceDataVO referenceDataVO;
    private UUID caseId;
    private Channel channel;
    private String oucodeL1Code;
    private String receivedFromCourtOUCode;
    private String initiationCode;
    private SummonsApprovedOutcome summonsApprovedOutcome;

    private LocalDate  custodyTimelineDefendant ;

    private MigrationSourceSystem migrationSourceSystem;

    private  HearingRequest listNewHearing;

    private Boolean isCivil;

    public Boolean getCivil() {
        return isCivil;
    }

    public void setCivil(final Boolean civil) {
        isCivil = civil;
    }

    public ReferenceDataVO getReferenceDataVO() {
        return referenceDataVO;
    }

    public void setReferenceDataVO(final ReferenceDataVO referenceDataVO) {
        this.referenceDataVO = referenceDataVO;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(final Channel channel) {
        this.channel = channel;
    }

    public String getOucodeL1Code() {
        return oucodeL1Code;
    }

    public void setOucodeL1Code(final String oucodeL1Code) {
        this.oucodeL1Code = oucodeL1Code;
    }

    public String getReceivedFromCourtOUCode() {
        return receivedFromCourtOUCode;
    }

    public void setReceivedFromCourtOUCode(final String receivedFromCourtOUCode) {
        this.receivedFromCourtOUCode = receivedFromCourtOUCode;
    }

    public String getInitiationCode() {
        return initiationCode;
    }

    public void setInitiationCode(final String initiationCode) {
        this.initiationCode = initiationCode;
    }

    public SummonsApprovedOutcome getSummonsApprovedOutcome() {
        return summonsApprovedOutcome;
    }

    public void setSummonsApprovedOutcome(final SummonsApprovedOutcome summonsApprovedOutcome) {
        this.summonsApprovedOutcome = summonsApprovedOutcome;
    }

    public LocalDate getCustodyTimelineDefendant() {
        return custodyTimelineDefendant;
    }

    public void setCustodyTimelineDefendant(final LocalDate custodyTimelineDefendant) {
        this.custodyTimelineDefendant = custodyTimelineDefendant;
    }

    public MigrationSourceSystem getMigrationSourceSystem() {
        return migrationSourceSystem;
    }

    public void setMigrationSourceSystem(final MigrationSourceSystem migrationSourceSystem) {
        this.migrationSourceSystem = migrationSourceSystem;
    }

    public HearingRequest getListNewHearing() {
        return listNewHearing;
    }

    public void setListNewHearing(final HearingRequest listNewHearing) {
        this.listNewHearing = listNewHearing;
    }
}
