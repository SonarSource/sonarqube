/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.core.util;

import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.Loggers;

import static org.assertj.core.api.Assertions.assertThat;

public class ProgressLoggerTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Test(timeout = 5_000L)
  public void log_at_fixed_intervals() {
    AtomicLong counter = new AtomicLong(42L);
    ProgressLogger progress = new ProgressLogger("ProgressLoggerTest", counter, Loggers.get(getClass()));
    progress.setPeriodMs(1L);
    progress.start();
    while (logTester.logs(LoggerLevel.INFO).size()<2) {
      Uninterruptibles.sleepUninterruptibly(1, TimeUnit.MILLISECONDS);
    }
    progress.stop();
    assertThat(hasInfoLog("42 rows processed")).isTrue();

    // ability to manual log, generally final status
    counter.incrementAndGet();
    progress.log();
    assertThat(hasInfoLog("43 rows processed")).isTrue();
  }

  @Test
  public void create() {
    ProgressLogger progress = ProgressLogger.create(getClass(), new AtomicLong());

    // default values
    assertThat(progress.getPeriodMs()).isEqualTo(60000L);
    assertThat(progress.getPluralLabel()).isEqualTo("rows");

    // override values
    progress.setPeriodMs(10L);
    progress.setPluralLabel("issues");
    assertThat(progress.getPeriodMs()).isEqualTo(10L);
    assertThat(progress.getPluralLabel()).isEqualTo("issues");
  }

  private boolean hasInfoLog(String expectedLog) {
    return logTester.logs(LoggerLevel.INFO).stream().filter(s -> s.startsWith(expectedLog)).findFirst().isPresent();
  }
}
