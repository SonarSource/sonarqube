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
package org.sonar.core.util;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import org.sonar.api.utils.log.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.startsWith;
import static org.mockito.Mockito.verify;

public class ProgressLoggerTest {

  @Test(timeout = 1000L)
  public void log_at_fixed_intervals() throws Exception {
    Logger logger = mock(Logger.class);
    AtomicLong counter = new AtomicLong(42L);
    ProgressLogger progress = new ProgressLogger("ProgressLoggerTest", counter, logger);
    progress.setPeriodMs(1L);
    progress.start();
    Thread.sleep(80L);
    progress.stop();
    verify(logger, atLeast(1)).info(startsWith("42 rows processed"));

    // ability to manual log, generally final status
    counter.incrementAndGet();
    progress.log();
    verify(logger).info(startsWith("43 rows processed"));
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
}
