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

package org.sonar.server.qualitygate.ws;

import com.google.common.base.Throwables;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.DbTests;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsQualityGates.ProjectStatusWsResponse;
import org.sonarqube.ws.WsQualityGates.ProjectStatusWsResponse.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.SnapshotTesting.newSnapshotForProject;
import static org.sonar.db.measure.MeasureTesting.newMeasureDto;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.test.JsonAssert.assertJson;

@Category(DbTests.class)
public class ProjectStatusActionTest {
  private static final String ANALYSIS_ID = "task-uuid";
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  WsActionTester ws;
  DbClient dbClient;
  DbSession dbSession;

  @Before
  public void setUp() {
    dbClient = db.getDbClient();
    dbSession = db.getSession();

    ws = new WsActionTester(new ProjectStatusAction(dbClient, userSession));
    userSession.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }

  @Test
  public void json_example() throws IOException {
    ComponentDto project = newProjectDto("project-uuid");
    dbClient.componentDao().insert(dbSession, project);
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newSnapshotForProject(project)
      .setPeriodMode(1, "last_period")
      .setPeriodDate(1, 956789123456L)
      .setPeriodMode(2, "last_version")
      .setPeriodParam(2, "2015-12-07")
      .setPeriodDate(2, 956789123987L)
      .setPeriodMode(3, "last_analysis")
      .setPeriodMode(5, "last_30_days")
      .setPeriodParam(5, "2015-11-07"));
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDto()
      .setEnabled(true)
      .setKey(CoreMetrics.QUALITY_GATE_DETAILS_KEY));
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(metric, snapshot.getId())
        .setData(IOUtils.toString(getClass().getResource("ProjectStatusActionTest/measure_data.json"))));
    dbSession.commit();

    String response = ws.newRequest()
      .setParam("analysisId", snapshot.getId().toString())
      .execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("project_status-example.json"));
  }

  @Test
  public void fail_if_no_snapshot_id_found() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Analysis with id 'task-uuid' is not found");

    newRequest(ANALYSIS_ID);
  }

  @Test
  public void return_undefined_status_if_measure_is_not_found() {
    ComponentDto project = newProjectDto("project-uuid");
    dbClient.componentDao().insert(dbSession, project);
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newSnapshotForProject(project));
    dbSession.commit();

    ProjectStatusWsResponse result = newRequest(snapshot.getId().toString());

    assertThat(result.getProjectStatus().getStatus()).isEqualTo(Status.NONE);
    assertThat(result.getProjectStatus().getConditionsCount()).isEqualTo(0);
  }

  @Test
  public void return_undefined_status_if_measure_data_is_not_well_formatted() {
    userSession.setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    ComponentDto project = newProjectDto("project-uuid");
    dbClient.componentDao().insert(dbSession, project);
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newSnapshotForProject(project));
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDto()
      .setEnabled(true)
      .setKey(CoreMetrics.QUALITY_GATE_DETAILS_KEY));
    MeasureDto measure = newMeasureDto(metric, snapshot.getId()).setData("");
    dbClient.measureDao().insert(dbSession, measure);
    dbSession.commit();

    ProjectStatusWsResponse result = newRequest(String.valueOf(snapshot.getId()));

    assertThat(result.getProjectStatus().getStatus()).isEqualTo(Status.NONE);
    assertThat(result.getProjectStatus().getConditionsCount()).isEqualTo(0);
  }

  @Test
  public void fail_if_insufficient_privileges() {
    userSession.setGlobalPermissions(GlobalPermissions.PREVIEW_EXECUTION);
    expectedException.expect(ForbiddenException.class);

    newRequest(ANALYSIS_ID);
  }

  private ProjectStatusWsResponse newRequest(String taskId) {
    try {
      return ProjectStatusWsResponse.parseFrom(
        ws.newRequest()
          .setParam("analysisId", taskId)
          .setMediaType(MediaTypes.PROTOBUF)
          .execute().getInputStream());
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
