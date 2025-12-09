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
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.server.v2.api.azurebilling.environment.AzureEnvironment;
import org.sonar.server.v2.api.azurebilling.response.AzureBillingRestResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultAzureBillingHandlerTest {

  private final OkHttpClient mockHttpClient = mock();
  private final Call mockTokenCall = mock();
  private final Response mockTokenResponse = mock();
  private final ResponseBody mockTokenResponseBody = mock();
  private final Call mockMeteringCall = mock();
  private final Response mockMeteringResponse = mock();
  private final ResponseBody mockMeteringResponseBody = mock();
  private final AzureEnvironment azureEnvironment = mock();
  private final DefaultAzureBillingHandler underTest = new DefaultAzureBillingHandler(mockHttpClient, azureEnvironment);

  @BeforeEach
  void setUp() throws IOException {
    when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockTokenCall, mockMeteringCall);
    when(mockTokenCall.execute()).thenReturn(mockTokenResponse);
    when(mockMeteringCall.execute()).thenReturn(mockMeteringResponse);

    when(azureEnvironment.getAzureClientId()).thenReturn(Optional.of("client-id"));
    when(azureEnvironment.getResourceId()).thenReturn(Optional.of("resource-id"));
    when(azureEnvironment.getPlanId()).thenReturn(Optional.of("plan-id"));

    when(mockTokenResponse.isSuccessful()).thenReturn(true);
    when(mockTokenResponse.body()).thenReturn(mockTokenResponseBody);
    when(mockTokenResponseBody.string()).thenReturn(
      """  
        {"access_token":"access_token_value",
        "client_id":"client_id_value","expires_in":"84939",
        "expires_on":"1759390648",
        "ext_expires_in":"86399","not_before":"1759303948",
        "resource":"20e940b3-4c77-4b0b-9a53-9e16a1b010a7","token_type":"Bearer"}
        """);
  }

  @Test
  void testBillAzureAccount_whenCallIsOkay_thenMessageShouldBeNull() throws IOException {
    when(mockMeteringResponse.isSuccessful()).thenReturn(true);
    when(mockMeteringResponse.body()).thenReturn(mockMeteringResponseBody);
    when(mockMeteringResponseBody.string()).thenReturn("Success");

    ResponseEntity<AzureBillingRestResponse> response = underTest.billAzureAccount();

    assertTrue(response.getBody().success());
    assertNull(response.getBody().message());
    verify(mockHttpClient, times(2)).newCall(any(Request.class));
    verify(mockTokenCall, times(1)).execute();
    verify(mockMeteringCall, times(1)).execute();
  }

  @Test
  void testBillAzureAccount_whenCallIsNotOkay_thenMessageShouldNotBeNull() throws IOException {
    when(mockMeteringResponse.isSuccessful()).thenReturn(false);
    when(mockMeteringResponse.code()).thenReturn(500);
    when(mockMeteringResponse.body()).thenReturn(okhttp3.ResponseBody.create("", okhttp3.MediaType.parse("application/json")));
    when(mockMeteringResponse.message()).thenReturn("Bad Request");

    ResponseEntity<AzureBillingRestResponse> response = underTest.billAzureAccount();

    assertFalse(response.getBody().success());
    assertEquals("Call to Azure marketplace failed. Details: Bad Request", response.getBody().message());
    verify(mockHttpClient, times(2)).newCall(any(Request.class));
    verify(mockTokenCall, times(1)).execute();
    verify(mockMeteringCall, times(1)).execute();
  }

  @Test
  void testBillAzureAccount_whenNetworkError_thenSuccessShouldBeFalse() throws IOException {
    when(mockMeteringCall.execute()).thenThrow(new IOException("Network error"));

    ResponseEntity<AzureBillingRestResponse> response = underTest.billAzureAccount();

    assertFalse(response.getBody().success());
    assertEquals("Connection to Azure marketplace failed. Details: Network error", response.getBody().message());
    verify(mockHttpClient, times(2)).newCall(any(Request.class));
    verify(mockTokenCall, times(1)).execute();
    verify(mockMeteringCall, times(1)).execute();
  }

  @Test
  void testBillAzureAccount_whenTokenCannotBeObtained_thenSuccessShouldBeFalse() throws IOException {
    when(mockTokenResponse.isSuccessful()).thenReturn(false);
    when(mockTokenResponse.message()).thenReturn("token error");
    ResponseEntity<AzureBillingRestResponse> response = underTest.billAzureAccount();

    assertFalse(response.getBody().success());
    assertEquals("Failed to build request. Details: Cannot obtain Azure Access Token. Details: token error", response.getBody().message());
    verify(mockHttpClient, times(1)).newCall(any(Request.class));
    verify(mockTokenCall, times(1)).execute();
  }

  @Test
  void testBillAzureAccount_whenCallIsNotOkay_thenMessageShouldBeCorrect() throws IOException {

    String meteringResponseBody = """
      {"additionalInfo":{"acceptedMessage":{"usageEventId":"8a5bda72-b269-42a8-b0c0-0acd2b2405bb","status":"Duplicate","messageTime":"2025-10-01T08:29:32.8754646Z","resourceId":"d340a2b2-336a-46e0-b69b-7f2459683aad","resourceUri":"/subscriptions/1d7d141b-b12f-474e-8ef5-231f9a6e8367/resourceGroups/rg-sonarqube-01/providers/Microsoft.ContainerService/managedclusters/test-marketplace/providers/Microsoft.KubernetesConfiguration/extensions/sonarazure","quantity":1.0,"dimension":"billing_test_free","effectiveStartTime":"2025-10-01T08:03:28.14Z","planId":"metered"}},"message":"This usage event already exist.","code":"Conflict"}
      """;

    when(mockMeteringCall.execute()).thenReturn(getMockMeteringResponse(409, meteringResponseBody));

    ResponseEntity<AzureBillingRestResponse> response = underTest.billAzureAccount();

    assertFalse(response.getBody().success());
    assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
    assertEquals("Call to Azure marketplace failed. Details: Request failed with status 409: Conflict. This usage event already exist.", response.getBody().message());
    verify(mockHttpClient, times(2)).newCall(any(Request.class));
    verify(mockTokenCall, times(1)).execute();
    verify(mockMeteringCall, times(1)).execute();
  }

  private static Response getMockMeteringResponse(int status, String responseBody) {
    return new Response.Builder()
      .code(status)
      .message("OK")
      .request(new Request.Builder().url("http://localhost/").build())
      .protocol(okhttp3.Protocol.HTTP_1_1)
      .body(okhttp3.ResponseBody.create(responseBody, okhttp3.MediaType.parse("application/json")))
      .build();
  }

}
