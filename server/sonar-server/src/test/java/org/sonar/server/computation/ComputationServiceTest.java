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

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.core.activity.Activity;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.computation.step.ComputationStep;
import org.sonar.server.computation.step.ComputationSteps;
import org.sonar.server.db.DbClient;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ComputationServiceTest {

  @ClassRule
  public static DbTester dbTester = new DbTester();

  ComputationStep projectStep1 = mockStep(Qualifiers.PROJECT);
  ComputationStep projectStep2 = mockStep(Qualifiers.PROJECT);
  ComputationStep viewStep = mockStep(Qualifiers.VIEW);
  ComputationSteps steps = mock(ComputationSteps.class);
  ActivityService activityService = mock(ActivityService.class);

  @Before
  public void setUp() throws Exception {
    dbTester.truncateTables();
  }

  @Test
  public void process_project() throws Exception {
    // db contains project with key "PROJECT_KEY"
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new ComponentDao());

    when(steps.orderedSteps()).thenReturn(Arrays.asList(projectStep1, projectStep2, viewStep));

    // load report from db and parse it
    ComputationService sut = new ComputationService(dbClient, steps, activityService);
    AnalysisReportDto report = AnalysisReportDto.newForTests(1L);
    report.setProjectKey("PROJECT_KEY");
    assertThat(report.getStatus()).isNull();
    sut.process(report);

    // status of report is set
    assertThat(report.getStatus()).isEqualTo(AnalysisReportDto.Status.SUCCESS);

    // execute only the steps supporting the project qualifier
    verify(projectStep1).execute(any(ComputationContext.class));
    verify(projectStep2).execute(any(ComputationContext.class));
    verify(viewStep, never()).execute(any(ComputationContext.class));
    verify(activityService).write(any(DbSession.class), eq(Activity.Type.ANALYSIS_REPORT), any(AnalysisReportLog.class));
  }

  @Test
  public void fail_to_parse_report() throws Exception {
    // db contains project with key "PROJECT_KEY"
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new ComponentDao());

    when(steps.orderedSteps()).thenReturn(Arrays.asList(projectStep1));
    doThrow(new UnsupportedOperationException()).when(projectStep1).execute(any(ComputationContext.class));

    // load report from db and parse it
    ComputationService sut = new ComputationService(dbClient, steps, activityService);
    AnalysisReportDto report = AnalysisReportDto.newForTests(1L);
    report.setProjectKey("PROJECT_KEY");
    try {
      sut.process(report);
      fail();
    } catch (UnsupportedOperationException e) {
      // status of report is set
      assertThat(report.getStatus()).isEqualTo(AnalysisReportDto.Status.FAILED);
      verify(activityService).write(any(DbSession.class), eq(Activity.Type.ANALYSIS_REPORT), any(AnalysisReportLog.class));
    }
  }

  private ComputationStep mockStep(String... qualifiers) {
    ComputationStep step = mock(ComputationStep.class);
    when(step.supportedProjectQualifiers()).thenReturn(qualifiers);
    return step;
  }
}
