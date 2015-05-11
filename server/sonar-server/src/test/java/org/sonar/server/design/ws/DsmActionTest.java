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

package org.sonar.server.design.ws;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.measure.db.MetricDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.SnapshotTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.component.db.SnapshotDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.design.db.DsmDataEncoder;
import org.sonar.server.design.db.DsmDb;
import org.sonar.server.measure.ServerMetrics;
import org.sonar.server.measure.persistence.MeasureDao;
import org.sonar.server.measure.persistence.MetricDao;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

public class DsmActionTest {

  private static final String PROJECT_UUID = "PROJECT";

  @ClassRule
  public static DbTester dbTester = new DbTester();

  DbClient dbClient;

  DbSession session;

  WebService.Controller controller;

  WsTester tester;

  MetricDto dsmMetric;

  @Before
  public void setUp() throws Exception {
    dbTester.truncateTables();
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new ComponentDao(), new SnapshotDao(System2.INSTANCE), new MeasureDao(), new MetricDao());
    session = dbClient.openSession(false);
    tester = new WsTester(new DependenciesWs(new DsmAction(dbClient)));
    controller = tester.controller("api/dependencies");

    dsmMetric = new MetricDto().setKey(ServerMetrics.DEPENDENCY_MATRIX_KEY).setEnabled(true);
    dbClient.metricDao().insert(session, dsmMetric);
    session.commit();
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void return_nothing() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto(PROJECT_UUID);
    dbClient.componentDao().insert(session, project);
    session.commit();
    MockUserSession.set().addProjectUuidPermissions(UserRole.USER, PROJECT_UUID);

    tester.newGetRequest("api/dependencies", "dsm")
      .setParam("componentUuid", PROJECT_UUID)
      .execute()
      .assertJson(getClass(), "return_nothing.json");
  }

  @Test
  public void return_dsm() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto(PROJECT_UUID);
    ComponentDto directory1 = ComponentTesting.newDirectory(project, "/src/main/java/dir1").setUuid("DIR1").setLongName("src/main/java/dir1");
    ComponentDto directory2 = ComponentTesting.newDirectory(project, "/src/main/java/dir2").setUuid("DIR2").setLongName("src/main/java/dir2");
    dbClient.componentDao().insert(session, project, directory1, directory2);

    SnapshotDto projectSnapshot = SnapshotTesting.createForProject(project);
    dbClient.snapshotDao().insert(session, projectSnapshot);
    session.commit();

    DsmDb.Data dsmData = DsmDb.Data.newBuilder()
      .addUuid(directory1.uuid())
      .addUuid(directory2.uuid())
      .addCell(DsmDb.Data.Cell.newBuilder()
        .setOffset(0)
        .setWeight(10)
        .build())
      .build();

    dbClient.measureDao().insert(session, new MeasureDto()
      .setMetricId(dsmMetric.getId())
      .setSnapshotId(projectSnapshot.getId())
      .setComponentId(project.getId())
      .setByteData(DsmDataEncoder.encodeSourceData(dsmData))
    );
    session.commit();

    MockUserSession.set().addProjectUuidPermissions(UserRole.USER, PROJECT_UUID);

    tester.newGetRequest("api/dependencies", "dsm")
      .setParam("componentUuid", PROJECT_UUID)
      .execute()
      .assertJson(getClass(), "return_dsm.json");
  }

}
