package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.Objects.nonNull;
import static uk.gov.justice.core.courts.ApplicationExternalCreatorType.PROSECUTOR;
import static uk.gov.justice.core.courts.ApplicationStatus.DRAFT;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings;

import uk.gov.justice.core.courts.BoxHearingRequest;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper.ApplicantMapper;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper.CodeCentreMapper;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper.CourtApplicationCaseMapper;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper.CourtApplicationPaymentMapper;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper.JurisdictionTypeMapper;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper.RespondentMapper;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper.SubjectMapper;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper.ThirdPartyMapper;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmitApplicationAccepted;

import java.time.LocalDate;

public class ApplicationAcceptedToCourtApplicationProceedingsConverter implements Converter<SubmitApplicationAccepted, InitiateCourtApplicationProceedings> {

    @Override
    public InitiateCourtApplicationProceedings convert(final SubmitApplicationAccepted source) {
        return initiateCourtApplicationProceedings()
                .withCourtApplication(toCourtApplication(source.getCourtApplication(), source.getEnrichedApplicationData().getCourtApplicationType()))
                .withBoxHearing(toBoxHearing(source.getBoxHearingRequest()))
                .build();
    }

    private CourtApplication toCourtApplication(final uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication sourceCourtApplication, final CourtApplicationType courtApplicationType) {
        return courtApplication()
                .withId(sourceCourtApplication.getId())
                .withApplicationStatus(DRAFT)
                .withApplicationExternalCreatorType(PROSECUTOR)
                .withApplicationReceivedDate(LocalDate.now().toString())
                .withCourtApplicationCases(CourtApplicationCaseMapper.convertCourtApplicationCase.apply(sourceCourtApplication.getCourtApplicationCases()))
                .withType(courtApplicationType)
                .withApplicationParticulars(sourceCourtApplication.getApplicationParticulars())
                .withCourtApplicationPayment(CourtApplicationPaymentMapper.convertCourtApplicationPayment.apply(sourceCourtApplication.getCourtApplicationPayment()))
                .withApplicationDecisionSoughtByDate(getApplicationDecisionSoughtByDate(sourceCourtApplication.getApplicationDecisionSoughtByDate()))
                .withApplicant(ApplicantMapper.convertApplicant.apply(sourceCourtApplication.getApplicant()))
                .withRespondents(RespondentMapper.convertRespondent.apply(sourceCourtApplication.getRespondents()))
                .withThirdParties(ThirdPartyMapper.convertThirdParty.apply(sourceCourtApplication.getThirdParties()))
                .withOutOfTimeReasons(sourceCourtApplication.getOutOfTimeReason())
                .withSubject(SubjectMapper.assignSubject.apply(sourceCourtApplication))
                .build();
    }

    private String getApplicationDecisionSoughtByDate(final LocalDate applicationDecisionSoughtByDate) {
        if (nonNull(applicationDecisionSoughtByDate)) {
            return applicationDecisionSoughtByDate.toString();
        }
        return null;
    }

    private BoxHearingRequest toBoxHearing(final uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.BoxHearingRequest sourceBoxHearingRequest) {
        return BoxHearingRequest.boxHearingRequest()
                .withId(sourceBoxHearingRequest.getId())
                .withCourtCentre(CodeCentreMapper.convertCourtCentre.apply(sourceBoxHearingRequest.getCourtCentre()))
                .withJurisdictionType(JurisdictionTypeMapper.convertJurisdictionType.apply(sourceBoxHearingRequest.getJurisdictionType()))
                .withApplicationDueDate(localDateToString(sourceBoxHearingRequest.getApplicationDueDate()))
                .withSendAppointmentLetter(sourceBoxHearingRequest.getSendAppointmentLetter())
                .build();
    }

    private String localDateToString(final LocalDate localDate) {
        return nonNull(localDate) ? localDate.toString() : null;
    }
}
