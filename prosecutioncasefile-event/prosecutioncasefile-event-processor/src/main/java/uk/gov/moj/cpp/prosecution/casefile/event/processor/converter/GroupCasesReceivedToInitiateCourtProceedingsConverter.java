package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CivilFees;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.FeeStatus;
import uk.gov.justice.core.courts.FeeType;
import uk.gov.justice.core.courts.InitiateCourtProceedingsForGroupCases;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ParamsVO;
import uk.gov.moj.cpp.prosecution.casefile.event.GroupCasesReceived;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseMarker;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.GroupProsecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;

import javax.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.core.courts.ContactNumber.contactNumber;
import static uk.gov.justice.core.courts.CourtReferral.courtReferral;
import static uk.gov.justice.core.courts.InitiationCode.valueFor;
import static uk.gov.justice.core.courts.Marker.marker;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;

public class GroupCasesReceivedToInitiateCourtProceedingsConverter implements Converter<GroupCasesReceived, InitiateCourtProceedingsForGroupCases> {

    @Inject
    private ProsecutionCaseFileDefendantToCCDefendantConverter prosecutionCaseFileDefendantToCCDefendantConverter;

    @Inject
    private ProsecutionCaseFileInitialHearingToCCHearingRequestConverter prosecutionCaseFileInitialHearingToCCHearingRequestConverter;


    public InitiateCourtProceedingsForGroupCases convert(final GroupCasesReceived groupCasesCreated) {
        final List<GroupProsecutionWithReferenceData> prosecutionList = groupCasesCreated.getGroupProsecutionList()
                .getGroupProsecutionWithReferenceDataList();

        final Channel channel = groupCasesCreated.getGroupProsecutionList().getChannel();

        final List<ProsecutionCase> prosecutionCases = prosecutionList.stream()
                .map(groupProsecutionWithReferenceData ->  convertToProsecutionCase(groupProsecutionWithReferenceData, channel))
                .collect(Collectors.toList());

        final Optional<GroupProsecutionWithReferenceData> masterCase = prosecutionList.stream()
                .filter(groupProsecutionWithReferenceData -> groupProsecutionWithReferenceData.getGroupProsecution().getIsGroupMaster())
                .findFirst();

        if (!masterCase.isPresent()){
            return null;
        }

        final List<ListHearingRequest> listHearingRequests = convertToListHearingRequest(masterCase.get(), channel);

        return InitiateCourtProceedingsForGroupCases.initiateCourtProceedingsForGroupCases()
                .withGroupId(prosecutionCases.get(0).getGroupId())
                .withCourtReferral(courtReferral()
                        .withProsecutionCases(prosecutionCases)
                        .withListHearingRequests(listHearingRequests)
                        .build())
                .build();
    }

    private List<ListHearingRequest> convertToListHearingRequest(final GroupProsecutionWithReferenceData groupProsecutionWithReferenceData, final Channel channel){
        final GroupProsecution groupProsecution = groupProsecutionWithReferenceData.getGroupProsecution();
        final ParamsVO paramsVO = buildParamV0(groupProsecutionWithReferenceData, channel);

        return prosecutionCaseFileInitialHearingToCCHearingRequestConverter.convert(
                groupProsecution.getDefendants(),
                paramsVO);
    }

    private ProsecutionCase convertToProsecutionCase(final GroupProsecutionWithReferenceData groupProsecutionWithReferenceData, final Channel channel) {
        final GroupProsecution groupProsecution = groupProsecutionWithReferenceData.getGroupProsecution();
        final CaseDetails caseDetails = groupProsecution.getCaseDetails();
        final ParamsVO paramsVO = buildParamV0(groupProsecutionWithReferenceData, channel);

        final String paymentReference = groupProsecutionWithReferenceData.getGroupProsecution().getPaymentReference();
        final var initialFee = createCivilFees(FeeType.INITIAL, paymentReference != null ? FeeStatus.SATISFIED : FeeStatus.OUTSTANDING, paymentReference);
        final var contestedFee = createCivilFees(FeeType.CONTESTED, FeeStatus.NOT_APPLICABLE, null);
        final ProsecutionCase.Builder prosecutionCaseBuilder = prosecutionCase()
                .withProsecutionCaseIdentifier(buildProsecutorCaseIdentifier(groupProsecution.getCaseDetails(), groupProsecutionWithReferenceData))//issue with ref data
                .withDefendants(prosecutionCaseFileDefendantToCCDefendantConverter.convert(groupProsecutionWithReferenceData.getGroupProsecution().getDefendants(), paramsVO))
                .withId(caseDetails.getCaseId())
                .withInitiationCode(valueFor(caseDetails.getInitiationCode()).orElse(null))
                .withSummonsCode(caseDetails.getSummonsCode())
                .withOriginatingOrganisation(caseDetails.getOriginatingOrganisation())
                .withCpsOrganisation(caseDetails.getCpsOrganisation())
                .withCpsOrganisationId(caseDetails.getCpsOrganisationId())
                .withStatementOfFacts(groupProsecution.getDefendants().get(0).getOffences().get(0).getStatementOfFacts())
                .withStatementOfFactsWelsh(groupProsecution.getDefendants().get(0).getOffences().get(0).getStatementOfFactsWelsh())
                .withClassOfCase(caseDetails.getClassOfCase())
                .withTrialReceiptType(caseDetails.getTrialReceiptType())
                .withIsGroupMember(groupProsecution.getIsGroupMember())
                .withGroupId(groupProsecution.getGroupId())
                .withIsCivil(groupProsecution.getIsCivil())
                .withCivilFees(List.of(initialFee, contestedFee))
                .withIsGroupMaster(groupProsecution.getIsGroupMaster());

        if (isNotEmpty(caseDetails.getCaseMarkers())){
            prosecutionCaseBuilder.withCaseMarkers(buildCaseMarkers(groupProsecutionWithReferenceData));
        }

        return prosecutionCaseBuilder.build();
    }

