package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static com.google.common.collect.ImmutableList.of;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.core.courts.ApplicationStatus.LISTED;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase;
import static uk.gov.justice.core.courts.InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings;
import static uk.gov.justice.services.common.converter.LocalDates.to;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationPayment;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCivilApplication;
import uk.gov.justice.core.courts.FeeStatus;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.domain.ParamsVO;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.event.DefendantsParkedForSummonsApplicationApproval;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"Duplicates", "duplicates"})
public class DefendantsParkedToCourtApplicationProceedingsConverter implements ParameterisedConverter<DefendantsParkedForSummonsApplicationApproval, InitiateCourtApplicationProceedings, Metadata> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantsParkedToCourtApplicationProceedingsConverter.class);

    public static final UUID FIRST_HEARING_APPLICATION_ID = UUID.fromString("bfa61811-b917-3bce-9cc1-7ea8e554bd3b");

    private static final String DEFAULT_CIVIL_FEE_STATUS = "OUTSTANDING";
    private static final String DEFAULT_CC_FEE_STATUS = "NOT_APPLICABLE";

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
    public InitiateCourtApplicationProceedings convert(final DefendantsParkedForSummonsApplicationApproval source, final Metadata metadata) {
        return initiateCourtApplicationProceedings()
                .withBoxHearing(prosecutionToBoxHearingRequestConverter.convert(source.getProsecutionWithReferenceData().getProsecution()))
                .withCourtApplication(getCourtApplication(source, metadata))
                .withSummonsApprovalRequired(TRUE)
                .build();
    }

    private CourtApplication getCourtApplication(final DefendantsParkedForSummonsApplicationApproval source, final Metadata metadata) {
        final Prosecution prosecution = source.getProsecutionWithReferenceData().getProsecution();
        final ReferenceDataVO referenceDataVO = source.getProsecutionWithReferenceData().getReferenceDataVO();
        final ParamsVO paramsVO = getParamsVO(referenceDataVO, prosecution);
        final CaseDetails caseDetails = prosecution.getCaseDetails();
        final List<Defendant> defendants = prosecution.getDefendants();
        final List<CourtApplicationParty> respondents = prosecutionCaseFileDefendantToCourtApplicationPartyConverter.convert(defendants, referenceDataVO, prosecution.getChannel());

        final CourtApplicationParty subject = respondents.get(0);
        final List<Offence> offences = defendants.stream().filter(d -> d.getId().equals(subject.getId().toString())).flatMap(defendant -> defendant.getOffences().stream()).collect(toList());
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseFileCaseDetailsToProsecutionCaseIdentifierConverter.convert(caseDetails, metadata);
        final List<uk.gov.justice.core.courts.Offence> courtApplicationOffences = prosecutionCaseFileOffenceToCourtApplicationOffenceConverter.convert(offences, paramsVO);
        final CourtApplicationParty applicant = prosecutionCaseFileProsecutorToCourtApplicationPartyConverter.convert(caseDetails.getProsecutor(), paramsVO, metadata);

        CourtApplicationPayment courtApplicationPayment;

        String initialFeePaymentRef = caseDetails.getPaymentReference();
        boolean isCivil = nonNull(prosecution.getIsCivil()) && prosecution.getIsCivil();
        String initialFeeStatus = getInitialFeeStatus(caseDetails.getFeeStatus(), initialFeePaymentRef, isCivil);
        courtApplicationPayment = CourtApplicationPayment
                    .courtApplicationPayment()
                    .withFeeStatus(FeeStatus.valueOf(initialFeeStatus))
                    .withPaymentReference(initialFeePaymentRef)
                    .build();
        return courtApplication()
                .withId(source.getApplicationId())
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
                .withId(source.getApplicationId())
                .withCourtApplicationPayment(courtApplicationPayment)
                .withCourtCivilApplication(CourtCivilApplication.courtCivilApplication()
                        .withIsCivil(isCivil)
                        .build())
                .build();
    }

    private static String getInitialFeeStatus(final String feeStatus, String paymentReference, final boolean isCivil) {
        if (feeStatus != null) {
            return feeStatus;
        } else if (paymentReference != null) {
            return FeeStatus.SATISFIED.name();
        }
        return isCivil ? DEFAULT_CIVIL_FEE_STATUS : DEFAULT_CC_FEE_STATUS;
    }

    @SuppressWarnings({"Duplicates", "duplicates"})
    private ParamsVO getParamsVO(final ReferenceDataVO referenceDataVO, final Prosecution prosecution) {
        final CaseDetails caseDetails = prosecution.getCaseDetails();
        final Optional<OrganisationUnitWithCourtroomReferenceData> organisationUnitWithCourtroomReferenceData = referenceDataVO.getOrganisationUnitWithCourtroomReferenceData();

        final ParamsVO paramsVO = new ParamsVO();
        if(nonNull(prosecution.getIsCivil()) && prosecution.getIsCivil()){
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

    @SuppressWarnings("squid:S2629")
    private CourtApplicationType getApplicationTypeForFirstHearing() {
        final CourtApplicationType applicationType = referenceDataQueryService.getApplicationType(FIRST_HEARING_APPLICATION_ID);
        if (isNull(applicationType)) {
            LOGGER.error(String.format("Reference data service returned FIRST HEARING application type is null -->>%s", FIRST_HEARING_APPLICATION_ID));
            throw new IllegalStateException("FIRST HEARING application type cannot be null");
        }
        return applicationType;
    }

}
