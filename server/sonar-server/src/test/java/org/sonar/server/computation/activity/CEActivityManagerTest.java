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
package org.sonar.server.computation.activity;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.activity.Activity;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@Category(DbTests.class)
public class CEActivityManagerTest {
  @ClassRule
  public static DbTester dbTester = new DbTester();

  ArgumentCaptor<Activity> activityArgumentCaptor = ArgumentCaptor.forClass(Activity.class);

  AnalysisReportDto reportDto = AnalysisReportDto.newForTests(1L).setProjectKey("P1").setUuid("U1").setStatus(AnalysisReportDto.Status.PENDING);

  ActivityService activityService = mock(ActivityService.class);
  CEActivityManager underTest;

  @Before
  public void setup() throws Exception {
    dbTester.truncateTables();
    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new ComponentDao());

    underTest = new CEActivityManager(activityService, dbClient);
  }

  @Test
  public void process_existing_project() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    underTest.saveActivity(reportDto);

    verify(activityService).save(activityArgumentCaptor.capture());

    assertThat(activityArgumentCaptor.getValue().getType()).isEqualTo(Activity.Type.ANALYSIS_REPORT);
    assertThat(activityArgumentCaptor.getValue().getAction()).isEqualTo("LOG_ANALYSIS_REPORT");
    assertThat(activityArgumentCaptor.getValue().getData()).containsEntry("projectKey", "P1");
    assertThat(activityArgumentCaptor.getValue().getData()).containsEntry("projectName", "Project 1");
    assertThat(activityArgumentCaptor.getValue().getData().get("projectUuid")).isEqualTo("ABCD");
  }

  @Test
  public void process_new_project() throws Exception {
    underTest.saveActivity(reportDto);

    // execute only the steps supporting the project qualifier
    verify(activityService).save(activityArgumentCaptor.capture());

    assertThat(activityArgumentCaptor.getValue().getType()).isEqualTo(Activity.Type.ANALYSIS_REPORT);
    assertThat(activityArgumentCaptor.getValue().getAction()).isEqualTo("LOG_ANALYSIS_REPORT");
    assertThat(activityArgumentCaptor.getValue().getData()).containsEntry("projectKey", "P1");
  }

}
