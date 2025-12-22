package uk.gov.moj.cpp.prosecution.casefile.event.listener;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;

import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.json.schemas.prosecutioncasefile.events.DefendantValidationPassed;
import uk.gov.moj.cpp.prosecution.casefile.event.CaseValidationFailed;
import uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceived;
import uk.gov.moj.cpp.prosecution.casefile.event.DefendantValidationFailed;
import uk.gov.moj.cpp.prosecution.casefile.event.ProsecutionDefendantsAdded;
import uk.gov.moj.cpp.prosecution.casefile.event.SjpValidationFailed;
import uk.gov.moj.cpp.prosecution.casefile.event.SpiProsecutionDefendantsAdded;
import uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.DefendantToDefendantDetails;
import uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.ProsecutionReceivedToCase;
import uk.gov.moj.cpp.prosecution.casefile.event.listener.model.ErrorCaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorCaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.event.listener.model.ErrorDefendantDetails;
import uk.gov.moj.cpp.prosecution.casefile.event.listener.model.ErrorOffenceDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.ResolvedCases;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorCaseDetailsRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.CaseDetailsRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.ResolvedCasesRepository;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ResolvedCase;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

@ServiceComponent(EVENT_LISTENER)
@SuppressWarnings("squid:CallToDeprecatedMethod")
public class ProsecutionReceivedListener {

    @Inject
    private ProsecutionReceivedToCase prosecutionReceivedToCaseConverter;

    @Inject
    private DefendantToDefendantDetails defendantToDefendantDetail;

    @Inject
    private CaseDetailsRepository caseDetailsRepository;

    @Inject
    private BusinessValidationErrorRepository businessValidationErrorRepository;

    @Inject
    private BusinessValidationErrorCaseDetailsRepository businessValidationErrorCaseDetailsRepository;

    @Inject
    private ResolvedCasesRepository resolvedCasesRepository;

    @Inject
    protected ObjectToJsonObjectConverter objectToJsonObjectConverter;

    private final ObjectMapper mapper= new ObjectMapperProducer().objectMapper();

    private static final Logger LOGGER = getLogger(ProsecutionReceivedListener.class);

    @Handles("prosecutioncasefile.events.cc-case-received")
    public void prosecutionReceived(final Envelope<CcCaseReceived> envelope) {
        final CcCaseReceived ccCaseReceived = envelope.payload();
        final CaseDetails caseDetails = prosecutionReceivedToCaseConverter.convert(ccCaseReceived.getProsecutionWithReferenceData().getProsecution());
        caseDetailsRepository.save(caseDetails);
        caseDetails.getDefendants().forEach(defendant -> {
            businessValidationErrorRepository.deleteByCaseIdAndDefendantId(caseDetails.getCaseId(), fromString(defendant.getDefendantId()));
            if (nonNull(defendant.getPersonalInformation())) {
                businessValidationErrorRepository.deleteByCaseIdAndFirstNameAndLastName(caseDetails.getCaseId(), defendant.getPersonalInformation().getFirstName(), defendant.getPersonalInformation().getLastName());
            } else if (nonNull(defendant.getOrganisationInformation())) {
                businessValidationErrorRepository.deleteByCaseIdAndOrganisationName(caseDetails.getCaseId(), defendant.getOrganisationInformation().getOrganisationName());
            }
        });
        deleteErrorCaseDetails(caseDetails.getCaseId());
    }

    @Handles("prosecutioncasefile.events.spi-prosecution-defendants-added")
    public void defendantReceived(final Envelope<SpiProsecutionDefendantsAdded> envelope) {
        final SpiProsecutionDefendantsAdded prosecutionDefendantsAdded = envelope.payload();
        final CaseDetails caseDetails = caseDetailsRepository.findBy(prosecutionDefendantsAdded.getCaseId());
        caseDetails.getDefendants().addAll(getDefendantDetails(prosecutionDefendantsAdded.getDefendants()));
        caseDetails.getDefendants().forEach(x -> businessValidationErrorRepository.deleteByDefendantId(fromString(x.getDefendantId())));
        deleteErrorCaseDetails(prosecutionDefendantsAdded.getCaseId());

    }

