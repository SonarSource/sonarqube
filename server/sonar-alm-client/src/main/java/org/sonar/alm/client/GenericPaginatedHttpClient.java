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
package org.sonar.alm.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.kohsuke.github.GHRateLimit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.alm.client.ApplicationHttpClient.GetResponse;
import org.sonar.auth.github.security.AccessToken;

import static java.lang.String.format;

public abstract class GenericPaginatedHttpClient implements PaginatedHttpClient {

  private static final Logger LOG = LoggerFactory.getLogger(GenericPaginatedHttpClient.class);
  private final ApplicationHttpClient appHttpClient;
  private final RatioBasedRateLimitChecker rateLimitChecker;

  protected GenericPaginatedHttpClient(ApplicationHttpClient appHttpClient, RatioBasedRateLimitChecker rateLimitChecker) {
    this.appHttpClient = appHttpClient;
    this.rateLimitChecker = rateLimitChecker;
  }

  @Override
  public <E> List<E> get(String appUrl, AccessToken token, String query, Function<String, List<E>> responseDeserializer) {
    List<E> results = new ArrayList<>();
    String nextEndpoint = query + "?per_page=100";
    if (query.contains("?")) {
      nextEndpoint = query + "&per_page=100";
    }
    ApplicationHttpClient.RateLimit rateLimit = null;
    while (nextEndpoint != null) {
      checkRateLimit(rateLimit);
      GetResponse response = executeCall(appUrl, token, nextEndpoint);
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
      GHRateLimit.Record rateLimitRecord = new GHRateLimit.Record(rateLimit.limit(), rateLimit.remaining(), rateLimit.reset());
      rateLimitChecker.checkRateLimit(rateLimitRecord, 0);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOG.warn(format("Thread interrupted: %s", e.getMessage()), e);
    }
  }

  private GetResponse executeCall(String appUrl, AccessToken token, String endpoint) {
    try {
      GetResponse response = appHttpClient.get(appUrl, token, endpoint);
      if (response.getCode() < 200 || response.getCode() >= 300) {
        throw new IllegalStateException(
          format("Error while executing a call to %s. Return code %s. Error message: %s.", appUrl, response.getCode(), response.getContent().orElse("")));
      }
      return response;
    } catch (Exception e) {
      String errorMessage = format("SonarQube was not able to retrieve resources from external system. Error while executing a paginated call to %s, endpoint:%s.",
        appUrl, endpoint);
      logException(errorMessage, e);
      throw new IllegalStateException(errorMessage + " " + e.getMessage());
    }
  }

  private static void logException(String message, Exception e) {
    if (LOG.isDebugEnabled()) {
      LOG.warn(message, e);
    } else {
      LOG.warn(message, e.getMessage());
    }
  }
}
