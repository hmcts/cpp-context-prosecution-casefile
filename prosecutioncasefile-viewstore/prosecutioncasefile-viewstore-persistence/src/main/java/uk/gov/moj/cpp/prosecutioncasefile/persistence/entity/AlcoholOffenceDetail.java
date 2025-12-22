package uk.gov.moj.cpp.prosecutioncasefile.persistence.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class AlcoholOffenceDetail implements Serializable {

    private static final long serialVersionUID = -3378233466021789874L;

    @Column(name="alcohol_level_amount")
    private Integer alcoholLevelAmount;

    @Column(name="alcohol_level_method")
    private String alcoholLevelMethod;

    public AlcoholOffenceDetail() {
    }

    public AlcoholOffenceDetail(final Integer alcoholLevelAmount, final String alcoholLevelMethod) {
        this.alcoholLevelAmount = alcoholLevelAmount;
        this.alcoholLevelMethod = alcoholLevelMethod;
    }

    public Integer getAlcoholLevelAmount() {
        return alcoholLevelAmount;
    }

    public void setAlcoholLevelAmount(final Integer alcoholLevelAmount) {
        this.alcoholLevelAmount = alcoholLevelAmount;
    }

    public String getAlcoholLevelMethod() {
        return alcoholLevelMethod;
    }

    public void setAlcoholLevelMethod(final String alcoholLevelMethod) {
        this.alcoholLevelMethod = alcoholLevelMethod;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }
}
