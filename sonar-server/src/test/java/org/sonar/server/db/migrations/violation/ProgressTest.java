/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.db.migrations.violation;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ProgressTest {
  @Test
  public void log_progress() throws Exception {
    Logger logger = mock(Logger.class);
    ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);

    Progress progress = new Progress(5000, logger);
    progress.run();
    progress.increment(200);
    progress.increment(130);
    progress.run();
    progress.increment(1670);
    progress.run();

    verify(logger, times(3)).info(argument.capture());
    assertThat(argument.getAllValues().get(0)).matches("0% \\[0/5000 violations, 0 violations/minute, \\d+ minutes remaining\\]");
    assertThat(argument.getAllValues().get(1)).matches("6% \\[330/5000 violations, \\d+ violations/minute, \\d+ minutes remaining\\]");
    assertThat(argument.getAllValues().get(2)).matches("40% \\[2000/5000 violations, \\d+ violations/minute, \\d+ minutes remaining\\]");
  }
}
