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
package org.sonar.server.monitoring;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MainCollectorTest {

  private final MonitoringTask task1 = mock(MonitoringTask.class);
  private final MonitoringTask task2 = mock(MonitoringTask.class);

  private MainCollector underTest;

  @Before
  public void before() {
    MonitoringTask[] tasks = {task1, task2};
    for(MonitoringTask task : tasks) {
      when(task.getDelay()).thenReturn(1L);
      when(task.getPeriod()).thenReturn(1L);
    }
    underTest = new MainCollector(tasks);
  }

  @After
  public void stop() {
    underTest.stop();
  }

  @Test
  public void startAndStop_executorServiceIsShutdown() {
    underTest.start();

    assertFalse(underTest.getScheduledExecutorService().isShutdown());

    underTest.stop();

    assertTrue(underTest.getScheduledExecutorService().isShutdown());
  }

  @Test
  public void start_givenTwoTasks_callsGetsDelayAndPeriodFromTasks() {
    underTest.start();

    verify(task1, times(1)).getDelay();
    verify(task1, times(1)).getPeriod();
    verify(task2, times(1)).getDelay();
    verify(task2, times(1)).getPeriod();
  }
}
