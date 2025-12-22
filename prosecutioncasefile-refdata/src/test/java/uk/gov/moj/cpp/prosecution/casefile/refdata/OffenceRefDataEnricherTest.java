package uk.gov.moj.cpp.prosecution.casefile.refdata;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.OffenceDataRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OffenceRefDataEnricherTest {

    private static final String DEFENDANT_ID = "1234243";
    private static final UUID OFFENCE_UUID = randomUUID();
    @Mock
    private Metadata metadata;
    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @InjectMocks
    private OffenceDataRefDataEnricher offenceDataRefDataEnricher;

    @BeforeEach
    public void setup() {
        when(referenceDataQueryService.retrieveOffenceDataList(any(), any())).thenReturn(getMockOffenceReferenceData());
    }

    @Test
    public void shouldPopulateOffenceRefData() {
        final List<DefendantsWithReferenceData> defendantsWithReferenceDataList = asList(getMockDefendantsWithReferenceDataWithCivilFlag(buildOffence(), null),
                getMockDefendantsWithReferenceData(buildOffence(), null));

        offenceDataRefDataEnricher.enrich(defendantsWithReferenceDataList);
        assertNotNull(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getOffenceReferenceData());
        assertThat(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getOffenceReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getOffenceReferenceData().get(0), isA(OffenceReferenceData.class));
        assertThat(defendantsWithReferenceDataList.get(0).getDefendants().get(0).getOffences().get(0).getOffenceLocation(), is(nullValue()));
        assertThat(defendantsWithReferenceDataList.get(0).getDefendants().get(0).getOffences().get(0).getMaxPenalty(), is("Max Penalty"));
        assertNotNull(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getOffenceReferenceData());
        assertThat(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getOffenceReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getOffenceReferenceData().get(0), isA(OffenceReferenceData.class));
        assertThat(defendantsWithReferenceDataList.get(1).getDefendants().get(0).getOffences().get(0).getOffenceLocation(), is(nullValue()));
        assertThat(defendantsWithReferenceDataList.get(1).getDefendants().get(0).getOffences().get(0).getMaxPenalty(), is("Max Penalty"));
        verify(referenceDataQueryService, times(1)).retrieveOffenceDataList(Lists.newArrayList(buildOffence().getOffenceCode()), Optional.of("MoJ"));
    }

    @Test
    public void shouldPopulateOffenceRefDataWithCustomOffenceLocationForDVLAProsecutorWhenEmpty() {
        final DefendantsWithReferenceData defendantsWithReferenceData = getMockDefendantsWithReferenceDataWithCivilFlag(buildOffenceWithEmptyOffenceLocation(), "DVLA");
        offenceDataRefDataEnricher.enrich(defendantsWithReferenceData);
        assertNotNull(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData());
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData().get(0), isA(OffenceReferenceData.class));
        assertThat(defendantsWithReferenceData.getDefendants().get(0).getOffences().get(0).getOffenceLocation(), is("No location provided"));
        verify(referenceDataQueryService, times(1)).retrieveOffenceDataList(Lists.newArrayList(buildOffenceWithEmptyOffenceLocation().getOffenceCode()), Optional.of("MoJ"));
    }

    @Test
    public void shouldPopulateOffenceRefDataWithCustomOffenceLocationForDVLAProsecutorWhenNull() {
        final DefendantsWithReferenceData defendantsWithReferenceData = getMockDefendantsWithReferenceData(buildOffenceWithNullOffenceLocation(), "DVLA");
        offenceDataRefDataEnricher.enrich(defendantsWithReferenceData);
        assertNotNull(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData());
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData().get(0), isA(OffenceReferenceData.class));
        assertThat(defendantsWithReferenceData.getDefendants().get(0).getOffences().get(0).getOffenceLocation(), is("No location provided"));
        verify(referenceDataQueryService, times(1)).retrieveOffenceDataList(Lists.newArrayList(buildOffenceWithNullOffenceLocation().getOffenceCode()), Optional.empty());
    }

    @Test
    public void shouldPopulateOffenceRefDataWithCustomOffenceLocationForDVLAProsecutorWhenSpace() {
        final DefendantsWithReferenceData defendantsWithReferenceData = getMockDefendantsWithReferenceData(buildOffenceWithSpacedOffenceLocation(), "DVLA");
        offenceDataRefDataEnricher.enrich(defendantsWithReferenceData);
        assertNotNull(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData());
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData().get(0), isA(OffenceReferenceData.class));
        assertThat(defendantsWithReferenceData.getDefendants().get(0).getOffences().get(0).getOffenceLocation(), is("No location provided"));
        verify(referenceDataQueryService, times(1)).retrieveOffenceDataList(Lists.newArrayList(buildOffenceWithSpacedOffenceLocation().getOffenceCode()), Optional.empty());
    }

    private DefendantsWithReferenceData getMockDefendantsWithReferenceData(final Offence offence, final String prosecutionAuthorityShortName) {
        final List<Offence> offences = new ArrayList<>();
        offences.add(offence);

        final Defendant defendant = new Defendant.Builder().withId(DEFENDANT_ID).withOffences(offences).withInitiationCode("S").build();
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(defendant);

        DefendantsWithReferenceData defendantsWithReferenceData = new DefendantsWithReferenceData(defendants, prosecutionAuthorityShortName);
        defendantsWithReferenceData.setCaseDetails(CaseDetails.caseDetails().withInitiationCode("P").build());
        return defendantsWithReferenceData;
    }

    private DefendantsWithReferenceData getMockDefendantsWithReferenceDataWithCivilFlag(final Offence offence, final String prosecutionAuthorityShortName) {
        final DefendantsWithReferenceData defendantsWithReferenceData = getMockDefendantsWithReferenceData(offence, prosecutionAuthorityShortName);
        defendantsWithReferenceData.setCivil(true);
        return defendantsWithReferenceData;
    }


    @Test
    public void shouldPopulateOffenceRefDataOnceWhenDuplicateOffencesPresent() {
        final DefendantsWithReferenceData defendantsWithReferenceData = getMockDefendantsWithSameOffences(buildOffence(), null);
        offenceDataRefDataEnricher.enrich(defendantsWithReferenceData);
        assertNotNull(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData());
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getOffenceReferenceData().get(0), isA(OffenceReferenceData.class));
        assertThat(defendantsWithReferenceData.getDefendants().get(0).getOffences().get(0).getOffenceLocation(), is(nullValue()));
        assertThat(defendantsWithReferenceData.getDefendants().get(0).getOffences().get(0).getMaxPenalty(), is("Max Penalty"));
        verify(referenceDataQueryService, times(1)).retrieveOffenceDataList(Lists.newArrayList(buildOffence().getOffenceCode()), Optional.empty());
    }

    private DefendantsWithReferenceData getMockDefendantsWithSameOffences(final Offence offence, final String prosecutionAuthorityShortName) {
        final List<Offence> offences = new ArrayList<>();
        offences.add(offence);

        final Defendant defendant = new Defendant.Builder().withId(DEFENDANT_ID).withOffences(offences).withInitiationCode("S").build();
        final Defendant defendant1 = new Defendant.Builder().withId(randomUUID().toString()).withOffences(offences).withInitiationCode("S").build();
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(defendant);
        defendants.add(defendant1);
        DefendantsWithReferenceData defendantsWithReferenceData = new DefendantsWithReferenceData(defendants, prosecutionAuthorityShortName);
        defendantsWithReferenceData.setCaseDetails(CaseDetails.caseDetails().withInitiationCode("P").build());
        return defendantsWithReferenceData;
    }

    private Offence buildOffence() {
        return Offence.offence().withOffenceCode("cjsOffenceCode").withMaxPenalty("Max Penalty").build();
    }

    private Offence buildOffenceWithEmptyOffenceLocation() {
        return Offence.offence()
                .withOffenceCode("cjsOffenceCode2")
                .withOffenceLocation("").build();
    }

    private Offence buildOffenceWithSpacedOffenceLocation() {
        return Offence.offence()
                .withOffenceCode("cjsOffenceCode3")
                .withOffenceLocation(" ").build();
    }

    private Offence buildOffenceWithNullOffenceLocation() {
        return Offence.offence()
                .withOffenceCode("cjsOffenceCode4")
                .withOffenceLocation(null).build();
    }

    private List<OffenceReferenceData> getMockOffenceReferenceData() {
        return asList(OffenceReferenceData
                .offenceReferenceData()
                .withCjsOffenceCode("cjsOffenceCode")
                .withOffenceId(OFFENCE_UUID)
                .withValidFrom("2019-04-01")
                .build()
        );
    }
}
