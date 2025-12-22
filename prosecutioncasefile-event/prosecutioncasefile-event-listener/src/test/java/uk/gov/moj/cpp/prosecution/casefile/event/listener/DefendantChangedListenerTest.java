package uk.gov.moj.cpp.prosecution.casefile.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.PersonalInformationToPersonalInformationDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.AddressDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.PersonalInformationDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.DefendantRepository;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseDefendantChanged;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantChangedListenerTest {

    @Mock
    private PersonalInformationToPersonalInformationDetails personalInformationToPersonalInformationDetailsConverter;

    @Mock
    private DefendantRepository defendantRepository;

    @Mock
    private Envelope<CaseDefendantChanged> caseDefendantChangedEnvelope;

    @InjectMocks
    private DefendantChangedListener defendantChangedListener;

    @Mock
    DefendantDetails defendantDetails;

    @Test
    public void shouldHandleCaseDefendantChanged() {
        when(caseDefendantChangedEnvelope.payload()).thenReturn(createCaseDefendantChanged());
        when(defendantRepository.findBy(any())).thenReturn(createDefendantDetails());
        when(personalInformationToPersonalInformationDetailsConverter.convert(any())).thenReturn(new PersonalInformationDetails());

        defendantChangedListener.caseDefendantChanged(caseDefendantChangedEnvelope);

        verify(defendantRepository).findBy(any());
        verify(defendantRepository).save(any());
    }

    public CaseDefendantChanged createCaseDefendantChanged(){
        return new CaseDefendantChanged.Builder()
                .withDefendantId(randomUUID())
                .withPersonDetails(new PersonalInformation.Builder()
                        .withFirstName("Mark")
                        .withTitle("Mr")
                        .withAddress(new Address.Builder()
                                .withAddress1("61")
                                .withAddress2("Victoria Road")
                                .withPostcode("CB1 2GS")
                                .build())
                        .withContactDetails(new ContactDetails.Builder()
                                .withPrimaryEmail("Mark@hmcts.net")
                                .build())
                        .build())
                .build();
    }

    private DefendantDetails createDefendantDetails(){
        final DefendantDetails defendantDetails = new DefendantDetails();
        final PersonalInformationDetails personalInformationDetails = new PersonalInformationDetails();
        personalInformationDetails.setTitle("Mr");

        final AddressDetails addressDetails = new AddressDetails();
        addressDetails.setAddress1("61");
        personalInformationDetails.setAddress(addressDetails);

        final uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.ContactDetails  contactDetails = new uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.ContactDetails();
        contactDetails.setPrimaryEmail("Mark@hmcts.net");
        personalInformationDetails.setContactDetails(contactDetails);

        defendantDetails.setPersonalInformation(personalInformationDetails);
        return defendantDetails;
    }
}
