package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.left;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.HearingDateTimeConstants.DATE_OF_HEARING_PATTERN;

import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.moj.cpp.prosecution.casefile.casemanagement.model.CreateGroupCaseRequest;
import uk.gov.moj.cpp.prosecution.casefile.casemanagement.model.LeadCase;
import uk.gov.moj.cpp.prosecution.casefile.casemanagement.model.ResultingIntent;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.event.GroupCasesReceived;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.GroupProsecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Builds the case management create-group-case request (CAD-1850) from the lead case of a received
 * group. Every value comes from the event, including the reference data PCF has already looked up.
 */
public class GroupCasesReceivedToCreateGroupCaseRequestConverter {

    static final int OFFENCE_TYPE_MAX_LENGTH = 250;

    /**
     * @return the request, or empty when the group has no lead case (nothing to create)
     * @throws IllegalStateException when the lead case is missing reference data the request needs
     */
    public Optional<CreateGroupCaseRequest> convert(final GroupCasesReceived groupCasesReceived) {
        final List<GroupProsecutionWithReferenceData> cases = groupCasesReceived.getGroupProsecutionList()
                .getGroupProsecutionWithReferenceDataList();

        return cases.stream()
                .filter(each -> Boolean.TRUE.equals(each.getGroupProsecution().getIsGroupMaster()))
                .findFirst()
                .map(leadCase -> toRequest(leadCase, cases.size()));
    }

    private CreateGroupCaseRequest toRequest(final GroupProsecutionWithReferenceData leadCase, final int caseCount) {
        final GroupProsecution groupProsecution = leadCase.getGroupProsecution();
        final UUID groupId = groupProsecution.getGroupId();
        final CaseDetails caseDetails = groupProsecution.getCaseDetails();
        final Defendant leadDefendant = groupProsecution.getDefendants().get(0);
        final InitialHearing initialHearing = leadDefendant.getInitialHearing();
        final ReferenceDataVO referenceData = leadCase.getReferenceDataVO();

        final OrganisationUnitWithCourtroomReferenceData courtCentre = referenceData.getOrganisationUnitWithCourtroomReferenceData()
                .orElseThrow(() -> new IllegalStateException("No court centre reference data for lead case of group " + groupId));
        final ProsecutorsReferenceData prosecutor = Optional.ofNullable(referenceData.getProsecutorsReferenceData())
                .orElseThrow(() -> new IllegalStateException("No prosecutor reference data for lead case of group " + groupId));

        return new CreateGroupCaseRequest()
                .groupId(groupId)
                .leadCase(new LeadCase()
                        .leadCaseId(caseDetails.getCaseId())
                        .caseUrn(caseDetails.getProsecutorCaseReference())
                        .defendantName(defendantName(leadDefendant)))
                .dateOfHearing(LocalDate.parse(initialHearing.getDateOfHearing(), DATE_OF_HEARING_PATTERN))
                .caseCount(caseCount)
                .offenceType(offenceType(leadDefendant, referenceData))
                .courtHearingLocation(initialHearing.getCourtHearingLocation())
                .courtCentreId(UUID.fromString(courtCentre.getId()))
                .courtroomId(CourtRoomIdResolver.resolveRoomId(submittedRoomId(initialHearing), courtCentre.getCourtRoom()))
                .prosecutingAuthority(prosecutor.getOucode())
                .prosecutingAuthorityName(prosecutor.getFullName())
                .resultingIntent(ResultingIntent.GRANT);
    }

    private static String defendantName(final Defendant defendant) {
        if (nonNull(defendant.getIndividual()) && nonNull(defendant.getIndividual().getPersonalInformation())) {
            final PersonalInformation personalInformation = defendant.getIndividual().getPersonalInformation();
            return Stream.of(personalInformation.getFirstName(), personalInformation.getLastName())
                    .filter(part -> isNotBlank(part))
                    .map(String::trim)
                    .collect(Collectors.joining(" "));
        }
        return defendant.getOrganisationName();
    }

    /**
     * The reference-data offence title for the lead defendant's first offence code; the code itself
     * when no title is found. Cut to the 250 characters case management stores.
     */
    private static String offenceType(final Defendant defendant, final ReferenceDataVO referenceData) {
        final String offenceCode = defendant.getOffences().get(0).getOffenceCode();
        final List<OffenceReferenceData> offenceReferenceData = referenceData.getOffenceReferenceData();
        final String offenceType = isNull(offenceReferenceData) ? offenceCode : offenceReferenceData.stream()
                .filter(each -> nonNull(offenceCode) && offenceCode.equals(each.getCjsOffenceCode()))
                .map(OffenceReferenceData::getTitle)
                .filter(title -> isNotBlank(title))
                .findFirst()
                .orElse(offenceCode);
        return left(offenceType, OFFENCE_TYPE_MAX_LENGTH);
    }

    private static UUID submittedRoomId(final InitialHearing initialHearing) {
        return isNotBlank(initialHearing.getRoomId()) ? UUID.fromString(initialHearing.getRoomId()) : null;
    }
}
