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
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class AzureBillingResponseHandlerTest {

  private final AzureBillingResponseHandler underTest = new AzureBillingResponseHandler();

  @Test
  void extractAccessTokenFromResponse_whenIoExceptionIsThrown_returnEmpty() throws IOException {
    Response mockResponse = mock();

    when(mockResponse.body()).thenReturn(mock());
    when(mockResponse.body().string()).thenThrow(new IOException("IO Exception"));

    var accessToken = underTest.extractAccessTokenFromResponse(mockResponse);
    assertTrue(accessToken.isEmpty());
  }

  @Test
  void extractAccessTokenFromResponse_whenAccessTokenIsAvailable_returnItsValue() {
    String responseBody = """
      {"access_token":"access_token_value",
      "client_id":"client_id_value","expires_in":"84939",
      "expires_on":"1759390648",
      "ext_expires_in":"86399","not_before":"1759303948",
      "resource":"20e940b3-4c77-4b0b-9a53-9e16a1b010a7","token_type":"Bearer"}
      """;

    Response mockResponse = getMockResponse(responseBody);

    var accessToken = underTest.extractAccessTokenFromResponse(mockResponse);
    assertTrue(accessToken.isPresent());
    assertEquals("access_token_value", accessToken.get());
  }

  @Test
  void extractAccessTokenFromResponse_whenAccessTokenIsNotAvailable_returnEmptyOptional() {
    String responseBody = """
      {
      "client_id":"client_id_value","expires_in":"84939",
      "expires_on":"1759390648",
      "ext_expires_in":"86399","not_before":"1759303948",
      "resource":"20e940b3-4c77-4b0b-9a53-9e16a1b010a7","token_type":"Bearer"}
      """;

    Response mockResponse = getMockResponse(responseBody);

    var accessToken = underTest.extractAccessTokenFromResponse(mockResponse);
    assertTrue(accessToken.isEmpty());
  }

  @Test
  void extractAccessTokenFromResponse_whenIoExceptionIsThrown_returnsEmpty() throws IOException {
    Response mockResponse = mock();

    when(mockResponse.body()).thenReturn(mock());
    when(mockResponse.body().string()).thenThrow(new IOException("IO Exception"));

    var accessToken = underTest.getErrorMessageFromResponse(mockResponse);
    assertTrue(accessToken.isEmpty());
  }

  @Test
  void getErrorMessageFromResponse_whenResponseSuccessful_returnsEmpty() {
    Response mockResponse = getMockResponse(200, "OK",
      """
        {
           "usageEventId": <guid>,
           "status": "Accepted",
           "messageTime": "2020-01-12T13:19:35.3458658Z",
           "resourceId": <guid>,
           "quantity": 5.0,
           "dimension": "dim1",
           "effectiveStartTime": "2018-12-01T08:30:14",
           "planId": "plan1",
         }
        """);

    Optional<String> errorMessage = underTest.getErrorMessageFromResponse(mockResponse);
    assertTrue(errorMessage.isEmpty());
  }

  @Test
  void getErrorMessageFromResponse_whenResponseHasMultipleDetails_returnAllOfThem() {
    Response mockResponse = getMockResponse(400, "Bad Request",
      """
        {
           "message": "One or more errors have occurred.",
           "target": "usageEventRequest",
           "details": [
             {
               "message": "The resourceId is required.",
               "target": "ResourceId",
               "code": "BadArgument"
             },
             {
               "message": "The clientId is required.",
               "target": "ClientId",
               "code": "BadArgument"
             }
           ],
           "code": "BadArgument"
        }
        """);

    Optional<String> errorMessage = underTest.getErrorMessageFromResponse(mockResponse);
    assertTrue(errorMessage.isPresent());
    assertEquals("Request failed with status 400: BadArgument. One or more errors have occurred. The resourceId is required. The clientId is required.", errorMessage.get());
  }

  @Test
  void getErrorMessageFromResponse_whenResponseHasNoDetails_returnMessageAndCode() {
    Response mockResponse = getMockResponse(409, "",
      """
        {
           "additionalInfo": {
             "acceptedMessage": {
               "usageEventId": "<guid>",
               "status": "Duplicate",
               "messageTime": "2020-01-12T13:19:35.3458658Z",
               "resourceId": "<guid>",
               "quantity": 1.0,
               "dimension": "dim1",
               "effectiveStartTime": "2020-01-12T11:03:28.14Z",
               "planId": "plan1"
             }
           },
           "message": "This usage event already exist.",
           "code": "Conflict"
         }
        """);

    Optional<String> errorMessage = underTest.getErrorMessageFromResponse(mockResponse);
    assertTrue(errorMessage.isPresent());
    assertEquals("Request failed with status 409: Conflict. This usage event already exist.", errorMessage.get());
  }

  private static Response getMockResponse(String responseBody) {
    return getMockResponse(200, "OK", responseBody);
  }

  private static Response getMockResponse(int status, String message, String responseBody) {
    return new Response.Builder()
      .code(status)
      .message(message)
      .request(new Request.Builder().url("http://localhost/").build())
      .protocol(okhttp3.Protocol.HTTP_1_1)
      .body(okhttp3.ResponseBody.create(responseBody, okhttp3.MediaType.parse("application/json")))
      .build();
  }

}
