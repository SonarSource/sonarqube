/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.ce.app;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.utils.log.LoggerLevel.DEBUG;
import static org.sonar.api.utils.log.LoggerLevel.ERROR;
import static org.sonar.api.utils.log.LoggerLevel.INFO;
import static org.sonar.api.utils.log.LoggerLevel.TRACE;
import static org.sonar.api.utils.log.LoggerLevel.WARN;

public class LogarithmicLoggerTest {
  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void logarithmically_logs_less_and_less_frequently_calls_to_same_Logger_method() throws InterruptedException {
    Logger logarithmicLogger = LogarithmicLogger.from(Loggers.get(getClass())).build();
    for (int i = 0; i < 1000; i++) {
      logarithmicLogger.error(String.valueOf(i));
    }

    assertThat(logTester.logs(ERROR)).containsOnly(
      "1", "3", "8", "21", "55", "149", "404"
      );
    assertThat(logTester.logs()).containsOnly(
      "1", "3", "8", "21", "55", "149", "404"
      );
  }

  @Test
  public void logarithmically_logs_less_and_less_frequently_calls_across_log_levels() throws InterruptedException {
    Logger logarithmicLogger = LogarithmicLogger.from(Loggers.get(getClass())).build();
    for (int i = 0; i < 1000; i++) {
      spawnMessageOnLevels(logarithmicLogger, i, String.valueOf(i));
    }

    assertThat(logTester.logs()).containsOnly(
      "1", "3", "8", "21", "55", "149", "404"
      );
    assertThat(logTester.logs(ERROR)).containsOnly("55");
    assertThat(logTester.logs(WARN)).containsOnly("1", "21");
    assertThat(logTester.logs(INFO)).isEmpty();
    assertThat(logTester.logs(DEBUG)).containsOnly("3", "8");
    assertThat(logTester.logs(TRACE)).containsOnly("149", "404");
  }

  @Test
  public void call_ratio_is_applied_before_logarithm() {
    int callRatio = 10;
    Logger logarithmicLogger = LogarithmicLogger.from(Loggers.get(getClass())).applyingCallRatio(callRatio).build();
    for (int i = 0; i < 1000 + callRatio; i++) {
      logarithmicLogger.error(String.valueOf(i));
    }

    assertThat(logTester.logs(ERROR)).containsOnly(
      "10", "30", "80", "210", "550"
      );
    assertThat(logTester.logs()).containsOnly(
      "10", "30", "80", "210", "550"
      );
  }

  private static void spawnMessageOnLevels(Logger logarithmicLogger, int i, String msg) {
    int c = i % 5;
    switch (c) {
      case 0:
        logarithmicLogger.error(msg);
        break;
      case 1:
        logarithmicLogger.warn(msg);
        break;
      case 2:
        logarithmicLogger.info(msg);
        break;
      case 3:
        logarithmicLogger.debug(msg);
        break;
      case 4:
        logarithmicLogger.trace(msg);
        break;
      default:
        throw new IllegalArgumentException("Unsupported value " + c);
    }
  }

}
