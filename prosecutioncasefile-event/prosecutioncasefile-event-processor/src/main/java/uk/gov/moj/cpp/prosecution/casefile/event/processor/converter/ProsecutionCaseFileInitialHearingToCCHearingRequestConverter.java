package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.core.courts.ListDefendantRequest.listDefendantRequest;
import static uk.gov.justice.core.courts.ListHearingRequest.listHearingRequest;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.PCFEnumMap.getHearingPCFToProgressionMap;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.justice.core.courts.SummonsType;
import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.moj.cpp.prosecution.casefile.domain.ParamsVO;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CourtRoom;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingRequest;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;

public class ProsecutionCaseFileInitialHearingToCCHearingRequestConverter implements ParameterisedConverter<List<Defendant>, List<ListHearingRequest>, ParamsVO> {

    private static final Logger LOGGER = getLogger(ProsecutionCaseFileInitialHearingToCCHearingRequestConverter.class);

    private static final String ORGANISATION_UNIT_LEVEL_CODE_FOR_CROWN = "C";

    @Override
    @SuppressWarnings({"squid:S1135"})
    public List<ListHearingRequest> convert(final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendants, final ParamsVO paramsVO) {
        final List<ListHearingRequest> hearingRequests = new ArrayList<>();

        final InitialHearing initialHearing = getInitialHearing(defendants);
        final boolean isMCCWithInitialHearing = isMCCWithInitialHearing(paramsVO, initialHearing);

        if (!isMCCWithInitialHearing) {
            hearingRequests.add(buildStandardHearingRequest(defendants, paramsVO, initialHearing));
        }

        if (isMCCChannel(paramsVO) && hasListNewHearing(paramsVO)) {
            hearingRequests.addAll(convertMCCWC(defendants, paramsVO));
        }

        return hearingRequests;
    }

    private InitialHearing getInitialHearing(final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendants) {
        return Optional.ofNullable(defendants)
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .map(uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant::getInitialHearing)
                .orElse(null);
    }

    private boolean isMCCWithInitialHearing(final ParamsVO paramsVO, final InitialHearing initialHearing) {
        return isMCCChannel(paramsVO) && hasListNewHearing(paramsVO) && isNull(initialHearing);
    }

    private boolean isMCCChannel(final ParamsVO paramsVO) {
        return Channel.MCC == paramsVO.getChannel();
    }

    private boolean hasListNewHearing(final ParamsVO paramsVO) {
        return nonNull(paramsVO.getListNewHearing());
    }

    private ListHearingRequest buildStandardHearingRequest(final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendants, 
                                                          final ParamsVO paramsVO, 
                                                          final InitialHearing initialHearing) {
        final ReferenceDataVO referenceDataVO = paramsVO.getReferenceDataVO();
        final UUID caseId = paramsVO.getCaseId();
        
        final ListHearingRequest.Builder builder = listHearingRequest()
                .withCourtCentre(buildCourtCentre(referenceDataVO, initialHearing))
                .withHearingType(buildHearingType(referenceDataVO))
                .withJurisdictionType(determineJurisdictionType(paramsVO.getChannel(), paramsVO.getOucodeL1Code()));

        configureEstimateMinutes(builder, initialHearing, referenceDataVO);
        configureOptionalHearingProperties(builder, initialHearing);
        configureDefendantProperties(builder, defendants, caseId, paramsVO);

        return builder.build();
    }

    private HearingType buildHearingType(final ReferenceDataVO referenceDataVO) {
        return HearingType.hearingType()
                .withId(referenceDataVO.getHearingType().getId())
                .withDescription(referenceDataVO.getHearingType().getHearingDescription())
                .build();
    }

    private void configureEstimateMinutes(final ListHearingRequest.Builder builder, 
                                        final InitialHearing initialHearing, 
                                        final ReferenceDataVO referenceDataVO) {
        Optional.ofNullable(initialHearing).ifPresent(hearing -> {
            final Integer duration = Optional.ofNullable(hearing.getHearingDuration())
                    .orElse(referenceDataVO.getHearingType().getDefaultDurationMin());
            builder.withEstimateMinutes(duration);
        });
    }

