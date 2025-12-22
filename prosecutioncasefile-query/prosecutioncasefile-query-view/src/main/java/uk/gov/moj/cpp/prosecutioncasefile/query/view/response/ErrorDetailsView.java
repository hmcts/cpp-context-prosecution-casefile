package uk.gov.moj.cpp.prosecutioncasefile.query.view.response;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ErrorDetailsView {

    private String id;
    private String fieldName;
    private String errorValue;
    private String displayName;
    private Long version;

    public ErrorDetailsView(final String id, final String fieldName, final String errorValue, final String displayName, final Long version) {
        this.id = id;
        this.fieldName = fieldName;
        this.errorValue = errorValue;
        this.displayName = displayName;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(final String fieldName) {
        this.fieldName = fieldName;
    }

    public String getErrorValue() {
        return errorValue;
    }

    public void setErrorValue(final String errorValue) {
        this.errorValue = errorValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(final Long version) {
        this.version = version;
    }
}
