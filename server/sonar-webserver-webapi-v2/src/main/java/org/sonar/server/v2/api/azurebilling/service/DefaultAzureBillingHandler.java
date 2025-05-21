/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.HttpHeaders;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.v2.api.azurebilling.response.AzureBillingRestResponse;

public class DefaultAzureBillingHandler implements AzureBillingHandler {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultAzureBillingHandler.class);

  // TODO: replace the value once we get access to the partner.microsoft.com
  private static final String SONARQUBE_SERVER_AZURE_RESOURCE_ID = "sonarqube-server-azure-resource-id";

  // TODO values for testing
  private static final String REQUEST_BODY_TEMPLATE = """
    {
      "resourceId": "%s",
      "quantity": %s,
      "dimension": "billing_test_free",
      "effectiveStartTime": "%s",
      "planId": "metered"
    }
    """;

  private final DbClient dbClient;
  private final OkHttpClient client;

  public DefaultAzureBillingHandler(DbClient dbClient, OkHttpClient httpClient) {
    this.dbClient = dbClient;
    this.client = httpClient;
  }

  @Override
  public AzureBillingRestResponse billAzureAccount(String azureUserToken) {
    String requestBody;
    try {
      requestBody = String.format(REQUEST_BODY_TEMPLATE,
        SONARQUBE_SERVER_AZURE_RESOURCE_ID,
        getLinesOfCode(),
        ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    } catch (RuntimeException e) {
      logError("Failed to build request. Details: " + e.getMessage());
      return new AzureBillingRestResponse(false, "Failed to build request. Details: " + e.getMessage());
    }

    Request request = getAzureBillingRequest(azureUserToken, requestBody);

    return handleAzureBillingRequest(request);
  }

  private String getLinesOfCode() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return String.valueOf(dbClient.projectDao().getNclocSum(dbSession));
    }
  }

  @NotNull
  private static Request getAzureBillingRequest(String azureUserToken, String requestBody) {
    return new Request.Builder()
      .url("https://marketplaceapi.microsoft.com/api/usageEvent?api-version=2018-08-31")
      .addHeader(HttpHeaders.CONTENT_TYPE, "application/json")
      .addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + azureUserToken)
      .post(okhttp3.RequestBody.create(requestBody, okhttp3.MediaType.parse("application/json")))
      .build();
  }

  @NotNull
  private AzureBillingRestResponse handleAzureBillingRequest(Request request) {
    try (Response response = client.newCall(request).execute()) {
      if (response.isSuccessful()) {
        return new AzureBillingRestResponse(true, null);
      } else {
        logError(response.message());
        return new AzureBillingRestResponse(false, "Call to Azure marketplace failed. Details: " + response.message());
      }
    } catch (IOException e) {
      logError(e.getMessage());
      return new AzureBillingRestResponse(false, "Connection to Azure marketplace failed. Details: " + e.getMessage());
    }
  }

  private static void logError(String message) {
    LOG.error("Error while billing Azure account: {}", message);
  }

}
