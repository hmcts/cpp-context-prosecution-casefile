package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;

import static java.util.Optional.ofNullable;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.AlcoholOffenceDetail;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.OffenceDetails;

import java.util.UUID;

import javax.inject.Inject;

public class OffenceToOffenceDetails implements Converter<Offence, OffenceDetails> {

    @Inject
    private OffenceIdGenerator idGenerator;

    @Override
    @SuppressWarnings("squid:S1135")
    public OffenceDetails convert(final Offence offence) {
        return new OffenceDetails(
                // TODO ATCM-3870: GENERATE ID FOR HISTORICAL EVENTS
                ofNullable(offence.getOffenceId()).orElseGet(() -> idGenerator.generateId()),
                offence.getAppliedCompensation(),
                offence.getBackDuty(),
                offence.getBackDutyDateFrom(),
                offence.getBackDutyDateTo(),
                offence.getChargeDate(),
                offence.getOffenceCode(),
                offence.getOffenceCommittedDate(),
                offence.getOffenceCommittedEndDate(),
                offence.getOffenceDateCode(),
                offence.getOffenceLocation(),
                offence.getOffenceSequenceNumber(),
                offence.getOffenceWording(),
                offence.getOffenceWordingWelsh(),
                offence.getStatementOfFacts(),
                offence.getStatementOfFactsWelsh(),
                offence.getVehicleMake(),
                offence.getVehicleRegistrationMark(),
                convertAlcoholRelatedOffence(offence)
        );
    }

    private AlcoholOffenceDetail convertAlcoholRelatedOffence(final Offence offence) {
        return ofNullable(offence.getAlcoholRelatedOffence())
                .map(alcoholOffence -> new AlcoholOffenceDetail(alcoholOffence.getAlcoholLevelAmount(), alcoholOffence.getAlcoholLevelMethod()))
                .orElse(null);
    }

    // for testing - allows to control ids
    public static class OffenceIdGenerator {

        public UUID generateId() {
            return UUID.randomUUID();
        }
    }
}