    private void configureOptionalHearingProperties(final ListHearingRequest.Builder builder, final InitialHearing initialHearing) {
        Optional.ofNullable(initialHearing).ifPresent(hearing -> {
            Optional.ofNullable(hearing.getCourtScheduleId())
                    .ifPresent(builder::withCourtScheduleId);
            Optional.ofNullable(hearing.getEndDate())
                    .map(this::getZonedDateTime)
                    .ifPresent(builder::withListedEndDateTime);
        });
    }

    private void configureDefendantProperties(final ListHearingRequest.Builder builder, 
                                            final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendants, 
                                            final UUID caseId, 
                                            final ParamsVO paramsVO) {
        Optional.ofNullable(defendants).ifPresent(defendantList -> {
            builder.withListedStartDateTime(getDateAndTimeOfHearing(defendantList))
                   .withListDefendantRequests(buildListDefendantRequest(defendantList, caseId, paramsVO.getSummonsApprovedOutcome()));
        });
    }

    List<ListHearingRequest> convertMCCWC(final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendants, final ParamsVO paramsVO) {


        final List<ListHearingRequest> hearingRequests = new ArrayList<>();
        final HearingRequest hearingRequest = paramsVO.getListNewHearing();
        if(nonNull(hearingRequest)){
            final  ListHearingRequest.Builder listhearingRequestBuilder = listHearingRequest()
                    .withCourtCentre(hearingRequest.getCourtCentre())
                    .withHearingType(hearingRequest.getHearingType())
                    .withJurisdictionType(hearingRequest.getJurisdictionType())
                    .withEstimateMinutes(hearingRequest.getEstimatedMinutes())
                    .withWeekCommencingDate(hearingRequest.getWeekCommencingDate())
                    .withEarliestStartDateTime(hearingRequest.getEarliestStartDateTime())
                    .withEstimatedDuration(hearingRequest.getEstimatedDuration())
                    .withListedStartDateTime(hearingRequest.getListedStartDateTime())
                    .withBookedSlots(hearingRequest.getBookedSlots())
                    .withBookingType(hearingRequest.getBookingType())
                    .withJudiciary(hearingRequest.getJudiciary())
                    .withNonDefaultDays(hearingRequest.getNonDefaultDays())
                    .withPriority(hearingRequest.getPriority())
                    .withSpecialRequirements(hearingRequest.getSpecialRequirements())
                    .withListDefendantRequests(
                            Optional.ofNullable(hearingRequest.getListDefendantRequests())
                                    .map(list -> list.stream()
                                            .map(external -> uk.gov.justice.core.courts.ListDefendantRequest.listDefendantRequest()
                                                    .withDatesToAvoid(external.getDatesToAvoid())
                                                    .withDefendantId(external.getDefendantId())
                                                    .withDefendantOffences(external.getDefendantOffences())
                                                    .withHearingLanguageNeeds(external.getHearingLanguageNeeds())
                                                    .withProsecutionCaseId(paramsVO.getCaseId())
                                                    .withReferralReason(external.getReferralReason())
                                                    .withSummonsApprovedOutcome(external.getSummonsApprovedOutcome())
                                                    .withSummonsRequired(external.getSummonsRequired())
                                                    .build())
                                            .collect(Collectors.toList()))
                                    .orElse(null)
                    );


            hearingRequests.add(listhearingRequestBuilder.build());
        }
        return hearingRequests;
    }

    private  ZonedDateTime getZonedDateTime(final String endDateISO) {
        if (isBlank(endDateISO)) {
            return null;
        }
        return ZonedDateTime.parse(endDateISO);
    }

    private JurisdictionType determineJurisdictionType(final Channel channel, final String oucodeL1Code) {
        LOGGER.info("Find jurisdiction type using channel {} and OU L1 code {}", channel, oucodeL1Code);

        if (nonNull(channel) && Channel.MCC.equals(channel) &&
                nonNull(oucodeL1Code) && oucodeL1Code.equals(ORGANISATION_UNIT_LEVEL_CODE_FOR_CROWN)) {
            return JurisdictionType.CROWN;
        }

        return JurisdictionType.MAGISTRATES;
    }