    @Handles("prosecutioncasefile.events.prosecution-defendants-added")
    public void prosecutionDefendantsAddedReceived(final Envelope<ProsecutionDefendantsAdded> envelope) {
        final ProsecutionDefendantsAdded prosecutionDefendantsAdded = envelope.payload();
        if (SPI.equals(prosecutionDefendantsAdded.getChannel())) {
            final CaseDetails caseDetails = caseDetailsRepository.findBy(prosecutionDefendantsAdded.getCaseId());
            caseDetails.getDefendants().addAll(getDefendantDetails(prosecutionDefendantsAdded.getDefendants()));
            caseDetails.getDefendants().forEach(details -> businessValidationErrorRepository.deleteByDefendantId(fromString(details.getDefendantId())));
            deleteErrorCaseDetails(prosecutionDefendantsAdded.getCaseId());
        }
    }

    @Handles("prosecutioncasefile.events.validation-completed")
    public void processValidationCompleted(final JsonEnvelope envelope) {
        final long version = envelope.metadata().eventNumber().orElse(0L);
        businessValidationErrorRepository.findByCaseId(fromString(envelope.payloadAsJsonObject().getString("caseId"))).forEach(x -> x.setVersion(version));

    }

    @Handles("prosecutioncasefile.events.sjp-validation-failed")
    public void sjpCaseValidationFailed(final Envelope<SjpValidationFailed> envelope) {
        final SjpValidationFailed sjpValidationFailed = envelope.payload();
        final Prosecution prosecution = sjpValidationFailed.getProsecution();

        if (SPI == prosecution.getChannel()) {
            final long version = envelope.metadata().eventNumber().orElse(0L);

            businessValidationErrorRepository.deleteByCaseId(prosecution.getCaseDetails().getCaseId());
            businessValidationErrorCaseDetailsRepository.deleteByCaseId(prosecution.getCaseDetails().getCaseId());

            final String urn = sjpValidationFailed.getProsecution().getCaseDetails().getProsecutorCaseReference();
            final UUID caseId = sjpValidationFailed.getProsecution().getCaseDetails().getCaseId();
            final List<BusinessValidationErrorDetails> businessValidationErrorDetails = new ArrayList<>();
            final String caseType = prosecution.getCaseDetails().getInitiationCode();

            sjpValidationFailed.getCaseProblems().forEach(problem ->
                    businessValidationErrorDetails.addAll(buildBusinessErrorCaseDetailsToPersist(prosecution, problem, version, sjpValidationFailed.getInitialHearing())));

            sjpValidationFailed.getDefendantProblemsVO().forEach(defendantProblemsVO ->
                    defendantProblemsVO.getProblems().forEach(problem ->
                            businessValidationErrorDetails.addAll(buildBusinessErrorDetailsToPersist(defendantProblemsVO.getDefendant(), urn, caseId, problem, version, caseType))
                    ));

            businessValidationErrorDetails.forEach(businessValidationErrorEntity ->
                    businessValidationErrorRepository.save(businessValidationErrorEntity));
            saveBusinessCaseDetails(caseId, prosecution.getDefendants().get(0));
        }
    }

    @Handles("prosecutioncasefile.events.case-validation-failed")
    public void caseValidationFailed(final Envelope<CaseValidationFailed> envelope) {
        final CaseValidationFailed caseValidationFailed = envelope.payload();
        final Prosecution prosecution = caseValidationFailed.getProsecution();
        final long version = envelope.metadata().eventNumber().orElse(0L);

        // clear all old violations
        businessValidationErrorRepository.findByCaseId(prosecution.getCaseDetails().getCaseId()).stream()
                .filter(x -> x.getDefendantId() == null)
                .forEach(x -> businessValidationErrorRepository.remove(x));

        caseValidationFailed.getProblems().forEach(problem -> {
                    final List<BusinessValidationErrorDetails> businessValidationErrorCaseDetails = buildBusinessErrorCaseDetailsToPersist(prosecution, problem, version, caseValidationFailed.getInitialHearing());
                    businessValidationErrorCaseDetails.forEach(businessValidationErrorCaseEntity ->
                            businessValidationErrorRepository.save(businessValidationErrorCaseEntity));
                }
        );
        if(prosecution.getDefendants() !=null && !prosecution.getDefendants().isEmpty()) {
            saveBusinessCaseDetails(prosecution.getCaseDetails().getCaseId(), prosecution.getDefendants().get(0));
        }

    }

