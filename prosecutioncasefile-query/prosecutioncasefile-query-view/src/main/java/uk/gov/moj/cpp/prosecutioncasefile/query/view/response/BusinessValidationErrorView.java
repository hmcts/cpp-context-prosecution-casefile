package uk.gov.moj.cpp.prosecutioncasefile.query.view.response;


import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.builder.ToStringStyle.JSON_STYLE;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorDetails;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@SuppressWarnings({"squid:S1612"})
public class BusinessValidationErrorView {

    public static final String REGION_NW = "NW";
    public static final String MULTIPLE_ERRORS = "Multiple Errors";
    public static final String SINGLE_ERROR = "Single Error";
    private final List<DefendantErrorsView> defendants;
    private final UUID id;
    private final String urn;
    private final LocalDate dateOfBirth;
    private final String court;
    private final String courtLocation;
    private final String region;
    private final String caseType;
    private final String errorDescription;
    private final Long version;
    private final List<ErrorDetailsView> errors;
    private final List<ErrorDetailsView> caseMarkersErrors;
    private final ErrorCaseDetails errorCaseDetails;

    private final LocalDate hearingDate;


    public BusinessValidationErrorView(final List<BusinessValidationErrorDetails> listWithCaseErrorDetails, final List<BusinessValidationErrorDetails> listWithDefendantErrorDetails, ErrorCaseDetails errorCaseDetails) {
        this.id = getFirstCaseId(listWithCaseErrorDetails, listWithDefendantErrorDetails);
        this.urn = getFirstUrn(listWithCaseErrorDetails, listWithDefendantErrorDetails);
        this.dateOfBirth = getFirstDob(listWithCaseErrorDetails, listWithDefendantErrorDetails);
        this.version = getVersion(listWithCaseErrorDetails, listWithDefendantErrorDetails);
        this.court = getFirstCourtName(listWithCaseErrorDetails, listWithDefendantErrorDetails);
        this.courtLocation = getFirstCourtHearingLocation(listWithDefendantErrorDetails);
        this.caseType = getFirstCaseType(listWithCaseErrorDetails, listWithDefendantErrorDetails);
        this.defendants = buildDefendantsErrorsView(listWithDefendantErrorDetails);
        this.errors = listWithCaseErrorDetails != null ? buildError(listWithCaseErrorDetails) : null;
        this.caseMarkersErrors = listWithCaseErrorDetails != null ? buildCaseMarkersErrors(listWithCaseErrorDetails) : null;
        this.errorDescription = buildErrorDescription(listWithCaseErrorDetails, listWithDefendantErrorDetails);
        this.region = REGION_NW;
        // hearing date is case level attribute for SPI so it will be same for given case id.
        this.hearingDate = listWithCaseErrorDetails != null ? getHearingDateForCase(listWithCaseErrorDetails) : null;
        this.errorCaseDetails = errorCaseDetails;
    }

    private Long getVersion(final List<BusinessValidationErrorDetails> listWithCaseErrorDetails, final List<BusinessValidationErrorDetails> listWithDefendantErrorDetails) {
        final List<BusinessValidationErrorDetails> errorDetailsList = (listWithCaseErrorDetails != null && !listWithCaseErrorDetails.isEmpty()) ? listWithCaseErrorDetails : listWithDefendantErrorDetails;

        return errorDetailsList.stream().map(BusinessValidationErrorDetails::getVersion).filter(Objects::nonNull).findFirst().orElse(null);

    }

    private String getFirstCourtHearingLocation(final List<BusinessValidationErrorDetails> listWithDefendantErrorDetails) {

        return listWithDefendantErrorDetails.stream().map(BusinessValidationErrorDetails::getCourtLocation).filter(Objects::nonNull).findFirst().orElse(null);

    }

