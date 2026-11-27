package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.OffenceLegacy;

import java.util.UUID;

import javax.inject.Inject;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class OffenceLegacyRepositoryTest {

    private static final UUID OFFENCE_ID = UUID.randomUUID();

    @Inject
    private OffenceLegacyRepository offenceLegacyRepository;

    @Test
    public void shouldFindOffence() {
        OffenceLegacy offence = getOffence();
        offenceLegacyRepository.save(offence);

        OffenceLegacy svdOffence = offenceLegacyRepository.findBy(offence.getOffenceId());
        assertThat(svdOffence.getOffenceId(), equalTo(offence.getOffenceId()));
        offenceLegacyRepository.remove(offence);
    }

    private OffenceLegacy getOffence() {
        OffenceLegacy offence = new OffenceLegacy();
        offence.setOffenceId(OFFENCE_ID);
        return offence;
    }
}
