/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.alm.client.github;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.alm.client.github.security.AccessToken;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

import static java.lang.String.format;

@ServerSide
@ComputeEngineSide
public class GithubPaginatedHttpClient implements PaginatedHttpClient {

  private static final Logger LOG = LoggerFactory.getLogger(GithubPaginatedHttpClient.class);
  private final ApplicationHttpClient appHttpClient;
  private final RatioBasedRateLimitChecker rateLimitChecker;

  public GithubPaginatedHttpClient(ApplicationHttpClient appHttpClient, RatioBasedRateLimitChecker rateLimitChecker) {
    this.appHttpClient = appHttpClient;
    this.rateLimitChecker = rateLimitChecker;
  }

  @Override
  public <E> List<E> get(String appUrl, AccessToken token, String query, Function<String, List<E>> responseDeserializer) throws IOException {
    List<E> results = new ArrayList<>();
    String nextEndpoint = query + "?per_page=100";
    if (query.contains("?")) {
      nextEndpoint = query + "&per_page=100";
    }
    ApplicationHttpClient.RateLimit rateLimit = null;
    while (nextEndpoint != null) {
      checkRateLimit(rateLimit);
      ApplicationHttpClient.GetResponse response = executeCall(appUrl, token, nextEndpoint);
      response.getContent()
        .ifPresent(content -> results.addAll(responseDeserializer.apply(content)));
      nextEndpoint = response.getNextEndPoint().orElse(null);
      rateLimit = response.getRateLimit();
    }
    return results;
  }

  private void checkRateLimit(@Nullable ApplicationHttpClient.RateLimit rateLimit) {
    if (rateLimit == null) {
      return;
    }
    try {
      rateLimitChecker.checkRateLimit(rateLimit);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOG.warn(format("Thread interrupted: %s", e.getMessage()), e);
    }
  }

  private ApplicationHttpClient.GetResponse executeCall(String appUrl, AccessToken token, String endpoint) throws IOException {
    ApplicationHttpClient.GetResponse response = appHttpClient.get(appUrl, token, endpoint);
    if (response.getCode() < 200 || response.getCode() >= 300) {
      throw new IllegalStateException(
        format("Error while executing a call to GitHub. Return code %s. Error message: %s.", response.getCode(), response.getContent().orElse("")));
    }
    return response;
  }
}
