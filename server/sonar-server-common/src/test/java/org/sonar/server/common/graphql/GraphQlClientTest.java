/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.common.graphql;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collections;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GraphQlClientTest {
  private static final String ACCESS_TOKEN = "OAUTHTOKEN";
  private final OkHttpClient httpClient = mock(OkHttpClient.class);
  private final GraphQlClient graphQlClient = new GraphQlClient(httpClient);

  @Test
  public void executeMutation_withSuccessfulMutation_doesNotThrowException() throws IOException {
    // Mock the response from the server
    String responseBody = "{\"data\": {\"success\": true}}";
    Response mockedResponse = mockResponse(responseBody);
    mockClientNewCall(mockedResponse);

    // Define the mutation parameters
    GraphQlMutationParameters.SimpleMutation mutationParameters = new GraphQlMutationParameters.SimpleMutation(
      "https://example.com/graphql",
      ACCESS_TOKEN,
      "mutationFile.graphql",
      Collections.emptyMap()
    );

    // Execute the mutation
    Assertions.assertThatCode(() -> graphQlClient.executeMutation(mutationParameters)).doesNotThrowAnyException();
  }

  @Test
  public void executeMutation_whenErrorsInThePayload_throwIllegalStateException() throws IOException {
    // Mock the response from the server
    Response mockedResponse = mockResponse(getResponseBody());
    mockClientNewCall(mockedResponse);

    // Define the mutation parameters
    GraphQlMutationParameters.SimpleMutation mutationParameters = new GraphQlMutationParameters.SimpleMutation(
      "https://example.com/graphql",
      ACCESS_TOKEN,
      "mutationFile.graphql",
      Collections.emptyMap()
    );

    // Execute the mutation
    Assertions.assertThatThrownBy(() -> graphQlClient.executeMutation(mutationParameters))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("The GraphQl answer contains errors: [{message=User Not Found, locations=[{line=1.0, column=40.0}], extensions={value=value, problems=[{path=[], explanation=explanation, message=message}]}}]");
  }

  @Test
  public void executeMutation_whenIOException_throwsIllegalStateException() throws IOException {
    Call call = mock(Call.class);
    when(httpClient.newCall(any())).thenReturn(call);
    when(call.execute()).thenThrow(new IOException("io exception"));

    // Define the mutation parameters
    GraphQlMutationParameters.SimpleMutation mutationParameters = new GraphQlMutationParameters.SimpleMutation(
      "https://example.com/graphql",
      ACCESS_TOKEN,
      "mutationFile.graphql",
      Collections.emptyMap()
    );

    // Execute the mutation
    Assertions.assertThatThrownBy(() -> graphQlClient.executeMutation(mutationParameters))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("java.io.IOException: io exception");
  }

  private static String getResponseBody() {
    return """
      {
        "errors": [
          {
            "message": "User Not Found",
            "locations": [
              {
                "line": 1,
                "column": 40
              }
            ],
            "extensions": {
              "value": "value",
              "problems": [
                {
                  "path": [],
                  "explanation": "explanation",
                  "message": "message"
                }
              ]
            }
          }
        ]
      }""";
  }

  private void mockClientNewCall(okhttp3.Response response) throws IOException {
    Call call = mock(Call.class);
    when(httpClient.newCall(any())).thenReturn(call);
    when(call.execute()).thenReturn(response);
  }

  private static okhttp3.Response mockResponse(String body) {
    okhttp3.Response response = mock(okhttp3.Response.class);
    when(response.code()).thenReturn(HttpURLConnection.HTTP_OK);
    when(response.isSuccessful()).thenReturn(true);
    when(response.body()).thenReturn(ResponseBody.create(body, MediaType.get("application/json")));
    return response;
  }

}
