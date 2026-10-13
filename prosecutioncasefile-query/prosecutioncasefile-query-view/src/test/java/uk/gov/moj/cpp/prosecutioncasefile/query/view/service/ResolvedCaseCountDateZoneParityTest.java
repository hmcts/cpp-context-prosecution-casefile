package uk.gov.moj.cpp.prosecutioncasefile.query.view.service;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.ResolvedCasesRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PCF-10 parity probe (J17 golden vs J25), count side. Identical source in both copies.
 *
 * <p>{@code countsCasesErrorsView} counts "resolved today" with {@code countOfCasesFixedByDate(LocalDate.now(), ...)}.
 * This pins that the date passed follows the JVM default zone on both lines. Findings are appended to
 * target/pcf10-zone-parity.txt.
 */
@ExtendWith(MockitoExtension.class)
class ResolvedCaseCountDateZoneParityTest {

    private static final ZoneId UTC_PLUS_14 = ZoneId.of("Pacific/Kiritimati");
    private static final ZoneId UTC_MINUS_12 = ZoneId.of("Etc/GMT+12");
    private static final Path REPORT = Paths.get("target", "pcf10-zone-parity.txt");

    @Mock
    private BusinessValidationErrorRepository businessValidationErrorRepository;

    @Mock
    private ResolvedCasesRepository resolvedCasesRepository;

    @Captor
    private ArgumentCaptor<LocalDate> resolutionDateCaptor;

    @InjectMocks
    private BusinessErrorDetailsService businessErrorDetailsService;

    private TimeZone originalDefaultZone;

    @BeforeEach
    void rememberDefaultZone() {
        originalDefaultZone = TimeZone.getDefault();
    }

    @AfterEach
    void restoreDefaultZone() {
        TimeZone.setDefault(originalDefaultZone);
    }

    @Test
    void resolvedTodayCountShouldUseJvmDefaultZoneDate() throws Exception {
        when(businessValidationErrorRepository.countOfCasesWithOutstandingErrors(empty(), empty())).thenReturn(0L);

        final LocalDate east = countDateWithDefaultZone(UTC_PLUS_14);
        final LocalDate west = countDateWithDefaultZone(UTC_MINUS_12);
        final LocalDate utc = LocalDate.now(ZoneOffset.UTC);

        Files.createDirectories(REPORT.getParent());
        Files.write(REPORT, ("query-view countOfCasesFixedByDate date: default=" + UTC_PLUS_14 + " -> " + east + ", default="
                + UTC_MINUS_12 + " -> " + west + ", UTC today=" + utc + ", surefire JVM start zone=" + originalDefaultZone.getID()
                + ", ResolvedCasesRepository interface=" + ResolvedCasesRepository.class.isInterface() + "\n").getBytes(UTF_8), CREATE, APPEND);
        assertThat(east, is(not(west)));
        assertThat(east.equals(utc) && west.equals(utc), is(false));
    }

    private LocalDate countDateWithDefaultZone(final ZoneId zone) {
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        final LocalDate before = LocalDate.now(zone);

        businessErrorDetailsService.countsCasesErrorsView(empty(), empty(), empty());

        final LocalDate after = LocalDate.now(zone);
        verify(resolvedCasesRepository, atLeastOnce()).countOfCasesFixedByDate(resolutionDateCaptor.capture(), eq(empty()), eq(empty()), eq(empty()));
        final LocalDate counted = resolutionDateCaptor.getValue();
        assertThat(counted, anyOf(is(before), is(after)));
        return counted;
    }
}
