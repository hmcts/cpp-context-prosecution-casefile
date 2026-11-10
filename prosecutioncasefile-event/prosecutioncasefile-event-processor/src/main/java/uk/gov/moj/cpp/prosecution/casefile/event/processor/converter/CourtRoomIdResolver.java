package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CourtRoom;

import java.util.UUID;

/**
 * Picks the courtroom for a hearing: the room submitted on the initial hearing wins, otherwise the
 * courtroom that reference data resolved for the court OU code, otherwise none. Shared so that
 * Progression's hearing request and case management's group case get the same room.
 */
public final class CourtRoomIdResolver {

    private CourtRoomIdResolver() {
    }

    public static UUID resolveRoomId(final UUID submittedRoomId, final CourtRoom referenceDataCourtRoom) {
        if (nonNull(submittedRoomId)) {
            return submittedRoomId;
        }
        if (nonNull(referenceDataCourtRoom) && nonNull(referenceDataCourtRoom.getId())) {
            return fromString(referenceDataCourtRoom.getId());
        }
        return null;
    }
}
