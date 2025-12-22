package uk.gov.moj.cpp.prosecution.casefile.command.api.service;


import static java.time.ZonedDateTime.now;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.command.api.service.IdGenerationService.TARGET_TYPE_CPI_MCC;
import static uk.gov.moj.cpp.prosecution.casefile.command.api.service.IdGenerationService.TARGET_TYPE_SJP;
import static uk.gov.moj.cpp.prosecution.casefile.command.api.service.IdGenerationService.TARGET_TYPE_SPI;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.systemidmapper.client.AdditionResponse;
import uk.gov.moj.cpp.systemidmapper.client.ResultCode;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMap;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.util.Optional;
import java.util.UUID;

import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IdGenerationServiceTest {

    private static final UUID USER_ID = randomUUID();

    @Mock
    private SystemUserProvider systemUserProvider;

    @Mock
    private SystemIdMapperClient systemIdMapperClient;

    @InjectMocks
    private IdGenerationService idGenerationService;

    private ArgumentCaptor<SystemIdMap> systemIdMapArgumentCaptor =  forClass(SystemIdMap.class);

    private static final String URN = "C2AAACD3455";
    private static final String SOURCE_ID =  "GAFTL00:C2AAACD3455";
    private static final String OU_CODE = "GAFTL00";

    @Test
    public void shouldGenerateCaseReferenceWithoutOUCode(){
        //Given

        //When
        final String generatedCaseReference =  idGenerationService.generateCaseReference();

        //Then
        assertThat("generatedCaseReference should not be null", generatedCaseReference, is(notNullValue()));
        assertThat("generatedCaseReference length should be 10 digits", generatedCaseReference.length(), is(10));
        assertThat(generatedCaseReference, startsWith("C"));
    }

    @Test
    public void shouldReturnCaseIdWhenMappingExists() {
        //Given

        final UUID mappedCaseId = randomUUID();
        final SystemIdMapping systemIdMapping = new SystemIdMapping(randomUUID(), SOURCE_ID, "", mappedCaseId, "", now());
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(USER_ID));
        when(systemIdMapperClient.findBy(USER_ID, URN, TARGET_TYPE_CPI_MCC, TARGET_TYPE_SPI, TARGET_TYPE_SJP)).thenReturn(Optional.of(systemIdMapping));

        //When
        final UUID cppCaseId = idGenerationService.generateCaseId(URN);

        //Then
        assertThat(cppCaseId, is(mappedCaseId));
    }

    @Test
    public void shouldReturnCaseIdWhenNoMappingExists() {
        //Given
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(USER_ID));
        when(systemIdMapperClient.findBy(USER_ID, URN, TARGET_TYPE_CPI_MCC,TARGET_TYPE_SPI,TARGET_TYPE_SJP)).thenReturn(Optional.empty());
        when(systemIdMapperClient.add(systemIdMapArgumentCaptor.capture(), any())).thenReturn(new AdditionResponse(randomUUID(), ResultCode.OK, empty()));

        //When
        final UUID caseId = idGenerationService.generateCaseId(URN);

        //Then
        assertThat("caseId should match", caseId, Is.is(systemIdMapArgumentCaptor.getValue().getTargetId()));
    }
}
