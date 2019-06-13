/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.taskprocessor;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.log.CeTaskLogging;
import org.sonar.db.ce.CeActivityDto;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class CeTaskLoggingWorkerExecutionListenerTest {
  private CeTaskLogging ceTaskLogging = Mockito.spy(CeTaskLogging.class);
  private CeLoggingWorkerExecutionListener underTest = new CeLoggingWorkerExecutionListener(ceTaskLogging);

  @Test
  public void onStart_calls_initForTask_with_method_argument() {
    CeTask ceTask = Mockito.mock(CeTask.class);

    underTest.onStart(ceTask);

    verify(ceTaskLogging).initForTask(ceTask);
    verifyNoMoreInteractions(ceTaskLogging);
  }

  @Test
  public void onEnd_calls_clearForTask() {
    underTest.onEnd(mock(CeTask.class),
      CeActivityDto.Status.values()[new Random().nextInt(CeActivityDto.Status.values().length)],
      Duration.of(1, ChronoUnit.SECONDS), null, null);

    verify(ceTaskLogging).clearForTask();
    verifyNoMoreInteractions(ceTaskLogging);
  }
}