    private CivilFees createCivilFees(FeeType feeType, FeeStatus feeStatus, String paymentReference){
        return CivilFees.civilFees()
                .withFeeId(randomUUID())
                .withFeeType(feeType)
                .withFeeStatus(feeStatus)
                .withPaymentReference(paymentReference)
                .build();
    }

    private ParamsVO buildParamV0(final GroupProsecutionWithReferenceData groupProsecutionWithReferenceData, final Channel channel){
        final GroupProsecution groupProsecution = groupProsecutionWithReferenceData.getGroupProsecution();
        final CaseDetails caseDetails = groupProsecution.getCaseDetails();

        final Optional<OrganisationUnitWithCourtroomReferenceData> organisationUnitWithCourtroomReferenceData = groupProsecutionWithReferenceData
                .getReferenceDataVO()
                .getOrganisationUnitWithCourtroomReferenceData();

        final ParamsVO paramsVO = new ParamsVO();
        if(nonNull(groupProsecution.getIsCivil()) && groupProsecution.getIsCivil()){
            paramsVO.setCivil(groupProsecution.getIsCivil());
        }
        paramsVO.setReferenceDataVO(groupProsecutionWithReferenceData.getReferenceDataVO());
        paramsVO.setCaseId(groupProsecution.getCaseDetails().getCaseId());
        paramsVO.setChannel(channel);
        paramsVO.setReceivedFromCourtOUCode(caseDetails.getCourtReceivedFromCode());
        paramsVO.setInitiationCode(caseDetails.getInitiationCode());
        organisationUnitWithCourtroomReferenceData.ifPresent(unitWithCourtroomReferenceData -> paramsVO.setOucodeL1Code(unitWithCourtroomReferenceData.getOucodeL1Code()));
        return paramsVO;
    }

    private ProsecutionCaseIdentifier buildProsecutorCaseIdentifier(final CaseDetails caseDetails, final GroupProsecutionWithReferenceData groupProsecutionWithReferenceData) {
        final ProsecutorsReferenceData prosecutorsReferenceData = groupProsecutionWithReferenceData.getReferenceDataVO().getProsecutorsReferenceData();
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

    private ContactNumber buildContact(final String informantEmailAddress, final String contactEmailAddress) {
        return ofNullable(informantEmailAddress)
                .map(email -> contactNumber().withPrimaryEmail(email).build())
                .orElse(ofNullable(contactEmailAddress)
                        .map(email -> contactNumber().withPrimaryEmail(email).build())
                        .orElse(null));
    }

    private List<Marker> buildCaseMarkers(final GroupProsecutionWithReferenceData groupProsecutionWithReferenceData) {

        final List<CaseMarker> allCaseMarkers = getEnrichedCaseMarkers(groupProsecutionWithReferenceData);

        return allCaseMarkers.stream()
                .map(this::buildMarker)
                .collect(Collectors.toList());
    }

    private List<CaseMarker> getEnrichedCaseMarkers(final GroupProsecutionWithReferenceData groupProsecutionWithReferenceData) {

        return groupProsecutionWithReferenceData.getGroupProsecution().getCaseDetails().getCaseMarkers().stream()
                .map(caseMarker -> getEnrichedCaseMarkerFromList(caseMarker.getMarkerTypeCode(), groupProsecutionWithReferenceData.getReferenceDataVO().getCaseMarkers()))
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
