/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.v2.api.azurebilling.service;

import java.io.IOException;
import java.util.Optional;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.v2.api.azurebilling.environment.AzureEnvironment;
import org.sonar.server.v2.api.azurebilling.response.AzureBillingRestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class DefaultAzureBillingHandler implements AzureBillingHandler {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultAzureBillingHandler.class);

  private final OkHttpClient client;
  private final AzureEnvironment azureEnvironment;
  private final AzureBillingRequestBuilder azureBillingRequestBuilder;
  private final AzureBillingResponseHandler azureBillingResponseHandler;

  public DefaultAzureBillingHandler(OkHttpClient httpClient, AzureEnvironment azureEnvironment) {
    this.client = httpClient;
    this.azureEnvironment = azureEnvironment;
    this.azureBillingRequestBuilder = new AzureBillingRequestBuilder();
    this.azureBillingResponseHandler = new AzureBillingResponseHandler();
  }

  @Override
  public ResponseEntity<AzureBillingRestResponse> billAzureAccount() {
    String requestBody;
    String azureUserToken;
    try {
      azureUserToken = getAzureUserToken();
      requestBody = azureBillingRequestBuilder.getAzureBillingRequestBody(getResourceId(), getPlanId());
    } catch (RuntimeException e) {
      logError("Failed to build request. Details: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AzureBillingRestResponse(false, "Failed to build request. Details: " + e.getMessage()));
    }

    Request request = azureBillingRequestBuilder.getAzureBillingRequest(azureUserToken, requestBody);

    return handleAzureBillingRequest(request);
  }

  private String getAzureUserToken() {

    String clientId = azureEnvironment.getAzureClientId()
      .orElseThrow(() -> new IllegalStateException("Azure Client ID is not configured"));

    Request tokenRequest = azureBillingRequestBuilder.getAzureUserTokenRequest(clientId);

    try (Response response = client.newCall(tokenRequest).execute()) {
      if (response.isSuccessful()) {
        Optional<String> accessToken = azureBillingResponseHandler.extractAccessTokenFromResponse(response);

        if (accessToken.isPresent()) {
          return accessToken.get();
        } else {
          logError("Cannot extract Azure Access Token from response.");
          throw new IllegalStateException("Cannot extract Azure Access Token from response");
        }
      } else {
        logError(response.message());
        throw new IllegalStateException("Cannot obtain Azure Access Token. Details: " + response.message());
      }
    } catch (IOException e) {
      logError(e.getMessage());
      throw new IllegalStateException("Cannot obtain Azure Access Token. Details: " + e.getMessage());
    }
  }

  private String getResourceId() {
    return azureEnvironment.getResourceId()
      .orElseThrow(() -> new IllegalStateException("Azure Resource ID is not configured"));
  }

  private String getPlanId() {
    return azureEnvironment.getPlanId()
      .orElseThrow(() -> new IllegalStateException("Azure Plan ID is not configured"));
  }

  @NotNull
  private ResponseEntity<AzureBillingRestResponse> handleAzureBillingRequest(Request request) {
    try (Response response = client.newCall(request).execute()) {
      if (response.isSuccessful()) {
        return ResponseEntity.status(HttpStatus.OK).body(new AzureBillingRestResponse(true, null));
      } else {
        Optional<String> errorMessage = azureBillingResponseHandler.getErrorMessageFromResponse(response);
        String errorMessageValue = errorMessage.orElse(response.message());

        logError(errorMessageValue);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AzureBillingRestResponse(false, "Call to Azure marketplace failed. Details: " + errorMessageValue));
      }
    } catch (IOException e) {
      logError(e.getMessage());
      return ResponseEntity.status(500).body(new AzureBillingRestResponse(false, "Connection to Azure marketplace failed. Details: " + e.getMessage()));
    }
  }

  private static void logError(String message) {
    LOG.error("Error while billing Azure account: {}", message);
  }

}
