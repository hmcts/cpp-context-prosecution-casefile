package uk.gov.moj.cpp.prosecutioncasefile.query.view.response;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorDetails;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@SuppressWarnings({"squid:S1612"})
public class DefendantErrorsView {

    private final LocalDate chargeDate;
    private final LocalDate hearingDate;
    private final UUID id;
    private final String bailStatus;
    private final List<ErrorDetailsView> errors;
    private final List<OffenceErrorsView> offences;

    private String firstName;
    private String lastName;
    private String organisationName;

    public DefendantErrorsView(final List<BusinessValidationErrorDetails> businessValidationErrorDetails) {
        this.chargeDate = getFirstChargeDate(businessValidationErrorDetails);
        this.hearingDate = getFirstHearingDate(businessValidationErrorDetails);
        this.id = getFirstDefendantId(businessValidationErrorDetails);
        this.errors = buildError(businessValidationErrorDetails);
        this.bailStatus = buildBailStatus(businessValidationErrorDetails);
        this.offences = buildOffenceErrorsView(businessValidationErrorDetails);
        this.firstName = getFirstFirstName(businessValidationErrorDetails);
        this.lastName = getFirstLastName(businessValidationErrorDetails);
        this.organisationName = getFirstOrganisationName(businessValidationErrorDetails);
    }

    private List<OffenceErrorsView> buildOffenceErrorsView(final List<BusinessValidationErrorDetails> businessValidationErrorDetails) {

        if (null == businessValidationErrorDetails) {
            return new ArrayList<>();
        }

        final Map<String, List<BusinessValidationErrorDetails>> collectionByDefendantId = businessValidationErrorDetails.stream().filter(s-> s.getFieldId()!=null).collect(Collectors.groupingBy(BusinessValidationErrorDetails::getFieldId));
        final List<OffenceErrorsView> offenceErrorsViews = new ArrayList<>();

        collectionByDefendantId.forEach((k, v) ->
                offenceErrorsViews.add(new OffenceErrorsView(v)));

        return offenceErrorsViews;
    }

    private String buildBailStatus(final List<BusinessValidationErrorDetails> businessValidationErrorDetails) {
        return businessValidationErrorDetails.stream().map(s -> s.getDefendantBailStatus()).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private List<ErrorDetailsView> buildError(final List<BusinessValidationErrorDetails> businessValidationErrorDetails) {
        return businessValidationErrorDetails.stream().filter(f -> f.getFieldId()==null).map(s -> new ErrorDetailsView(s.getFieldId(), s.getFieldName(), s.getErrorValue(), s.getDisplayName(), s.getVersion())).collect(Collectors.toList());
    }

    private UUID getFirstDefendantId(final List<BusinessValidationErrorDetails> businessValidationErrorDetails) {
        return businessValidationErrorDetails.stream().map(s -> s.getDefendantId()).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private LocalDate getFirstHearingDate(final List<BusinessValidationErrorDetails> businessValidationErrorDetails) {
        return businessValidationErrorDetails.stream().map(s -> s.getDefendantHearingDate()).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private LocalDate getFirstChargeDate(final List<BusinessValidationErrorDetails> businessValidationErrorDetails) {
        return businessValidationErrorDetails.stream().map(s -> s.getDefendantChargeDate()).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private String getFirstFirstName(final List<BusinessValidationErrorDetails> businessValidationErrorDetails) {
        return businessValidationErrorDetails.stream().map(s -> s.getFirstName()).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private String getFirstLastName(final List<BusinessValidationErrorDetails> businessValidationErrorDetails) {
        return businessValidationErrorDetails.stream().map(s -> s.getLastName()).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private String getFirstOrganisationName(final List<BusinessValidationErrorDetails> businessValidationErrorDetails) {
        return businessValidationErrorDetails.stream().map(s -> s.getOrganisationName()).filter(Objects::nonNull).findFirst().orElse(null);
    }

    public String getFirstName() { return this.firstName; }

    public String getLastName() { return this.lastName; }

    public String getOrganisationName() { return this.organisationName; }


    public LocalDate getChargeDate() {
        return chargeDate;
    }

    public LocalDate getHearingDate() {
        return hearingDate;
    }

    public UUID getId() {
        return id;
    }

    public List<ErrorDetailsView> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public String getBailStatus() {
        return bailStatus;
    }

    public List<OffenceErrorsView> getOffences() {
        return Collections.unmodifiableList(offences);
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
