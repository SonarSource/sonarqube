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
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.computation.db.AnalysisReportDto.Status;
import org.sonar.server.computation.activity.ActivityManager;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.language.LanguageRepository;
import org.sonar.server.computation.step.ComputationStep;
import org.sonar.server.computation.step.ComputationSteps;
import org.sonar.server.db.DbClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ComputationServiceTest {

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public LogTester logTester = new LogTester();

  ComputationStep projectStep1 = mockStep();
  ComputationStep projectStep2 = mockStep();
  ComputationSteps steps = mock(ComputationSteps.class);
  ActivityManager activityManager = mock(ActivityManager.class);
  System2 system = mock(System2.class);
  AnalysisReportDto dto = AnalysisReportDto.newForTests(1L).setProjectKey("P1").setUuid("U1").setStatus(Status.PENDING);
  ComputationService sut;

  @Before
  public void setUp() throws IOException {
    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey("project key")
      .setName("Project name")
      .build());

    sut = new ComputationService(new ReportQueue.Item(dto, new File("Do_not_care")), steps, activityManager, system,
      reportReader, mock(DbClient.class), mock(LanguageRepository.class));
  }

  @Test
  public void process_new_project() throws Exception {
    logTester.setLevel(LoggerLevel.INFO);

    when(steps.instances()).thenReturn(Arrays.asList(projectStep1, projectStep2));

    sut.process();

    // report is integrated -> status is set to SUCCESS
    assertThat(dto.getStatus()).isEqualTo(Status.SUCCESS);
    assertThat(dto.getFinishedAt()).isNotNull();

    // one info log at the end
    assertThat(logTester.logs(LoggerLevel.INFO)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.INFO).get(0)).startsWith("Analysis of project P1 (report 1) (done) | time=");

    // execute only the steps supporting the project qualifier
    verify(projectStep1).execute(any(ComputationContext.class));
    verify(projectStep2).execute(any(ComputationContext.class));
    verify(activityManager).saveActivity(dto);
  }

  @Test
  public void debug_logs() throws Exception {
    when(steps.instances()).thenReturn(Collections.<ComputationStep>emptyList());
    logTester.setLevel(LoggerLevel.DEBUG);

    sut.process();

    assertThat(logTester.logs(LoggerLevel.DEBUG)).isNotEmpty();
  }

  @Test
  public void fail_if_step_throws_error() throws Exception {
    String errorMessage = "Failed to unzip";
    when(steps.instances()).thenReturn(ImmutableList.of(projectStep1));
    doThrow(new IllegalStateException(errorMessage)).when(projectStep1).execute(any(ComputationContext.class));

    try {
      sut.process();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo(errorMessage);
      assertThat(dto.getStatus()).isEqualTo(Status.FAILED);
      assertThat(dto.getFinishedAt()).isNotNull();
    }
  }

  @Test
  public void step_error() throws Exception {
    when(steps.instances()).thenReturn(Arrays.asList(projectStep1));
    doThrow(new IllegalStateException("pb")).when(projectStep1).execute(any(ComputationContext.class));

    try {
      sut.process();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("pb");
      assertThat(dto.getStatus()).isEqualTo(Status.FAILED);
      assertThat(dto.getFinishedAt()).isNotNull();
    }
  }

  private ComputationStep mockStep() {
    ComputationStep step = mock(ComputationStep.class);
    when(step.getDescription()).thenReturn(RandomStringUtils.randomAscii(5));
    return step;
  }

}
