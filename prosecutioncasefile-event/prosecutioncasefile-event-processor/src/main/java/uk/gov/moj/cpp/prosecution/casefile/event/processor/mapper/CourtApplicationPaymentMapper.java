package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.core.courts.CourtApplicationPayment.courtApplicationPayment;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplicationPayment;

import java.util.function.Function;

public class CourtApplicationPaymentMapper {

    public static final Function<CourtApplicationPayment, uk.gov.justice.core.courts.CourtApplicationPayment> convertCourtApplicationPayment
            = sourceCourtApplicationPayment -> ofNullable(sourceCourtApplicationPayment)
            .map(sca -> courtApplicationPayment()
                    .withFeeStatus(sca.getFeeStatus())
                    .withPaymentReference(sca.getPaymentReference())
                    .withContestedFeeStatus(sca.getContestedFeeStatus())
                    .withContestedPaymentReference(sca.getContestedPaymentReference())
                    .build())
            .orElse(null);

    private CourtApplicationPaymentMapper() {
    }
}
