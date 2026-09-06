package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static com.google.common.collect.ImmutableList.of;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.core.courts.ApplicationStatus.LISTED;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase;
import static uk.gov.justice.core.courts.InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings;
import static uk.gov.justice.core.courts.LinkType.FIRST_HEARING;
import static uk.gov.justice.services.common.converter.LocalDates.to;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ParamsVO;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.event.GroupCasesParkedForApproval;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
@SuppressWarnings({"Duplicates", "duplicates"})
public class GroupCasesParkedForApprovalToCourtApplicationProceedingsConverter implements ParameterisedConverter<GroupCasesParkedForApproval, InitiateCourtApplicationProceedings, Metadata> {

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Inject
    private ProsecutionToBoxHearingRequestConverter prosecutionToBoxHearingRequestConverter;

    @Inject
    private ProsecutionCaseFileCaseDetailsToProsecutionCaseIdentifierConverter prosecutionCaseFileCaseDetailsToProsecutionCaseIdentifierConverter;

    @Inject
    private ProsecutionCaseFileOffenceToCourtApplicationOffenceConverter prosecutionCaseFileOffenceToCourtApplicationOffenceConverter;

    @Inject
    private ProsecutionCaseFileDefendantToCourtApplicationPartyConverter prosecutionCaseFileDefendantToCourtApplicationPartyConverter;

    @Inject
    private ProsecutionCaseFileProsecutorToCourtApplicationPartyConverter prosecutionCaseFileProsecutorToCourtApplicationPartyConverter;

    @Override
    public InitiateCourtApplicationProceedings convert(final GroupCasesParkedForApproval source, final Metadata metadata) {
        final Optional<GroupProsecutionWithReferenceData> masterCaseProsecution = source.getGroupProsecutionList().getGroupProsecutionWithReferenceDataList().stream()
                .filter(groupProsecutionWithReferenceData -> groupProsecutionWithReferenceData.getGroupProsecution().getIsGroupMaster())
                .findFirst();

        if (!masterCaseProsecution.isPresent()) {
            return null;
        }

        final Prosecution prosecution = Prosecution.prosecution()
                .withCaseDetails(masterCaseProsecution.get().getGroupProsecution().getCaseDetails())
                .withChannel(source.getGroupProsecutionList().getChannel())
                .withDefendants(masterCaseProsecution.get().getGroupProsecution().getDefendants())
                .build();

        return initiateCourtApplicationProceedings()
                .withBoxHearing(prosecutionToBoxHearingRequestConverter.convert(prosecution))
                .withCourtApplication(getCourtApplication(prosecution, masterCaseProsecution.get(), source.getApplicationId(), metadata))
                .withSummonsApprovalRequired(TRUE)
                .build();
    }

    private CourtApplication getCourtApplication(final Prosecution prosecution, final GroupProsecutionWithReferenceData groupProsecutionWithReferenceData, final UUID applicationId, final Metadata metadata) {
        final ParamsVO paramsVO = getParamsVO(groupProsecutionWithReferenceData.getReferenceDataVO(), prosecution);
        final CaseDetails caseDetails = prosecution.getCaseDetails();
        final List<Defendant> defendants = prosecution.getDefendants();
        final List<CourtApplicationParty> respondents = prosecutionCaseFileDefendantToCourtApplicationPartyConverter.convert(defendants, groupProsecutionWithReferenceData.getReferenceDataVO(), prosecution.getChannel());

        final CourtApplicationParty subject = respondents.get(0);
        final List<Offence> offences = defendants.stream().filter(d -> d.getId().equals(subject.getId().toString())).flatMap(defendant -> defendant.getOffences().stream()).collect(toList());
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseFileCaseDetailsToProsecutionCaseIdentifierConverter.convert(caseDetails, metadata);
        final List<uk.gov.justice.core.courts.Offence> courtApplicationOffences = prosecutionCaseFileOffenceToCourtApplicationOffenceConverter.convert(offences, paramsVO);
        final CourtApplicationParty applicant = prosecutionCaseFileProsecutorToCourtApplicationPartyConverter.convert(caseDetails.getProsecutor(), paramsVO, metadata);

        return courtApplication()
                .withId(applicationId)
                .withCourtApplicationCases(of(courtApplicationCase()
                        .withProsecutionCaseId(caseDetails.getCaseId())
                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier)
                        .withOffences(courtApplicationOffences)
                        .withIsSJP(false)
                        .withCaseStatus("ACTIVE")
                        .build()))
                .withType(getApplicationTypeForFirstHearing())
                .withApplicationReceivedDate(to(caseDetails.getDateReceived()))
                .withApplicant(applicant)
                .withSubject(subject)
                .withApplicationStatus(LISTED)
                .withRespondents(respondents)
                .withIsGroupCaseApplication(true)
                .build();
    }

    @SuppressWarnings({"Duplicates", "duplicates"})
    private ParamsVO getParamsVO(final ReferenceDataVO referenceDataVO, final Prosecution prosecution) {
        final CaseDetails caseDetails = prosecution.getCaseDetails();
        final Optional<OrganisationUnitWithCourtroomReferenceData> organisationUnitWithCourtroomReferenceData = referenceDataVO.getOrganisationUnitWithCourtroomReferenceData();

        final ParamsVO paramsVO = new ParamsVO();
        boolean isCivil = nonNull(prosecution.getIsCivil()) && prosecution.getIsCivil();
        if (isCivil) {
            paramsVO.setCivil(prosecution.getIsCivil());
        }
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setCaseId(caseDetails.getCaseId());
        paramsVO.setChannel(prosecution.getChannel());
        paramsVO.setInitiationCode(caseDetails.getInitiationCode());
        paramsVO.setReceivedFromCourtOUCode(caseDetails.getCourtReceivedFromCode());
        organisationUnitWithCourtroomReferenceData.ifPresent(organisationUnit -> paramsVO.setOucodeL1Code(organisationUnit.getOucodeL1Code()));
        return paramsVO;
    }

    private CourtApplicationType getApplicationTypeForFirstHearing() {
        final List<CourtApplicationType> applicationTypes = referenceDataQueryService.retrieveApplicationTypes();
        if (isNotEmpty(applicationTypes)) {
            return applicationTypes.stream().filter(applicationType -> applicationType.getLinkType() == FIRST_HEARING).findFirst().orElse(null);
        }
        return null;
    }
}
