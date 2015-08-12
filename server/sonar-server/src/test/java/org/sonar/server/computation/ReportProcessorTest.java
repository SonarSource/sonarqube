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

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.compute.AnalysisReportDto;
import org.sonar.db.compute.AnalysisReportDto.Status;
import org.sonar.server.computation.activity.ActivityManager;
import org.sonar.server.computation.monitoring.CEQueueStatus;
import org.sonar.server.computation.step.ComputationStep;
import org.sonar.server.computation.step.ComputationSteps;
import org.sonar.server.computation.step.ReportComputationSteps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ReportProcessorTest {

  @Rule
  public LogTester logTester = new LogTester();

  ComputationStep projectStep1 = mockStep();
  ComputationStep projectStep2 = mockStep();
  ComputationSteps steps = mock(ReportComputationSteps.class);
  ActivityManager activityManager = mock(ActivityManager.class);
  System2 system = mock(System2.class);
  CEQueueStatus queueStatus = mock(CEQueueStatus.class);
  AnalysisReportDto dto = AnalysisReportDto.newForTests(1L).setProjectKey("P1").setUuid("U1").setStatus(Status.PENDING);
  ReportProcessor underTest;

  @Before
  public void setUp() {
    underTest = new ReportProcessor(steps, new ReportQueue.Item(dto, new File("Do_not_care")), activityManager, system, queueStatus);
  }

  @Test
  public void process_new_project() {
    logTester.setLevel(LoggerLevel.INFO);

    when(steps.instances()).thenReturn(Arrays.asList(projectStep1, projectStep2));

    underTest.process();

    // report is integrated -> status is set to SUCCESS
    assertThat(dto.getStatus()).isEqualTo(Status.SUCCESS);
    assertThat(dto.getFinishedAt()).isNotNull();

    // one info log at the end
    assertThat(logTester.logs(LoggerLevel.INFO)).hasSize(3);
//    assertThat(logTester.logs(LoggerLevel.INFO).get(0)).startsWith("Analysis of project P1 (report 1) (done) | time=");
    assertThat(logTester.logs(LoggerLevel.INFO).get(2)).startsWith("Analysis of project P1 (report 1) total time spent in steps=");

    // execute only the steps supporting the project qualifier
    verify(projectStep1).execute();
    verify(projectStep2).execute();
    verify(activityManager).saveActivity(dto);

    verify(queueStatus).addInProgress();
    verify(queueStatus).addSuccess(anyLong());
    verifyNoMoreInteractions(queueStatus);
  }

  @Test
  public void debug_logs() {
    when(steps.instances()).thenReturn(Collections.<ComputationStep>emptyList());
    logTester.setLevel(LoggerLevel.DEBUG);

    underTest.process();

    assertThat(logTester.logs(LoggerLevel.DEBUG)).isNotEmpty();
  }

  @Test
  public void fail_if_step_throws_error() {
    String errorMessage = "Failed to unzip";
    when(steps.instances()).thenReturn(ImmutableList.of(projectStep1));
    doThrow(new IllegalStateException(errorMessage)).when(projectStep1).execute();

    try {
      underTest.process();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo(errorMessage);
      assertThat(dto.getStatus()).isEqualTo(Status.FAILED);
      assertThat(dto.getFinishedAt()).isNotNull();

      verify(queueStatus).addInProgress();
      verify(queueStatus).addError(anyLong());
      verifyNoMoreInteractions(queueStatus);
    }
  }

  @Test
  public void step_error() {
    when(steps.instances()).thenReturn(Collections.singleton(projectStep1));
    doThrow(new IllegalStateException("pb")).when(projectStep1).execute();

    try {
      underTest.process();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("pb");
      assertThat(dto.getStatus()).isEqualTo(Status.FAILED);
      assertThat(dto.getFinishedAt()).isNotNull();

      verify(queueStatus).addInProgress();
      verify(queueStatus).addError(anyLong());
      verifyNoMoreInteractions(queueStatus);
    }
  }

  private ComputationStep mockStep() {
    ComputationStep step = mock(ComputationStep.class);
    when(step.getDescription()).thenReturn(RandomStringUtils.randomAscii(5));
    return step;
  }

}