    private String getFirstCaseType(final List<BusinessValidationErrorDetails> listWithCaseErrorDetails, final List<BusinessValidationErrorDetails> listWithDefendantErrorDetails) {
        final List<BusinessValidationErrorDetails> errorDetailsList = (listWithCaseErrorDetails != null && !listWithCaseErrorDetails.isEmpty()) ? listWithCaseErrorDetails : listWithDefendantErrorDetails;


        return errorDetailsList.stream().map(BusinessValidationErrorDetails::getCaseType).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private String getFirstCourtName(final List<BusinessValidationErrorDetails> listWithCaseErrorDetails, final List<BusinessValidationErrorDetails> listWithDefendantErrorDetails) {
        final List<BusinessValidationErrorDetails> errorDetailsList = (listWithCaseErrorDetails != null && !listWithCaseErrorDetails.isEmpty()) ? listWithCaseErrorDetails : listWithDefendantErrorDetails;

        return errorDetailsList.stream().map(BusinessValidationErrorDetails::getCourtName).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private String getFirstUrn(final List<BusinessValidationErrorDetails> listWithCaseErrorDetails, final List<BusinessValidationErrorDetails> listWithDefendantErrorDetails) {
        final List<BusinessValidationErrorDetails> errorDetailsList = (listWithCaseErrorDetails != null && !listWithCaseErrorDetails.isEmpty()) ? listWithCaseErrorDetails : listWithDefendantErrorDetails;


        return errorDetailsList.stream().map(BusinessValidationErrorDetails::getUrn).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private LocalDate getFirstDob(final List<BusinessValidationErrorDetails> listWithCaseErrorDetails, final List<BusinessValidationErrorDetails> listWithDefendantErrorDetails) {
        final List<BusinessValidationErrorDetails> errorDetailsList = (listWithCaseErrorDetails != null && !listWithCaseErrorDetails.isEmpty()) ? listWithCaseErrorDetails : listWithDefendantErrorDetails;
        return errorDetailsList.stream().map(BusinessValidationErrorDetails::getDateOfBirth).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private UUID getFirstCaseId(final List<BusinessValidationErrorDetails> listWithCaseErrorDetails, final List<BusinessValidationErrorDetails> listWithDefendantErrorDetails) {
        final List<BusinessValidationErrorDetails> errorDetailsList = (listWithCaseErrorDetails != null && !listWithCaseErrorDetails.isEmpty()) ? listWithCaseErrorDetails : listWithDefendantErrorDetails;


        return errorDetailsList.stream().map(BusinessValidationErrorDetails::getCaseId).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private List<DefendantErrorsView> buildDefendantsErrorsView(final List<BusinessValidationErrorDetails> listWithDefendantErrorDetails) {

        if (null == listWithDefendantErrorDetails) {
            return new ArrayList<>();
        }

        final Map<UUID, List<BusinessValidationErrorDetails>> collectionByDefendantId = listWithDefendantErrorDetails.stream().collect(Collectors.groupingBy(BusinessValidationErrorDetails::getDefendantId, LinkedHashMap::new, toList()));
        final List<DefendantErrorsView> defendantErrorsViewList = new ArrayList<>();

        collectionByDefendantId.forEach((k, v) ->
                defendantErrorsViewList.add(new DefendantErrorsView(v)));

        return defendantErrorsViewList;

    }

    public String getRegion() {
        return region;
    }

    private String buildErrorDescription(final List<BusinessValidationErrorDetails> listWithCaseErrorDetails, final List<BusinessValidationErrorDetails> listWithDefendantErrorDetails) {
        return ((getCount(listWithCaseErrorDetails) + getCount(listWithDefendantErrorDetails)) > 1) ? MULTIPLE_ERRORS : SINGLE_ERROR;
    }

    private long getCount(final List<BusinessValidationErrorDetails> listWithCaseErrorDetails) {
        return listWithCaseErrorDetails != null ? listWithCaseErrorDetails.size() : 0;
    }

    private List<ErrorDetailsView> buildError(final List<BusinessValidationErrorDetails> listWithCaseErrorDetails) {
        return listWithCaseErrorDetails.stream().filter(s -> s.getFieldId() == null).map(s -> new ErrorDetailsView(s.getFieldId(), s.getFieldName(), s.getErrorValue(), s.getDisplayName(), s.getVersion())).collect(toList());
    }

    private List<ErrorDetailsView> buildCaseMarkersErrors(final List<BusinessValidationErrorDetails> listWithCaseErrorDetails) {
        return listWithCaseErrorDetails.stream().filter(s -> s.getFieldId() != null).map(s -> new ErrorDetailsView(s.getFieldId(), s.getFieldName(), s.getErrorValue(), s.getDisplayName(), s.getVersion())).collect(toList());
    }

    private LocalDate getHearingDateForCase(final List<BusinessValidationErrorDetails> businessValidationErrorDetails) {
        return businessValidationErrorDetails.stream().map(BusinessValidationErrorDetails::getDefendantHearingDate).filter(Objects::nonNull).findAny().orElse(null);
    }

    public List<DefendantErrorsView> getDefendants() {
        return Collections.unmodifiableList(defendants);
    }

    public UUID getId() {
        return id;
    }

    public String getUrn() {
        return urn;
    }

    public String getCourt() {
        return court;
    }

    public String getCaseType() {
        return caseType;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public List<ErrorDetailsView> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public List<ErrorDetailsView> getCaseMarkersErrors() {
        return Collections.unmodifiableList(caseMarkersErrors);
    }

    public ErrorCaseDetails getErrorCaseDetails() {
        return errorCaseDetails;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getCourtLocation() {
        return courtLocation;
    }

    public Long getVersion() { return version; }

    public LocalDate getHearingDate() {
        return hearingDate;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, JSON_STYLE);
    }
}
