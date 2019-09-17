/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.auth.gitlab;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class GitLabRestClient {

  private static final int DEFAULT_PAGE_SIZE = 100;
  private static final Pattern NEXT_LINK_PATTERN = Pattern.compile(".*<(.*)>; rel=\"next\"");

  private static final String API_SUFFIX = "/api/v4";

  private final GitLabSettings settings;

  public GitLabRestClient(GitLabSettings settings) {
    this.settings = settings;
  }

  GsonUser getUser(OAuth20Service scribe, OAuth2AccessToken accessToken) {
    try (Response response = executeRequest(settings.url() + API_SUFFIX + "/user", scribe, accessToken)) {
      String responseBody = response.getBody();
      return GsonUser.parse(responseBody);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to get gitlab user", e);
    }
  }

  List<GsonGroup> getGroups(OAuth20Service scribe, OAuth2AccessToken accessToken) {
    return executePaginatedQuery(settings.url() + API_SUFFIX + "/groups", scribe, accessToken, GsonGroup::parse);
  }

  private static Response executeRequest(String requestUrl, OAuth20Service scribe, OAuth2AccessToken accessToken) throws IOException {
    OAuthRequest request = new OAuthRequest(Verb.GET, requestUrl);
    scribe.signRequest(accessToken, request);
    try {
      Response response = scribe.execute(request);
      if (!response.isSuccessful()) {
        throw unexpectedResponseCode(requestUrl, response);
      }
      return response;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    } catch (ExecutionException e) {
      throw new IllegalStateException(e);
    }
  }

  private static <E> List<E> executePaginatedQuery(String query, OAuth20Service scribe, OAuth2AccessToken accessToken, Function<String, List<E>> function) {
    List<E> result = new ArrayList<>();
    readNextPage(result, scribe, accessToken, query + "?per_page=" + DEFAULT_PAGE_SIZE, function);
    return result;
  }

  private static <E> void readNextPage(List<E> result, OAuth20Service scribe, OAuth2AccessToken accessToken, String nextEndPoint, Function<String, List<E>> function) {
    try (Response nextResponse = executeRequest(nextEndPoint, scribe, accessToken)) {
      String content = nextResponse.getBody();
      if (content == null) {
        return;
      }
      result.addAll(function.apply(content));
      readNextEndPoint(nextResponse).ifPresent(newNextEndPoint -> readNextPage(result, scribe, accessToken, newNextEndPoint, function));
    } catch (IOException e) {
      throw new IllegalStateException(format("Failed to get %s", nextEndPoint), e);
    }
  }

  private static Optional<String> readNextEndPoint(Response response) {
    String link = response.getHeader("Link");
    if (link == null || link.isEmpty() || !link.contains("rel=\"next\"")) {
      return Optional.empty();
    }
    Matcher nextLinkMatcher = NEXT_LINK_PATTERN.matcher(link);
    if (!nextLinkMatcher.find()) {
      return Optional.empty();
    }
    return Optional.of(nextLinkMatcher.group(1));
  }

  private static IllegalStateException unexpectedResponseCode(String requestUrl, Response response) throws IOException {
    return new IllegalStateException(format("Fail to execute request '%s'. HTTP code: %s, response: %s", requestUrl, response.getCode(), response.getBody()));
  }

}
