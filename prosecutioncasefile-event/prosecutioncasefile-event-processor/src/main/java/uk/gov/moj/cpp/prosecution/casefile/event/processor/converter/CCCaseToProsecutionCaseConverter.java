package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.core.courts.ContactNumber.contactNumber;
import static uk.gov.justice.core.courts.CourtReferral.courtReferral;
import static uk.gov.justice.core.courts.InitiateCourtProceedings.initiateCourtProceedings;
import static uk.gov.justice.core.courts.InitiationCode.valueFor;
import static uk.gov.justice.core.courts.Marker.marker;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CivilFees;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.InitiateCourtProceedings;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.MigrationSourceSystem;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.domain.ParamsVO;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceived;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseMarker;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class CCCaseToProsecutionCaseConverter implements Converter<CcCaseReceived, InitiateCourtProceedings> {

    @Inject
    private ProsecutionCaseFileDefendantToCCDefendantConverter prosecutionCaseFileDefendantToCCDefendantConverter;

    @Inject
    private ProsecutionCaseFileInitialHearingToCCHearingRequestConverter prosecutionCaseFileInitialHearingToCCHearingRequestConverter;

    @Inject
    private CaseDetailsToCivilFeesConverter caseDetailsToCivilFeesConverter;

    @Override
    public InitiateCourtProceedings convert(final CcCaseReceived source) {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = source.getProsecutionWithReferenceData();

        final Prosecution prosecution = prosecutionWithReferenceData.getProsecution();
        final List<ProsecutionCase> prosecutionCases = new ArrayList<>();
        final CaseDetails caseDetails = prosecution.getCaseDetails();
        final Optional<OrganisationUnitWithCourtroomReferenceData> organisationUnitWithCourtroomReferenceData = prosecutionWithReferenceData
                .getReferenceDataVO()
                .getOrganisationUnitWithCourtroomReferenceData();

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(prosecutionWithReferenceData.getReferenceDataVO());
        paramsVO.setCaseId(prosecution.getCaseDetails().getCaseId());
        paramsVO.setChannel(prosecution.getChannel());
        paramsVO.setReceivedFromCourtOUCode(caseDetails.getCourtReceivedFromCode());
        paramsVO.setInitiationCode(caseDetails.getInitiationCode());
        paramsVO.setSummonsApprovedOutcome(source.getSummonsApprovedOutcome());
        paramsVO.setListNewHearing(prosecution.getListNewHearing());
        if(nonNull(prosecution.getIsCivil()) && prosecution.getIsCivil()){
            paramsVO.setCivil(prosecution.getIsCivil());
        }

        organisationUnitWithCourtroomReferenceData.ifPresent(unitWithCourtroomReferenceData -> paramsVO.setOucodeL1Code(unitWithCourtroomReferenceData.getOucodeL1Code()));

        final List<CivilFees> civilFees = caseDetailsToCivilFeesConverter.convert(caseDetails);

        final MigrationSourceSystem migrationSourceSystem = ofNullable(prosecution.getMigrationSourceSystem()).filter(system -> nonNull(system.getMigrationSourceSystemName())).orElse(null);

        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withProsecutionCaseIdentifier(buildProsecutorCaseIdentifier(caseDetails, prosecutionWithReferenceData))//issue with ref data
                .withCaseMarkers(caseDetails.getCaseMarkers() != null && !caseDetails.getCaseMarkers().isEmpty() ? buildCaseMarkers(prosecutionWithReferenceData) : null)
                .withDefendants(prosecutionCaseFileDefendantToCCDefendantConverter.convert(source.getProsecutionWithReferenceData().getProsecution().getDefendants(), paramsVO))
                .withId(caseDetails.getCaseId())
                .withInitiationCode(valueFor(caseDetails.getInitiationCode()).orElse(null))
                .withSummonsCode(caseDetails.getSummonsCode())
                .withOriginatingOrganisation(caseDetails.getOriginatingOrganisation())
                .withCpsOrganisation(caseDetails.getCpsOrganisation())
                .withCpsOrganisationId(caseDetails.getCpsOrganisationId())
                .withStatementOfFacts(getStatementOfFacts(prosecution))
                .withStatementOfFactsWelsh(getStatementOfFactsWelsh(prosecution))
                .withClassOfCase(caseDetails.getClassOfCase())
                .withTrialReceiptType(caseDetails.getTrialReceiptType())
                .withCivilFees(civilFees)
                .withIsCivil(prosecution.getIsCivil())
                .withRelatedUrn(caseDetails.getRelatedUrn())
                .withMigrationSourceSystem(migrationSourceSystem)
                .withIsRetrial(caseDetails.getIsRetrial())
                .build();

        prosecutionCases.add(prosecutionCase);

        return initiateCourtProceedings()
                .withInitiateCourtProceedings(courtReferral()
                        .withProsecutionCases(prosecutionCases)
                        .withListHearingRequests(prosecutionCaseFileInitialHearingToCCHearingRequestConverter.
                                convert(source.getProsecutionWithReferenceData().getProsecution().getDefendants(), paramsVO))
                        .build())
                .withId(source.getId())
                .build();
    }

    private String getStatementOfFactsWelsh(Prosecution prosecution) {
        return !prosecution.getDefendants().isEmpty() && nonNull(prosecution.getDefendants()) ? prosecution.getDefendants().get(0).getOffences().get(0).getStatementOfFactsWelsh() : "";
    }

    private String getStatementOfFacts(Prosecution prosecution) {
        return !prosecution.getDefendants().isEmpty() && nonNull(prosecution.getDefendants()) ? prosecution.getDefendants().get(0).getOffences().get(0).getStatementOfFacts() : "";
    }

    @SuppressWarnings({"squid:S1135"})
    private ProsecutionCaseIdentifier buildProsecutorCaseIdentifier(final CaseDetails caseDetails, final ProsecutionWithReferenceData prosecutionWithReferenceData) {
        final ProsecutorsReferenceData prosecutorsReferenceData = prosecutionWithReferenceData.getReferenceDataVO().getProsecutorsReferenceData();
        return prosecutionCaseIdentifier().withCaseURN(caseDetails.getProsecutorCaseReference())
                .withProsecutionAuthorityId(prosecutorsReferenceData.getId())
                .withProsecutionAuthorityCode(prosecutorsReferenceData.getShortName())
                .withAddress(ofNullable(prosecutorsReferenceData.getAddress()).map(address -> Address.address()
                        .withAddress1(address.getAddress1())
                        .withAddress2(address.getAddress2())
                        .withAddress3(address.getAddress3())
                        .withAddress4(address.getAddress4())
                        .withAddress5(address.getAddress5())
                        .withPostcode(address.getPostcode()).build()).orElse(null))
                .withMajorCreditorCode(prosecutorsReferenceData.getMajorCreditorCode())
                .withProsecutionAuthorityName(prosecutorsReferenceData.getFullName())
                .withProsecutionAuthorityOUCode(prosecutorsReferenceData.getOucode())
                .withContact(buildContact(prosecutorsReferenceData.getInformantEmailAddress(), prosecutorsReferenceData.getContactEmailAddress()))
                .withProsecutorCategory(prosecutorsReferenceData.getProsecutorCategory())
                .build();

    }

    private ContactNumber buildContact(String informantEmailAddress, String contactEmailAddress) {
        return ofNullable(informantEmailAddress)
                .map(email -> contactNumber().withPrimaryEmail(email).build())
                .orElse(ofNullable(contactEmailAddress)
                        .map(email -> contactNumber().withPrimaryEmail(email).build())
                        .orElse(null));
    }

    private List<Marker> buildCaseMarkers(final ProsecutionWithReferenceData prosecutionWithReferenceData) {

        final List<CaseMarker> allCaseMarkers = getEnrichedCaseMarkers(prosecutionWithReferenceData);

        return allCaseMarkers.stream()
                .map(this::buildMarker)
                .collect(Collectors.toList());
    }

    private List<CaseMarker> getEnrichedCaseMarkers(final ProsecutionWithReferenceData prosecutionWithReferenceData) {

        return prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseMarkers().stream()
                .map(caseMarker -> getEnrichedCaseMarkerFromList(caseMarker.getMarkerTypeCode(), prosecutionWithReferenceData.getReferenceDataVO().getCaseMarkers()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private CaseMarker getEnrichedCaseMarkerFromList(final String caseMarkerTypeCode, final List<CaseMarker> caseMarkers) {
        for (final CaseMarker caseMarker : caseMarkers) {
            if (caseMarker.getMarkerTypeCode().equals(caseMarkerTypeCode)) {
                return caseMarker;
            }
        }
        return null;
    }

    private Marker buildMarker(final CaseMarker caseMarker) {
        return marker()
                .withId(randomUUID())
                .withMarkerTypeCode(caseMarker.getMarkerTypeCode())
                .withMarkerTypeDescription(caseMarker.getMarkerTypeDescription())
                .withMarkerTypeid(caseMarker.getMarkerTypeId())
                .build();
    }

}