    private List<ListDefendantRequest> buildListDefendantRequest(final List<Defendant> defendants, final UUID caseId, final SummonsApprovedOutcome summonsApprovedOutcome) {
        return defendants.stream().map(defendant -> {
            final ListDefendantRequest.Builder builder = listDefendantRequest();

            return builder.withProsecutionCaseId(caseId)
                    .withDefendantOffences(defendant.getOffences().stream().
                            map(Offence::getOffenceId).collect(toList()))
                    .withHearingLanguageNeeds(getHearingPCFToProgressionMap().get(defendant.getHearingLanguage()))
                    .withSummonsApprovedOutcome(summonsApprovedOutcome)
                    .withDefendantId(fromString(defendant.getId()))
                    .withSummonsRequired(SummonsType.FIRST_HEARING)
                    .build();
        }).collect(toList());
    }

    private ZonedDateTime getDateAndTimeOfHearing(final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendants) {
        final Optional<Defendant> defendant = defendants.stream().filter(p -> p.getInitialHearing() != null &&
                p.getInitialHearing().getDateOfHearing() != null && p.getInitialHearing().getTimeOfHearing() != null).findAny();
        if (defendant.isPresent()) {
            final String dateTimeString = defendant.get().getInitialHearing().getDateOfHearing() + defendant.get().getInitialHearing().getTimeOfHearing();
            DateTimeFormatter dateTimeFormatterWithMilliSeconds = DateTimeFormatter.ofPattern("yyyy-MM-ddHH:mm:ss.SSS");
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-ddHH:mm:ss");

            final DateTimeFormatterBuilder dateTimeFormatterBuilder = new DateTimeFormatterBuilder();
            if (dateTimeString.length() == 18) {
                dateTimeFormatterBuilder.append(dateTimeFormatter);
            } else {
                dateTimeFormatterBuilder.append(dateTimeFormatterWithMilliSeconds);
            }
            return ZonedDateTime.parse(dateTimeString, dateTimeFormatterBuilder.toFormatter().withZone(ZoneId.of("UTC")));
        }
        return null;
    }

    private CourtCentre buildCourtCentre(final ReferenceDataVO referenceDataVO, InitialHearing initialHearing) {
        final CourtCentre.Builder courtCentreBuilder = CourtCentre.courtCentre();
        UUID roomId = null;
        String roomName = null;
        if (nonNull(initialHearing)) {
            if (nonNull(initialHearing.getRoomId())) {
                roomId = UUID.fromString(initialHearing.getRoomId());
            }
            roomName = initialHearing.getRoomName();
        }

        final Optional<OrganisationUnitWithCourtroomReferenceData> organisationUnitWithCourtroomReferenceDataOptional = referenceDataVO.getOrganisationUnitWithCourtroomReferenceData();
        if (organisationUnitWithCourtroomReferenceDataOptional.isPresent()) {
            final OrganisationUnitWithCourtroomReferenceData organisationUnitWithCourtroomReferenceData = organisationUnitWithCourtroomReferenceDataOptional.get();
            final CourtRoom courtRoom = organisationUnitWithCourtroomReferenceData.getCourtRoom();
            roomId = getRoomId(roomId, courtRoom);
            roomName = getRoomName(roomName, courtRoom);
            courtCentreBuilder.withId(fromString(organisationUnitWithCourtroomReferenceData.getId()));
            courtCentreBuilder.withName(organisationUnitWithCourtroomReferenceData.getOucodeL3Name());
            courtCentreBuilder.withWelshName(organisationUnitWithCourtroomReferenceData.getOucodeL3WelshName());
            courtCentreBuilder.withRoomId(roomId);
            courtCentreBuilder.withRoomName(roomName);
        }

        return courtCentreBuilder.build();
    }

    private UUID getRoomId(final UUID roomId, final CourtRoom courtRoom) {
        if (nonNull(roomId)) {
            return roomId;
        } else {
            if (nonNull(courtRoom)) {
                return fromString(courtRoom.getId());
            } else {
                return null;
            }
        }
    }

    private String getRoomName(final String roomName, final CourtRoom courtRoom) {
        if (nonNull(roomName)) {
            return roomName;
        } else {
            if (nonNull(courtRoom)) {
                return courtRoom.getCourtroomName();
            } else {
                return null;
            }
        }
    }

}