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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import okhttp3.Request;
import org.apache.http.HttpHeaders;

public class AzureBillingRequestBuilder {

  // URL to obtain the token from Azure Instance Metadata Service, this is accessible only from Azure VMs and App Services.
  public static final String AZURE_INSTANCE_METADATA_SERVICE_URL = "http://169.254.169.254/metadata/identity/oauth2/token?api-version=2018-02-01&client_id=%s&resource=%s";
  //This is the resource ID for the Azure Marketplace API.
  public static final String AZURE_MARKETPLACE_RESOURCE_ID = "20e940b3-4c77-4b0b-9a53-9e16a1b010a7";
  public static final String AZURE_MARKETPLACE_API_URL = "https://marketplaceapi.microsoft.com/api/usageEvent?api-version=2018-08-31";
  public static final String DIMENSION = "subscription_dim";

  private static final String REQUEST_BODY_TEMPLATE = """
    {
      "resourceUri": "%s",
      "quantity": 1,
      "dimension": "%s",
      "effectiveStartTime": "%s",
      "planId": "%s"
    }
    """;

  public String getAzureBillingRequestBody(String resourceId, String planId) {
    return String.format(REQUEST_BODY_TEMPLATE,
      resourceId,
      DIMENSION,
      ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
      planId);
  }

  public Request getAzureBillingRequest(String azureUserToken, String requestBody) {
    return new Request.Builder()
      .url(AZURE_MARKETPLACE_API_URL)
      .addHeader(HttpHeaders.CONTENT_TYPE, "application/json")
      .addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + azureUserToken)
      .post(okhttp3.RequestBody.create(requestBody, okhttp3.MediaType.parse("application/json")))
      .build();
  }

  public Request getAzureUserTokenRequest(String clientId) {

    String url = String.format(AZURE_INSTANCE_METADATA_SERVICE_URL, clientId, AZURE_MARKETPLACE_RESOURCE_ID);

    return new Request.Builder()
      .url(url)
      .addHeader("Metadata", "true")
      .get()
      .build();
  }
}
