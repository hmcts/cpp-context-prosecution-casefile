package uk.gov.moj.cpp.prosecution.casefile.event.listener;

import static java.time.LocalDate.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.cps.prosecutioncasefile.InitialHearing.initialHearing;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.FIRST_NAME;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.LAST_NAME;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.createCorporateDefendant;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.createDefendant;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.createProsecution;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.util.CaseType.CHARGE_CASE_TYPE;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CPPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual.individual;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation.personalInformation;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation.selfDefinedInformation;

import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.json.schemas.prosecutioncasefile.events.DefendantValidationPassed;
import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantProblemsVO;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.event.CaseValidationFailed;
import uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceived;
import uk.gov.moj.cpp.prosecution.casefile.event.DefendantValidationFailed;
import uk.gov.moj.cpp.prosecution.casefile.event.ProsecutionDefendantsAdded;
import uk.gov.moj.cpp.prosecution.casefile.event.SjpValidationFailed;
import uk.gov.moj.cpp.prosecution.casefile.event.SpiProsecutionDefendantsAdded;
import uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.DefendantToDefendantDetails;
import uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.ProsecutionReceivedToCase;
import uk.gov.moj.cpp.prosecution.casefile.event.listener.model.ErrorCaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.event.listener.model.ErrorDefendantDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorCaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.OrganisationInformationDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.PersonalInformationDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.ResolvedCases;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorCaseDetailsRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.CaseDetailsRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.ResolvedCasesRepository;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ResolvedCase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.json.Json;
import javax.json.JsonObject;

@ExtendWith(MockitoExtension.class)
public class ProsecutionReceivedListenerTest {

    private final UUID caseId = randomUUID();

    private final UUID defendantId = randomUUID();

    private final UUID externalId = randomUUID();

    @Mock
    private DefendantDetails defendantDetails;
    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @InjectMocks
    private ProsecutionReceivedListener prosecutionReceivedListener;
    @Mock
    private ProsecutionReceivedToCase prosecutionReceivedToCaseConverter;
    @Mock
    private CaseDetailsRepository caseDetailsRepository;
    @Mock
    private BusinessValidationErrorRepository businessValidationErrorRepository;
    @Mock
    private BusinessValidationErrorCaseDetailsRepository businessValidationErrorCaseDetailsRepository;
    @Mock
    private ProsecutionWithReferenceData prosecutionWithReferenceData;
    @Mock
    private CaseDetails caseDetails;
    @Mock
    private PersonalInformationDetails personalInformationDetails;
    @Mock
    private OrganisationInformationDetails organisationInformation;
    @Mock
    private CcCaseReceived ccCaseReceived;
    @Mock
    private SpiProsecutionDefendantsAdded spiProsecutionDefendantsAdded;

    @Mock
    private ProsecutionDefendantsAdded prosecutionDefendantsAdded;

    @Mock
    private DefendantToDefendantDetails defendantToDefendantDetails;

    @Mock
    private Envelope<CcCaseReceived> ccCaseReceivedEnvelope;
    @Mock
    private Envelope<SpiProsecutionDefendantsAdded> spiProsecutionDefendantsAddedEnvelope;

    @Mock
    private Envelope<ProsecutionDefendantsAdded> prosecutionDefendantsAddedEnvelope;

    @Mock
    private ResolvedCase resolvedCaseEvent;

    @Mock
    private Envelope<ResolvedCase> resolvedCaseErrorsEnvelope;

    @Mock
    private ResolvedCasesRepository resolvedCasesRepository;

    @Captor
    private ArgumentCaptor<BusinessValidationErrorDetails> businessValidationErrorDetailsArgumentCaptor;

    @Captor
    private ArgumentCaptor<BusinessValidationErrorCaseDetails> businessValidationErrorCaseDetailsArgumentCaptor;

    @Captor
    private ArgumentCaptor<UUID> businessValidationErrorDeleteArgumentCaptor;

