package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.CaseRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AssignCase;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AssociateEnterpriseId;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.UnassignCase;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

@ServiceComponent(COMMAND_HANDLER)
public class SjpProsecutionHandler extends BaseProsecutionCaseFileHandler {

    @Inject
    ReferenceDataQueryService referenceDataQueryService;

    @Inject
    private Instance<DefendantRefDataEnricher> defendantRefDataEnrichers;

    @Inject
    private Instance<CaseRefDataEnricher> caseRefDataEnrichers;

    @Handles("prosecutioncasefile.command.initiate-sjp-prosecution-with-reference-data")
    public void handleInitiateSjpProsecutionWithReferenceData(final Envelope<ProsecutionWithReferenceData> envelope) throws EventStreamException {
        final ProsecutionWithReferenceData initiateSjpProsecutionWithReferenceData = envelope.payload();

        appendEventsToStream(initiateSjpProsecutionWithReferenceData.getProsecution().getCaseDetails().getCaseId(), envelope,
                prosecutionCaseFile ->
                        prosecutionCaseFile.receiveSjpProsecution(initiateSjpProsecutionWithReferenceData,
                                newArrayList(caseRefDataEnrichers.iterator()),
                                newArrayList(defendantRefDataEnrichers.iterator()),
                                referenceDataQueryService));
    }

    @Handles("prosecutioncasefile.command.associate-enterprise-id")
    public void handleSjpProsecutionAssociateEnterpriseId(final Envelope<AssociateEnterpriseId> envelope) throws EventStreamException {
        final AssociateEnterpriseId associateEnterpriseId = envelope.payload();

        appendEventsToStream(
                associateEnterpriseId.getCaseId(),
                envelope,
                prosecutionCaseFile -> prosecutionCaseFile.associateEnterpriseId(associateEnterpriseId.getEnterpriseId()));
    }

    @Handles("prosecutioncasefile.command.assign-case")
    public void handleAssignCase(final Envelope<AssignCase> assignCaseEnvelope) throws EventStreamException {
        final AssignCase assignCase = assignCaseEnvelope.payload();
        appendEventsToStream(assignCase.getCaseId(), assignCaseEnvelope, prosecutionCaseFile -> prosecutionCaseFile.assignCase(assignCase.getCaseId()));
    }

    @Handles("prosecutioncasefile.command.unassign-case")
    public void handleUnAssignCase(final Envelope<UnassignCase> unassignCaseEnvelope) throws EventStreamException {
        final UnassignCase unassignCase = unassignCaseEnvelope.payload();
        appendEventsToStream(unassignCase.getCaseId(), unassignCaseEnvelope, prosecutionCaseFile -> prosecutionCaseFile.unassignCase(unassignCase.getCaseId()));
    }

}
