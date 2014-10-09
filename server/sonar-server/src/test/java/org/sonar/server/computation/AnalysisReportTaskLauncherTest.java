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

package org.sonar.server.computation;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

public class AnalysisReportTaskLauncherTest {

  @Rule
  public Timeout timeout = new Timeout(5000);

  private AnalysisReportTaskLauncher sut;
  private ComputationService service;

  @Before
  public void before() {
    this.service = mock(ComputationService.class);
  }

  @After
  public void after() {
    sut.stop();
  }

  @Test
  public void call_findAndBook_when_launching_a_recurrent_task() {
    sut = new AnalysisReportTaskLauncher(service, 0, 1, TimeUnit.MILLISECONDS);

    sut.start();

    sleep();

    verify(service, atLeastOnce()).findAndBookNextAvailableAnalysisReport();
  }

  @Test
  public void call_findAndBook_when_executing_task_immediatly() {
    sut = new AnalysisReportTaskLauncher(service, 1, 1, TimeUnit.HOURS);
    sut.start();

    sut.startAnalysisTaskNow();

    sleep();

    verify(service, atLeastOnce()).findAndBookNextAvailableAnalysisReport();
  }

  private void sleep() {
    try {
      TimeUnit.MILLISECONDS.sleep(50L);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

}
