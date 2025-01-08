/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.measure;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotTesting;
import org.sonar.db.metric.MetricDto;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;

class ProjectMeasureDaoIT {

  private MetricDto coverage;
  private MetricDto complexity;
  private MetricDto ncloc;

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);
  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final ProjectMeasureDao underTest = db.getDbClient().projectMeasureDao();

  @BeforeEach
  void before() {
    coverage = db.measures().insertMetric(m -> m.setKey("coverage"));
    complexity = db.measures().insertMetric(m -> m.setKey("complexity"));
    ncloc = db.measures().insertMetric(m -> m.setKey("ncloc"));
    db.measures().insertMetric(m -> m.setKey("ncloc_language_distribution"));
  }

  @Test
  void test_selectLastMeasure() {
    MetricDto metric = db.measures().insertMetric();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    SnapshotDto lastAnalysis = insertAnalysis(project.uuid(), true);
    SnapshotDto pastAnalysis = insertAnalysis(project.uuid(), false);

    ProjectMeasureDto pastMeasure = MeasureTesting.newProjectMeasureDto(metric, file, pastAnalysis);
    ProjectMeasureDto lastMeasure = MeasureTesting.newProjectMeasureDto(metric, file, lastAnalysis);
    underTest.insert(db.getSession(), pastMeasure);
    underTest.insert(db.getSession(), lastMeasure);

    ProjectMeasureDto selected = underTest.selectLastMeasure(db.getSession(), file.uuid(), metric.getKey()).get();
    assertThat(selected).isEqualToComparingFieldByField(lastMeasure);

    assertThat(underTest.selectLastMeasure(dbSession, "_missing_", metric.getKey())).isEmpty();
    assertThat(underTest.selectLastMeasure(dbSession, file.uuid(), "_missing_")).isEmpty();
    assertThat(underTest.selectLastMeasure(dbSession, "_missing_", "_missing_")).isEmpty();
  }

  @Test
  void test_selectMeasure() {
    MetricDto metric = db.measures().insertMetric();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    SnapshotDto lastAnalysis = insertAnalysis(project.uuid(), true);
    SnapshotDto pastAnalysis = insertAnalysis(project.uuid(), false);

    ProjectMeasureDto pastMeasure = MeasureTesting.newProjectMeasureDto(metric, file, pastAnalysis);
    ProjectMeasureDto lastMeasure = MeasureTesting.newProjectMeasureDto(metric, file, lastAnalysis);
    underTest.insert(db.getSession(), pastMeasure);
    underTest.insert(db.getSession(), lastMeasure);

    assertThat(underTest.selectMeasure(db.getSession(), lastAnalysis.getUuid(), file.uuid(), metric.getKey()).get())
      .isEqualToComparingFieldByField(lastMeasure);

    assertThat(underTest.selectMeasure(db.getSession(), pastAnalysis.getUuid(), file.uuid(), metric.getKey()).get())
      .isEqualToComparingFieldByField(pastMeasure);

    assertThat(underTest.selectMeasure(db.getSession(), "_missing_", file.uuid(), metric.getKey())).isEmpty();
    assertThat(underTest.selectMeasure(db.getSession(), pastAnalysis.getUuid(), "_missing_", metric.getKey())).isEmpty();
    assertThat(underTest.selectMeasure(db.getSession(), pastAnalysis.getUuid(), file.uuid(), "_missing_")).isEmpty();
  }

  @Test
  void test_selects() {
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto dir = db.components().insertComponent(newDirectory(project1, "path"));
    db.components().insertComponent(newFileDto(dir).setUuid("C1"));
    db.components().insertComponent(newFileDto(dir).setUuid("C2"));
    SnapshotDto lastAnalysis = insertAnalysis(project1.uuid(), true);
    SnapshotDto pastAnalysis = insertAnalysis(project1.uuid(), false);

    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();
    SnapshotDto project2LastAnalysis = insertAnalysis(project2.uuid(), true);

    // project 1
    insertMeasure("P1_M1", lastAnalysis.getUuid(), project1.uuid(), ncloc.getUuid());
    insertMeasure("P1_M2", lastAnalysis.getUuid(), project1.uuid(), coverage.getUuid());
    insertMeasure("P1_M3", pastAnalysis.getUuid(), project1.uuid(), ncloc.getUuid());
    // project 2
    insertMeasure("P2_M1", project2LastAnalysis.getUuid(), project2.uuid(), ncloc.getUuid());
    insertMeasure("P2_M2", project2LastAnalysis.getUuid(), project2.uuid(), coverage.getUuid());
    // component C1
    insertMeasure("M1", pastAnalysis.getUuid(), "C1", ncloc.getUuid());
    insertMeasure("M2", lastAnalysis.getUuid(), "C1", ncloc.getUuid());
    insertMeasure("M3", lastAnalysis.getUuid(), "C1", coverage.getUuid());
    // component C2
    insertMeasure("M6", lastAnalysis.getUuid(), "C2", ncloc.getUuid());
    db.commit();

    verifyNoMeasure("C1", ncloc.getKey(), "invalid_analysis");
    verifyNoMeasure("C1", "INVALID_KEY");
    verifyNoMeasure("C1", "INVALID_KEY", pastAnalysis.getUuid());
    verifyNoMeasure("MISSING_COMPONENT", ncloc.getKey());
    verifyNoMeasure("MISSING_COMPONENT", ncloc.getKey(), pastAnalysis.getUuid());

    // ncloc measure of component C1 of last analysis
    verifyMeasure("C1", ncloc.getKey(), "M2");
    // ncloc measure of component C1 of non last analysis
    verifyMeasure("C1", ncloc.getKey(), pastAnalysis.getUuid(), "M1");
    // ncloc measure of component C1 of last analysis by UUID
    verifyMeasure("C1", ncloc.getKey(), lastAnalysis.getUuid(), "M2");

    // missing measure of component C1 of last analysis
    verifyNoMeasure("C1", complexity.getKey());
    // missing measure of component C1 of non last analysis
    verifyNoMeasure("C1", complexity.getKey(), pastAnalysis.getUuid());
    // missing measure of component C1 of last analysis by UUID
    verifyNoMeasure("C1", complexity.getKey(), lastAnalysis.getUuid());

    // projects measures of last analysis
    verifyMeasure(project1.uuid(), ncloc.getKey(), "P1_M1");

    // projects measures of none last analysis
    verifyMeasure(project1.uuid(), ncloc.getKey(), pastAnalysis.getUuid(), "P1_M3");
  }

  @Test
  void select_past_measures_with_several_analyses() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    long lastAnalysisDate = parseDate("2017-01-25").getTime();
    long previousAnalysisDate = lastAnalysisDate - 10_000_000_000L;
    long oldAnalysisDate = lastAnalysisDate - 100_000_000_000L;
    SnapshotDto lastAnalysis = dbClient.snapshotDao().insert(dbSession, newAnalysis(project).setCreatedAt(lastAnalysisDate));
    SnapshotDto pastAnalysis = dbClient.snapshotDao().insert(dbSession,
      newAnalysis(project).setCreatedAt(previousAnalysisDate).setLast(false));
    dbClient.snapshotDao().insert(dbSession, newAnalysis(project).setCreatedAt(oldAnalysisDate).setLast(false));
    db.commit();

    // project
    insertMeasure("PROJECT_M1", lastAnalysis.getUuid(), project.uuid(), ncloc.getUuid());
    insertMeasure("PROJECT_M2", pastAnalysis.getUuid(), project.uuid(), ncloc.getUuid());
    insertMeasure("PROJECT_M3", "OLD_ANALYSIS_UUID", project.uuid(), ncloc.getUuid());
    db.commit();

    // Measures of project for last and previous analyses
    List<ProjectMeasureDto> result = underTest.selectPastMeasures(db.getSession(),
      new PastMeasureQuery(project.uuid(), singletonList(ncloc.getUuid()), previousAnalysisDate, lastAnalysisDate + 1_000L));

    assertThat(result).hasSize(2).extracting(ProjectMeasureDto::getData).containsOnly("PROJECT_M1", "PROJECT_M2");
  }

  private void verifyMeasure(String componentUuid, String metricKey, String analysisUuid, String value) {
    Optional<ProjectMeasureDto> measure = underTest.selectMeasure(db.getSession(), analysisUuid, componentUuid, metricKey);
    assertThat(measure.map(ProjectMeasureDto::getData)).contains(value);
    assertThat(measure.map(ProjectMeasureDto::getUuid)).isNotEmpty();
  }

  private void verifyMeasure(String componentUuid, String metricKey, String value) {
    Optional<ProjectMeasureDto> measure = underTest.selectLastMeasure(db.getSession(), componentUuid, metricKey);
    assertThat(measure.map(ProjectMeasureDto::getData)).contains(value);
    assertThat(measure.map(ProjectMeasureDto::getUuid)).isNotEmpty();
  }

  private void verifyNoMeasure(String componentUuid, String metricKey, String analysisUuid) {
    assertThat(underTest.selectMeasure(db.getSession(), analysisUuid, componentUuid, metricKey)).isEmpty();
  }

  private void verifyNoMeasure(String componentUuid, String metricKey) {
    assertThat(underTest.selectLastMeasure(db.getSession(), componentUuid, metricKey)).isEmpty();
  }

  private void insertMeasure(String value, String analysisUuid, String componentUuid, String metricUuid) {
    ProjectMeasureDto measure = MeasureTesting.newProjectMeasure()
      .setAnalysisUuid(analysisUuid)
      .setComponentUuid(componentUuid)
      .setMetricUuid(metricUuid)
      // as ids can't be forced when inserting measures, the field "data"
      // is used to store a virtual value. It is used then in assertions.
      .setData(value);
    db.getDbClient().projectMeasureDao().insert(db.getSession(), measure);
  }

  private SnapshotDto insertAnalysis(String projectUuid, boolean isLast) {
    return db.getDbClient().snapshotDao().insert(db.getSession(), SnapshotTesting.newSnapshot()
      .setUuid(Uuids.createFast())
      .setRootComponentUuid(projectUuid)
      .setLast(isLast));
  }

}
