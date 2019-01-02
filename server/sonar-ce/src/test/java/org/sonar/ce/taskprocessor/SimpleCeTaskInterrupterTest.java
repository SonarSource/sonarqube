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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.CeTaskCanceledException;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class SimpleCeTaskInterrupterTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private SimpleCeTaskInterrupter underTest = new SimpleCeTaskInterrupter();

  @Test
  public void check_throws_CeTaskCanceledException_if_provided_thread_is_interrupted() throws InterruptedException {
    String threadName = randomAlphabetic(30);
    ComputingThread t = new ComputingThread(threadName);

    try {
      t.start();

      // will not fail
      underTest.check(t);

      t.interrupt();

      expectedException.expect(CeTaskCanceledException.class);
      expectedException.expectMessage("CeWorker executing in Thread '" + threadName + "' has been interrupted");

      underTest.check(t);
    } finally {
      t.kill();
      t.join(1_000);
    }
  }

  @Test
  public void onStart_has_no_effect() {
    CeTask ceTask = mock(CeTask.class);

    underTest.onStart(ceTask);

    verifyZeroInteractions(ceTask);
  }

  @Test
  public void onEnd_has_no_effect() {
    CeTask ceTask = mock(CeTask.class);

    underTest.onEnd(ceTask);

    verifyZeroInteractions(ceTask);
  }
}
