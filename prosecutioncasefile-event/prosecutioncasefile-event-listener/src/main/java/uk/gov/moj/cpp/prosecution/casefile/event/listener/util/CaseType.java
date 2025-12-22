package uk.gov.moj.cpp.prosecution.casefile.event.listener.util;

public enum CaseType {
    SUMMONS_CASE_TYPE("S"),
    CHARGE_CASE_TYPE("C");

    private final String value;

    CaseType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

}