    public void saveBusinessValidationErrorCaseDetails(ErrorCaseDetails errorCaseDetails) {
        final BusinessValidationErrorCaseDetails businessValidationErrorCaseDetails =
                new BusinessValidationErrorCaseDetails(errorCaseDetails.getCaseId(), objectToJsonObjectConverter.convert(errorCaseDetails).toString());
        businessValidationErrorCaseDetailsRepository.save(businessValidationErrorCaseDetails);
    }

    public void deleteErrorCaseDetails(UUID caseId) {
        final List<BusinessValidationErrorDetails> errorDetails = businessValidationErrorRepository.findByCaseId(caseId);
        if (errorDetails.isEmpty()) {
            businessValidationErrorCaseDetailsRepository.deleteByCaseId(caseId);
        }
    }


    private ErrorDefendantDetails createDefendantDetails(Defendant defendant) {
        final List<ErrorOffenceDetails> offences = defendant.getOffences().stream()
                .map(offence -> new ErrorOffenceDetails(offence.getOffenceId(), offence.getOffenceWording()))
                .collect(Collectors.toList());
        return new ErrorDefendantDetails(defendant.getId(), getDefendantFirstName(defendant), getDefendantLastName(defendant), defendant.getOrganisationName(), offences);
    }

    private ErrorDefendantDetails createDefendantDetails(DefendantDetails defendant) {
        final List<ErrorOffenceDetails> offences = defendant.getOffences().stream()
                .map(offence -> new ErrorOffenceDetails(offence.getOffenceId(), offence.getOffenceWording()))
                .collect(Collectors.toList());
        return new ErrorDefendantDetails(defendant.getDefendantId(), getDefendantFirstName(defendant), getDefendantLastName(defendant), getOrganisationName(defendant), offences);
    }

    private List<BusinessValidationErrorDetails> buildBusinessErrorCaseDetailsToPersist(final Prosecution prosecution, final Problem problem, long version, final InitialHearing initialHearing) {

        final List<BusinessValidationErrorDetails> businessValidationErrorCaseDetails = new ArrayList<>();

        problem.getValues().forEach(problemValue -> businessValidationErrorCaseDetails.
                add(buildBusinessValidationErrorCaseDetailsEntity(prosecution, problem, problemValue, version, initialHearing)));

        return businessValidationErrorCaseDetails;
    }

    private BusinessValidationErrorDetails buildBusinessValidationErrorCaseDetailsEntity(Prosecution prosecution, Problem problem, ProblemValue problemValue, long version, final InitialHearing initialHearing) {

        LocalDate defendantHearingLocalDate = null;
        try {
            if (initialHearing != null && initialHearing.getDateOfHearing() != null) {
                defendantHearingLocalDate = LocalDate.parse(initialHearing.getDateOfHearing());
            }
        } catch (DateTimeParseException e) {
            // in case of invalid date, store null value
            LOGGER.error("Unable to parse date of hearing", e);
        }

        final BusinessValidationErrorDetails businessValidationErrorDetails = new BusinessValidationErrorDetails(randomUUID(),
                problemValue.getValue(), //error value
                problemValue.getId(), //field id
                problem.getCode(), //diplayname
                prosecution.getCaseDetails().getCaseId(),
                null, //defendant id
                problemValue.getKey(), //field name
                null, // court name,
                initialHearing != null ? initialHearing.getCourtHearingLocation() : null, // court location
                prosecution.getCaseDetails().getInitiationCode(), //case type
                prosecution.getCaseDetails().getProsecutorCaseReference(), //ptiurn
                null, //bail status
                null, //first name
                null, // last name
                null,
                null, //charge date
                defendantHearingLocalDate, //hearing date,
                null
        );

        businessValidationErrorDetails.setVersion(version);
        return businessValidationErrorDetails;
    }

