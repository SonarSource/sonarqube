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

import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GraphQlClientTest {
  private static final String ACCESS_TOKEN = "OAUTHTOKEN";
  private static final Type PAGE_ANSWER_TYPE = TypeToken.getParameterized(GsonGraphQlAnswer.class, PageData.class).getType();
  private OkHttpClient.Builder httpClientBuilder;
  private final OkHttpClient httpClient = mockOkHttpClient();
  private final GraphQlClient graphQlClient = new GraphQlClient(httpClient);

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void constructor_appliesCallTimeoutToHttpClient() {
    verify(httpClientBuilder).callTimeout(60_000, TimeUnit.MILLISECONDS);
  }

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

  @Test
  public void executeQuery_whenMultiplePages_thenAggregatesResultsFromAllPagesAndStopsOnLastPage() throws IOException {
    Response firstPageResponse = mockResponse(pageResponseBody("a", true, "cursor1"));
    Response lastPageResponse = mockResponse(pageResponseBody("b", false, null));
    Call call = mock(Call.class);
    when(httpClient.newCall(any())).thenReturn(call);
    when(call.execute()).thenReturn(firstPageResponse, lastPageResponse);

    List<String> results = graphQlClient.executeQuery(queryWithPagination(GraphQlQueryParameters.QueryWithPagination.DEFAULT_MAX_PAGES));

    assertThat(results).containsExactly("a", "b");
    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  public void executeQuery_whenMaxPagesIsReachedButMorePagesRemain_thenStopsPaginatingAndLogsWarning() throws IOException {
    // GitLab (or any GraphQl server) keeps saying there is a next page: without the maxPages ceiling this would loop forever.
    Response page1Response = mockResponse(pageResponseBody("a", true, "cursor1"));
    Response page2Response = mockResponse(pageResponseBody("a", true, "cursor1"));
    Response page3Response = mockResponse(pageResponseBody("a", true, "cursor1"));
    Call call = mock(Call.class);
    when(httpClient.newCall(any())).thenReturn(call);
    when(call.execute()).thenReturn(page1Response, page2Response, page3Response);

    List<String> results = graphQlClient.executeQuery(queryWithPagination(3));

    assertThat(results).containsExactly("a", "a", "a");
    assertThat(logTester.logs(Level.WARN))
      .hasSize(1)
      .allMatch(message -> message.contains("reached its maximum of 3 pages"));
  }

  @Test
  public void executeQuery_whenLastPageIsReachedExactlyAtMaxPages_thenDoesNotLogWarning() throws IOException {
    // the ceiling must only trigger when pagination would otherwise continue, not merely because the count matches
    Response firstPageResponse = mockResponse(pageResponseBody("a", true, "cursor1"));
    Response lastPageResponse = mockResponse(pageResponseBody("b", false, null));
    Call call = mock(Call.class);
    when(httpClient.newCall(any())).thenReturn(call);
    when(call.execute()).thenReturn(firstPageResponse, lastPageResponse);

    List<String> results = graphQlClient.executeQuery(queryWithPagination(2));

    assertThat(results).containsExactly("a", "b");
    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  private GraphQlQueryParameters.QueryWithPagination<PageData, String, String> queryWithPagination(int maxPages) {
    return new GraphQlQueryParameters.QueryWithPagination<>(
      "https://example.com/graphql",
      ACCESS_TOKEN,
      "query",
      Collections.emptyMap(),
      answer -> List.of(answer.getNonNullData().value()),
      answer -> answer.getNonNullData().cursor(),
      answer -> answer.getNonNullData().hasNextPage(),
      PAGE_ANSWER_TYPE,
      maxPages);
  }

  private static String pageResponseBody(String value, boolean hasNextPage, @Nullable String cursor) {
    return "{\"data\": {\"value\": \"" + value + "\", \"hasNextPage\": " + hasNextPage + ", \"cursor\": " + (cursor == null ? "null" : ("\"" + cursor + "\"")) + "}}";
  }

  private record PageData(String value, boolean hasNextPage, @Nullable String cursor) {
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

  private OkHttpClient mockOkHttpClient() {
    OkHttpClient client = mock(OkHttpClient.class);
    httpClientBuilder = mock(OkHttpClient.Builder.class);
    when(client.newBuilder()).thenReturn(httpClientBuilder);
    when(httpClientBuilder.callTimeout(anyLong(), any())).thenReturn(httpClientBuilder);
    when(httpClientBuilder.build()).thenReturn(client);
    return client;
  }

}
