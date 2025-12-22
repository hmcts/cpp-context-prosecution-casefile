package uk.gov.moj.cpp.prosecutioncasefile.query.view.response;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorDetails;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@SuppressWarnings({"squid:S1612"})
public class OffenceErrorsView {

    private final String id;
    private final List<ErrorDetailsView> errors;


    public OffenceErrorsView(final List<BusinessValidationErrorDetails> businessValidationErrorDetails) {
        this.id = getOffenceId(businessValidationErrorDetails);
        this.errors = buildError(businessValidationErrorDetails);

    }

    private List<ErrorDetailsView> buildError(final List<BusinessValidationErrorDetails> businessValidationErrorDetails) {
        return businessValidationErrorDetails.stream().map(s -> new ErrorDetailsView(s.getFieldId(), s.getFieldName(), s.getErrorValue(), s.getDisplayName(), s.getVersion())).collect(Collectors.toList());
    }

    private String getOffenceId(final List<BusinessValidationErrorDetails> businessValidationErrorDetails) {
        return businessValidationErrorDetails.stream().map(s -> s.getFieldId()).filter(Objects::nonNull).findFirst().orElse(null);
    }

    public String getId() {
        return id;
    }

    public List<ErrorDetailsView> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
