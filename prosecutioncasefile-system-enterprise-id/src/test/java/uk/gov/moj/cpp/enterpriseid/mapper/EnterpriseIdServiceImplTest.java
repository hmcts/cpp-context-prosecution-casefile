package uk.gov.moj.cpp.enterpriseid.mapper;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.enterpriseid.generator.EnterpriseIdGenerator;
import uk.gov.moj.cpp.systemidmapper.client.AdditionResponse;
import uk.gov.moj.cpp.systemidmapper.client.ResultCode;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMap;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;

import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;


@ExtendWith(MockitoExtension.class)
public class EnterpriseIdServiceImplTest {

    private static final String SOURCE_TYPE = "enterpriseId";
    private static final String TARGET_TYPE = "caseId";

    @Mock
    private Logger logger;

    @Mock
    private SystemIdMapperClient mapperClient;

    @Mock
    private EnterpriseIdGenerator idGenerator;

    @Mock
    private SystemUserProvider systemUserProvider;


    @InjectMocks
    private EnterpriseIdServiceImpl enterpriseIdService;

    @Test
    public void shouldThrowIllegalStateExceptionWhenSystemUserIdIsNotAvailable() {

        final UUID caseId = randomUUID();

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.empty());

        final IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> enterpriseIdService.enterpriseIdForCase(caseId));

        assertThat(exception.getMessage(), is(
                "Cannot register enterprise ID for case '" + caseId + "': context system user ID is not available"));
    }

    @Test
    public void shouldGenerateAnIdWhenIdDoesNotExistsAlready() {

        final UUID caseId = randomUUID();
        final Optional<UUID> userId = Optional.of(randomUUID());
        final String expEnterpriseId = RandomStringUtils.randomAlphanumeric(10);
        final AdditionResponse response = new AdditionResponse(randomUUID(), ResultCode.OK, Optional.of("OK"));

        when(systemUserProvider.getContextSystemUserId()).thenReturn(userId);
        when(idGenerator.enterpriseId()).thenReturn(expEnterpriseId);
        when(mapperClient.add(any(SystemIdMap.class), eq(userId.get()))).thenReturn(response);

        final String actualEnterpriseId = enterpriseIdService.enterpriseIdForCase(caseId);

        assertThat(actualEnterpriseId, is(expEnterpriseId));
        verify(idGenerator, times(1)).enterpriseId();

        final ArgumentCaptor<SystemIdMap> systemIdMapArgumentCaptor = forClass(SystemIdMap.class);
        verify(mapperClient).add(systemIdMapArgumentCaptor.capture(), eq(userId.get()));
        final SystemIdMap capturedSystemIdMao = systemIdMapArgumentCaptor.getValue();
        assertThat(capturedSystemIdMao.getSourceId(), is(equalTo(expEnterpriseId)));
        assertThat(capturedSystemIdMao.getSourceType(), is(equalTo(SOURCE_TYPE)));
        assertThat(capturedSystemIdMao.getTargetId(), is(equalTo(caseId)));
        assertThat(capturedSystemIdMao.getTargetType(), is(equalTo(TARGET_TYPE)));

    }

    @Test
    public void shouldReturnNewlyGeneratedIdAfterConflict() {

        final UUID caseId = randomUUID();
        final Optional<UUID> userId = Optional.of(randomUUID());
        final String conflictedId = RandomStringUtils.randomAlphanumeric(10);
        final String resolvedId = RandomStringUtils.randomAlphanumeric(10);
        final AdditionResponse conflictResponse = new AdditionResponse(randomUUID(), ResultCode.CONFLICT, Optional.of("ALREADY EXISTS"));
        final AdditionResponse successResponse = new AdditionResponse(randomUUID(), ResultCode.OK, Optional.of("OK"));

        when(systemUserProvider.getContextSystemUserId()).thenReturn(userId);
        when(idGenerator.enterpriseId()).thenReturn(conflictedId).thenReturn(resolvedId);
        when(mapperClient.add(any(SystemIdMap.class), eq(userId.get())))
                .thenReturn(conflictResponse)
                .thenReturn(successResponse);

        final String actualEnterpriseId = enterpriseIdService.enterpriseIdForCase(caseId);

        assertThat(actualEnterpriseId, is(resolvedId));
        verify(idGenerator, times(2)).enterpriseId();
    }

    @Test
    public void shouldRetryAndSucceedAfterMultipleConflicts() {

        final UUID caseId = randomUUID();
        final Optional<UUID> userId = Optional.of(randomUUID());

        final String expEnterpriseId = RandomStringUtils.randomAlphanumeric(10);
        final AdditionResponse conflictResponse = new AdditionResponse(randomUUID(), ResultCode.CONFLICT, Optional.of("ALREADY EXISTS"));
        final AdditionResponse successResponse = new AdditionResponse(randomUUID(), ResultCode.OK, Optional.of("OK"));

        when(systemUserProvider.getContextSystemUserId()).thenReturn(userId);
        when(idGenerator.enterpriseId()).thenReturn(expEnterpriseId);
        when(mapperClient.add(any(SystemIdMap.class), eq(userId.get()))).thenReturn(conflictResponse).thenReturn(conflictResponse).thenReturn(successResponse);

        final String actualEnterpriseId = enterpriseIdService.enterpriseIdForCase(caseId);

        verify(idGenerator, times(3)).enterpriseId();
        final String logMessage = "Conflict registering enterprise id: " + expEnterpriseId + " for case id: " + caseId;

        final ArgumentCaptor<SystemIdMap> systemIdMapArgumentCaptor = forClass(SystemIdMap.class);
        verify(mapperClient, times(3)).add(systemIdMapArgumentCaptor.capture(), eq(userId.get()));
        final SystemIdMap capturedSystemIdMap = systemIdMapArgumentCaptor.getValue();
        assertThat(capturedSystemIdMap.getSourceId(), is(equalTo(expEnterpriseId)));
        assertThat(capturedSystemIdMap.getSourceType(), is(equalTo(SOURCE_TYPE)));
        assertThat(capturedSystemIdMap.getTargetId(), is(equalTo(caseId)));
        assertThat(capturedSystemIdMap.getTargetType(), is(equalTo(TARGET_TYPE)));

        verify(logger, times(2)).info(logMessage);
        assertThat(actualEnterpriseId, is(expEnterpriseId));
    }

    @Test
    public void shouldThrowEnterpriseIdAllocationExceptionAfterTenConsecutiveConflicts() {

        final UUID caseId = randomUUID();
        final Optional<UUID> userId = Optional.of(randomUUID());
        final AdditionResponse conflictResponse = new AdditionResponse(randomUUID(), ResultCode.CONFLICT, Optional.of("ALREADY EXISTS"));

        when(systemUserProvider.getContextSystemUserId()).thenReturn(userId);
        when(idGenerator.enterpriseId()).thenReturn(RandomStringUtils.randomAlphanumeric(10));
        when(mapperClient.add(any(SystemIdMap.class), eq(userId.get()))).thenReturn(conflictResponse);

        final EnterpriseIdAllocationException exception = assertThrows(EnterpriseIdAllocationException.class,
                () -> enterpriseIdService.enterpriseIdForCase(caseId));

        assertThat(exception.getMessage(), is("Failed to allocate a unique enterprise ID for case '" + caseId + "' after 10 attempts"));
        verify(idGenerator, times(10)).enterpriseId();
    }
}