    @Captor
    private ArgumentCaptor<ErrorCaseDetails> caseDetailsCaptor;

    @Captor
    private ArgumentCaptor<ResolvedCases> resolvedCasesArgumentCaptor;

    @Test
    public void shouldPersistBusinessValidationErrorDetailsOnDefendantValidationFailed() {
        final Defendant defendant = defendant()
                .withId(randomUUID().toString())
                .withCustodyStatus("custodyStatus")
                .withOrganisationName("organisationName")
                .withInitialHearing(
                        initialHearing()
                                .withDateOfHearing(now().toString())
                                .withCourtHearingLocation("courtHearingLocation")
                                .build())
                .withIndividual(
                        individual()
                                .withPersonalInformation(
                                        personalInformation()
                                                .withFirstName("FirstName")
                                                .withLastName("LastName")
                                                .build())
                                .withSelfDefinedInformation(
                                        selfDefinedInformation()
                                                .withDateOfBirth(LocalDate.of(2000, 01, 01))
                                                .build()
                                )
                                .build())
                .withOffences(singletonList(offence()
                        .withChargeDate(now())
                        .build()))
                .build();

        final ProblemValue problemValue = new ProblemValue("id", "key", "value");
        final Problem problem = new Problem("Code", singletonList(new ProblemValue("id", "key", "value")));

        final DefendantValidationFailed defendantValidationFailed = new DefendantValidationFailed(
                defendant,
                singletonList(problem),
                caseId,
                "caseUrn",
                CHARGE_CASE_TYPE.toString(),
                "policeSystemId"
        );
        final Envelope<DefendantValidationFailed> defendantValidationFailedEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUIDAndName().withEventNumber(1L).build(),
                defendantValidationFailed);

        when(objectToJsonObjectConverter.convert(any())).thenReturn(createObjectBuilder().build());
        prosecutionReceivedListener.defendantsValidationFailed(defendantValidationFailedEnvelope);

