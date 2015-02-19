/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.db.migrations.v36;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.log.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ProgressTest {
  @Test
  public void log_progress() throws Exception {
    Logger logger = mock(Logger.class);
    ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);

    Progress progress = new Progress(5000, logger, System.currentTimeMillis());
    progress.run();
    progress.increment(200);
    progress.increment(130);
    progress.run();
    progress.increment(1670);
    progress.run();

    verify(logger, times(3)).info(argument.capture());
    assertThat(argument.getAllValues().get(0)).isEqualTo("0% [0/5000 violations]");
    assertThat(argument.getAllValues().get(1)).isEqualTo("6% [330/5000 violations]");
    assertThat(argument.getAllValues().get(2)).isEqualTo("40% [2000/5000 violations]");
  }

  @Test
  public void log_remaining_time() throws Exception {
    Logger logger = mock(Logger.class);
    ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);

    long fiveMinutesAgo = System.currentTimeMillis() - 5 * 60 * 1000;
    Progress progress = new Progress(5000, logger, fiveMinutesAgo);
    progress.increment(2000);
    progress.run();

    verify(logger).info(argument.capture());
    assertThat(argument.getValue()).isEqualTo("40% [2000/5000 violations, 7 minutes remaining]");
  }
}
