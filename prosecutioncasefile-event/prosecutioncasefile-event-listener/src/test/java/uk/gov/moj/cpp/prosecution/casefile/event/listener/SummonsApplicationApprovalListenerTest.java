package uk.gov.moj.cpp.prosecution.casefile.event.listener;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.moj.cpp.prosecution.casefile.event.DefendantsParkedForSummonsApplicationApproval.defendantsParkedForSummonsApplicationApproval;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution.prosecution;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.event.DefendantsParkedForSummonsApplicationApproval;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorCaseDetailsRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorRepository;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SummonsApplicationApprovalListenerTest {

    @Mock
    private BusinessValidationErrorRepository businessValidationErrorRepository;

    @InjectMocks
    private SummonsApplicationApprovalListener summonsApplicationApprovalListener;

    @Mock
    private BusinessValidationErrorCaseDetailsRepository businessValidationErrorCaseDetailsRepository;

    @Test
    public void processApplicationApprovalRequest_AlwaysRemoveCaseAndAssociatedDefendantErrors() {

        final Envelope<DefendantsParkedForSummonsApplicationApproval> envelope = getEnvelope();
        final UUID caseId = envelope.payload().getProsecutionWithReferenceData().getProsecution().getCaseDetails().getCaseId();
        final List<String> defendantIds = envelope.payload().getProsecutionWithReferenceData().getProsecution().getDefendants().stream().map(d -> d.getId()).collect(toList());
        summonsApplicationApprovalListener.processApplicationApprovalRequest(envelope);

        assertThat(defendantIds, hasSize(2));
        verify(businessValidationErrorRepository).deleteByCaseIdAndDefendantIdIsNull(caseId);
        defendantIds.forEach(d -> {
            verify(businessValidationErrorRepository).deleteByDefendantId(fromString(d));
        });
    }

    private Envelope<DefendantsParkedForSummonsApplicationApproval> getEnvelope() {
        final DefendantsParkedForSummonsApplicationApproval payload = defendantsParkedForSummonsApplicationApproval()
                .withApplicationId(randomUUID())
                .withProsecutionWithReferenceData(new ProsecutionWithReferenceData(prosecution()
                        .withCaseDetails(caseDetails().withCaseId(randomUUID()).build())
                        .withDefendants(newArrayList(
                                defendant().withId(randomUUID().toString()).build(),
                                defendant().withId(randomUUID().toString()).build()
                        ))
                        .build()))
                .build();

        final MetadataBuilder metadataBuilder = metadataBuilder().withId(randomUUID()).withName("prosecutioncasefile.events.defendants-parked-for-summons-application-approval");
        return envelopeFrom(metadataBuilder, payload);
    }
}