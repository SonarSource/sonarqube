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

import okhttp3.Request;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sonar.server.v2.api.azurebilling.service.AzureBillingRequestBuilder.AZURE_MARKETPLACE_API_URL;
import static org.sonar.server.v2.api.azurebilling.service.AzureBillingRequestBuilder.DIMENSION;


class AzureBillingRequestBuilderTest {

  private final AzureBillingRequestBuilder underTest = new AzureBillingRequestBuilder();

  @Test
  void getAzureBillingRequestBodyTest() {
    String azureBillingRequestBody = underTest.getAzureBillingRequestBody("resource-id", "plan-id");

    assertTrue(azureBillingRequestBody.contains("\"resourceUri\": \"resource-id\""));
    assertTrue(azureBillingRequestBody.contains("\"quantity\": 1"));
    assertTrue(azureBillingRequestBody.contains("\"dimension\": \"" + DIMENSION + "\""));
    assertTrue(azureBillingRequestBody.contains("\"planId\": \"plan-id\""));
  }

  @Test
  void getAzureBillingRequestTest() {
    String requestBody = underTest.getAzureBillingRequestBody("resource-id", "plan-id");
    String azureUserToken = "azure-user-token";

    Request azureBillingRequest = underTest.getAzureBillingRequest(azureUserToken, requestBody);
    Assertions.assertNotNull(azureBillingRequest);

    assertEquals(AZURE_MARKETPLACE_API_URL, azureBillingRequest.url().toString());
    assertEquals("POST", azureBillingRequest.method());
    assertEquals("application/json", azureBillingRequest.header(HttpHeaders.CONTENT_TYPE));
    assertEquals("Bearer " + azureUserToken, azureBillingRequest.header(HttpHeaders.AUTHORIZATION));
  }

  @Test
  void getAzureUserTokenRequestTest() {
    String clientId = "client-id";

    Request azureUserTokenRequest = underTest.getAzureUserTokenRequest(clientId);
    Assertions.assertNotNull(azureUserTokenRequest);

    assertEquals("http://169.254.169.254/metadata/identity/oauth2/token?api-version=2018-02-01&client_id=client-id&resource=20e940b3-4c77-4b0b-9a53-9e16a1b010a7", azureUserTokenRequest.url().toString());
    assertEquals("GET", azureUserTokenRequest.method());
    assertEquals("true", azureUserTokenRequest.header("metadata"));
  }

}
