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
package org.sonar.auth;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class OAuthRestClient {

  private static final int DEFAULT_PAGE_SIZE = 100;
  private static final Pattern NEXT_LINK_PATTERN = Pattern.compile(".*<(.*)>; rel=\"next\"");

  private OAuthRestClient() {
    // Only static method
  }

  public static Response executeRequest(String requestUrl, OAuth20Service scribe, OAuth2AccessToken accessToken) throws IOException {
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

  public static <E> List<E> executePaginatedRequest(String request, OAuth20Service scribe, OAuth2AccessToken accessToken, Function<String, List<E>> function) {
    List<E> result = new ArrayList<>();
    readPage(result, scribe, accessToken, addPerPageQueryParameter(request, DEFAULT_PAGE_SIZE), function);
    return result;
  }

  public static String addPerPageQueryParameter(String request, int pageSize) {
    String separator = request.contains("?") ? "&" : "?";
    return request + separator + "per_page=" + pageSize;
  }

  private static <E> void readPage(List<E> result, OAuth20Service scribe, OAuth2AccessToken accessToken, String endPoint, Function<String, List<E>> function) {
    try (Response nextResponse = executeRequest(endPoint, scribe, accessToken)) {
      String content = nextResponse.getBody();
      if (content == null) {
        return;
      }
      result.addAll(function.apply(content));
      readNextEndPoint(nextResponse).ifPresent(newNextEndPoint -> readPage(result, scribe, accessToken, newNextEndPoint, function));
    } catch (IOException e) {
      throw new IllegalStateException(format("Failed to get %s", endPoint), e);
    }
  }

  private static Optional<String> readNextEndPoint(Response response) {
    Optional<String> link = response.getHeaders().entrySet().stream()
      .filter(e -> "Link".equalsIgnoreCase(e.getKey()))
      .map(Map.Entry::getValue)
      .findAny();
    if (link.isEmpty() || link.get().isEmpty() || !link.get().contains("rel=\"next\"")) {
      return Optional.empty();
    }
    Matcher nextLinkMatcher = NEXT_LINK_PATTERN.matcher(link.get());
    if (!nextLinkMatcher.find()) {
      return Optional.empty();
    }
    return Optional.of(nextLinkMatcher.group(1));
  }

  private static IllegalStateException unexpectedResponseCode(String requestUrl, Response response) throws IOException {
    return new IllegalStateException(format("Fail to execute request '%s'. HTTP code: %s, response: %s", requestUrl, response.getCode(), response.getBody()));
  }

}