        verify(businessValidationErrorRepository).save(businessValidationErrorDetailsArgumentCaptor.capture());
        final BusinessValidationErrorDetails savedBusinessValidationErrorDetails = businessValidationErrorDetailsArgumentCaptor.getValue();
        assertThat(savedBusinessValidationErrorDetails.getFieldId(), is(problemValue.getId()));
        assertThat(savedBusinessValidationErrorDetails.getErrorValue(), is(problemValue.getValue()));
        assertThat(savedBusinessValidationErrorDetails.getDisplayName(), is(problem.getCode()));
        assertThat(savedBusinessValidationErrorDetails.getCaseId(), is(caseId));
        assertThat(savedBusinessValidationErrorDetails.getDefendantId().toString(), is(defendant.getId()));
        assertThat(savedBusinessValidationErrorDetails.getFieldName(), is(problemValue.getKey()));
        assertNull(savedBusinessValidationErrorDetails.getCourtName());
        assertThat(savedBusinessValidationErrorDetails.getCourtLocation(), is(defendant.getInitialHearing().getCourtHearingLocation()));
        assertThat(savedBusinessValidationErrorDetails.getCaseType(), is(defendantValidationFailed.getCaseType()));
        assertThat(savedBusinessValidationErrorDetails.getUrn(), is(defendantValidationFailed.getUrn()));
        assertThat(savedBusinessValidationErrorDetails.getDefendantBailStatus(), is(defendant.getCustodyStatus()));
        assertThat(savedBusinessValidationErrorDetails.getFirstName(), is(defendant.getIndividual().getPersonalInformation().getFirstName()));
        assertThat(savedBusinessValidationErrorDetails.getLastName(), is(defendant.getIndividual().getPersonalInformation().getLastName()));
        assertThat(savedBusinessValidationErrorDetails.getOrganisationName(), is(defendant.getOrganisationName()));
        assertThat(savedBusinessValidationErrorDetails.getDefendantChargeDate(), is(defendant.getOffences().get(0).getChargeDate()));
        assertThat(savedBusinessValidationErrorDetails.getDefendantHearingDate().toString(), is(defendant.getInitialHearing().getDateOfHearing()));
        assertThat(savedBusinessValidationErrorDetails.getDateOfBirth(), is(defendant.getIndividual().getSelfDefinedInformation().getDateOfBirth()));
    }


    @Test
    public void shouldRemoveBusinessValidationErrorDetailsOnDefendantValidationPassed() {

        final DefendantValidationPassed defendantValidationPassed = DefendantValidationPassed.defendantValidationPassed().withDefendantId(randomUUID()).build();
        final Envelope<DefendantValidationPassed> defendantValidationPassedEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUIDAndName().withEventNumber(1L).build(),
                defendantValidationPassed);

        BusinessValidationErrorDetails businessValidationErrorDetails = new BusinessValidationErrorDetails();
        prosecutionReceivedListener.defendantsValidationPassed(defendantValidationPassedEnvelope);

        verify(businessValidationErrorRepository).deleteByDefendantId(businessValidationErrorDeleteArgumentCaptor.capture());
        assertThat(businessValidationErrorDeleteArgumentCaptor.getValue(), is(defendantValidationPassed.getDefendantId()));

    }

    @Test
    public void shouldPersistBusinessValidationErrorDetailsOnCaseValidationFailed() {
        final Prosecution prosecution = Prosecution.prosecution()
                .withCaseDetails(uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.
                        caseDetails()
                        .withCaseId(caseId)
                        .withInitiationCode("C")
                        .withProsecutorCaseReference("prosecutorCaseReference")
                        .build())
                .withChannel(SPI)
                .withDefendants(emptyList())
                .build();

        final ProblemValue problemValue = new ProblemValue("id", "key", "value");
        final Problem problem = new Problem("Code", singletonList(new ProblemValue("id", "key", "value")));

        final CaseValidationFailed caseValidationFailed = new CaseValidationFailed(
                prosecution,
                singletonList(problem),
                externalId,
                null);
        final Envelope<CaseValidationFailed> caseValidationFailedEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUIDAndName().withEventNumber(1L).build(),
                caseValidationFailed);

        prosecutionReceivedListener.caseValidationFailed(caseValidationFailedEnvelope);

        verify(businessValidationErrorRepository).save(businessValidationErrorDetailsArgumentCaptor.capture());
        final BusinessValidationErrorDetails savedBusinessValidationErrorDetails = businessValidationErrorDetailsArgumentCaptor.getValue();
        assertThat(savedBusinessValidationErrorDetails.getFieldId(), is(problemValue.getId()));
        assertThat(savedBusinessValidationErrorDetails.getErrorValue(), is(problemValue.getValue()));
        assertThat(savedBusinessValidationErrorDetails.getDisplayName(), is(problem.getCode()));
        assertThat(savedBusinessValidationErrorDetails.getCaseId(), is(caseId));
        assertNull(savedBusinessValidationErrorDetails.getDefendantId());
        assertThat(savedBusinessValidationErrorDetails.getFieldName(), is(problemValue.getKey()));
        assertNull(savedBusinessValidationErrorDetails.getCourtName());
        assertNull(savedBusinessValidationErrorDetails.getCourtLocation());
        assertThat(savedBusinessValidationErrorDetails.getCaseType(), is(prosecution.getCaseDetails().getInitiationCode()));
        assertThat(savedBusinessValidationErrorDetails.getUrn(), is(prosecution.getCaseDetails().getProsecutorCaseReference()));
        assertNull(savedBusinessValidationErrorDetails.getDefendantBailStatus());
        assertNull(savedBusinessValidationErrorDetails.getFirstName());
        assertNull(savedBusinessValidationErrorDetails.getLastName());
        assertNull(savedBusinessValidationErrorDetails.getOrganisationName());
        assertNull(savedBusinessValidationErrorDetails.getDefendantChargeDate());
        assertNull(savedBusinessValidationErrorDetails.getDefendantHearingDate());
    }

    @Test
    public void shouldReceiveCCcaseAndDeleteValidationError() {
        Set<DefendantDetails> defendantDetailsSet = new HashSet<>();
        when(ccCaseReceivedEnvelope.payload()).thenReturn(ccCaseReceived);
        when(ccCaseReceived.getProsecutionWithReferenceData()).thenReturn(prosecutionWithReferenceData);
        when(prosecutionReceivedToCaseConverter.convert(ccCaseReceived.getProsecutionWithReferenceData().getProsecution())).thenReturn(caseDetails);
        when(caseDetails.getCaseId()).thenReturn(caseId);
        when(defendantDetails.getDefendantId()).thenReturn(defendantId.toString());
        when(defendantDetails.getPersonalInformation()).thenReturn(personalInformationDetails);
        when(personalInformationDetails.getLastName()).thenReturn(LAST_NAME);
        when(personalInformationDetails.getFirstName()).thenReturn(FIRST_NAME);

        defendantDetailsSet.add(defendantDetails);
        when(caseDetails.getDefendants()).thenReturn(defendantDetailsSet);

        prosecutionReceivedListener.prosecutionReceived(ccCaseReceivedEnvelope);

        verify(caseDetailsRepository).save(eq(caseDetails));
        verify(businessValidationErrorRepository).deleteByCaseIdAndDefendantId(caseId, defendantId);
        verify(businessValidationErrorRepository).deleteByCaseIdAndFirstNameAndLastName(caseId, personalInformationDetails.getFirstName(), personalInformationDetails.getLastName());
        verify(businessValidationErrorCaseDetailsRepository).deleteByCaseId(caseId);
    }

    @Test
    public void shouldReceiveCCcaseWithNewDefendantAndShouldNotDeleteExistingValidationError() {
        Set<DefendantDetails> defendantDetailsSet = new HashSet<>();
        when(ccCaseReceivedEnvelope.payload()).thenReturn(ccCaseReceived);
        when(ccCaseReceived.getProsecutionWithReferenceData()).thenReturn(prosecutionWithReferenceData);
        when(prosecutionReceivedToCaseConverter.convert(ccCaseReceived.getProsecutionWithReferenceData().getProsecution())).thenReturn(caseDetails);

        when(caseDetails.getCaseId()).thenReturn(caseId);
        when(defendantDetails.getDefendantId()).thenReturn(defendantId.toString());
        when(defendantDetails.getPersonalInformation()).thenReturn(personalInformationDetails);
        when(personalInformationDetails.getLastName()).thenReturn(LAST_NAME);
        when(personalInformationDetails.getFirstName()).thenReturn(FIRST_NAME);

        defendantDetailsSet.add(defendantDetails);
        when(caseDetails.getDefendants()).thenReturn(defendantDetailsSet);
        BusinessValidationErrorDetails businessValidationErrorDetails = new BusinessValidationErrorDetails();
        when(businessValidationErrorRepository.findByCaseId(caseId)).thenReturn(asList(businessValidationErrorDetails));

        prosecutionReceivedListener.prosecutionReceived(ccCaseReceivedEnvelope);

        verify(caseDetailsRepository).save(eq(caseDetails));
        verify(businessValidationErrorRepository).deleteByCaseIdAndDefendantId(caseId, defendantId);
        verify(businessValidationErrorRepository).deleteByCaseIdAndFirstNameAndLastName(caseId, personalInformationDetails.getFirstName(), personalInformationDetails.getLastName());
        verify(businessValidationErrorRepository).findByCaseId(caseId);
        verifyNoInteractions(businessValidationErrorCaseDetailsRepository);
    }

    @Test
    public void shouldReceiveCCcaseWithNewOrganisationDefendantAndShouldNotDeleteExistingValidationError() {
        Set<DefendantDetails> defendantDetailsSet = new HashSet<>();
        when(ccCaseReceivedEnvelope.payload()).thenReturn(ccCaseReceived);
        when(ccCaseReceived.getProsecutionWithReferenceData()).thenReturn(prosecutionWithReferenceData);
        when(prosecutionReceivedToCaseConverter.convert(ccCaseReceived.getProsecutionWithReferenceData().getProsecution())).thenReturn(caseDetails);

        when(caseDetails.getCaseId()).thenReturn(caseId);
        when(defendantDetails.getDefendantId()).thenReturn(defendantId.toString());
        when(defendantDetails.getOrganisationInformation()).thenReturn(organisationInformation);
        when(organisationInformation.getOrganisationName()).thenReturn("TFL");

        when(defendantDetails.getPersonalInformation()).thenReturn(null);

        defendantDetailsSet.add(defendantDetails);
        when(caseDetails.getDefendants()).thenReturn(defendantDetailsSet);
        BusinessValidationErrorDetails businessValidationErrorDetails = new BusinessValidationErrorDetails();
        when(businessValidationErrorRepository.findByCaseId(caseId)).thenReturn(asList(businessValidationErrorDetails));

        prosecutionReceivedListener.prosecutionReceived(ccCaseReceivedEnvelope);

        verify(caseDetailsRepository).save(eq(caseDetails));
        verify(businessValidationErrorRepository).deleteByCaseIdAndDefendantId(caseId, defendantId);
        verify(businessValidationErrorRepository).deleteByCaseIdAndOrganisationName(caseId, organisationInformation.getOrganisationName());
        verify(businessValidationErrorRepository).findByCaseId(caseId);
        verifyNoInteractions(businessValidationErrorCaseDetailsRepository);
    }

    @Test
    public void shouldAddDefendantAndDeleteValidationErrors() {
        Set<DefendantDetails> defendantDetailsSet = new HashSet<>();
        when(spiProsecutionDefendantsAddedEnvelope.payload()).thenReturn(spiProsecutionDefendantsAdded);
        when(caseDetailsRepository.findBy(spiProsecutionDefendantsAdded.getCaseId())).thenReturn(caseDetails);
        when(defendantDetails.getDefendantId()).thenReturn(defendantId.toString());
        defendantDetailsSet.add(defendantDetails);
        when(caseDetails.getDefendants()).thenReturn(defendantDetailsSet);

        prosecutionReceivedListener.defendantReceived(spiProsecutionDefendantsAddedEnvelope);

        verify(businessValidationErrorRepository).deleteByDefendantId(defendantId);
    }

    @Test
    public void shouldNotAddDefendantsAndDeleteValidationErrorsWhenProsecutionDefendantsAreReceivedWithChanel() {
        ProsecutionDefendantsAdded prosecutionDefendants = constructProsecutionDefendantsAdded(CPPI);
        Envelope<ProsecutionDefendantsAdded> envelope = envelopeFrom(metadataWithDefaults(), prosecutionDefendants);

        prosecutionReceivedListener.prosecutionDefendantsAddedReceived(envelope);

        verifyNoInteractions(caseDetailsRepository);
        verifyNoInteractions(businessValidationErrorRepository);
    }

    @Test
    public void shouldAddDefendantsAndDeleteValidationErrorsWhenProsecutionDefendantsAreReceived() {
        CaseDetails caseDetails = getCaseAndDefendantDetails();
        ProsecutionDefendantsAdded prosecutionDefendants = constructProsecutionDefendantsAdded(SPI);
        Envelope<ProsecutionDefendantsAdded> envelope = envelopeFrom(metadataWithDefaults(), prosecutionDefendants);

        when(defendantToDefendantDetails.convert(any())).thenCallRealMethod();
        when(caseDetailsRepository.findBy(prosecutionDefendants.getCaseId())).thenReturn(caseDetails);

        prosecutionReceivedListener.prosecutionDefendantsAddedReceived(envelope);

        verify(caseDetailsRepository).findBy(prosecutionDefendants.getCaseId());
        assertThat(caseDetails.getDefendants(), hasSize(2));
        verify(businessValidationErrorRepository, times(2)).deleteByDefendantId(any());
        verify(businessValidationErrorRepository).deleteByDefendantId(fromString(caseDetails.getDefendants().iterator().next().getDefendantId()));
        verify(businessValidationErrorRepository).deleteByDefendantId(fromString(prosecutionDefendants.getDefendants().get(0).getId()));
    }

    @Test
    public void shouldResolvedCaseErrorsReceived() {
        when(resolvedCaseErrorsEnvelope.payload()).thenReturn(resolvedCaseEvent);
        when(resolvedCaseEvent.getCaseId()).thenReturn(caseId);
        prosecutionReceivedListener.resolvedCaseErrorsReceived(resolvedCaseErrorsEnvelope);

        verify(resolvedCasesRepository).save(resolvedCasesArgumentCaptor.capture());

        ResolvedCases savedResolvedCases = resolvedCasesArgumentCaptor.getValue();
        assertThat(savedResolvedCases.getCaseId(), is(caseId));
    }

    private CaseDetails getCaseAndDefendantDetails() {
        DefendantDetails defendantDetails = new DefendantDetails();
        defendantDetails.setDefendantId(randomUUID().toString());

        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setDefendants(Collections.singleton(defendantDetails));
        return caseDetails;
    }

    private ProsecutionDefendantsAdded constructProsecutionDefendantsAdded(Channel channel) {
        return ProsecutionDefendantsAdded.prosecutionDefendantsAdded()
                .withDefendants(singletonList(defendant().withId(randomUUID().toString()).withOffences(emptyList()).build()))
                .withCaseId(randomUUID()).withChannel(channel)
                .build();
    }

    @Test
    public void shouldUpdateSjpCaseValidationFailed() {
        final UUID offenceId = randomUUID();
        final String defendantId = randomUUID().toString();
        final Defendant defendant = createDefendant();
        final Prosecution prosecution = Prosecution.prosecution()
                .withCaseDetails( uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.
                        caseDetails()
                        .withCaseId(caseId)
                        .withInitiationCode("C")
                        .withProsecutorCaseReference("prosecutorCaseReference")
                        .build())
                .withChannel(SPI)
                .withDefendants(Collections.singletonList(defendant))
                .build();
        Envelope<SjpValidationFailed> envelope = createSjpValidationFailedEnvelope(defendant, prosecution);
        final JsonObject jsonObject = createObjectBuilder().add("id", defendantId)
                .add("firstName", "FirstName")
                .add("lastName", "LastName")
                .add("offences", createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("id", offenceId.toString())
                                .add("description", "Duty not paid")
                                .build()))
                .build();

        when(objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);
        prosecutionReceivedListener.sjpCaseValidationFailed(envelope);
        verify(businessValidationErrorRepository, times(2)).save(businessValidationErrorDetailsArgumentCaptor.capture());
        verify(businessValidationErrorCaseDetailsRepository).save(businessValidationErrorCaseDetailsArgumentCaptor.capture());

        final BusinessValidationErrorCaseDetails businessValidationErrorCaseDetails = businessValidationErrorCaseDetailsArgumentCaptor.getValue();
        assertThat(businessValidationErrorCaseDetails.getCaseId(), is(caseId));
        assertThat(businessValidationErrorCaseDetails.getCaseDetails(), is(jsonObject.toString()));
    }


    @Test
    public void shouldUpdateErrorCasesDetailsIncludesCaseDefendants() {
        final Defendant defendant = createDefendant();
        final Prosecution prosecution = createProsecution();
        Envelope<SjpValidationFailed> envelope = createSjpValidationFailedEnvelope(defendant, prosecution);

        when(objectToJsonObjectConverter.convert(any())).thenReturn(createObjectBuilder().build());
        Set<DefendantDetails> defendantDetailsSet = new HashSet<>();
        defendantDetailsSet.add(defendantDetails);
        when(caseDetailsRepository.findBy(any())).thenReturn(caseDetails);
        when(caseDetails.getDefendants()).thenReturn(defendantDetailsSet);
        when(defendantDetails.getPersonalInformation()).thenReturn(personalInformationDetails);
        when(personalInformationDetails.getFirstName()).thenReturn("EFirstName");
        when(personalInformationDetails.getLastName()).thenReturn("ELastName");

        prosecutionReceivedListener.sjpCaseValidationFailed(envelope);
        verify(objectToJsonObjectConverter).convert(caseDetailsCaptor.capture());
        final ErrorCaseDetails actualErrorDetails = caseDetailsCaptor.getValue();
        final List<ErrorDefendantDetails> actualErrorDetailsDefendants = actualErrorDetails.getDefendants();
        assertEquals(2, actualErrorDetailsDefendants.size());
        assertEquals("John", actualErrorDetailsDefendants.get(0).getFirstName());
        assertEquals("Doe", actualErrorDetailsDefendants.get(0).getLastName());

        //assert existing defendant without errors
        assertEquals("EFirstName", actualErrorDetailsDefendants.get(1).getFirstName());
        assertEquals("ELastName", actualErrorDetailsDefendants.get(1).getLastName());

    }

    @Test
    public void shouldUpdateErrorCasesDetailsIncludesAllDefendants() {
        final Defendant defendant = createDefendant();
        final Prosecution prosecution = createProsecution();
        Envelope<SjpValidationFailed> envelope = createSjpValidationFailedEnvelope(defendant, prosecution);

        BusinessValidationErrorCaseDetails details = new BusinessValidationErrorCaseDetails();
        details.setCaseDetails("{\"caseId\":\"baf8f99f-aa41-4298-907f-396c94a9ad83\",\"defendants\":[{\"id\":\"7f13e400-6962-4013-a031-ef7550b1fedd\",\"firstName\":\"Leo\",\"lastName\":\"Donald\",\"offences\":[{\"id\":\"7fec70c3-d8ff-4c1b-ac02-18694b3bafa3\",\"description\":\"Duty not paid\"}]}]}");
        List<BusinessValidationErrorCaseDetails> existingObject = new ArrayList<>();
        existingObject.add(details);
        when(businessValidationErrorCaseDetailsRepository.findByCaseId(any())).thenReturn(existingObject);

        when(objectToJsonObjectConverter.convert(any())).thenReturn(createObjectBuilder().build());
        Set<DefendantDetails> defendantDetailsSet = new HashSet<>();
        defendantDetailsSet.add(defendantDetails);
        when(caseDetailsRepository.findBy(any())).thenReturn(caseDetails);
        when(caseDetails.getDefendants()).thenReturn(defendantDetailsSet);
        when(defendantDetails.getPersonalInformation()).thenReturn(personalInformationDetails);
        when(personalInformationDetails.getFirstName()).thenReturn("EFirstName");
        when(personalInformationDetails.getLastName()).thenReturn("ELastName");

        prosecutionReceivedListener.sjpCaseValidationFailed(envelope);
        verify(objectToJsonObjectConverter).convert(caseDetailsCaptor.capture());
        final ErrorCaseDetails actualErrorDetails = caseDetailsCaptor.getValue();
        final List<ErrorDefendantDetails> actualErrorDetailsDefendants = actualErrorDetails.getDefendants();
        assertEquals(3, actualErrorDetailsDefendants.size());

        assertEquals("Leo", actualErrorDetailsDefendants.get(0).getFirstName());
        assertEquals("Donald", actualErrorDetailsDefendants.get(0).getLastName());

        assertEquals("John", actualErrorDetailsDefendants.get(1).getFirstName());
        assertEquals("Doe", actualErrorDetailsDefendants.get(1).getLastName());

        //assert existing defendant without errors
        assertEquals("EFirstName", actualErrorDetailsDefendants.get(2).getFirstName());
        assertEquals("ELastName", actualErrorDetailsDefendants.get(2).getLastName());

    }

    @Test
    public void shouldUpdateErrorCasesDetailsWhenDefendantAsOrganisation() {
        final Defendant defendant = createCorporateDefendant();
        final Prosecution prosecution = createProsecution();
        Envelope<SjpValidationFailed> envelope = createSjpValidationFailedEnvelope(defendant, prosecution);

        BusinessValidationErrorCaseDetails details = new BusinessValidationErrorCaseDetails();
        details.setCaseDetails("{\n" +
                "  \"caseId\": \"baf8f99f-aa41-4298-907f-396c94a9ad83\",\n" +
                "  \"defendants\": [\n" +
                "    {\n" +
                "      \"id\": \"7f13e400-6962-4013-a031-ef7550b1fedd\",\n" +
                "      \"organisationName\": \"No Tax Shipping Ltd\",\n" +
                "      \"offences\": [\n" +
                "        {\n" +
                "          \"id\": \"7fec70c3-d8ff-4c1b-ac02-18694b3bafa3\",\n" +
                "          \"description\": \"Duty not paid\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}");
        List<BusinessValidationErrorCaseDetails> existingObject = new ArrayList<>();
        existingObject.add(details);
        when(businessValidationErrorCaseDetailsRepository.findByCaseId(any())).thenReturn(existingObject);

        when(objectToJsonObjectConverter.convert(any())).thenReturn(createObjectBuilder().build());
        Set<DefendantDetails> defendantDetailsSet = new HashSet<>();
        defendantDetailsSet.add(defendantDetails);
        when(caseDetailsRepository.findBy(any())).thenReturn(caseDetails);
        when(caseDetails.getDefendants()).thenReturn(defendantDetailsSet);
        when(defendantDetails.getOrganisationInformation()).thenReturn(organisationInformation);
        when(organisationInformation.getOrganisationName()).thenReturn("TFL");

        prosecutionReceivedListener.sjpCaseValidationFailed(envelope);
        verify(objectToJsonObjectConverter).convert(caseDetailsCaptor.capture());
        final ErrorCaseDetails actualErrorDetails = caseDetailsCaptor.getValue();
        final List<ErrorDefendantDetails> actualErrorDetailsDefendants = actualErrorDetails.getDefendants();
        assertEquals(3, actualErrorDetailsDefendants.size());

        assertNull(actualErrorDetailsDefendants.get(0).getFirstName());
        assertEquals("No Tax Shipping Ltd", actualErrorDetailsDefendants.get(0).getOrganisationName());

        assertEquals("John", actualErrorDetailsDefendants.get(1).getFirstName());
        assertEquals("Doe", actualErrorDetailsDefendants.get(1).getLastName());

        //assert existing defendant without errors
        assertEquals("TFL", actualErrorDetailsDefendants.get(2).getOrganisationName());
        assertNull(actualErrorDetailsDefendants.get(2).getLastName());

    }
    private Envelope<SjpValidationFailed> createSjpValidationFailedEnvelope(final Defendant defendant, final Prosecution prosecution) {
        List<Problem> caseProblems = new ArrayList<>();
        caseProblems.add(Problem.problem()
                .withCode("code")
                .withValues(Arrays.asList(ProblemValue.problemValue().build()))
                .build());

        List<DefendantProblemsVO> defendantProblems = createDefendantProblems(defendant);

        SjpValidationFailed sjpValidationFailed = new SjpValidationFailed(prosecution, caseProblems, defendantProblems, null, InitialHearing.initialHearing().build());
        return envelopeFrom(metadataWithDefaults(), sjpValidationFailed);
    }

    private List<DefendantProblemsVO> createDefendantProblems(final Defendant defendant) {
        List<DefendantProblemsVO> defendantProblems = new ArrayList<>();

        defendantProblems.add(new DefendantProblemsVO(defendant, Arrays.asList(Problem.problem()
                .withCode("code")
                .withValues(asList(ProblemValue.problemValue()
                        .withId(randomUUID().toString())
                        .build()))
                .build())));
        return defendantProblems;
    }
}
