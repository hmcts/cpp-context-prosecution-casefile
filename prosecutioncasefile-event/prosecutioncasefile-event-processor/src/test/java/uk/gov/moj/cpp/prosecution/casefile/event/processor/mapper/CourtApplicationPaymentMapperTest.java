package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.Objects.isNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplicationPayment;

import org.junit.jupiter.api.Test;

public class CourtApplicationPaymentMapperTest extends MapperBase {
    @Test
    public void shouldConvertEventPayloadToCourtApplicationCourtApplicationPayment() {
        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final CourtApplicationPayment sourceCourtApplicationPayment = sourceCourtApplication.getCourtApplicationPayment();

        final uk.gov.justice.core.courts.CourtApplicationPayment targetCourtApplicationPayment = CourtApplicationPaymentMapper.convertCourtApplicationPayment.apply(sourceCourtApplicationPayment);
        assertThat(targetCourtApplicationPayment.getPaymentReference(), is(sourceCourtApplicationPayment.getPaymentReference()));
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationWhenCourtApplicationPaymentIsNull() {
        final uk.gov.justice.core.courts.CourtApplicationPayment targetCourtApplicationPayment =
                CourtApplicationPaymentMapper.convertCourtApplicationPayment.apply(null);
        assertTrue(isNull(targetCourtApplicationPayment));
    }
}