    @Handles("prosecutioncasefile.events.defendant-validation-failed")
    public void defendantsValidationFailed(final Envelope<DefendantValidationFailed> envelope) {
        final long version = envelope.metadata().eventNumber().orElse(0l);
        final DefendantValidationFailed defendantValidationFailed = envelope.payload();
        final Defendant defendant = defendantValidationFailed.getDefendant();
        final String caseType = defendantValidationFailed.getCaseType();

        // clear all old violations of defendant
        businessValidationErrorRepository.deleteByDefendantId(fromString(defendant.getId()));

        defendantValidationFailed.getProblems().forEach(problem -> {
            final List<BusinessValidationErrorDetails> businessValidationErrorDetails = buildBusinessErrorDetailsToPersist(defendant, defendantValidationFailed.getUrn(), defendantValidationFailed.getCaseId(), problem, version, caseType);
            businessValidationErrorDetails.forEach(businessValidationErrorEntity ->
                    businessValidationErrorRepository.save(businessValidationErrorEntity));
        });
        saveBusinessCaseDetails(defendantValidationFailed.getCaseId(), defendantValidationFailed.getDefendant());

    }

    @Handles("prosecutioncasefile.events.defendant-validation-passed")
    public void defendantsValidationPassed(final Envelope<DefendantValidationPassed> envelope) {
        final DefendantValidationPassed defendantValidationFailedPassed = envelope.payload();
        businessValidationErrorRepository.deleteByDefendantId(defendantValidationFailedPassed.getDefendantId());
        if (defendantValidationFailedPassed.getCaseId() != null) {
            deleteErrorCaseDetails(defendantValidationFailedPassed.getCaseId());
        }
    }

    @Handles("prosecutioncasefile.event.resolved-case")
    public void resolvedCaseErrorsReceived(final Envelope<ResolvedCase> envelope) {
        final ResolvedCase resolvedCase = envelope.payload();
        final ResolvedCases resolvedCases = this.createResolvedCases(randomUUID(), resolvedCase.getCaseId(), LocalDate.now(), resolvedCase.getRegion(),
                resolvedCase.getCourtLocation(), resolvedCase.getCaseType());
        resolvedCasesRepository.save(resolvedCases);
    }

    private void saveBusinessCaseDetails(UUID caseId, Defendant defendant) {
        ErrorCaseDetails errorCaseDetails;
        final List<BusinessValidationErrorCaseDetails> caseErrors = businessValidationErrorCaseDetailsRepository.findByCaseId(caseId);
        if (caseErrors == null || caseErrors.isEmpty()) {
            errorCaseDetails = new ErrorCaseDetails(caseId, new ArrayList<>());
            errorCaseDetails.addDefendant(createDefendantDetails(defendant));
        } else {
            final BusinessValidationErrorCaseDetails businessValidationErrorCaseDetails = caseErrors.get(0);
            final JsonObject jsonObject = new StringToJsonObjectConverter().convert(businessValidationErrorCaseDetails.getCaseDetails());
            errorCaseDetails = new JsonObjectToObjectConverter(mapper).convert(jsonObject, ErrorCaseDetails.class);
            errorCaseDetails.addDefendant(createDefendantDetails(defendant));
        }
        final CaseDetails caseDetails = caseDetailsRepository.findBy(caseId);
        if (caseDetails != null) {
            errorCaseDetails.addAllDefendant(caseDetails.getDefendants().stream().map(this::createDefendantDetails).collect(Collectors.toList()));
        }
        saveBusinessValidationErrorCaseDetails(errorCaseDetails);
    }

