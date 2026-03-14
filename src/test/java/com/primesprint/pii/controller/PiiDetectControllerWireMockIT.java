package com.primesprint.pii.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = PiiDetectControllerWireMockIT.Initializer.class)
class PiiDetectControllerWireMockIT {

    private static WireMockServer wireMock;

    @org.springframework.beans.factory.annotation.Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeAll
    static void start() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        configureFor("localhost", wireMock.port());

        String groqOk = "{\"choices\":[{\"message\":{\"content\":\"[{\\\"type\\\":\\\"PERSON\\\",\\\"value\\\":\\\"John Silva\\\",\\\"start\\\":7,\\\"end\\\":17}]\"}}]}";

        wireMock.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(groqOk)));
    }

    @org.junit.jupiter.api.BeforeEach
    void setupMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @AfterAll
    static void stop() {
        if (wireMock != null) wireMock.stop();
    }

    @Test
    void detect_returnsCombinedArray() throws Exception {
        String body = "{\n" +
                "  \"requestId\":\"req-1\",\n" +
                "  \"documentExtractedContent\":\"Client John Silva\",\n" +
                "  \"userPrompt\":\"Hello\"\n" +
                "}";

        String response = mockMvc.perform(post("/api/v1/internal/pii/detect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertNotNull(response);
        assertTrue(response.contains("\"source\":"));
        assertTrue(response.contains("document"));
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext ctx) {
            if (wireMock == null) return;
            TestPropertyValues.of(
                    "llm.base-url=http://localhost:" + wireMock.port(),
                    "llm.api-key=test",
                    "llm.model=test",
                    "llm.chat-endpoint=/chat/completions",
                    "llm.timeout-ms=5000",
                    "llm.max-retries=0",
                    "pii.log-llm-raw-response=false"
            ).applyTo(ctx);
        }
    }
}






