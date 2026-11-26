package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.justice.core.courts.CivilFees;
import uk.gov.justice.core.courts.FeeStatus;
import uk.gov.justice.core.courts.FeeType;
import uk.gov.justice.core.courts.InitiateCourtProceedingsForGroupCases;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.prosecution.casefile.event.GroupCasesReceived;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GroupCasesReceivedToInitiateCourtProceedingsConverterTest {

    private static final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @InjectMocks
    private GroupCasesReceivedToInitiateCourtProceedingsConverter groupCasesToProsecutionCaseConverter;

    @Mock
    private ProsecutionCaseFileDefendantToCCDefendantConverter prosecutionCaseFileDefendantToCCDefendantConverter;

    @Mock
    private ProsecutionCaseFileInitialHearingToCCHearingRequestConverter prosecutionCaseFileInitialHearingToCCHearingRequestConverter;

    private UUID groupId;
    private UUID caseId1;
    private UUID defendantId1;
    private UUID offenceId1;

    private UUID caseId2;
    private UUID defendantId2;
    private UUID offenceId2;

    @BeforeEach
    public void setUp() throws Exception {
        groupId = randomUUID();
        caseId1 = randomUUID();
        defendantId1 = randomUUID();
        offenceId1 = randomUUID();
        caseId2 = randomUUID();
        defendantId2 = randomUUID();
        offenceId2 = randomUUID();
    }

    @Test
    public void shouldConvertToInitiateCourtProceedingsForGroupCases() throws IOException {
        final String payload = resourceToString("payloads/GroupCasesReceivedToInitiateCourtProceedingsConverterTest.json");
        final String groupCasesReceivedPayload = replaceVariables(payload);
        final GroupCasesReceived groupCasesReceived = objectMapper.readValue(groupCasesReceivedPayload, GroupCasesReceived.class);
        when(prosecutionCaseFileInitialHearingToCCHearingRequestConverter.convert(any(), any())).thenAnswer(this::buildListHearingRequestAnswer);
        when(prosecutionCaseFileDefendantToCCDefendantConverter.convert(any(), any())).thenAnswer(this::buildDefendantListAnswer);

        final InitiateCourtProceedingsForGroupCases initiateCourtProceedingsForGroupCases = groupCasesToProsecutionCaseConverter.convert(groupCasesReceived);
        assertThat(initiateCourtProceedingsForGroupCases.getGroupId(), is(groupId));
        final List<ListHearingRequest> listHearingRequests = initiateCourtProceedingsForGroupCases.getCourtReferral().getListHearingRequests();
        assertThat(listHearingRequests.size(), is(1));
        assertThat(listHearingRequests.get(0).getListDefendantRequests().get(0).getDefendantId(), is(defendantId1));

        final List<ProsecutionCase> prosecutionCases = initiateCourtProceedingsForGroupCases.getCourtReferral().getProsecutionCases();
        assertThat(prosecutionCases.size(), is(2));
        final ProsecutionCase prosecutionCase1 = prosecutionCases.stream().filter(prosecutionCase -> caseId1.equals(prosecutionCase.getId())).findFirst().orElse(null);
        assertThat(prosecutionCase1, is(notNullValue()));
        assertThat(prosecutionCase1.getGroupId(), is(groupId));
        assertThat(prosecutionCase1.getIsGroupMaster(), is(true));
        assertThat(prosecutionCase1.getIsGroupMember(), is(true));
        assertThat(prosecutionCase1.getIsCivil(), is(true));
        assertThat(prosecutionCase1.getDefendants().size(), is(1));
        assertThat(prosecutionCase1.getDefendants().get(0).getId(), is(defendantId1));
        assertThat(prosecutionCase1.getDefendants().get(0).getOffences().size(), is(1));
        assertThat(prosecutionCase1.getDefendants().get(0).getOffences().get(0).getId(), is(offenceId1));
        assertCivilFees(prosecutionCase1, FeeStatus.OUTSTANDING, null);

        final ProsecutionCase prosecutionCase2 = prosecutionCases.stream().filter(prosecutionCase -> caseId2.equals(prosecutionCase.getId())).findFirst().orElse(null);
        assertThat(prosecutionCase2, is(notNullValue()));
        assertThat(prosecutionCase2.getGroupId(), is(groupId));
        assertThat(prosecutionCase2.getIsGroupMaster(), is(false));
        assertThat(prosecutionCase2.getIsGroupMember(), is(true));
        assertThat(prosecutionCase2.getIsCivil(), is(true));
        assertThat(prosecutionCase2.getDefendants().size(), is(1));
        assertThat(prosecutionCase2.getDefendants().get(0).getId(), is(defendantId2));
        assertThat(prosecutionCase2.getDefendants().get(0).getOffences().size(), is(1));
        assertThat(prosecutionCase2.getDefendants().get(0).getOffences().get(0).getId(), is(offenceId2));
        assertCivilFees(prosecutionCase2, FeeStatus.OUTSTANDING, null);
    }

    @Test
    void shouldConvertToInitiateCourtProceedingsForGroupCasesWithPaymentReference() throws IOException {
        final String payload = resourceToString("payloads/GroupCasesReceivedToInitiateCourtProceedingsConverterTestWithPaymentReference.json");
        final String groupCasesReceivedPayload = replaceVariables(payload);
        final GroupCasesReceived groupCasesReceived = objectMapper.readValue(groupCasesReceivedPayload, GroupCasesReceived.class);

        final InitiateCourtProceedingsForGroupCases initiateCourtProceedingsForGroupCases = groupCasesToProsecutionCaseConverter.convert(groupCasesReceived);

        final List<ProsecutionCase> prosecutionCases = initiateCourtProceedingsForGroupCases.getCourtReferral().getProsecutionCases();
        assertThat(prosecutionCases.size(), is(2));
        final ProsecutionCase prosecutionCase1 = prosecutionCases.stream().filter(prosecutionCase -> caseId1.equals(prosecutionCase.getId())).findFirst().orElse(null);
        final ProsecutionCase prosecutionCase2 = prosecutionCases.stream().filter(prosecutionCase -> caseId2.equals(prosecutionCase.getId())).findFirst().orElse(null);
        assertCivilFees(prosecutionCase1, FeeStatus.SATISFIED, "paymentReference01");
        assertCivilFees(prosecutionCase2, FeeStatus.SATISFIED, "paymentReference01");
    }

    private static void assertCivilFees(final ProsecutionCase prosecutionCase, final FeeStatus feeStatus, final String paymentReference) {
        final List<CivilFees> civilFees = prosecutionCase.getCivilFees();
        assertThat(civilFees.size(), is(2));

        final CivilFees initialFee = civilFees.get(0);
        Assertions.assertNotNull(initialFee);
        assertThat(initialFee.getFeeType(), is(FeeType.INITIAL));
        assertThat(initialFee.getFeeStatus(), is(feeStatus));
        assertThat(initialFee.getPaymentReference(), is(paymentReference));

        final CivilFees contestedFee = civilFees.get(1);
        Assertions.assertNotNull(contestedFee);
        assertThat(contestedFee.getFeeType(), is(FeeType.CONTESTED));
        assertThat(contestedFee.getFeeStatus(), is(FeeStatus.NOT_APPLICABLE));
        Assertions.assertNull(contestedFee.getPaymentReference());
    }

    private List<ListHearingRequest> buildListHearingRequestAnswer(final InvocationOnMock invocation){
        final Defendant defendant = (Defendant)invocation.getArgument(0, List.class).get(0);
        return asList(ListHearingRequest.listHearingRequest()
                .withListDefendantRequests(asList(ListDefendantRequest.listDefendantRequest()
                        .withDefendantId(fromString(defendant.getId()))
                        .build()))
                .build());
    }

    private List<uk.gov.justice.core.courts.Defendant> buildDefendantListAnswer(final InvocationOnMock invocation){
        final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant defendant = (uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant)invocation.getArgument(0, List.class).get(0);
        return asList(uk.gov.justice.core.courts.Defendant.defendant()
                .withId(fromString(defendant.getId()))
                .withOffences(asList(Offence.offence()
                        .withId(defendant.getOffences().get(0).getOffenceId())
                        .build()))
                .build());
    }

    private String resourceToString(final String path, final Object... placeholders) {
        try (final InputStream systemResourceAsStream = getSystemResourceAsStream(path)) {
            assertThat(systemResourceAsStream, is(notNullValue()));
            return format(IOUtils.toString(systemResourceAsStream), placeholders);
        } catch (final IOException e) {
            fail("Error consuming file from location " + path);
            throw new UncheckedIOException(e);
        }
    }

    private String replaceVariables(final String payload){
        return payload.replaceAll("GROUP_ID", groupId.toString())
                .replaceAll("CASE_ID1", caseId1.toString())
                .replaceAll("DEFENDANT_ID1", defendantId1.toString())
                .replaceAll("OFFENCE_ID1", offenceId1.toString())
                .replaceAll("CASE_ID2", caseId2.toString())
                .replaceAll("DEFENDANT_ID2", defendantId2.toString())
                .replaceAll("OFFENCE_ID2", offenceId2.toString());
    }
}