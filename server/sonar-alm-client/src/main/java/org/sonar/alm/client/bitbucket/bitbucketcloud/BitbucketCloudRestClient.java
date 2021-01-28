/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import java.net.HttpURLConnection;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.sonar.api.internal.apachecommons.io.IOUtils;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.exceptions.NotFoundException;

import static org.sonar.api.internal.apachecommons.lang.StringUtils.removeEnd;

@ServerSide
public class BitbucketCloudRestClient {
  private static final Logger LOG = Loggers.get(BitbucketCloudRestClient.class);
  private static final String GET = "GET";
  private static final String ENDPOINT = "https://api.bitbucket.org";
  private static final String ACCESS_TOKEN_ENDPOINT = "https://bitbucket.org/site/oauth2/access_token";
  private static final String VERSION = "2.0";
  private static final String UNABLE_TO_CONTACT_BBC_SERVERS = "Unable to contact Bitbucket Cloud servers";
  protected static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

  private final OkHttpClient client;
  private final String bitbucketCloudEndpoint;
  private final String accessTokenEndpoint;

  public BitbucketCloudRestClient(OkHttpClient okHttpClient) {
    this(okHttpClient, ENDPOINT, ACCESS_TOKEN_ENDPOINT);
  }

  protected BitbucketCloudRestClient(OkHttpClient okHttpClient, String bitbucketCloudEndpoint, String accessTokenEndpoint) {
    this.client = okHttpClient;
    this.bitbucketCloudEndpoint = bitbucketCloudEndpoint;
    this.accessTokenEndpoint = accessTokenEndpoint;
  }

  /**
   * Validate parameters provided.
   */
  public void validate(String clientId, String clientSecret, String workspace) {
    String accessToken = validateAccessToken(clientId, clientSecret);
    doGet(accessToken, buildUrl("/workspaces/" + workspace + "/permissions"), r -> null, true);
  }

  private String validateAccessToken(String clientId, String clientSecret) {
    Response response = null;
    try {
      Request request = createAccessTokenRequest(clientId, clientSecret);
      response = client.newCall(request).execute();
      if (response.isSuccessful()) {
        return buildGson().fromJson(response.body().charStream(), Token.class).getAccessToken();
      }

      ErrorDetails errorMsg = getTokenError(response.body());
      if (errorMsg.parsedErrorMsg != null) {
        switch (errorMsg.parsedErrorMsg) {
          case "invalid_grant":
            throw new IllegalArgumentException(UNABLE_TO_CONTACT_BBC_SERVERS +
              ": Configure the OAuth consumer in the Bitbucket workspace to be a private consumer");
          case "unauthorized_client":
            throw new IllegalArgumentException(UNABLE_TO_CONTACT_BBC_SERVERS + ": Check your credentials");
          default:
            LOG.info("Validation failed: " + errorMsg.parsedErrorMsg);
        }
      } else {
        LOG.info("Validation failed: " + errorMsg.body);
      }
      throw new IllegalArgumentException(UNABLE_TO_CONTACT_BBC_SERVERS);

    } catch (IOException e) {
      throw new IllegalArgumentException(UNABLE_TO_CONTACT_BBC_SERVERS, e);
    } finally {
      if (response != null && response.body() != null) {
        IOUtils.closeQuietly(response);
      }
    }
  }

  public String createAccessToken(String clientId, String clientSecret) {
    Request request = createAccessTokenRequest(clientId, clientSecret);
    return doCall(request, r -> buildGson().fromJson(r.body().charStream(), Token.class), false).getAccessToken();
  }

  private Request createAccessTokenRequest(String clientId, String clientSecret) {
    RequestBody body = new FormBody.Builder()
      .add("grant_type", "client_credentials")
      .build();
    HttpUrl url = HttpUrl.parse(accessTokenEndpoint);
    String credential = Credentials.basic(clientId, clientSecret);
    return new Request.Builder()
      .method("POST", body)
      .url(url)
      .header("Authorization", credential)
      .build();
  }

  protected HttpUrl buildUrl(String relativeUrl) {
    return HttpUrl.parse(removeEnd(bitbucketCloudEndpoint, "/") + "/" + VERSION + relativeUrl);
  }

  protected <G> G doGet(String accessToken, HttpUrl url, Function<Response, G> handler, boolean throwErrorDetails) {
    Request request = prepareRequestWithAccessToken(accessToken, GET, url, null);
    return doCall(request, handler, throwErrorDetails);
  }

  protected <G> G doGet(String accessToken, HttpUrl url, Function<Response, G> handler) {
    Request request = prepareRequestWithAccessToken(accessToken, GET, url, null);
    return doCall(request, handler, false);
  }

  protected void doPost(String accessToken, HttpUrl url, RequestBody body) {
    Request request = prepareRequestWithAccessToken(accessToken, "POST", url, body);
    doCall(request, r -> null, false);
  }

  protected void doPut(String accessToken, HttpUrl url, String json) {
    RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);
    Request request = prepareRequestWithAccessToken(accessToken, "PUT", url, body);
    doCall(request, r -> null, false);
  }

  protected void doDelete(String accessToken, HttpUrl url) {
    Request request = prepareRequestWithAccessToken(accessToken, "DELETE", url, null);
    doCall(request, r -> null, false);
  }

  private <G> G doCall(Request request, Function<Response, G> handler, boolean throwErrorDetails) {
    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        handleError(response, throwErrorDetails);
      }
      return handler.apply(response);
    } catch (IOException e) {
      throw new IllegalStateException(UNABLE_TO_CONTACT_BBC_SERVERS, e);
    }
  }

  private static Request prepareRequestWithAccessToken(String accessToken, String method, HttpUrl url, @Nullable RequestBody body) {
    return new Request.Builder()
      .method(method, body)
      .url(url)
      .header("Authorization", "Bearer " + accessToken)
      .build();
  }

  public static Gson buildGson() {
    return new GsonBuilder().create();
  }

  private static ErrorDetails getTokenError(@Nullable ResponseBody body) throws IOException {
    return getErrorDetails(body, s -> {
      TokenError gsonError = buildGson().fromJson(s, TokenError.class);
      if (gsonError != null && gsonError.error != null) {
        return gsonError.error;
      }
      return null;
    });
  }

  private static <T> ErrorDetails getErrorDetails(@Nullable ResponseBody body, Function<String, String> parser) throws IOException {
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

  private static void handleError(Response response, boolean throwErrorDetails) throws IOException {
    int responseCode = response.code();
    ErrorDetails error = getError(response.body());
    if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
      String errorMsg = error.parsedErrorMsg != null ? error.parsedErrorMsg : "";
      if (throwErrorDetails) {
        throw new IllegalArgumentException(errorMsg);
      } else {
        throw new NotFoundException(errorMsg);
      }
    }
    LOG.info(UNABLE_TO_CONTACT_BBC_SERVERS + ": {} {}", responseCode, error.parsedErrorMsg != null ? error.parsedErrorMsg : error.body);

    if (throwErrorDetails && error.parsedErrorMsg != null) {
      throw new IllegalArgumentException(UNABLE_TO_CONTACT_BBC_SERVERS + ": " + error.parsedErrorMsg);
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

  private static class ErrorDetails {
    private String body;
    @Nullable
    private String parsedErrorMsg;

    public ErrorDetails(String body, @Nullable String parsedErrorMsg) {
      this.body = body;
      this.parsedErrorMsg = parsedErrorMsg;
    }
  }
}
