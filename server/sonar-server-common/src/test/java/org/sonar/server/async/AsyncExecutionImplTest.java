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
package org.sonar.server.async;

import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AsyncExecutionImplTest {
  @Rule
  public LogTester logTester = new LogTester();

  private AsyncExecutionExecutorService synchronousExecutorService = Runnable::run;
  private AsyncExecutionImpl underTest = new AsyncExecutionImpl(synchronousExecutorService);

  @Test
  public void addToQueue_fails_with_NPE_if_Runnable_is_null() {
    assertThatThrownBy(() -> underTest.addToQueue(null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void addToQueue_submits_runnable_to_executorService_which_does_not_fail_if_Runnable_argument_throws_exception() {
    underTest.addToQueue(() -> {
      throw new RuntimeException("Faking an exception thrown by Runnable argument");
    });

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(Level.ERROR)).containsOnly("Asynchronous task failed");
  }

  @Test
  public void addToQueue_submits_runnable_that_fails_if_Runnable_argument_throws_Error() {
    Error expected = new Error("Faking an exception thrown by Runnable argument");
    Runnable runnable = () -> {
      throw expected;
    };

    assertThatThrownBy(() -> underTest.addToQueue(runnable))
      .isInstanceOf(Error.class)
      .hasMessage(expected.getMessage());
  }
}
