/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.alm.client.bitbucket.bitbucketcloud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import javax.inject.Inject;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.sonar.api.server.ServerSide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.exceptions.NotFoundException;

import static org.apache.commons.lang3.StringUtils.removeEnd;

@ServerSide
public class BitbucketCloudRestClient {
  private static final Logger LOG = LoggerFactory.getLogger(BitbucketCloudRestClient.class);
  private static final String AUTHORIZATION = "Authorization";
  private static final String GET = "GET";
  private static final String ENDPOINT = "https://api.bitbucket.org";
  private static final String ACCESS_TOKEN_ENDPOINT = "https://bitbucket.org/site/oauth2/access_token";
  private static final String VERSION = "2.0";
  protected static final String ERROR_BBC_SERVERS = "Error returned by Bitbucket Cloud";
  protected static final String UNABLE_TO_CONTACT_BBC_SERVERS = "Unable to contact Bitbucket Cloud servers";
  protected static final String MISSING_PULL_REQUEST_READ_PERMISSION = "The OAuth consumer in the Bitbucket workspace is not configured with the permission to read pull requests.";
  protected static final String SCOPE = "Scope is: %s";
  protected static final String UNAUTHORIZED_CLIENT = "Check your credentials";
  protected static final String OAUTH_CONSUMER_NOT_PRIVATE = "Configure the OAuth consumer in the Bitbucket workspace to be a private consumer";
  protected static final String BBC_FAIL_WITH_RESPONSE = "Bitbucket Cloud API call to [%s] failed with %s http code. Bitbucket Cloud response content : [%s]";
  protected static final String BBC_FAIL_WITH_ERROR = "Bitbucket Cloud API call to [%s] failed with error: %s";

  protected static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

  private final OkHttpClient client;
  private final String bitbucketCloudEndpoint;
  private final String accessTokenEndpoint;


  @Inject
  public BitbucketCloudRestClient(OkHttpClient bitBucketCloudHttpClient) {
    this(bitBucketCloudHttpClient, ENDPOINT, ACCESS_TOKEN_ENDPOINT);
  }

  protected BitbucketCloudRestClient(OkHttpClient bitBucketCloudHttpClient, String bitbucketCloudEndpoint, String accessTokenEndpoint) {
    this.client = bitBucketCloudHttpClient;
    this.bitbucketCloudEndpoint = bitbucketCloudEndpoint;
    this.accessTokenEndpoint = accessTokenEndpoint;
  }

  /**
   * Validate parameters provided.
   */
  public void validate(String clientId, String clientSecret, String workspace) {
    Token token = validateAccessToken(clientId, clientSecret);

    if (token.getScopes() == null || !token.getScopes().contains("pullrequest")) {
      LOG.info(MISSING_PULL_REQUEST_READ_PERMISSION + String.format(SCOPE, token.getScopes()));
      throw new IllegalArgumentException(ERROR_BBC_SERVERS + ": " + MISSING_PULL_REQUEST_READ_PERMISSION);
    }

    try {
      doGet(token.getAccessToken(), buildUrl("/repositories/" + workspace), r -> null);
    } catch (NotFoundException | IllegalStateException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
  }

  /**
   * Validate parameters provided.
   */
  public void validateAppPassword(String encodedCredentials, String workspace) {
    try {
      doGetWithBasicAuth(encodedCredentials, buildUrl("/repositories/" + workspace), r -> null);
    } catch (NotFoundException | IllegalStateException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
  }

  private Token validateAccessToken(String clientId, String clientSecret) {
    Request request = createAccessTokenRequest(clientId, clientSecret);
    try (Response response = client.newCall(request).execute()) {
      if (response.isSuccessful()) {
        return buildGson().fromJson(response.body().charStream(), Token.class);
      }

      ErrorDetails errorMsg = getTokenError(response.body());
      if (errorMsg.body != null) {
        LOG.info(String.format(BBC_FAIL_WITH_RESPONSE, response.request().url(), response.code(), errorMsg.body));
        switch (errorMsg.body) {
          case "invalid_grant":
            throw new IllegalArgumentException(UNABLE_TO_CONTACT_BBC_SERVERS + ": " + OAUTH_CONSUMER_NOT_PRIVATE);
          case "unauthorized_client":
            throw new IllegalArgumentException(UNABLE_TO_CONTACT_BBC_SERVERS + ": " + UNAUTHORIZED_CLIENT);
          default:
            if (errorMsg.parsedErrorMsg != null) {
              throw new IllegalArgumentException(ERROR_BBC_SERVERS + ": " + errorMsg.parsedErrorMsg);
            } else {
              throw new IllegalArgumentException(UNABLE_TO_CONTACT_BBC_SERVERS);
            }
        }
      } else {
        LOG.info(String.format(BBC_FAIL_WITH_RESPONSE, response.request().url(), response.code(), response.message()));
      }
      throw new IllegalArgumentException(UNABLE_TO_CONTACT_BBC_SERVERS);

    } catch (IOException e) {
      LOG.info(String.format(BBC_FAIL_WITH_ERROR, request.url(), e.getMessage()));
      throw new IllegalArgumentException(UNABLE_TO_CONTACT_BBC_SERVERS, e);
    }
  }

  public RepositoryList searchRepos(String encodedCredentials, String workspace, @Nullable String repoName, Integer page, Integer pageSize) {
    String filterQuery = String.format("q=name~\"%s\"", repoName != null ? repoName : "");
    HttpUrl url = buildUrl(String.format("/repositories/%s?%s&page=%s&pagelen=%s", workspace, filterQuery, page, pageSize));
    return doGetWithBasicAuth(encodedCredentials, url, r -> buildGson().fromJson(r.body().charStream(), RepositoryList.class));
  }

  public Repository getRepo(String encodedCredentials, String workspace, String slug) {
    HttpUrl url = buildUrl(String.format("/repositories/%s/%s", workspace, slug));
    return doGetWithBasicAuth(encodedCredentials, url, r -> buildGson().fromJson(r.body().charStream(), Repository.class));
  }

  public String createAccessToken(String clientId, String clientSecret) {
    Request request = createAccessTokenRequest(clientId, clientSecret);
    return doCall(request, r -> buildGson().fromJson(r.body().charStream(), Token.class)).getAccessToken();
  }

  private Request createAccessTokenRequest(String clientId, String clientSecret) {
    RequestBody body = new FormBody.Builder()
      .add("grant_type", "client_credentials")
      .build();
    HttpUrl url = HttpUrl.parse(accessTokenEndpoint);
    String credential = Credentials.basic(clientId, clientSecret);
    return prepareRequestWithBasicAuthCredentials(credential, "POST", url, body);
  }

  protected HttpUrl buildUrl(String relativeUrl) {
    return HttpUrl.parse(removeEnd(bitbucketCloudEndpoint, "/") + "/" + VERSION + relativeUrl);
  }

  protected <G> G doGet(String accessToken, HttpUrl url, Function<Response, G> handler) {
    Request request = prepareRequestWithAccessToken(accessToken, GET, url, null);
    return doCall(request, handler);
  }

  protected <G> G doGetWithBasicAuth(String encodedCredentials, HttpUrl url, Function<Response, G> handler) {
    Request request = prepareRequestWithBasicAuthCredentials("Basic " + encodedCredentials, GET, url, null);
    return doCall(request, handler);
  }

  protected <G> G doCall(Request request, Function<Response, G> handler) {
    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        handleError(response);
      }
      return handler.apply(response);
    } catch (IOException e) {
      LOG.info(ERROR_BBC_SERVERS + ": {}", e.getMessage());
      throw new IllegalStateException(ERROR_BBC_SERVERS, e);
    }
  }

