package uk.gov.moj.cpp.prosecution.casefile;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.cps.prosecutioncasefile.InitialHearing.initialHearing;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData.offenceReferenceData;

import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.event.CaseValidationFailed;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValidationHelperTest {

    private static final String MOCK_OFFENCE_CODE = "MOCK_CODE";
    private static final String OTHER_OFFENCE_CODE = "OTHER_CODE";
    private static final String INITIATION_CODE = "S";

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Test
    void buildCaseValidationFailedEvent_whenDefendantsEmpty_should_useNullInitialHearing() {
        final Prosecution prosecution = Prosecution.prosecution().build();
        final UUID externalId = UUID.randomUUID();
        final List<Problem> problems = emptyList();
        final DefendantsWithReferenceData defendantsWithReferenceData = new DefendantsWithReferenceData(emptyList());

        final CaseValidationFailed event = ValidationHelper.buildCaseValidationFailedEvent(prosecution, externalId, problems, defendantsWithReferenceData);

        assertThat(event, is(notNullValue()));
        assertThat(event.getProsecution(), is(sameInstance(prosecution)));
        assertThat(event.getExternalId(), is(externalId));
        assertThat(event.getProblems(), is(sameInstance(problems)));
        assertThat(event.getInitialHearing(), is(nullValue()));
    }

    @Test
    void buildCaseValidationFailedEvent_whenDefendantsPresent_should_useFirstDefendantInitialHearing() {
        final Prosecution prosecution = Prosecution.prosecution().build();
        final UUID externalId = UUID.randomUUID();
        final List<Problem> problems = emptyList();
        final InitialHearing hearing = initialHearing().build();
        final Defendant defendant = new Defendant.Builder()
                .withId("D1")
                .withInitialHearing(hearing)
                .build();
        final DefendantsWithReferenceData defendantsWithReferenceData = new DefendantsWithReferenceData(singletonList(defendant));

        final CaseValidationFailed event = ValidationHelper.buildCaseValidationFailedEvent(prosecution, externalId, problems, defendantsWithReferenceData);

        assertThat(event.getInitialHearing(), is(sameInstance(hearing)));
    }

    @Test
    void offenceReferenceDataList_whenCivil_should_callRetrieveOffenceDataListWithMojSowRef() {
        final Offence offence = offence(MOCK_OFFENCE_CODE);
        final OffenceReferenceData matching = offenceReferenceData().withCjsOffenceCode(MOCK_OFFENCE_CODE).build();
        when(referenceDataQueryService.retrieveOffenceDataList(eq(List.of(MOCK_OFFENCE_CODE)), eq(Optional.of(ValidationHelper.SOW_REF_VALUE_MOJ))))
                .thenReturn(singletonList(matching));

        final List<OffenceReferenceData> result = ValidationHelper.offenceReferenceDataList(referenceDataQueryService, offence, INITIATION_CODE, true);

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getCjsOffenceCode(), is(MOCK_OFFENCE_CODE));
        verify(referenceDataQueryService, never()).retrieveOffenceData(any(), any());
    }

    @Test
    void offenceReferenceDataList_whenNonCivil_should_callRetrieveOffenceData() {
        final Offence offence = offence(MOCK_OFFENCE_CODE);
        final OffenceReferenceData matching = offenceReferenceData().withCjsOffenceCode(MOCK_OFFENCE_CODE).build();
        when(referenceDataQueryService.retrieveOffenceData(offence, INITIATION_CODE)).thenReturn(singletonList(matching));

        final List<OffenceReferenceData> result = ValidationHelper.offenceReferenceDataList(referenceDataQueryService, offence, INITIATION_CODE, false);

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getCjsOffenceCode(), is(MOCK_OFFENCE_CODE));
        verify(referenceDataQueryService, never()).retrieveOffenceDataList(any(), any());
    }

    @Test
    void offenceReferenceDataList_whenCivilAndCodeMismatch_should_filterOutNonMatching() {
        final Offence offence = offence(MOCK_OFFENCE_CODE);
        final OffenceReferenceData nonMatching = offenceReferenceData().withCjsOffenceCode(OTHER_OFFENCE_CODE).build();
        final OffenceReferenceData matching = offenceReferenceData().withCjsOffenceCode(MOCK_OFFENCE_CODE).build();
        when(referenceDataQueryService.retrieveOffenceDataList(any(), any())).thenReturn(asList(nonMatching, matching));

        final List<OffenceReferenceData> result = ValidationHelper.offenceReferenceDataList(referenceDataQueryService, offence, INITIATION_CODE, true);

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getCjsOffenceCode(), is(MOCK_OFFENCE_CODE));
    }

    @Test
    void offenceReferenceDataList_whenNonCivilAndCodeMismatch_should_filterOutNonMatching() {
        final Offence offence = offence(MOCK_OFFENCE_CODE);
        final OffenceReferenceData nonMatching = offenceReferenceData().withCjsOffenceCode(OTHER_OFFENCE_CODE).build();
        final OffenceReferenceData matching = offenceReferenceData().withCjsOffenceCode(MOCK_OFFENCE_CODE).build();
        when(referenceDataQueryService.retrieveOffenceData(any(), any())).thenReturn(asList(nonMatching, matching));

        final List<OffenceReferenceData> result = ValidationHelper.offenceReferenceDataList(referenceDataQueryService, offence, INITIATION_CODE, false);

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getCjsOffenceCode(), is(MOCK_OFFENCE_CODE));
    }

    @Test
    void offenceReferenceDataList_whenCivilAndServiceReturnsEmpty_should_returnEmptyList() {
        final Offence offence = offence(MOCK_OFFENCE_CODE);
        when(referenceDataQueryService.retrieveOffenceDataList(any(), any())).thenReturn(emptyList());

        final List<OffenceReferenceData> result = ValidationHelper.offenceReferenceDataList(referenceDataQueryService, offence, INITIATION_CODE, true);

        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(0));
    }

    @Test
    void offenceReferenceDataList_whenNonCivilAndServiceReturnsEmpty_should_returnEmptyList() {
        final Offence offence = offence(MOCK_OFFENCE_CODE);
        when(referenceDataQueryService.retrieveOffenceData(any(), any())).thenReturn(emptyList());

        final List<OffenceReferenceData> result = ValidationHelper.offenceReferenceDataList(referenceDataQueryService, offence, INITIATION_CODE, false);

        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(0));
    }

    @Test
    void privateConstructor_should_beInaccessibleAndStillInstantiableViaReflection() throws Exception {
        final Constructor<ValidationHelper> constructor = ValidationHelper.class.getDeclaredConstructor();
        assertThat(constructor.canAccess(null), is(false));
        constructor.setAccessible(true);
        final ValidationHelper instance = constructor.newInstance();
        assertThat(instance, is(notNullValue()));
    }

    private Offence offence(final String code) {
        return Offence.offence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(code)
                .withOffenceSequenceNumber(1)
                .build();
    }
}
