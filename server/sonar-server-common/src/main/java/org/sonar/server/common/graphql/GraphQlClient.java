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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

import static java.lang.String.format;

@ServerSide
@ComputeEngineSide
public class GraphQlClient {

  private static final Logger LOG = LoggerFactory.getLogger(GraphQlClient.class);

  private static final Gson GSON = new GsonBuilder().serializeNulls().create();
  private final OkHttpClient client;


  public GraphQlClient(OkHttpClient client) {
    this.client = client;
  }

  public <T, U> List<U> executeQuery(GraphQlQueryParameters.QueryWithPagination<T, U> graphQlQueryParameters) {
    GsonGraphQlAnswer<T> graphQlAnswer;
    String cursor = null;
    List<U> results = new ArrayList<>();
    GsonGraphQlQuery graphQlQuery = new GsonGraphQlQuery(graphQlQueryParameters.queryString(), graphQlQueryParameters.queryVariables());
    do {
      GsonGraphQlQuery paginatedQuery = buildQueryForCursor(graphQlQuery, cursor);
      try {
        LOG.debug("Executing GraphQl query, url: {}", graphQlQueryParameters.appUrl());
        graphQlAnswer = fetchValidDataOrThrow(
          graphQlQueryParameters.appUrl(),
          graphQlQueryParameters.accessToken(),
          graphQlQueryParameters.answerDataType(),
          paginatedQuery
        );
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
      cursor = graphQlQueryParameters.extractCursorFunction().apply(graphQlAnswer);
      results.addAll(graphQlQueryParameters.extractAndMapResultsFunction().apply(graphQlAnswer));
    }
    while (graphQlQueryParameters.hasNextPage().test(graphQlAnswer));
    return results;
  }

  public void executeMutation(GraphQlMutationParameters.SimpleMutation mutationParameters) {
    GsonGraphQlQuery graphQlQuery = new GsonGraphQlQuery(mutationParameters.queryString(), mutationParameters.queryVariables());
    try {
      fetchValidDataOrThrow(
        mutationParameters.url(),
        mutationParameters.accessToken(),
        GsonGraphQlAnswer.class,
        graphQlQuery
      );
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static GsonGraphQlQuery buildQueryForCursor(GsonGraphQlQuery query, @Nullable String cursor) {
    Map<String, String> variables = new HashMap<>(query.variables());
    variables.put("cursor", cursor);
    return new GsonGraphQlQuery(query.query(), variables);
  }

  private <T> GsonGraphQlAnswer<T> fetchValidDataOrThrow(
    String graphQlApiUrl,
    String accessToken,
    Type answerDataType,
    GsonGraphQlQuery paginatedQuery
  ) throws IOException {
    RequestBody body = RequestBody.create(GSON.toJson(paginatedQuery), MediaType.parse("application/json; charset=utf-8"));
    Request request = new Request.Builder()
      .url(graphQlApiUrl)
      .post(body)
      .addHeader("Authorization", "Bearer " + accessToken)
      .build();
    return executeCallAndDeserializeAnswer(answerDataType, request);
  }

  private <T> @NotNull GsonGraphQlAnswer<T> executeCallAndDeserializeAnswer(Type answerDataType, Request request) throws IOException {
    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IllegalStateException(format("Error while executing GraphQl query. Return code %s. Error message: %s.", response.code(), response.body().string()));
      }
      return deserializeValidAnswerOrThrow(answerDataType, response);
    }
  }

  private static <T> GsonGraphQlAnswer<T> deserializeValidAnswerOrThrow(Type answerDataType, Response response) {
    GsonGraphQlAnswer<T> graphQlAnswer;
    Function<String, GsonGraphQlAnswer<T>> toObject = stringPayload -> GSON.fromJson(stringPayload, answerDataType);
    Optional<String> bodyString = Optional.empty();
    try {
      bodyString = Optional.ofNullable(response.body()).map(body -> {
        try {
          return body.string();
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
      });
      graphQlAnswer = bodyString.map(toObject).orElseThrow();
    } catch (Exception exception) {
      throw new IllegalStateException(format("Error while deserializing GraphQL payload: %s. %s", exception.getMessage(), bodyString), exception);
    }
    if (!graphQlAnswer.isValid()) {
      throw new IllegalStateException("The GraphQl answer contains errors: " + graphQlAnswer.errors());
    }
    return graphQlAnswer;
  }

}
