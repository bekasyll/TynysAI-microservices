package com.tynysai.xrayservice.service;

import com.tynysai.xrayservice.dto.response.AiAnalysisResult;
import com.tynysai.xrayservice.dto.response.PythonAnalysisResponse;
import com.tynysai.xrayservice.model.enums.DiseaseType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAnalysisServiceTest {
    @Mock
    private RestClient restClient;

    private AiAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new AiAnalysisService(restClient);
        ReflectionTestUtils.setField(service, "aiEnabled", false);
        ReflectionTestUtils.setField(service, "confidenceThreshold", 0.70);
    }

    @Test
    void analyzeImage_returnsStubResult_whenAiDisabled() {
        ReflectionTestUtils.setField(service, "aiEnabled", false);

        AiAnalysisResult result = service.analyzeImage("/tmp/anything.png");

        assertThat(result).isNotNull();
        assertThat(result.getPrimaryDiagnosis()).isEqualTo(DiseaseType.NORMAL);
        assertThat(result.getPrimaryConfidence()).isEqualTo(0.87);
        assertThat(result.getModelVersion()).isEqualTo("stub-1.0");
        assertThat(result.isRequiresDoctorReview()).isTrue();
        assertThat(result.getDetectedAbnormalities()).isEmpty();
        assertThat(result.getAllPredictions())
                .containsKeys(DiseaseType.NORMAL, DiseaseType.BACTERIAL_PNEUMONIA);
    }

    @Test
    void analyzeImage_returnsStub_whenFileMissingAndAiEnabled() {
        ReflectionTestUtils.setField(service, "aiEnabled", true);

        AiAnalysisResult result = service.analyzeImage("/no/such/path-zzz.png");

        // The internal callAiService throws because the file doesn't exist;
        // analyzeImage catches it and falls back to the stub.
        assertThat(result).isNotNull();
        assertThat(result.getModelVersion()).isEqualTo("stub-1.0");
        assertThat(result.getPrimaryDiagnosis()).isEqualTo(DiseaseType.NORMAL);
    }

    @Test
    void analyzeImage_mapsPneumoniaResponse_whenAiEnabledAndFileExists() throws IOException {
        ReflectionTestUtils.setField(service, "aiEnabled", true);

        Path tempFile = Files.createTempFile("xray-test-", ".png");
        Files.writeString(tempFile, "fake-image");
        try {
            PythonAnalysisResponse python = pneumoniaResponse();
            stubRestClient(python);

            AiAnalysisResult result = service.analyzeImage(tempFile.toString());

            assertThat(result.getPrimaryDiagnosis()).isEqualTo(DiseaseType.BACTERIAL_PNEUMONIA);
            assertThat(result.getPrimaryConfidence()).isEqualTo(0.92);
            assertThat(result.getDetectedAbnormalities()).isNotEmpty();
            assertThat(result.getModelVersion()).isEqualTo("py-1.2.3");
            assertThat(result.isRequiresDoctorReview()).isFalse();
            assertThat(result.getAllPredictions().get(DiseaseType.BACTERIAL_PNEUMONIA)).isEqualTo(0.92);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void analyzeImage_mapsNormalResponse_andTriggersReviewBelowThreshold() throws IOException {
        ReflectionTestUtils.setField(service, "aiEnabled", true);
        ReflectionTestUtils.setField(service, "confidenceThreshold", 0.95);

        Path tempFile = Files.createTempFile("xray-test-", ".png");
        Files.writeString(tempFile, "fake");
        try {
            PythonAnalysisResponse python = normalResponse();
            stubRestClient(python);

            AiAnalysisResult result = service.analyzeImage(tempFile.toString());

            assertThat(result.getPrimaryDiagnosis()).isEqualTo(DiseaseType.NORMAL);
            assertThat(result.isRequiresDoctorReview()).isTrue(); // confidence 0.85 < threshold 0.95
            assertThat(result.getDetectedAbnormalities()).isEmpty();
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void analyzeImage_returnsStub_whenRestClientReturnsNullBody() throws IOException {
        ReflectionTestUtils.setField(service, "aiEnabled", true);

        Path tempFile = Files.createTempFile("xray-test-", ".png");
        Files.writeString(tempFile, "fake");
        try {
            stubRestClient(null);

            AiAnalysisResult result = service.analyzeImage(tempFile.toString());

            // Empty body causes a RuntimeException inside callAiService -> stub fallback
            assertThat(result.getModelVersion()).isEqualTo("stub-1.0");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void analyzeImage_returnsStub_whenRestClientThrows() throws IOException {
        ReflectionTestUtils.setField(service, "aiEnabled", true);

        Path tempFile = Files.createTempFile("xray-test-", ".png");
        Files.writeString(tempFile, "fake");
        try {
            RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
            when(restClient.post()).thenReturn(uriSpec);
            when(uriSpec.uri(anyString())).thenThrow(new RuntimeException("AI down"));

            AiAnalysisResult result = service.analyzeImage(tempFile.toString());

            assertThat(result.getModelVersion()).isEqualTo("stub-1.0");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private void stubRestClient(PythonAnalysisResponse responseBody) {
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        lenient().when(bodySpec.contentType(eq(MediaType.MULTIPART_FORM_DATA))).thenReturn(bodySpec);
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(PythonAnalysisResponse.class))).thenReturn(responseBody);
    }

    private static PythonAnalysisResponse pneumoniaResponse() {
        PythonAnalysisResponse p = new PythonAnalysisResponse();
        p.setDiagnosis("PNEUMONIA");
        p.setConfidence(0.92);
        p.setRawScore(0.95);
        p.setSeverity("MODERATE");
        p.setFindings("Visible consolidation in right lower lobe");
        p.setRequiresDoctorReview(false);
        p.setModelVersion("py-1.2.3");
        return p;
    }

    private static PythonAnalysisResponse normalResponse() {
        PythonAnalysisResponse p = new PythonAnalysisResponse();
        p.setDiagnosis("NORMAL");
        p.setConfidence(0.85);
        p.setRawScore(0.10);
        p.setSeverity("NONE");
        p.setFindings("No abnormalities detected");
        p.setRequiresDoctorReview(false);
        p.setModelVersion("py-1.2.3");
        return p;
    }

    @SuppressWarnings("unused")
    private static File anyFile() {
        return mock(File.class);
    }
}