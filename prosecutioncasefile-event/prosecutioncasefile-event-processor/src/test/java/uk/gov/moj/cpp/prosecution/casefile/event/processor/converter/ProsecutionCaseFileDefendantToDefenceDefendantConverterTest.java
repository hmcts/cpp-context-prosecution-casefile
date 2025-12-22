package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefenceDefendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseFileDefendantToDefenceDefendantConverterTest {

    @Mock
    private List<Offence> offences;

    @Mock
    private List<uk.gov.justice.cps.prosecutioncasefile.Offence> convertedOffences;

    @Mock
    private PersonalInformation personalInformation;

    @Mock
    private SelfDefinedInformation selfDefinedInformation;

    @Mock
    private Individual individual;

    @Mock
    private ProsecutionCaseFileOffenceToDefenceOffenceConverter prosecutionCaseFileOffenceToDefenceOffenceConverter;

    @InjectMocks
    private ProsecutionCaseFileDefendantToDefenceDefendantConverter prosecutionCaseFileDefendantToDefenceDefendantConverter;

    @Test
    public void convert() {
        when(individual.getPersonalInformation()).thenReturn(personalInformation);
        when(individual.getSelfDefinedInformation()).thenReturn(selfDefinedInformation);
        when(personalInformation.getFirstName()).thenReturn("FirstName");
        when(personalInformation.getLastName()).thenReturn("LastName");
        when(selfDefinedInformation.getDateOfBirth()).thenReturn(LocalDate.now());
        when(prosecutionCaseFileOffenceToDefenceOffenceConverter.convert(offences)).thenReturn(convertedOffences);

        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant()
                .withId(randomUUID().toString())
                .withAsn(randomUUID().toString())
                .withProsecutorDefendantReference(randomUUID().toString())
                .withIndividual(individual)
                .withOffences(offences)
                .build());

        final List<DefenceDefendant> defenceDefendants = prosecutionCaseFileDefendantToDefenceDefendantConverter.convert(defendants);
        assertThat(defenceDefendants.size(), equalTo(defendants.size()));
        assertThat(defenceDefendants.get(0).getId(), equalTo(defendants.get(0).getId()));
        assertThat(defenceDefendants.get(0).getAsn(), equalTo(defendants.get(0).getAsn()));
        assertThat(defenceDefendants.get(0).getProsecutorDefendantReference(), equalTo(defendants.get(0).getProsecutorDefendantReference()));
        assertThat(defenceDefendants.get(0).getFirstName(), equalTo(defendants.get(0).getIndividual().getPersonalInformation().getFirstName()));
        assertThat(defenceDefendants.get(0).getLastName(), equalTo(defendants.get(0).getIndividual().getPersonalInformation().getLastName()));
        assertThat(defenceDefendants.get(0).getDateOfBirth(), equalTo(defendants.get(0).getIndividual().getSelfDefinedInformation().getDateOfBirth()));
        assertThat(defenceDefendants.get(0).getOffences(), equalTo(convertedOffences));
    }

}



