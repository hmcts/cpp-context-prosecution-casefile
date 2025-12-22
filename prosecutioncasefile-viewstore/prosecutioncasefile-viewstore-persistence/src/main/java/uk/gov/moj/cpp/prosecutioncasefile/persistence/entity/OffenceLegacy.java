package uk.gov.moj.cpp.prosecutioncasefile.persistence.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "offence_legacy")
public class OffenceLegacy implements Serializable {

    private static final long serialVersionUID = -6173322170572695945L;

    @Id
    @Column(name = "offence_id")
    private UUID offenceId;

    @Column(name = "code")
    private String code;

    @Column(name = "plea")
    private String plea;

    @Column(name = "seq_no")
    private Integer sequenceNumber;

    @Column(name = "wording")
    private String wording;

    @Column(name = "indicated_plea")
    private String indicatedPlea;

    @Column(name = "section")
    private String section;

    @Column(name = "police_offence_id")
    private String policeOffenceId;

    @Embedded
    private CPRDetails cpr;

    @Column(name = "reason")
    private String reason;

    @Column(name = "description")
    private String description;

    @Column(name = "category")
    private String category;

    @Column(name = "arrest_date")
    private LocalDate arrestDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "charge_date")
    private LocalDate chargeDate;

    @ManyToOne
    @JoinColumn(name = "defendant_id")
    private DefendantLegacy defendant;

    @Column(name = "order_index")
    private int orderIndex;

    @Column(name = "count")
    private Integer count;

    public OffenceLegacy() {
        super();
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public void setOffenceId(UUID offenceId) {
        this.offenceId = offenceId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPlea() {
        return plea;
    }

    public void setPlea(String plea) {
        this.plea = plea;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getWording() {
        return wording;
    }

    public void setWording(String wording) {
        this.wording = wording;
    }

    public String getIndicatedPlea() {
        return indicatedPlea;
    }

    public void setIndicatedPlea(String indicatedPlea) {
        this.indicatedPlea = indicatedPlea;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getPoliceOffenceId() {
        return policeOffenceId;
    }

    public void setPoliceOffenceId(String policeOffenceId) {
        this.policeOffenceId = policeOffenceId;
    }

    public CPRDetails getCpr() {
        return cpr;
    }

    public void setCpr(CPRDetails cpr) {
        this.cpr = cpr;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getArrestDate() {
        return arrestDate;
    }

    public void setArrestDate(LocalDate arrestDate) {
        this.arrestDate = arrestDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getChargeDate() {
        return chargeDate;
    }

    public void setChargeDate(LocalDate chargeDate) {
        this.chargeDate = chargeDate;
    }

    public DefendantLegacy getDefendant() {
        return defendant;
    }

    public void setDefendant(DefendantLegacy defendant) {
        this.defendant = defendant;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

}

