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

package org.sonar.server.computation.step;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.computation.db.AnalysisReportDto.Status;
import org.sonar.core.computation.dbcleaner.DbCleanerConstants;
import org.sonar.core.computation.dbcleaner.ProjectCleaner;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.SnapshotTesting;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.db.DbClient;
import org.sonar.server.properties.ProjectSettingsFactory;
import org.sonar.server.tester.ServerTester;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class PurgeDatastoresStepMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  private PurgeDatastoresStep sut;
  private DbClient dbClient;
  private DbSession dbSession;
  private ProjectCleaner purgeTask;

  @Before
  public void before() throws Exception {
    this.dbClient = tester.get(DbClient.class);
    this.dbSession = dbClient.openSession(false);

    this.purgeTask = tester.get(ProjectCleaner.class);

    this.sut = new PurgeDatastoresStep(dbClient, purgeTask);
  }

  @After
  public void after() throws Exception {
    MyBatis.closeQuietly(dbSession);
  }

  @Test
  public void use_global_settings_when_no_other_specified() throws Exception {
    // ARRANGE
    Date now = new Date();
    Date aWeekAgo = DateUtils.addDays(now, -7);

    ComponentDto project = ComponentTesting.newProjectDto()
      .setId(1L)
      .setKey("123")
      .setCreatedAt(aWeekAgo)
      .setUpdatedAt(aWeekAgo);
    dbClient.componentDao().insert(dbSession, project);

    SnapshotDto snapshot = SnapshotTesting.createForProject(project)
      .setCreatedAt(aWeekAgo.getTime());
    dbClient.snapshotDao().insert(dbSession, snapshot);

    AnalysisReportDto report = AnalysisReportDto.newForTests(1L)
      .setProjectKey("123")
      .setSnapshotId(snapshot.getId())
      .setStatus(Status.PENDING);
    dbClient.analysisReportDao().insert(dbSession, report);

    dbClient.propertiesDao().setProperty(new PropertyDto().setKey(DbCleanerConstants.WEEKS_BEFORE_DELETING_ALL_SNAPSHOTS).setValue("52"));
    dbSession.commit();
    ComputationContext context = new ComputationContext(report, project);
    context.setProjectSettings(new ProjectSettingsFactory(tester.get(Settings.class), tester.get(PropertiesDao.class)).newProjectSettings(dbSession, project.getId()));

    // ACT
    sut.execute(context);

    // ASSERT
    assertThat(dbClient.snapshotDao().getNullableByKey(dbSession, snapshot.getId())).isNotNull();
  }

  @Test
  public void use_project_settings_if_specified() throws Exception {
    // ARRANGE
    Date now = new Date();
    Date twoWeeksAgo = DateUtils.addDays(now, -2 * 7);
    Date aLongTimeAgo = DateUtils.addDays(now, -365 * 7);

    ComponentDto project = ComponentTesting.newProjectDto()
      .setKey("123")
      .setCreatedAt(aLongTimeAgo)
      .setUpdatedAt(aLongTimeAgo);
    dbClient.componentDao().insert(dbSession, project);

    SnapshotDto snapshot = SnapshotTesting.createForProject(project)
      .setCreatedAt(twoWeeksAgo.getTime())
      .setStatus("P")
      .setLast(true);

    dbClient.snapshotDao().insert(dbSession, snapshot);

    AnalysisReportDto report = new AnalysisReportDto()
      .setProjectKey("123")
      .setSnapshotId(snapshot.getId())
      .setStatus(Status.PENDING);
    dbClient.analysisReportDao().insert(dbSession, report);

    dbClient.propertiesDao().setProperty(new PropertyDto().setKey(DbCleanerConstants.WEEKS_BEFORE_DELETING_ALL_SNAPSHOTS).setValue("4"));
    dbClient.propertiesDao().setProperty(new PropertyDto().setKey(DbCleanerConstants.WEEKS_BEFORE_DELETING_ALL_SNAPSHOTS).setValue("1").setResourceId(project.getId()));
    dbSession.commit();
    ComputationContext context = new ComputationContext(report, project);
    context.setProjectSettings(new ProjectSettingsFactory(tester.get(Settings.class), tester.get(PropertiesDao.class)).newProjectSettings(dbSession, project.getId()));

    // ACT
    sut.execute(context);
    dbSession.commit();

    // ASSERT
    assertThat(dbClient.snapshotDao().getNullableByKey(dbSession, snapshot.getId())).isNull();
  }
}
