package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static com.google.common.base.Strings.nullToEmpty;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.justice.core.courts.OffenceActiveOrder.OFFENCE;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Address.address;
import static uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.AssociatedPerson.associatedPerson;
import static uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.ContactNumber.contactNumber;
import static uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication.courtApplication;
import static uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Organisation.organisation;
import static uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Person.person;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.AssociatedPerson;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.ContactNumber;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplicationCase;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplicationType;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Ethnicity;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Gender;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.HearingLanguage;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Marker;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Organisation;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Person;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Respondent;
import uk.gov.moj.cpp.prosecution.casefile.domain.AdditionalInformation;
import uk.gov.moj.cpp.prosecution.casefile.validation.SubmitApplicationValidator;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.SubmitApplication;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S1168")
@ServiceComponent(COMMAND_HANDLER)
public class SubmitApplicationHandler extends AbstractCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmitApplicationHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private SubmitApplicationValidator submitApplicationValidator;

    @Handles("prosecutioncasefile.command.submit-application")
    public void submitCCApplication(final Envelope<SubmitApplication> submitApplicationEnvelope) throws EventStreamException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("prosecutioncasefile.command.submit-application with PocaFileId: {} and CourtApplicationId: {}", submitApplicationEnvelope.payload().getPocaFileId(), submitApplicationEnvelope.payload().getCourtApplication().getId());
        }

        final SubmitApplication sourceSubmitApplication = submitApplicationEnvelope.payload();

        final AdditionalInformation additionalInformation = new AdditionalInformation(sourceSubmitApplication.getProsecutionCases(), sourceSubmitApplication.getApplicationTypes(), sourceSubmitApplication.getCourtroomReferenceData());

        final EventStream eventStream = eventSource.getStreamById(sourceSubmitApplication.getCourtApplication().getId());
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);

        final List<Respondent> enrichedRespondentList = enrichAndMatchRespondents(sourceSubmitApplication);

        final uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication submitApplication = uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication.submitApplication()
                .withBoxHearingRequest(sourceSubmitApplication.getBoxHearingRequest())
                .withProsecutionCases(sourceSubmitApplication.getProsecutionCases())
                .withCourtApplication(courtApplication()
                        .withValuesFrom(sourceSubmitApplication.getCourtApplication())
                        .withRespondents(enrichedRespondentList)
                        .withCourtApplicationCases(getEnrichedCourtApplicationCases(sourceSubmitApplication.getCourtApplication(), additionalInformation, enrichedRespondentList))
                        .build())
                .withPocaFileId(sourceSubmitApplication.getPocaFileId())
                .withSenderEmail(sourceSubmitApplication.getSenderEmail())
                .withEmailSubject(sourceSubmitApplication.getEmailSubject())
                .build();

        appendEventsToStream(submitApplicationEnvelope, eventStream,
                applicationAggregate.acceptSubmitApplication(submitApplication, submitApplicationValidator, additionalInformation));
    }

    private List<Respondent> enrichAndMatchRespondents(final SubmitApplication sourceSubmitApplication) {
        if (isNull(sourceSubmitApplication.getCourtApplication().getRespondents())) {
            return null;
        }
        return sourceSubmitApplication.getCourtApplication().getRespondents().stream()
                .map(respondent -> {
                    final List<Respondent> respList = ofNullable(sourceSubmitApplication.getProsecutionCases())
                            .orElseGet(Collections::emptyList)
                            .stream().filter(Objects::nonNull)
                            .map(prosecutionCase -> prosecutionCase.getDefendants().stream()
                                    .map(defendant -> {
                                                if (nonNull(respondent.getAsn())) {
                                                    return doMatchingForPoliceCase(defendant, respondent, prosecutionCase);
                                                } else if (nonNull(respondent.getCpsDefendantId())) {
                                                    return doMatchingForNonPoliceCase(defendant, respondent, prosecutionCase);
                                                }
                                                return null;
                                            }
                                    ).filter(Objects::nonNull).collect(Collectors.toList()))
                            .flatMap(Collection::stream).filter(Objects::nonNull).collect(Collectors.toList());

                    return getRespondent(respondent, respList);

                }).collect(Collectors.toList());
    }

    private Respondent getRespondent(final Respondent respondent, final List<Respondent> respList) {
        if (isEmpty(respList)) {
            return getUnMatchedRespondent(respondent);
        } else if (respList.size() > 1) {
            return getMultiMatchedRespondent(respList.get(0));
        } else {
            return respList.get(0);
        }
    }

    private Respondent getMultiMatchedRespondent(final Respondent respondent) {
        return Respondent.respondent().withValuesFrom(respondent)
                .withIsMultipleDefendantMatched(true)
                .build();
    }

    @SuppressWarnings({"squid:S1172", "squid:S1481", "squid:S1854", "squid:S1854"})
    private List<CourtApplicationCase> getEnrichedCourtApplicationCases(final CourtApplication courtApplication, final AdditionalInformation additionalInformation, final List<Respondent> enrichedRespondentList) {
        if (isNull(additionalInformation.getProsecutionCases())) {
            return courtApplication.getCourtApplicationCases();
        }

        final uk.gov.justice.core.courts.CourtApplicationType courtApplicationType = findCourtApplicationType(courtApplication.getCourtApplicationType(), additionalInformation.getApplicationTypes());

        return courtApplication.getCourtApplicationCases().stream()
                .map(courtApplicationCase -> {

                            final ProsecutionCase prosecutionCase = findProsecutionCaseWithUrn(additionalInformation, courtApplicationCase);

                            return CourtApplicationCase.courtApplicationCase()
                                    .withValuesFrom(courtApplicationCase)
                                    .withProsecutionCaseId(prosecutionCase.getId())
                                    .withCaseStatus(prosecutionCase.getCaseStatus())
                                    .withIsSJP(false)
                                    .withOffences(getOffences(courtApplicationType, enrichedRespondentList, prosecutionCase))
                                    .withProsecutionCaseIdentifier(getProsecutionCaseIdentifier(courtApplicationCase, additionalInformation.getProsecutionCases()))
                                    .build();
                        }
                )
                .collect(Collectors.toList());
    }

    public List<Offence> getOffences(final uk.gov.justice.core.courts.CourtApplicationType courtApplicationType, final List<Respondent> enrichedRespondentList, final ProsecutionCase prosecutionCase) {
        if (needToSendOffence(courtApplicationType, prosecutionCase.getCaseStatus())) {
            final List<Offence> offences = ofNullable(prosecutionCase.getDefendants()).orElse(emptyList()).stream()
                    .filter(defendant ->
                            ofNullable(enrichedRespondentList).orElse(emptyList()).stream()
                                    .filter(Respondent::getIsSubject)
                                    .filter(respondent -> nonNull(respondent.getDefendantId()))
                                    .anyMatch(respondent -> respondent.getDefendantId().equals(defendant.getId()))
                    ).flatMap(defendant -> defendant.getOffences().stream()).collect(Collectors.toList());

            if (isEmpty(offences)) {
                return null;
            }

            return offences;
        }
        return null;
    }

    private boolean needToSendOffence(final uk.gov.justice.core.courts.CourtApplicationType courtApplicationType, final String caseStatus) {
        return nonNull(courtApplicationType) && OFFENCE == courtApplicationType.getOffenceActiveOrder() && "INACTIVE".equals(caseStatus);
    }

    private uk.gov.justice.core.courts.CourtApplicationType findCourtApplicationType(final CourtApplicationType courtApplicationType, final List<uk.gov.justice.core.courts.CourtApplicationType> applicationTypes) {
        return ofNullable(applicationTypes).orElse(emptyList()).stream()
                .filter(applicationType -> nonNull(applicationType.getCode()))
                .filter(applicationType -> applicationType.getCode().equals(courtApplicationType.getCode())).findAny().orElse(null);

    }

    private ProsecutionCase findProsecutionCaseWithUrn(final AdditionalInformation additionalInformation, final CourtApplicationCase courtApplicationCase) {
        return additionalInformation.getProsecutionCases().stream()
                .filter(pCase -> nonNull(pCase.getProsecutionCaseIdentifier()))
                .filter(pCase -> pCase.getProsecutionCaseIdentifier().getCaseURN().equals(courtApplicationCase.getCaseURN()))
                .findAny().orElse(null);
    }

    private ProsecutionCaseIdentifier getProsecutionCaseIdentifier(final CourtApplicationCase courtApplicationCase, final List<ProsecutionCase> prosecutionCases) {
        return prosecutionCases.stream()
                .map(ProsecutionCase::getProsecutionCaseIdentifier)
                .filter(Objects::nonNull)
                .filter(prosecutionCaseIdentifier -> prosecutionCaseIdentifier.getCaseURN().equals(courtApplicationCase.getCaseURN()))
                .findAny().orElse(null);
    }

    private Respondent doMatchingForNonPoliceCase(final Defendant defendant, final Respondent respondent, final ProsecutionCase prosecutionCase) {
        if (fromString(respondent.getCpsDefendantId()).equals(defendant.getCpsDefendantId())) {
            return getMatchedRespondent(defendant, respondent, prosecutionCase);
        } else {
            if (nonNull(respondent.getPersonDetails()) && nonNull(defendant.getPersonDefendant())) {
                if (isPersonalDetailsMatched(defendant, respondent)) {
                    return getMatchedRespondent(defendant, respondent, prosecutionCase);
                } else {
                    return null;
                }
            } else if (nonNull(respondent.getOrganisation()) && nonNull(defendant.getLegalEntityDefendant())) {
                if (isOrganisationDetailsMatched(defendant, respondent)) {
                    return getMatchedRespondent(defendant, respondent, prosecutionCase);
                } else {
                    return null;
                }
            }
            return null;
        }
    }

    private boolean isOrganisationDetailsMatched(final Defendant defendant, final Respondent respondent) {
        return nullToEmpty(respondent.getOrganisation().getName()).equals(defendant.getLegalEntityDefendant().getOrganisation().getName());
    }

    private Respondent doMatchingForPoliceCase(final Defendant defendant, final Respondent respondent, final ProsecutionCase prosecutionCase) {
        if (nonNull(defendant.getPersonDefendant()) && respondent.getAsn().equals(defendant.getPersonDefendant().getArrestSummonsNumber())) {
            return getMatchedRespondent(defendant, respondent, prosecutionCase);
        } else if (nonNull(defendant.getLegalEntityDefendant()) && respondent.getAsn().equals(defendant.getProsecutionAuthorityReference())) {
            return getMatchedRespondent(defendant, respondent, prosecutionCase);
        } else {
            return null;
        }
    }

    private boolean isPersonalDetailsMatched(final Defendant defendant, final Respondent respondent) {
        return nullToEmpty(respondent.getPersonDetails().getFirstName()).equals(nullToEmpty(defendant.getPersonDefendant().getPersonDetails().getFirstName())) &&
                nullToEmpty(respondent.getPersonDetails().getMiddleName()).equals(nullToEmpty(defendant.getPersonDefendant().getPersonDetails().getMiddleName())) &&
                nullToEmpty(respondent.getPersonDetails().getLastName()).equals(nullToEmpty(defendant.getPersonDefendant().getPersonDetails().getLastName())) &&
                isDateOfBirthMatches(respondent.getPersonDetails(), defendant.getPersonDefendant());
    }

    private boolean isDateOfBirthMatches(final Person personDetails, final PersonDefendant personDefendant) {
        return (isNull(personDefendant.getPersonDetails().getDateOfBirth()) && (isNull(personDetails.getDateOfBirth()))) ||
                (nonNull(personDefendant.getPersonDetails().getDateOfBirth()) && LocalDate.parse(personDefendant.getPersonDetails().getDateOfBirth()).equals(personDetails.getDateOfBirth()));
    }

    private Respondent getMatchedRespondent(final Defendant defendant, final Respondent respondent, final ProsecutionCase prosecutionCase) {
        return Respondent.respondent().withValuesFrom(respondent)
                .withAsn(respondent.getAsn())
                .withCpsDefendantId(respondent.getCpsDefendantId())
                .withCaseId(prosecutionCase.getId())
                .withDefendantId(defendant.getMasterDefendantId())
                .withIsDefendantMatched(true)
                .withOrganisation(getOrganisation(defendant.getLegalEntityDefendant()))
                .withOrganisationPersons(getOrganisationPersons(defendant))
                .withPersonDetails(getPersonDetails(defendant.getPersonDefendant()))
                .build();
    }

    private Person getPersonDetails(final PersonDefendant personDefendant) {
        if (isNull(personDefendant)) {
            return null;
        }
        return getPerson(personDefendant.getPersonDetails());
    }

    private List<AssociatedPerson> getOrganisationPersons(final Defendant defendant) {
        if (isNull(defendant.getAssociatedPersons())) {
            return null;
        }
        return defendant.getAssociatedPersons().stream()
                .map(this::buildAssociatedPerson)
                .collect(Collectors.toList());
    }


    private AssociatedPerson buildAssociatedPerson(final uk.gov.justice.core.courts.AssociatedPerson defendantAssociatedPerson) {
        if (isNull(defendantAssociatedPerson)) {
            return null;
        }
        return associatedPerson()
                .withRole(defendantAssociatedPerson.getRole())
                .withPerson(getPerson(defendantAssociatedPerson.getPerson()))
                .build();
    }

    private Person getPerson(final uk.gov.justice.core.courts.Person person) {
        if (isNull(person)) {
            return null;
        }
        return person()
                .withAdditionalNationalityCode(person.getAdditionalNationalityCode())
                .withAdditionalNationalityDescription(person.getAdditionalNationalityDescription())
                .withAdditionalNationalityId(person.getAdditionalNationalityId())
                .withAddress(getAddress(person.getAddress()))
                .withContact(getContact(person.getContact()))
                .withDateOfBirth(ofNullable(person.getDateOfBirth()).map(LocalDate::parse).orElse(null))
                .withDisabilityStatus(person.getDisabilityStatus())
                .withDocumentationLanguageNeeds(ofNullable(person.getDocumentationLanguageNeeds()).flatMap(hearingLanguage -> HearingLanguage.valueFor(hearingLanguage.name())).orElse(null))
                .withEthnicity(getEthnicity(person.getEthnicity()))
                .withFirstName(person.getFirstName())
                .withGender(ofNullable(person.getGender()).flatMap(gender -> Gender.valueFor(gender.name())).orElse(null))
                .withHearingLanguageNeeds(ofNullable(person.getHearingLanguageNeeds()).flatMap(hearingLanguage -> HearingLanguage.valueFor(hearingLanguage.name())).orElse(null))
                .withInterpreterLanguageNeeds(person.getInterpreterLanguageNeeds())
                .withLastName(person.getLastName())
                .withMiddleName(person.getMiddleName())
                .withNationalInsuranceNumber(person.getNationalInsuranceNumber())
                .withNationalityCode(person.getNationalityCode())
                .withNationalityDescription(person.getNationalityDescription())
                .withNationalityId(person.getNationalityId())
                .withOccupation(person.getOccupation())
                .withOccupationCode(person.getOccupationCode())
                .withTitle(person.getTitle())
                .withPersonMarkers(getPersonMarkers(person.getPersonMarkers()))
                .build();
    }

    private List<Marker> getPersonMarkers(final List<uk.gov.justice.core.courts.Marker> markers) {
        if (isNull(markers)) {
            return null;
        }
        return markers.stream()
                .map(defendantMarker -> Marker.marker()
                        .withId(defendantMarker.getId())
                        .withMarkerTypeCode(defendantMarker.getMarkerTypeCode())
                        .withMarkerTypeDescription(defendantMarker.getMarkerTypeDescription())
                        .withMarkerTypeid(defendantMarker.getMarkerTypeid())
                        .build())
                .collect(Collectors.toList());
    }

    private Ethnicity getEthnicity(final uk.gov.justice.core.courts.Ethnicity ethnicity) {
        if (isNull(ethnicity)) {
            return null;
        }
        return Ethnicity.ethnicity()
                .withObservedEthnicityCode(ethnicity.getObservedEthnicityCode())
                .withObservedEthnicityDescription(ethnicity.getObservedEthnicityDescription())
                .withObservedEthnicityId(ethnicity.getObservedEthnicityId())
                .withSelfDefinedEthnicityCode(ethnicity.getSelfDefinedEthnicityCode())
                .withSelfDefinedEthnicityDescription(ethnicity.getSelfDefinedEthnicityDescription())
                .withSelfDefinedEthnicityId(ethnicity.getSelfDefinedEthnicityId())
                .build();
    }

    private Organisation getOrganisation(uk.gov.justice.core.courts.LegalEntityDefendant legalEntityDefendant) {
        if (isNull(legalEntityDefendant)) {
            return null;
        }
        return organisation()
                .withName(legalEntityDefendant.getOrganisation().getName())
                .withIncorporationNumber(legalEntityDefendant.getOrganisation().getIncorporationNumber())
                .withRegisteredCharityNumber(legalEntityDefendant.getOrganisation().getRegisteredCharityNumber())
                .withAddress(getAddress(legalEntityDefendant.getOrganisation().getAddress()))
                .withContact(getContact(legalEntityDefendant.getOrganisation().getContact()))
                .build();
    }

    private ContactNumber getContact(final uk.gov.justice.core.courts.ContactNumber contactNumber) {
        if (isNull(contactNumber)) {
            return null;
        }
        return contactNumber()
                .withFax(contactNumber.getFax())
                .withHome(contactNumber.getHome())
                .withMobile(contactNumber.getMobile())
                .withPrimaryEmail(contactNumber.getPrimaryEmail())
                .withSecondaryEmail(contactNumber.getSecondaryEmail())
                .build();
    }

    private Address getAddress(final uk.gov.justice.core.courts.Address address) {
        if (isNull(address)) {
            return null;
        }
        return address()
                .withAddress1(address.getAddress1())
                .withAddress2(address.getAddress2())
                .withAddress3(address.getAddress3())
                .withAddress4(address.getAddress4())
                .withAddress5(address.getAddress5())
                .withPostcode(address.getPostcode())
                .withWelshAddress1(address.getWelshAddress1())
                .withWelshAddress2(address.getWelshAddress2())
                .withWelshAddress3(address.getWelshAddress3())
                .withWelshAddress4(address.getWelshAddress4())
                .withWelshAddress5(address.getWelshAddress5())
                .build();
    }

    private Respondent getUnMatchedRespondent(final Respondent respondent) {
        return Respondent.respondent().withValuesFrom(respondent)
                .withIsDefendantMatched(false)
                .build();
    }
}