package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.systemidmapper.client.ResultCode.CONFLICT;
import static uk.gov.moj.cpp.systemidmapper.client.ResultCode.OK;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.enterpriseid.generator.EnterpriseIdGenerator;
import uk.gov.moj.cpp.enterpriseid.generator.EnterpriseIdGeneratorImpl;
import uk.gov.moj.cpp.enterpriseid.mapper.EnterpriseIdServiceImpl;
import uk.gov.moj.cpp.systemidmapper.client.AdditionResponse;
import uk.gov.moj.cpp.systemidmapper.client.ResultCode;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMap;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

/**
 * PCF-08 parity characterisation (J17 golden master) of the enterprise-id allocation used by
 * {@link SjpProsecutionProcessor}. J17 runs the released {@code system-enterprise-id:0.0.3} jar;
 * J25 runs the vendored {@code prosecutioncasefile-system-enterprise-id} module (same packages and
 * API). Every assertion encodes the J17 result. The method bodies between the SCENARIOS markers are
 * identical in the J17 and J25 copies.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EnterpriseIdParityTest {

    // ---- SCENARIOS-BEGIN ----

    private static final String ALPHABET = "BCDFGHJKLMNPQRSTVWXYZ123456789";
    private static final UUID CASE_ID = UUID.fromString("2b4e2a54-5d1a-4f43-9d0e-3f7c1c8f9a01");
    private static final UUID SYSTEM_USER_ID = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final int ATTEMPT_BUDGET = 1000;

    @Mock
    private Logger logger;

    @Mock
    private SystemIdMapperClient idMapperClient;

    @Mock
    private EnterpriseIdGenerator idGenerator;

    @Mock
    private SystemUserProvider systemUserProvider;

    @InjectMocks
    private EnterpriseIdServiceImpl enterpriseIdService;

    private final AtomicInteger generated = new AtomicInteger();

    @BeforeEach
    public void setUp() {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(SYSTEM_USER_ID));
        when(idGenerator.enterpriseId()).thenAnswer(invocation -> "ID" + generated.incrementAndGet());
    }

    @Test
    public void generator_idIsTwelveCharsFromTheThirtyCharAlphabetWithModElevenChecksum() {
        final EnterpriseIdGeneratorImpl generator = new EnterpriseIdGeneratorImpl();

        for (int i = 0; i < 5000; i++) {
            final String id = generator.enterpriseId();
            assertThat(id, id.length(), is(12));
            int sum = 0;
            for (int c = 0; c < 11; c++) {
                final int index = ALPHABET.indexOf(id.charAt(c));
                assertThat(id, index >= 0, is(true));
                sum += index;
            }
            assertThat(id, id.charAt(11), is(ALPHABET.charAt(sum % 11)));
        }
    }

    @Test
    public void generator_bodyUsesAllThirtyCharactersAndChecksumOnlyTheFirstEleven() {
        final EnterpriseIdGeneratorImpl generator = new EnterpriseIdGeneratorImpl();
        final Set<Character> body = new TreeSet<>();
        final Set<Character> checksum = new TreeSet<>();

        for (int i = 0; i < 5000; i++) {
            final String id = generator.enterpriseId();
            for (int c = 0; c < 11; c++) {
                body.add(id.charAt(c));
            }
            checksum.add(id.charAt(11));
        }

        assertThat(body, is(chars(ALPHABET)));
        assertThat(checksum, is(chars("BCDFGHJKLMN")));
    }

    @Test
    public void allocation_firstAttemptOk_returnsTheGeneratedIdAndMapsItToTheCase() {
        when(idMapperClient.add(any(SystemIdMap.class), any(UUID.class))).thenReturn(response(OK));

        final String enterpriseId = enterpriseIdService.enterpriseIdForCase(CASE_ID);

        assertThat(enterpriseId, is("ID1"));
        verify(idMapperClient).add(new SystemIdMap("ID1", "enterpriseId", CASE_ID, "caseId"), SYSTEM_USER_ID);
        verify(idGenerator, times(1)).enterpriseId();
        verifyNoInteractions(logger);
    }

    @Test
    public void allocation_threeConflictsThenOk_returnsTheFourthIdAfterFourAttempts() {
        when(idMapperClient.add(any(SystemIdMap.class), any(UUID.class)))
                .thenReturn(response(CONFLICT), response(CONFLICT), response(CONFLICT), response(OK));

        final String enterpriseId = enterpriseIdService.enterpriseIdForCase(CASE_ID);

        assertThat(enterpriseId, is("ID4"));
        verify(idGenerator, times(4)).enterpriseId();
        verify(idMapperClient, times(4)).add(any(SystemIdMap.class), eq(SYSTEM_USER_ID));
        verify(idMapperClient).add(new SystemIdMap("ID4", "enterpriseId", CASE_ID, "caseId"), SYSTEM_USER_ID);
        verify(logger, times(3)).info(anyString());
    }

    @Test
    public void allocation_threeConflictsThenOk_readsTheSystemUserOncePerAttempt() {
        when(idMapperClient.add(any(SystemIdMap.class), any(UUID.class)))
                .thenReturn(response(CONFLICT), response(CONFLICT), response(CONFLICT), response(OK));

        enterpriseIdService.enterpriseIdForCase(CASE_ID);

        verify(systemUserProvider, times(4)).getContextSystemUserId();
    }

    @Test
    public void allocation_nineConflictsThenOk_succeedsOnTheTenthAttempt() {
        when(idMapperClient.add(any(SystemIdMap.class), any(UUID.class))).thenReturn(response(CONFLICT),
                response(CONFLICT), response(CONFLICT), response(CONFLICT), response(CONFLICT), response(CONFLICT),
                response(CONFLICT), response(CONFLICT), response(CONFLICT), response(OK));

        final String enterpriseId = enterpriseIdService.enterpriseIdForCase(CASE_ID);

        assertThat(enterpriseId, is("ID10"));
        verify(idGenerator, times(10)).enterpriseId();
    }

    @Test
    public void allocation_tenConflictsThenOk_succeedsOnTheEleventhAttempt() {
        when(idMapperClient.add(any(SystemIdMap.class), any(UUID.class))).thenReturn(response(CONFLICT),
                response(CONFLICT), response(CONFLICT), response(CONFLICT), response(CONFLICT), response(CONFLICT),
                response(CONFLICT), response(CONFLICT), response(CONFLICT), response(CONFLICT), response(OK));

        final Throwable outcome = outcomeOf(() -> enterpriseIdService.enterpriseIdForCase(CASE_ID));
        record("tenConflictsThenOk", outcome, generated.get());

        assertThat(String.valueOf(outcome), outcome == null, is(true));
        verify(idGenerator, times(11)).enterpriseId();
    }

    @Test
    public void allocation_alwaysConflict_isStillRetryingAfterOneThousandAttempts() {
        when(idMapperClient.add(any(SystemIdMap.class), any(UUID.class))).thenReturn(response(CONFLICT));
        doAnswer(invocation -> {
            if (generated.incrementAndGet() > ATTEMPT_BUDGET) {
                throw new AttemptBudgetExhausted();
            }
            return "ID" + generated.get();
        }).when(idGenerator).enterpriseId();

        final Throwable outcome = assertTimeoutPreemptively(Duration.ofSeconds(60),
                () -> outcomeOf(() -> enterpriseIdService.enterpriseIdForCase(CASE_ID)));
        record("alwaysConflictBudget" + ATTEMPT_BUDGET, outcome, generated.get());

        assertThat(String.valueOf(outcome), outcome, instanceOf(AttemptBudgetExhausted.class));
        assertThat(generated.get(), is(ATTEMPT_BUDGET + 1));
        verify(idMapperClient, times(ATTEMPT_BUDGET)).add(any(SystemIdMap.class), any(UUID.class));
    }

    @Test
    public void allocation_alwaysConflict_unbounded_endsInStackOverflowError() {
        when(idMapperClient.add(any(SystemIdMap.class), any(UUID.class))).thenReturn(response(CONFLICT));

        final Throwable outcome = assertTimeoutPreemptively(Duration.ofSeconds(120),
                () -> outcomeOf(() -> enterpriseIdService.enterpriseIdForCase(CASE_ID)));
        record("alwaysConflictUnbounded", outcome, generated.get());

        assertThat(String.valueOf(outcome), outcome, instanceOf(StackOverflowError.class));
        assertThat(generated.get(), greaterThan(ATTEMPT_BUDGET));
    }

    @Test
    public void allocation_noContextSystemUser_throwsNoSuchElementExceptionAfterGeneratingOneId() {
        doReturn(Optional.empty()).when(systemUserProvider).getContextSystemUserId();

        assertThrows(NoSuchElementException.class, () -> enterpriseIdService.enterpriseIdForCase(CASE_ID));

        verify(idGenerator, times(1)).enterpriseId();
        verifyNoInteractions(idMapperClient);
    }

    @Test
    public void cdi_serviceAndGeneratorDeclareNoClassAnnotations() {
        assertThat(annotationNames(EnterpriseIdServiceImpl.class), is(Collections.<String>emptyList()));
        assertThat(annotationNames(EnterpriseIdGeneratorImpl.class), is(Collections.<String>emptyList()));
    }

    @Test
    public void cdi_serviceHoldsOnlyItsFourInjectedCollaborators() {
        assertThat(instanceFields(EnterpriseIdServiceImpl.class), is(asList(
                "idGenerator:EnterpriseIdGenerator",
                "idMapperClient:SystemIdMapperClient",
                "logger:Logger",
                "systemUserProvider:SystemUserProvider")));
    }

    @Test
    public void cdi_generatorHoldsOneJavaUtilRandom() {
        assertThat(instanceFields(EnterpriseIdGeneratorImpl.class), is(asList("random:Random")));
    }

    @Test
    public void cdi_bothImplementationsAreProxyable() throws NoSuchMethodException {
        for (final Class<?> type : asList(EnterpriseIdServiceImpl.class, EnterpriseIdGeneratorImpl.class)) {
            assertThat(type.getName(), Modifier.isFinal(type.getModifiers()), is(false));
            assertThat(type.getName(), Modifier.isPrivate(type.getDeclaredConstructor().getModifiers()), is(false));
            for (final Method method : type.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers())) {
                    assertThat(method.toString(), Modifier.isFinal(method.getModifiers()), is(false));
                }
            }
        }
    }

    private static final class AttemptBudgetExhausted extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    // ---- SCENARIOS-END ----

    private static AdditionResponse response(final ResultCode code) {
        return new AdditionResponse(UUID.randomUUID(), code, Optional.empty());
    }

    private static Throwable outcomeOf(final Runnable call) {
        try {
            call.run();
            return null;
        } catch (final Throwable thrown) {
            return thrown;
        }
    }

    private static void record(final String scenario, final Throwable outcome, final int attempts) {
        final String line = scenario + " attempts=" + attempts + " outcome=" + (outcome == null ? "returned" : outcome.getClass().getName()) + System.lineSeparator();
        final Path file = Paths.get("target", "enterprise-id-parity-observations.txt");
        try {
            Files.write(file, line.getBytes(UTF_8), CREATE, APPEND);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Set<Character> chars(final String characters) {
        final Set<Character> set = new TreeSet<>();
        for (final char c : characters.toCharArray()) {
            set.add(c);
        }
        return set;
    }

    private static List<String> annotationNames(final Class<?> type) {
        final List<String> names = new ArrayList<>();
        for (final Annotation annotation : type.getAnnotations()) {
            names.add(annotation.annotationType().getName());
        }
        return names;
    }

    private static List<String> instanceFields(final Class<?> type) {
        final List<String> fields = new ArrayList<>();
        for (final Field field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                fields.add(field.getName() + ":" + field.getType().getSimpleName());
            }
        }
        Collections.sort(fields);
        return fields;
    }
}
