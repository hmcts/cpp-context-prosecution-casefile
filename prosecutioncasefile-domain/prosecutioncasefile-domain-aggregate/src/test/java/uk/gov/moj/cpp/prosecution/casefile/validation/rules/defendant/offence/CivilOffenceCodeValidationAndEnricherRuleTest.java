package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData.offenceReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.OFFENCE_CODE_NOT_SUPPORTED;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.OFFENCE_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.OFFENCE_SEQUENCE_NO;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CivilOffenceCodeValidationAndEnricherRuleTest {

    private static final String VALID_OFFENCE_CODE = "AB12345";
    private static final String UNSUPPORTED_OFFENCE_CODE = "ZZ99999";
    private static final String DIFFERENT_OFFENCE_CODE = "DIFF000";
    private static final String GENERIC_ALTERED_OFFENCE_CODE_UPPER = "998A";
    private static final String GENERIC_ALTERED_OFFENCE_CODE_LOWER = "998a";
    private static final String INITIATION_CODE = "S";
    private static final String FEE_STATUS_PAID = "PAID";
    private static final String SOW_REF_VALUE_MOJ = "moj";
    private static final String DEFENDANT_ID = "1234243";

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @InjectMocks
    private CivilOffenceCodeValidationAndEnricherRule rule;

    @Test
    void shouldReturnValidWhenFeeStatusIsNotNull() {
        final CaseDetails caseDetails = CaseDetails.caseDetails().withFeeStatus(FEE_STATUS_PAID).build();
        final DefendantWithReferenceData input = new DefendantWithReferenceData(null, new ReferenceDataVO(), caseDetails);

        final ValidationResult result = rule.validate(input, referenceDataQueryService);

        assertThat(result.isValid(), is(true));
        verifyNoInteractions(referenceDataQueryService);
    }

    @Test
    void shouldReturnValidWhenDefendantIsNull() {
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode(INITIATION_CODE).build();
        final DefendantWithReferenceData input = new DefendantWithReferenceData(null, new ReferenceDataVO(), caseDetails);

        final ValidationResult result = rule.validate(input, referenceDataQueryService);

        assertThat(result.isValid(), is(true));
        verifyNoInteractions(referenceDataQueryService);
    }

    @Test
    void shouldReturnValidWhenDefendantOffencesIsNull() {
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode(INITIATION_CODE).build();
        final Defendant defendant = new Defendant.Builder().withId(DEFENDANT_ID).build();
        final DefendantWithReferenceData input = new DefendantWithReferenceData(defendant, new ReferenceDataVO(), caseDetails);

        final ValidationResult result = rule.validate(input, referenceDataQueryService);

        assertThat(result.isValid(), is(true));
        verifyNoInteractions(referenceDataQueryService);
    }

    @Test
    void shouldReturnValidWhenDefendantOffencesIsEmpty() {
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode(INITIATION_CODE).build();
        final Defendant defendant = new Defendant.Builder().withId(DEFENDANT_ID).withOffences(emptyList()).build();
        final DefendantWithReferenceData input = new DefendantWithReferenceData(defendant, new ReferenceDataVO(), caseDetails);

        final ValidationResult result = rule.validate(input, referenceDataQueryService);

        assertThat(result.isValid(), is(true));
        verifyNoInteractions(referenceDataQueryService);
    }

    @Test
    void shouldReturnValidWhenOffenceCodeIsGenericAlteredUpperCase() {
        final DefendantWithReferenceData input = buildSingleOffenceInput(GENERIC_ALTERED_OFFENCE_CODE_UPPER, new ReferenceDataVO());

        final ValidationResult result = rule.validate(input, referenceDataQueryService);

        assertThat(result.isValid(), is(true));
        verifyNoInteractions(referenceDataQueryService);
    }

    @Test
    void shouldReturnValidWhenOffenceCodeIsGenericAlteredLowerCase() {
        final DefendantWithReferenceData input = buildSingleOffenceInput(GENERIC_ALTERED_OFFENCE_CODE_LOWER, new ReferenceDataVO());

        final ValidationResult result = rule.validate(input, referenceDataQueryService);

        assertThat(result.isValid(), is(true));
        verifyNoInteractions(referenceDataQueryService);
    }

    @Test
    void shouldReturnValidWhenOffenceCodeAlreadyPresentInVOReferenceData() {
        final ReferenceDataVO vo = new ReferenceDataVO();
        vo.setOffenceReferenceData(new ArrayList<>(List.of(
                offenceReferenceData().withCjsOffenceCode(VALID_OFFENCE_CODE).build())));
        final DefendantWithReferenceData input = buildSingleOffenceInput(VALID_OFFENCE_CODE, vo);

        final ValidationResult result = rule.validate(input, referenceDataQueryService);

        assertThat(result.isValid(), is(true));
        verifyNoInteractions(referenceDataQueryService);
    }

    @Test
    void shouldEnrichExistingVOListAndReturnValidWhenServiceReturnsMatch() {
        final ReferenceDataVO vo = new ReferenceDataVO();
        when(referenceDataQueryService.retrieveOffenceDataList(List.of(VALID_OFFENCE_CODE), Optional.of(SOW_REF_VALUE_MOJ)))
                .thenReturn(List.of(offenceReferenceData().withCjsOffenceCode(VALID_OFFENCE_CODE).build()));

        final DefendantWithReferenceData input = buildSingleOffenceInput(VALID_OFFENCE_CODE, vo);

        final ValidationResult result = rule.validate(input, referenceDataQueryService);

        assertThat(result.isValid(), is(true));
        assertThat(vo.getOffenceReferenceData(), hasSize(1));
        assertThat(vo.getOffenceReferenceData().get(0).getCjsOffenceCode(), is(VALID_OFFENCE_CODE));
    }

    @Test
    void shouldSetVOOffenceReferenceDataWhenItIsNullAndServiceReturnsMatch() {
        final ReferenceDataVO referenceDataVO = mock(ReferenceDataVO.class);
        when(referenceDataVO.getOffenceReferenceData())
                .thenReturn(new ArrayList<>())
                .thenReturn(null);
        when(referenceDataQueryService.retrieveOffenceDataList(List.of(VALID_OFFENCE_CODE), Optional.of(SOW_REF_VALUE_MOJ)))
                .thenReturn(List.of(offenceReferenceData().withCjsOffenceCode(VALID_OFFENCE_CODE).build()));

        final DefendantWithReferenceData input = buildSingleOffenceInput(VALID_OFFENCE_CODE, referenceDataVO);

        final ValidationResult result = rule.validate(input, referenceDataQueryService);

        assertThat(result.isValid(), is(true));

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<List<OffenceReferenceData>> captor = ArgumentCaptor.forClass(List.class);
        verify(referenceDataVO).setOffenceReferenceData(captor.capture());
        assertThat(captor.getValue(), hasSize(1));
        assertThat(captor.getValue().get(0).getCjsOffenceCode(), is(VALID_OFFENCE_CODE));
    }

    @Test
    void shouldReturnProblemWhenServiceReturnsEmptyListAndOffenceNotInVO() {
        when(referenceDataQueryService.retrieveOffenceDataList(List.of(UNSUPPORTED_OFFENCE_CODE), Optional.of(SOW_REF_VALUE_MOJ)))
                .thenReturn(emptyList());

        final UUID offenceId = UUID.randomUUID();
        final DefendantWithReferenceData input = buildSingleOffenceInput(UNSUPPORTED_OFFENCE_CODE, offenceId, 7, new ReferenceDataVO());

        final ValidationResult result = rule.validate(input, referenceDataQueryService);

        assertThat(result.isValid(), is(false));
        assertThat(result.problems(), hasSize(1));
        final Problem problem = result.problems().get(0);
        assertThat(problem.getCode(), is(OFFENCE_CODE_NOT_SUPPORTED.name()));
        assertThat(problem.getValues(), hasSize(2));
        final ProblemValue codeValue = problem.getValues().get(0);
        assertThat(codeValue.getId(), is(offenceId.toString()));
        assertThat(codeValue.getKey(), is(OFFENCE_CODE.getValue()));
        assertThat(codeValue.getValue(), is(UNSUPPORTED_OFFENCE_CODE));
        final ProblemValue sequenceValue = problem.getValues().get(1);
        assertThat(sequenceValue.getId(), is(offenceId.toString()));
        assertThat(sequenceValue.getKey(), is(OFFENCE_SEQUENCE_NO.getValue()));
        assertThat(sequenceValue.getValue(), is("7"));
    }

    @Test
    void shouldReturnProblemWhenServiceReturnsOnlyNonMatchingReferenceData() {
        when(referenceDataQueryService.retrieveOffenceDataList(List.of(UNSUPPORTED_OFFENCE_CODE), Optional.of(SOW_REF_VALUE_MOJ)))
                .thenReturn(List.of(offenceReferenceData().withCjsOffenceCode(DIFFERENT_OFFENCE_CODE).build()));

        final DefendantWithReferenceData input = buildSingleOffenceInput(UNSUPPORTED_OFFENCE_CODE, new ReferenceDataVO());

        final ValidationResult result = rule.validate(input, referenceDataQueryService);

        assertThat(result.isValid(), is(false));
        assertThat(result.problems(), hasSize(1));
        assertThat(result.problems().get(0).getCode(), is(OFFENCE_CODE_NOT_SUPPORTED.name()));
    }

    @Test
    void shouldRaiseProblemOnlyForUnsupportedOffenceWhenMixedOffences() {
        when(referenceDataQueryService.retrieveOffenceDataList(List.of(VALID_OFFENCE_CODE), Optional.of(SOW_REF_VALUE_MOJ)))
                .thenReturn(List.of(offenceReferenceData().withCjsOffenceCode(VALID_OFFENCE_CODE).build()));
        when(referenceDataQueryService.retrieveOffenceDataList(List.of(UNSUPPORTED_OFFENCE_CODE), Optional.of(SOW_REF_VALUE_MOJ)))
                .thenReturn(emptyList());

        final ReferenceDataVO vo = new ReferenceDataVO();
        final Offence validOffence = Offence.offence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(VALID_OFFENCE_CODE)
                .withOffenceSequenceNumber(1)
                .build();
        final Offence unsupportedOffence = Offence.offence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(UNSUPPORTED_OFFENCE_CODE)
                .withOffenceSequenceNumber(2)
                .build();
        final Defendant defendant = new Defendant.Builder()
                .withId(DEFENDANT_ID)
                .withOffences(List.of(validOffence, unsupportedOffence))
                .withInitiationCode("C")
                .build();
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode(INITIATION_CODE).build();
        final DefendantWithReferenceData input = new DefendantWithReferenceData(defendant, vo, caseDetails);

        final ValidationResult result = rule.validate(input, referenceDataQueryService);

        assertThat(result.isValid(), is(false));
        assertThat(result.problems(), hasSize(1));
        final Problem problem = result.problems().get(0);
        assertThat(problem.getCode(), is(OFFENCE_CODE_NOT_SUPPORTED.name()));
        assertThat(problem.getValues().get(0).getValue(), is(UNSUPPORTED_OFFENCE_CODE));
        assertThat(vo.getOffenceReferenceData(), hasSize(1));
        assertThat(vo.getOffenceReferenceData().get(0).getCjsOffenceCode(), is(VALID_OFFENCE_CODE));
    }

    @Test
    void shouldNotInvokeServiceForGenericOffenceWhenMixedWithEnrichableOffence() {
        when(referenceDataQueryService.retrieveOffenceDataList(List.of(VALID_OFFENCE_CODE), Optional.of(SOW_REF_VALUE_MOJ)))
                .thenReturn(List.of(offenceReferenceData().withCjsOffenceCode(VALID_OFFENCE_CODE).build()));

        final ReferenceDataVO vo = new ReferenceDataVO();
        final Offence enrichableOffence = Offence.offence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(VALID_OFFENCE_CODE)
                .withOffenceSequenceNumber(1)
                .build();
        final Offence genericOffence = Offence.offence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(GENERIC_ALTERED_OFFENCE_CODE_UPPER)
                .withOffenceSequenceNumber(2)
                .build();
        final Defendant defendant = new Defendant.Builder()
                .withId(DEFENDANT_ID)
                .withOffences(List.of(enrichableOffence, genericOffence))
                .build();
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode(INITIATION_CODE).build();
        final DefendantWithReferenceData input = new DefendantWithReferenceData(defendant, vo, caseDetails);

        final ValidationResult result = rule.validate(input, referenceDataQueryService);

        assertThat(result.isValid(), is(true));
        assertThat(result.problems(), is(empty()));
        verify(referenceDataQueryService, never())
                .retrieveOffenceDataList(List.of(GENERIC_ALTERED_OFFENCE_CODE_UPPER), Optional.of(SOW_REF_VALUE_MOJ));
        assertThat(vo.getOffenceReferenceData(), hasSize(1));
    }

    @Test
    void shouldRaiseProblemForEachUnsupportedOffenceWhenMultipleAreUnsupported() {
        final String secondUnsupportedCode = "QQ22222";
        when(referenceDataQueryService.retrieveOffenceDataList(List.of(UNSUPPORTED_OFFENCE_CODE), Optional.of(SOW_REF_VALUE_MOJ)))
                .thenReturn(emptyList());
        when(referenceDataQueryService.retrieveOffenceDataList(List.of(secondUnsupportedCode), Optional.of(SOW_REF_VALUE_MOJ)))
                .thenReturn(emptyList());

        final Offence offenceOne = Offence.offence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(UNSUPPORTED_OFFENCE_CODE)
                .withOffenceSequenceNumber(1)
                .build();
        final Offence offenceTwo = Offence.offence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(secondUnsupportedCode)
                .withOffenceSequenceNumber(2)
                .build();
        final Defendant defendant = new Defendant.Builder()
                .withId(DEFENDANT_ID)
                .withOffences(List.of(offenceOne, offenceTwo))
                .build();
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode(INITIATION_CODE).build();
        final DefendantWithReferenceData input = new DefendantWithReferenceData(defendant, new ReferenceDataVO(), caseDetails);

        final ValidationResult result = rule.validate(input, referenceDataQueryService);

        assertThat(result.isValid(), is(false));
        assertThat(result.problems(), hasSize(2));
        final List<String> reportedCodes = result.problems().stream()
                .map(p -> p.getValues().get(0).getValue())
                .toList();
        assertThat(reportedCodes, containsInAnyOrder(UNSUPPORTED_OFFENCE_CODE, secondUnsupportedCode));
    }

    private DefendantWithReferenceData buildSingleOffenceInput(final String offenceCode, final ReferenceDataVO vo) {
        return buildSingleOffenceInput(offenceCode, UUID.randomUUID(), 1, vo);
    }

    private DefendantWithReferenceData buildSingleOffenceInput(final String offenceCode,
                                                                final UUID offenceId,
                                                                final int sequenceNumber,
                                                                final ReferenceDataVO vo) {
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode(INITIATION_CODE).build();
        final Offence offence = Offence.offence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceSequenceNumber(sequenceNumber)
                .build();
        final Defendant defendant = new Defendant.Builder()
                .withId(DEFENDANT_ID)
                .withOffences(List.of(offence))
                .withInitiationCode("C")
                .build();
        return new DefendantWithReferenceData(defendant, vo, caseDetails);
    }
}
