/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.qualitygate;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.measure.index.ProjectMeasuresIndex;
import org.sonar.server.measure.index.ProjectMeasuresIndexer;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.util.GlobalLockManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.Metric.Level.WARN;
import static org.sonar.db.measure.MeasureTesting.newLiveMeasure;
import static org.sonar.server.qualitygate.ProjectsInWarningDaemon.PROJECTS_IN_WARNING_INTERNAL_PROPERTY;

public class ProjectsInWarningDaemonTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public LogTester logger = new LogTester().setLevel(LoggerLevel.DEBUG);

  private PermissionIndexerTester authorizationIndexerTester = new PermissionIndexerTester(es, new ProjectMeasuresIndexer(db.getDbClient(), es.client()));
  private ProjectMeasuresIndexer projectMeasuresIndexer = new ProjectMeasuresIndexer(db.getDbClient(), es.client());
  private ProjectMeasuresIndex projectMeasuresIndex = new ProjectMeasuresIndex(es.client(), new WebAuthorizationTypeSupport(null), System2.INSTANCE);

  private MapSettings settings = new MapSettings();
  private GlobalLockManager lockManager = mock(GlobalLockManager.class);
  private ProjectsInWarning projectsInWarning = new ProjectsInWarning();

  private ProjectsInWarningDaemon underTest = new ProjectsInWarningDaemon(db.getDbClient(), projectMeasuresIndex, settings.asConfig(), lockManager, projectsInWarning);

  @Before
  public void setUp() throws Exception {
    settings.setProperty("sonar.projectsInWarning.frequencyInMilliseconds", "100");
  }

  @After
  public void tearDown() {
    underTest.stop();
  }

  @Test
  public void store_projects_in_warning() throws InterruptedException {
    allowLockToBeAcquired();
    MetricDto qualityGateStatus = insertQualityGateStatusMetric();
    insertProjectInWarning(qualityGateStatus);
    insertProjectInWarning(qualityGateStatus);
    // Setting does not exist
    assertThat(db.getDbClient().internalPropertiesDao().selectByKey(db.getSession(), PROJECTS_IN_WARNING_INTERNAL_PROPERTY)).isEmpty();

    underTest.notifyStart();

    assertProjectsInWarningValue(2L);
    assertThat(logger.logs(LoggerLevel.INFO)).contains("Counting number of projects in warning is enabled.");
  }

  @Test
  public void update_projects_in_warning_when_new_project_in_warning() throws InterruptedException {
    allowLockToBeAcquired();
    MetricDto qualityGateStatus = insertQualityGateStatusMetric();
    ;
    insertProjectInWarning(qualityGateStatus);
    insertProjectInWarning(qualityGateStatus);
    // Setting does not exist
    assertThat(db.getDbClient().internalPropertiesDao().selectByKey(db.getSession(), PROJECTS_IN_WARNING_INTERNAL_PROPERTY)).isEmpty();

    underTest.notifyStart();
    // Add a project in warning after the start in order to let the thread do his job
    insertProjectInWarning(qualityGateStatus);

    assertProjectsInWarningValue(3L);
    assertThat(logger.logs(LoggerLevel.INFO)).contains("Counting number of projects in warning is enabled.");
  }

  @Test
  public void stop_thread_when_number_of_projects_in_warning_reach_zero() throws InterruptedException {
    allowLockToBeAcquired();
    MetricDto qualityGateStatus = insertQualityGateStatusMetric();
    ;
    ComponentDto project = insertProjectInWarning(qualityGateStatus);

    underTest.notifyStart();
    assertProjectsInWarningValue(1L);
    // Set quality gate status of the project to OK => No more projects in warning
    db.getDbClient().liveMeasureDao().insertOrUpdate(db.getSession(),
      newLiveMeasure(project, qualityGateStatus).setData(Metric.Level.OK.name()).setValue(null));
    db.commit();
    projectMeasuresIndexer.indexOnAnalysis(project.uuid());

    assertProjectsInWarningValue(0L);
    assertThat(logger.logs(LoggerLevel.INFO))
      .contains(
        "Counting number of projects in warning is enabled.",
        "Counting number of projects in warning will be disabled as there are no more projects in warning.");
  }

  @Test
  public void update_internal_properties_when_already_exits_and_projects_in_warnings_more_than_zero() throws InterruptedException {
    allowLockToBeAcquired();
    MetricDto qualityGateStatus = insertQualityGateStatusMetric();
    insertProjectInWarning(qualityGateStatus);
    insertProjectInWarning(qualityGateStatus);
    // Setting contains 10, it should be updated with new value
    db.getDbClient().internalPropertiesDao().save(db.getSession(), PROJECTS_IN_WARNING_INTERNAL_PROPERTY, "10");
    db.commit();

    underTest.notifyStart();

    assertProjectsInWarningValue(2L);
    assertThat(logger.logs(LoggerLevel.INFO)).contains("Counting number of projects in warning is enabled.");
  }

  @Test
  public void store_zero_projects_in_warning_when_no_projects() throws InterruptedException {
    allowLockToBeAcquired();
    assertThat(db.getDbClient().internalPropertiesDao().selectByKey(db.getSession(), PROJECTS_IN_WARNING_INTERNAL_PROPERTY)).isEmpty();

    underTest.notifyStart();

    assertProjectsInWarningValue(0L);
    assertThat(logger.logs(LoggerLevel.INFO)).contains("Counting number of projects in warning is enabled.");
  }

  @Test
  public void do_not_compute_projects_in_warning_when_internal_property_is_zero() throws InterruptedException {
    allowLockToBeAcquired();
    MetricDto qualityGateStatus = insertQualityGateStatusMetric();
    ;
    insertProjectInWarning(qualityGateStatus);
    // Setting contains 0, even if there are projects in warning it will stay 0 (as it's not possible to have new projects in warning)
    db.getDbClient().internalPropertiesDao().save(db.getSession(), PROJECTS_IN_WARNING_INTERNAL_PROPERTY, "0");
    db.commit();

    underTest.notifyStart();

    assertProjectsInWarningValue(0L);
    assertThat(logger.logs(LoggerLevel.INFO)).contains("Counting number of projects in warning is not started as there are no projects in this situation.");
  }

  @Test
  public void do_not_store_projects_in_warning_in_db_when_cannot_acquire_lock() throws InterruptedException {
    when(lockManager.tryLock(any(), anyInt())).thenReturn(false);
    MetricDto qualityGateStatus = insertQualityGateStatusMetric();
    ;
    insertProjectInWarning(qualityGateStatus);

    underTest.notifyStart();

    waitForValueToBeComputed(1L);
    assertThat(projectsInWarning.count()).isEqualTo(1L);
    assertThat(countNumberOfProjectsInWarning()).isEqualTo(0L);
  }

  private void waitForValueToBeComputed(long expectedValue) throws InterruptedException {
    for (int i = 0; i < 1000; i++) {
      if (projectsInWarning.isInitialized() && projectsInWarning.count() == expectedValue) {
        break;
      }
      Thread.sleep(100);
    }
  }

  private void assertProjectsInWarningValue(long expectedValue) throws InterruptedException {
    waitForValueToBeComputed(expectedValue);
    assertThat(projectsInWarning.count()).isEqualTo(expectedValue);
    assertThat(countNumberOfProjectsInWarning()).isEqualTo(expectedValue);
  }

  private long countNumberOfProjectsInWarning() {
    return db.getDbClient().internalPropertiesDao().selectByKey(db.getSession(), PROJECTS_IN_WARNING_INTERNAL_PROPERTY)
      .map(Long::valueOf)
      .orElse(0L);
  }

  private ComponentDto insertProjectInWarning(MetricDto qualityGateStatus) {
    ComponentDto project = db.components().insertPrivateProject();
    db.measures().insertLiveMeasure(project, qualityGateStatus, lm -> lm.setData(WARN.name()).setValue(null));
    authorizationIndexerTester.allowOnlyAnyone(project);
    projectMeasuresIndexer.indexOnAnalysis(project.uuid());
    return project;
  }

  private MetricDto insertQualityGateStatusMetric() {
    return db.measures().insertMetric(m -> m.setKey(CoreMetrics.ALERT_STATUS_KEY).setValueType(Metric.ValueType.LEVEL.name()));
  }

  private void allowLockToBeAcquired() {
    when(lockManager.tryLock(any(), anyInt())).thenReturn(true);
  }

}
