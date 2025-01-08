/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.RateLimitChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

@ComputeEngineSide
@ServerSide
public class RatioBasedRateLimitChecker extends RateLimitChecker {
  private static final Logger LOGGER = LoggerFactory.getLogger(RatioBasedRateLimitChecker.class);

  @VisibleForTesting
  static final String RATE_RATIO_EXCEEDED_MESSAGE = "The external system API rate limit is almost reached. Pausing GitHub provisioning until the next rate limit reset. "
                                                    + "{} out of {} calls were used.";

  private static final int MAX_PERCENTAGE_OF_CALLS_FOR_PROVISIONING = 90;

  @Override
  protected boolean checkRateLimit(GHRateLimit.Record rateLimitRecord, long count) throws InterruptedException {
    int limit = rateLimitRecord.getLimit();
    int apiCallsUsed = limit - rateLimitRecord.getRemaining();
    double percentageOfCallsUsed = computePercentageOfCallsUsed(apiCallsUsed, limit);
    LOGGER.debug("{} external system API calls used of {}", apiCallsUsed, limit);
    if (percentageOfCallsUsed >= MAX_PERCENTAGE_OF_CALLS_FOR_PROVISIONING) {
      LOGGER.warn(RATE_RATIO_EXCEEDED_MESSAGE, apiCallsUsed, limit);
      return sleepUntilReset(rateLimitRecord);
    }
    return false;
  }

  private static double computePercentageOfCallsUsed(int used, int limit) {
    return (double) used * 100 / limit;
  }
}
