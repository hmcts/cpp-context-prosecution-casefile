package uk.gov.moj.cpp.prosecution.casefile.event.listener;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.ResolvedCases;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.ResolvedCasesRepository;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ResolvedCase;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
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

/**
 * PCF-10 parity probe (J17 golden vs J25). Identical source in both copies.
 *
 * <p>{@code resolvedCaseErrorsReceived} stores {@code LocalDate.now()} as {@code resolution_date}. These tests pin
 * that the stored date follows the JVM default zone on both lines, that the framework
 * {@code EntityManagerProducer.create()} resets the default zone to UTC, and which EntityManager source
 * {@code ResolvedCasesRepository} uses (DeltaSpike/CDI producer on J17, {@code @PersistenceContext} on J25).
 * Findings are written to target/pcf10-zone-parity.txt.
 */
@ExtendWith(MockitoExtension.class)
public class ResolvedCaseDateZoneParityTest {

    private static final ZoneId UTC_PLUS_14 = ZoneId.of("Pacific/Kiritimati");
    private static final ZoneId UTC_MINUS_12 = ZoneId.of("Etc/GMT+12");
    private static final String FRAMEWORK_ENTITY_MANAGER_PRODUCER = "uk.gov.justice.services.persistence.EntityManagerProducer";
    private static final String DELTASPIKE_REPOSITORY = "org.apache.deltaspike.data.api.Repository";
    private static final Path REPORT = Paths.get("target", "pcf10-zone-parity.txt");

    @Mock
    private ResolvedCasesRepository resolvedCasesRepository;

    @Mock
    private Envelope<ResolvedCase> resolvedCaseEnvelope;

    @Mock
    private ResolvedCase resolvedCase;

    @Captor
    private ArgumentCaptor<ResolvedCases> resolvedCasesCaptor;

    @InjectMocks
    private ProsecutionReceivedListener prosecutionReceivedListener;

    private TimeZone originalDefaultZone;

    @BeforeEach
    public void rememberDefaultZone() {
        originalDefaultZone = TimeZone.getDefault();
    }

    @AfterEach
    public void restoreDefaultZone() {
        TimeZone.setDefault(originalDefaultZone);
    }

    @Test
    public void resolutionDateShouldFollowJvmDefaultZone() throws Exception {
        when(resolvedCaseEnvelope.payload()).thenReturn(resolvedCase);

        final LocalDate east = resolutionDateWrittenWithDefaultZone(UTC_PLUS_14);
        final LocalDate west = resolutionDateWrittenWithDefaultZone(UTC_MINUS_12);
        final LocalDate utc = LocalDate.now(ZoneOffset.UTC);

        report("listener resolution_date: default=" + UTC_PLUS_14 + " -> " + east + ", default=" + UTC_MINUS_12 + " -> " + west
                + ", UTC today=" + utc + ", surefire JVM start zone=" + originalDefaultZone.getID());
        // The two zones are 26 hours apart: the writes always land on different dates and at least one is not the UTC date
        assertThat(east, is(not(west)));
        assertThat(east.equals(utc) && west.equals(utc), is(false));
    }

    @Test
    public void frameworkEntityManagerProducerShouldResetJvmDefaultZoneToUtc() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone(UTC_PLUS_14));
        final Class<?> producerClass = Class.forName(FRAMEWORK_ENTITY_MANAGER_PRODUCER);
        final Object producer = producerClass.getDeclaredConstructor().newInstance();
        final Field entityManagerFactory = producerClass.getDeclaredField("entityManagerFactory");
        entityManagerFactory.setAccessible(true);
        entityManagerFactory.set(producer, mock(entityManagerFactory.getType()));

        producerClass.getMethod("create").invoke(producer);

        report("framework EntityManagerProducer.create() from " + codeSource(producerClass) + ": default zone "
                + UTC_PLUS_14 + " -> " + TimeZone.getDefault().getID());
        assertThat(TimeZone.getDefault().getID(), is("UTC"));
    }

    @Test
    public void resolvedCasesRepositoryEntityManagerSourceShouldBeKnown() throws Exception {
        final String source = entityManagerSource(ResolvedCasesRepository.class);

        report("ResolvedCasesRepository (" + codeSource(ResolvedCasesRepository.class) + ") EntityManager source: " + source);
        assertThat(source, anyOf(is("deltaspike-repository: CDI-produced EntityManager (framework producer, resets zone to UTC)"),
                is("@PersistenceContext: container-managed EntityManager (framework producer not reached)")));
    }

    private LocalDate resolutionDateWrittenWithDefaultZone(final ZoneId zone) {
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        final LocalDate before = LocalDate.now(zone);

        prosecutionReceivedListener.resolvedCaseErrorsReceived(resolvedCaseEnvelope);

        final LocalDate after = LocalDate.now(zone);
        verify(resolvedCasesRepository, atLeastOnce()).save(resolvedCasesCaptor.capture());
        final LocalDate written = resolvedCasesCaptor.getValue().getResolutionDate();
        assertThat(written, anyOf(is(before), is(after)));
        return written;
    }

    private static String entityManagerSource(final Class<?> repositoryClass) {
        if (repositoryClass.isInterface()) {
            for (final Annotation annotation : repositoryClass.getAnnotations()) {
                if (annotation.annotationType().getName().equals(DELTASPIKE_REPOSITORY)) {
                    return "deltaspike-repository: CDI-produced EntityManager (framework producer, resets zone to UTC)";
                }
            }
            return "interface without DeltaSpike @Repository";
        }
        for (final Field field : repositoryClass.getDeclaredFields()) {
            if (field.getType().getName().endsWith(".persistence.EntityManager")) {
                for (final Annotation annotation : field.getAnnotations()) {
                    if (annotation.annotationType().getSimpleName().equals("PersistenceContext")) {
                        return "@PersistenceContext: container-managed EntityManager (framework producer not reached)";
                    }
                    if (annotation.annotationType().getSimpleName().equals("Inject")) {
                        return "@Inject: CDI-produced EntityManager (framework producer, resets zone to UTC)";
                    }
                }
            }
        }
        return "unknown";
    }

    private static String codeSource(final Class<?> type) {
        final String location = type.getProtectionDomain().getCodeSource().getLocation().getPath();
        return location.substring(location.lastIndexOf('/', location.length() - 2) + 1);
    }

    private static void report(final String line) throws Exception {
        Files.createDirectories(REPORT.getParent());
        Files.write(REPORT, (line + "\n").getBytes(UTF_8), CREATE, APPEND);
    }
}
