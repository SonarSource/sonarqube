/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.monitoring;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MainCollectorTest {

  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private final MonitoringTask task1 = mock(MonitoringTask.class);
  private final MonitoringTask task2 = mock(MonitoringTask.class);

  private MainCollector underTest;

  @BeforeEach
  void before() {
    MonitoringTask[] tasks = {task1, task2};
    for(MonitoringTask task : tasks) {
      when(task.getDelay()).thenReturn(1L);
      when(task.getPeriod()).thenReturn(1L);
    }
    underTest = new MainCollector(tasks);
  }

  @AfterEach
  void stop() {
    underTest.stop();
  }

  @Test
  void startAndStop_executorServiceIsShutdown() {
    underTest.start();

    assertFalse(underTest.getScheduledExecutorService().isShutdown());

    underTest.stop();

    assertTrue(underTest.getScheduledExecutorService().isShutdown());
  }

  @Test
  void start_givenTwoTasks_callsGetsDelayAndPeriodFromTasks() {
    underTest.start();

    verify(task1, times(1)).getDelay();
    verify(task1, times(1)).getPeriod();
    verify(task2, times(1)).getDelay();
    verify(task2, times(1)).getPeriod();
  }

  @Test
  void start_logsExceptionAsWarn_whenTaskThrowsException() {
    doThrow(new RuntimeException()).when(task1).run();

    underTest.start();

    String expectedLogMessage = "Error while executing monitoring task in " + task1.getClass().getSimpleName();
    await()
      .atMost(1, TimeUnit.SECONDS)
      .until(() -> logTester.logs().stream()
        .anyMatch(log -> log.contains(expectedLogMessage)));
  }
}
