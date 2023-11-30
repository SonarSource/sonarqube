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
package org.sonar.alm.client;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.alm.client.RatioBasedRateLimitChecker.RATE_RATIO_EXCEEDED_MESSAGE;

@RunWith(DataProviderRunner.class)
public class RatioBasedRateLimitCheckerTest {

  @Rule
  public LogTester logTester = new LogTester();
  private static final long MILLIS_BEFORE_RESET = 100L;
  RatioBasedRateLimitChecker ratioBasedRateLimitChecker = new RatioBasedRateLimitChecker();

  @DataProvider
  public static Object[][] rates() {
    return new Object[][] {
      {10000, 100000, false},
      {10000, 10000, false},
      {10000, 9999, false},
      {10000, 9900, false},
      {10000, 1001, false},
      {10000, 1000, true},
      {10000, 500, true},
      {10000, 0, true},
    };
  }

  @Test
  @UseDataProvider("rates")
  public void checkRateLimit(int limit, int remaining, boolean rateLimitShouldBeExceeded) throws InterruptedException {
    ApplicationHttpClient.RateLimit record = mock();
    when(record.limit()).thenReturn(limit);
    when(record.remaining()).thenReturn(remaining);
    when(record.reset()).thenReturn(System.currentTimeMillis() / 1000 + 1);

    long start = System.currentTimeMillis();
    boolean result = ratioBasedRateLimitChecker.checkRateLimit(record);
    long stop = System.currentTimeMillis();
    long totalTime = stop - start;

    if (rateLimitShouldBeExceeded) {
      assertThat(result).isTrue();
      assertThat(stop).isGreaterThanOrEqualTo(record.reset());
      assertThat(logTester.logs(Level.WARN)).contains(
        format(RATE_RATIO_EXCEEDED_MESSAGE.replaceAll("\\{\\}", "%s"), limit - remaining, limit));
    } else {
      assertThat(result).isFalse();
      assertThat(totalTime).isLessThan(MILLIS_BEFORE_RESET);
      assertThat(logTester.logs(Level.WARN)).isEmpty();
    }
  }
}
