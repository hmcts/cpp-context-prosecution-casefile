package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.Objects.isNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.BoxHearingRequest;

import org.junit.jupiter.api.Test;

public class JurisdictionTypeMapperTest extends MapperBase {

    @Test
    public void shouldConvertEventPayloadToBoxHearingRequestJurisdictionType() {
        final BoxHearingRequest boxHearingRequest = sourceSubmitApplication.getBoxHearingRequest();

        final JurisdictionType targetJurisdictionType = JurisdictionTypeMapper.convertJurisdictionType.apply(boxHearingRequest.getJurisdictionType());
        assertThat(targetJurisdictionType.name(), is(boxHearingRequest.getJurisdictionType().name()));
    }

    @Test
    public void shouldConvertEventPayloadToBoxHearingRequestWhenJurisdictionTypeIsNull() {
        final BoxHearingRequest boxHearingRequest = getBoxHearingRequest();

        final JurisdictionType targetJurisdictionType = JurisdictionTypeMapper.convertJurisdictionType.apply(boxHearingRequest.getJurisdictionType());
        assertTrue(isNull(targetJurisdictionType));
    }

    private BoxHearingRequest getBoxHearingRequest() {
        final BoxHearingRequest boxHearingRequest = sourceSubmitApplication.getBoxHearingRequest();
        return new BoxHearingRequest(boxHearingRequest.getApplicationDueDate(),
                boxHearingRequest.getCourtCentre(),
                boxHearingRequest.getId(),
                null,
                boxHearingRequest.getSendAppointmentLetter()
        );
    }
}