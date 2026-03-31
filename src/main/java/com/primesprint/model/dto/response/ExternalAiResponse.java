package com.primesprint.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalAiResponse {
	private String requestId;
	private String provider;
	private String model;
	private String tokenizedResponse;
	private ProviderMetadata providerMetadata;

	@Data
	@NoArgsConstructor
    @AllArgsConstructor
	public static class ProviderMetadata {
		private Long latencyMs;
		private Integer usageTokens;
		private String rawResponseId;
	}
}
