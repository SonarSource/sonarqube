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
package org.sonar.ce.taskprocessor;

import java.util.Random;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.ce.log.CeLogging;
import org.sonar.ce.queue.CeTask;
import org.sonar.db.ce.CeActivityDto;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class CeLoggingWorkerExecutionListenerTest {
  private CeLogging ceLogging = Mockito.spy(CeLogging.class);
  private CeLoggingWorkerExecutionListener underTest = new CeLoggingWorkerExecutionListener(ceLogging);

  @Test
  public void onStart_calls_initForTask_with_method_argument() {
    CeTask ceTask = Mockito.mock(CeTask.class);

    underTest.onStart(ceTask);

    verify(ceLogging).initForTask(ceTask);
    verifyNoMoreInteractions(ceLogging);
  }

  @Test
  public void onEnd_calls_clearForTask() {
    underTest.onEnd(mock(CeTask.class),
      CeActivityDto.Status.values()[new Random().nextInt(CeActivityDto.Status.values().length)],
      null, null);

    verify(ceLogging).clearForTask();
    verifyNoMoreInteractions(ceLogging);
  }
}
