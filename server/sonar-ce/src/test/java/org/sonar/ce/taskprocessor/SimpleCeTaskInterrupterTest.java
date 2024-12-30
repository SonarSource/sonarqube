/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import org.junit.Test;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.CeTaskCanceledException;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

public class SimpleCeTaskInterrupterTest {

  private SimpleCeTaskInterrupter underTest = new SimpleCeTaskInterrupter();

  @Test
  public void check_throws_CeTaskCanceledException_if_provided_thread_is_interrupted() throws InterruptedException {
    String threadName = secure().nextAlphabetic(30);
    ComputingThread t = new ComputingThread(threadName);

    try {
      t.start();

      // will not fail
      underTest.check(t);

      t.interrupt();

      assertThatThrownBy(() -> underTest.check(t))
        .isInstanceOf(CeTaskCanceledException.class)
        .hasMessage("CeWorker executing in Thread '" + threadName + "' has been interrupted");
    } finally {
      t.kill();
      t.join(1_000);
    }
  }

  @Test
  public void onStart_has_no_effect() {
    CeTask ceTask = mock(CeTask.class);

    underTest.onStart(ceTask);

    verifyNoInteractions(ceTask);
  }

  @Test
  public void onEnd_has_no_effect() {
    CeTask ceTask = mock(CeTask.class);

    underTest.onEnd(ceTask);

    verifyNoInteractions(ceTask);
  }
}