    private List<BusinessValidationErrorDetails> buildBusinessErrorDetailsToPersist(final Defendant defendant, final String urn, final UUID caseId, final Problem problem, long version, final String caseType) {

        final List<BusinessValidationErrorDetails> businessValidationErrorDetails = new ArrayList<>();

        problem.getValues().forEach(problemValue -> businessValidationErrorDetails.
                add(buildBusinessValidationErrorDetailsEntity(problem, problemValue, urn, caseId, defendant, version, caseType)));

        return businessValidationErrorDetails;
    }

    private BusinessValidationErrorDetails buildBusinessValidationErrorDetailsEntity(final Problem problem, final ProblemValue problemValue, final String urn, final UUID caseId, final Defendant defendant, long version, final String caseType) {
        final BusinessValidationErrorDetails businessValidationErrorDetails = new BusinessValidationErrorDetails(randomUUID(),
                problemValue.getValue(), //error value
                problemValue.getId(), //field id
                problem.getCode(), //diplayname
                caseId,
                fromString(defendant.getId()), //defendant id
                problemValue.getKey(), //field name
                null, // court name,
                defendant.getInitialHearing() != null ? defendant.getInitialHearing().getCourtHearingLocation() : null,
                caseType, //case type
                urn, //ptiurn
                defendant.getCustodyStatus(), //bail status,
                getDefendantFirstName(defendant), //firstName
                getDefendantLastName(defendant), //lastName
                defendant.getOrganisationName(), //organisationName
                defendant.getOffences().stream().findAny().orElseGet(() -> Offence.offence().build()).getChargeDate(), //charge date
                defendant.getInitialHearing() != null ? LocalDate.parse(defendant.getInitialHearing().getDateOfHearing()) : null, //hearing date,
                getDateOfBirth(defendant)
        );
        businessValidationErrorDetails.setVersion(version);
        return businessValidationErrorDetails;
    }


    private LocalDate getDateOfBirth(final Defendant defendant) {
        LocalDate dateOfBirth = null;
        if (defendant.getIndividual() != null && defendant.getIndividual().getSelfDefinedInformation() != null) {
            dateOfBirth = defendant.getIndividual().getSelfDefinedInformation().getDateOfBirth();
        }
        return dateOfBirth;
    }

    private String getDefendantFirstName(final Defendant defendant) {
        return defendant.getIndividual() != null ? defendant.getIndividual().getPersonalInformation().getFirstName() : null;
    }

    private String getDefendantLastName(final Defendant defendant) {
        return defendant.getIndividual() != null ? defendant.getIndividual().getPersonalInformation().getLastName() : null;
    }

    private String getDefendantFirstName(final DefendantDetails defendant) {
        return defendant.getPersonalInformation() != null ? defendant.getPersonalInformation().getFirstName() : null;
    }

    private String getDefendantLastName(final DefendantDetails defendant) {
        return defendant.getPersonalInformation() != null ? defendant.getPersonalInformation().getLastName() : null;
    }

    private String getOrganisationName(final DefendantDetails defendant) {
        return defendant.getOrganisationInformation() != null ? defendant.getOrganisationInformation().getOrganisationName() : null;
    }

    private Set<DefendantDetails> getDefendantDetails(final List<Defendant> defendants) {
        return defendants.stream().map(defendantToDefendantDetail::convert).collect(Collectors.toSet());
    }

    private ResolvedCases createResolvedCases(final UUID id,
                                              final UUID caseId,
                                              final LocalDate localDate,
                                              final String region,
                                              final String courtLocation,
                                              final String caseType) {
        final ResolvedCases resolvedCases = new ResolvedCases();

        resolvedCases.setCaseId(caseId);
        resolvedCases.setId(id);
        resolvedCases.setResolutionDate(localDate);
        resolvedCases.setRegion(region);
        resolvedCases.setCourtLocation(courtLocation);
        resolvedCases.setCaseType(caseType);
        return resolvedCases;
    }
}
