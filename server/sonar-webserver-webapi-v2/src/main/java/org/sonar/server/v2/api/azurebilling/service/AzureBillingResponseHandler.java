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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Optional;
import okhttp3.Response;

public class AzureBillingResponseHandler {

  public static final String ACCESS_TOKEN_FIELD = "access_token";
  public static final String MESSAGE_FIELD = "message";
  public static final String CODE_FIELD = "code";
  public static final String DETAILS_FIELD = "details";
  public static final String ERROR_MESSAGE_TEMPLATE = "Request failed with status %s: %s. %s";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public Optional<String> extractAccessTokenFromResponse(Response response) {
    String fullResponseBody;

    try {
      fullResponseBody = response.body().string();
      if (fullResponseBody.contains(ACCESS_TOKEN_FIELD)) {
        String accessTokenValue = fullResponseBody.split("\"" + ACCESS_TOKEN_FIELD + "\":\"")[1].split("\"")[0];
        return Optional.of(accessTokenValue);
      }
    } catch (IOException e) {
      return Optional.empty();
    }
    return Optional.empty();
  }

  public Optional<String> getErrorMessageFromResponse(Response response) {
    String fullResponseBody;

    if (!response.isSuccessful()) {
      try {
        fullResponseBody = response.body().string();
        if (!fullResponseBody.isBlank()) {
          Optional<String> code = extractCodeFromResponse(fullResponseBody);
          Optional<String> message = extractMessageFromResponse(fullResponseBody);
          return Optional.of(String.format(ERROR_MESSAGE_TEMPLATE, response.code(), code.orElse("No code"), message.orElse("No message")));
        }
      } catch (IOException e) {
        return Optional.empty();
      }
    }
    return Optional.empty();
  }

  private static Optional<String> extractMessageFromResponse(String responseBody) {

    Optional<String> mainMessage = extractSingleFieldFromResponse(responseBody, MESSAGE_FIELD);
    Optional<String> details = extractDetailedMessagesFromResponse(responseBody);

    if (mainMessage.isPresent() && details.isPresent()) {
      return Optional.of(mainMessage.get() + " " + details.get().trim());
    }

    return mainMessage;
  }

  private static Optional<String> extractCodeFromResponse(String responseBody) {
    return extractSingleFieldFromResponse(responseBody, CODE_FIELD);
  }

  private static Optional<String> extractSingleFieldFromResponse(String responseBody, String fieldName) {
    try {
      JsonNode rootNode = MAPPER.readTree(responseBody);
      return Optional.ofNullable(rootNode.path(fieldName).asText());
    } catch (JsonProcessingException e) {
      return Optional.empty();
    }
  }

  private static Optional<String> extractDetailedMessagesFromResponse(String responseBody) {
    try {
      JsonNode rootNode = MAPPER.readTree(responseBody);

      JsonNode arrayNode = rootNode.path(DETAILS_FIELD);
      if (!arrayNode.isArray()) {
        return Optional.empty();
      }

      StringBuilder stringBuilder = new StringBuilder();

      arrayNode.forEach(node -> stringBuilder.append(node.path(MESSAGE_FIELD).asText()).append(" "));

      return Optional.of(stringBuilder.toString());

    } catch (JsonProcessingException e) {
      return Optional.empty();
    }
  }

}
