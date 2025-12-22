package uk.gov.moj.cpp.prosecution.casefile.domain;

import java.util.Objects;
import java.util.UUID;

public class BailStatus {

    private UUID id;

    private String code;

    private String description;

    public BailStatus(final UUID id, final String code, final String description) {
        this.id = id;
        this.code = code;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final BailStatus that = (BailStatus) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(code, that.code) &&
                Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code, description);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private UUID id;
        private String statusCode;
        private String statusDescription;

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withStatusCode(final String statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder withStatusDescription(final String statusDescription) {
            this.statusDescription = statusDescription;
            return this;
        }

        public BailStatus build(){
            return new BailStatus(id, statusCode, statusDescription);
        }
    }
}
