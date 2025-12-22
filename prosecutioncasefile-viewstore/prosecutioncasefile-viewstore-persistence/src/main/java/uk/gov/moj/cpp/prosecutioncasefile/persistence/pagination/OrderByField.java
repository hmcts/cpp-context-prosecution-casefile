package uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination;

public enum OrderByField {
    HEARING_DATE("defendantHearingDate"), REMAND_STATUS("defendantBailStatus");

    private final String fieldName;

    OrderByField(final String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return this.fieldName;
    }

}