  private static void handleError(Response response) throws IOException {
    ErrorDetails error = getError(response.body());
    LOG.info(String.format(BBC_FAIL_WITH_RESPONSE, response.request().url(), response.code(), error.body));
    if (error.parsedErrorMsg != null) {
      throw new IllegalStateException(ERROR_BBC_SERVERS + ": " + error.parsedErrorMsg);
    } else {
      throw new IllegalStateException(UNABLE_TO_CONTACT_BBC_SERVERS);
    }
  }

  private static ErrorDetails getError(@Nullable ResponseBody body) throws IOException {
    return getErrorDetails(body, s -> {
      Error gsonError = buildGson().fromJson(s, Error.class);
      if (gsonError != null && gsonError.errorMsg != null && gsonError.errorMsg.message != null) {
        return gsonError.errorMsg.message;
      }
      return null;
    });
  }

  private static ErrorDetails getTokenError(@Nullable ResponseBody body) throws IOException {
    if (body == null) {
      return new ErrorDetails(null, null);
    }
    String bodyStr = body.string();
    if (body.contentType() != null && Objects.equals(JSON_MEDIA_TYPE.type(), body.contentType().type())) {
      try {
        TokenError gsonError = buildGson().fromJson(bodyStr, TokenError.class);
        if (gsonError != null && gsonError.error != null) {
          return new ErrorDetails(gsonError.error, gsonError.errorDescription);
        }
      } catch (JsonParseException e) {
        // ignore
      }
    }

    return new ErrorDetails(bodyStr, null);
  }

  private static class ErrorDetails {
    @Nullable
    private final String body;
    @Nullable
    private final String parsedErrorMsg;

    public ErrorDetails(@Nullable String body, @Nullable String parsedErrorMsg) {
      this.body = body;
      this.parsedErrorMsg = parsedErrorMsg;
    }
  }

  private static ErrorDetails getErrorDetails(@Nullable ResponseBody body, UnaryOperator<String> parser) throws IOException {
    if (body == null) {
      return new ErrorDetails("", null);
    }
    String bodyStr = body.string();
    if (body.contentType() != null && Objects.equals(JSON_MEDIA_TYPE.type(), body.contentType().type())) {
      try {
        return new ErrorDetails(bodyStr, parser.apply(bodyStr));
      } catch (JsonParseException e) {
        // ignore
      }
    }
    return new ErrorDetails(bodyStr, null);
  }

  protected static Request prepareRequestWithAccessToken(String accessToken, String method, HttpUrl url, @Nullable RequestBody body) {
    return new Request.Builder()
      .method(method, body)
      .url(url)
      .header(AUTHORIZATION, "Bearer " + accessToken)
      .build();
  }

  protected static Request prepareRequestWithBasicAuthCredentials(String encodedCredentials, String method,
    HttpUrl url, @Nullable RequestBody body) {
    return new Request.Builder()
      .method(method, body)
      .url(url)
      .header(AUTHORIZATION, encodedCredentials)
      .build();
  }

  public static Gson buildGson() {
    return new GsonBuilder().create();
  }
}
