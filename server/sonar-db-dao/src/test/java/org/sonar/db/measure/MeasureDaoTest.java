/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotTesting;
import org.sonar.db.metric.MetricDto;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;

public class MeasureDaoTest {

  private static final int COVERAGE_METRIC_ID = 10;
  private static final int COMPLEXITY_METRIC_ID = 11;
  private static final int NCLOC_METRIC_ID = 12;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private MeasureDao underTest = db.getDbClient().measureDao();

  @Test
  public void test_selectLastMeasure() {
    MetricDto metric = db.measures().insertMetric();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    SnapshotDto lastAnalysis = insertAnalysis(project.uuid(), true);
    SnapshotDto pastAnalysis = insertAnalysis(project.uuid(), false);

    MeasureDto pastMeasure = MeasureTesting.newMeasureDto(metric, file, pastAnalysis);
    MeasureDto lastMeasure = MeasureTesting.newMeasureDto(metric, file, lastAnalysis);
    underTest.insert(db.getSession(), pastMeasure);
    underTest.insert(db.getSession(), lastMeasure);

    MeasureDto selected = underTest.selectLastMeasure(db.getSession(), file.uuid(), metric.getKey()).get();
    assertThat(selected).isEqualToComparingFieldByField(lastMeasure);

    assertThat(underTest.selectLastMeasure(dbSession, "_missing_", metric.getKey())).isEmpty();
    assertThat(underTest.selectLastMeasure(dbSession, file.uuid(), "_missing_")).isEmpty();
    assertThat(underTest.selectLastMeasure(dbSession, "_missing_", "_missing_")).isEmpty();
  }

  @Test
  public void test_selectMeasure() {
    MetricDto metric = db.measures().insertMetric();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    SnapshotDto lastAnalysis = insertAnalysis(project.uuid(), true);
    SnapshotDto pastAnalysis = insertAnalysis(project.uuid(), false);

    MeasureDto pastMeasure = MeasureTesting.newMeasureDto(metric, file, pastAnalysis);
    MeasureDto lastMeasure = MeasureTesting.newMeasureDto(metric, file, lastAnalysis);
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
  public void selectByQuery() {
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project1));
    db.components().insertComponent(newFileDto(module).setUuid("C1"));
    db.components().insertComponent(newFileDto(module).setUuid("C2"));
    SnapshotDto lastAnalysis = insertAnalysis(project1.uuid(), true);
    SnapshotDto pastAnalysis = insertAnalysis(project1.uuid(), false);

    ComponentDto project2 = db.components().insertPrivateProject();
    SnapshotDto project2LastAnalysis = insertAnalysis(project2.uuid(), true);

    // project 1
    insertMeasure("P1_M1", lastAnalysis.getUuid(), project1.uuid(), NCLOC_METRIC_ID);
    insertMeasure("P1_M2", lastAnalysis.getUuid(), project1.uuid(), COVERAGE_METRIC_ID);
    insertMeasure("P1_M3", pastAnalysis.getUuid(), project1.uuid(), NCLOC_METRIC_ID);
    // project 2
    insertMeasure("P2_M1", project2LastAnalysis.getUuid(), project2.uuid(), NCLOC_METRIC_ID);
    insertMeasure("P2_M2", project2LastAnalysis.getUuid(), project2.uuid(), COVERAGE_METRIC_ID);
    // component C1
    insertMeasure("M1", pastAnalysis.getUuid(), "C1", NCLOC_METRIC_ID);
    insertMeasure("M2", lastAnalysis.getUuid(), "C1", NCLOC_METRIC_ID);
    insertMeasure("M3", lastAnalysis.getUuid(), "C1", COVERAGE_METRIC_ID);
    // component C2
    insertMeasure("M6", lastAnalysis.getUuid(), "C2", NCLOC_METRIC_ID);
    db.commit();

    verifyZeroMeasures(MeasureQuery.builder().setComponentUuids(project1.uuid(), emptyList()));
    verifyZeroMeasures(MeasureQuery.builder().setComponentUuid("MISSING_COMPONENT"));
    verifyZeroMeasures(MeasureQuery.builder().setProjectUuids(emptyList()));
    verifyZeroMeasures(MeasureQuery.builder().setProjectUuids(singletonList("MISSING_COMPONENT")));

    // all measures of component C1 of last analysis
    verifyMeasures(MeasureQuery.builder().setComponentUuid("C1"), "M2", "M3");
    // all measures of component C1 of non last analysis
    verifyMeasures(MeasureQuery.builder().setComponentUuid("C1").setAnalysisUuid(pastAnalysis.getUuid()), "M1");
    // all measures of component C1 of last analysis by UUID
    verifyMeasures(MeasureQuery.builder().setComponentUuid("C1").setAnalysisUuid(lastAnalysis.getUuid()), "M2", "M3");

    // ncloc measure of component C1 of last analysis
    verifyMeasures(MeasureQuery.builder().setComponentUuid("C1").setMetricId(NCLOC_METRIC_ID), "M2");
    // ncloc measure of component C1 of non last analysis
    verifyMeasures(MeasureQuery.builder().setComponentUuid("C1").setAnalysisUuid(pastAnalysis.getUuid()).setMetricId(NCLOC_METRIC_ID), "M1");
    // ncloc measure of component C1 of last analysis by UUID
    verifyMeasures(MeasureQuery.builder().setComponentUuid("C1").setAnalysisUuid(lastAnalysis.getUuid()).setMetricId(NCLOC_METRIC_ID), "M2");

    // multiple measures of component C1 of last analysis
    verifyMeasures(MeasureQuery.builder().setComponentUuid("C1").setMetricIds(asList(NCLOC_METRIC_ID, COVERAGE_METRIC_ID)), "M2", "M3");
    // multiple measures of component C1 of non last analysis
    verifyMeasures(MeasureQuery.builder().setComponentUuid("C1").setAnalysisUuid(pastAnalysis.getUuid()).setMetricIds(asList(NCLOC_METRIC_ID, COVERAGE_METRIC_ID)), "M1");
    // multiple measures of component C1 of last analysis by UUID
    verifyMeasures(MeasureQuery.builder().setComponentUuid("C1").setAnalysisUuid(lastAnalysis.getUuid()).setMetricIds(asList(NCLOC_METRIC_ID, COVERAGE_METRIC_ID)), "M2", "M3");

    // missing measure of component C1 of last analysis
    verifyZeroMeasures(MeasureQuery.builder().setComponentUuid("C1").setMetricId(COMPLEXITY_METRIC_ID));
    // missing measure of component C1 of non last analysis
    verifyZeroMeasures(MeasureQuery.builder().setComponentUuid("C1").setAnalysisUuid(pastAnalysis.getUuid()).setMetricId(COMPLEXITY_METRIC_ID));
    // missing measure of component C1 of last analysis by UUID
    verifyZeroMeasures(MeasureQuery.builder().setComponentUuid("C1").setAnalysisUuid(lastAnalysis.getUuid()).setMetricId(COMPLEXITY_METRIC_ID));

    // ncloc measures of components C1, C2 and C3 (which does not exist) of last analysis
    verifyMeasures(MeasureQuery.builder().setComponentUuids(project1.uuid(), asList("C1", "C2", "C3")), "M2", "M3", "M6");
    // ncloc measures of components C1, C2 and C3 (which does not exist) of non last analysis
    verifyMeasures(MeasureQuery.builder().setComponentUuids(project1.uuid(), asList("C1", "C2", "C3")).setAnalysisUuid(pastAnalysis.getUuid()), "M1");
    // ncloc measures of components C1, C2 and C3 (which does not exist) of last analysis by UUID
    verifyMeasures(MeasureQuery.builder().setComponentUuids(project1.uuid(), asList("C1", "C2", "C3")).setAnalysisUuid(lastAnalysis.getUuid()), "M2", "M3", "M6");

    // projects measures of last analysis
    verifyMeasures(MeasureQuery.builder().setProjectUuids(singletonList(project1.uuid())).setMetricId(NCLOC_METRIC_ID), "P1_M1");
    verifyMeasures(MeasureQuery.builder().setProjectUuids(asList(project1.uuid(), project2.uuid())).setMetricIds(asList(NCLOC_METRIC_ID, COVERAGE_METRIC_ID)),
      "P1_M1", "P1_M2", "P2_M1", "P2_M2", "P2_M2");
    verifyMeasures(MeasureQuery.builder().setProjectUuids(asList(project1.uuid(), project2.uuid(), "UNKNOWN")).setMetricId(NCLOC_METRIC_ID), "P1_M1", "P2_M1");

    // projects measures of none last analysis
    verifyMeasures(MeasureQuery.builder().setProjectUuids(singletonList(project1.uuid())).setMetricId(NCLOC_METRIC_ID).setAnalysisUuid(pastAnalysis.getUuid()), "P1_M3");
    verifyMeasures(MeasureQuery.builder().setProjectUuids(asList(project1.uuid(), project2.uuid())).setMetricId(NCLOC_METRIC_ID).setAnalysisUuid(pastAnalysis.getUuid()), "P1_M3");
  }

  @Test
  public void select_past_measures_with_several_analyses() {
    ComponentDto project = db.components().insertPrivateProject();
    long lastAnalysisDate = parseDate("2017-01-25").getTime();
    long previousAnalysisDate = lastAnalysisDate - 10_000_000_000L;
    long oldAnalysisDate = lastAnalysisDate - 100_000_000_000L;
    SnapshotDto lastAnalysis = dbClient.snapshotDao().insert(dbSession, newAnalysis(project).setCreatedAt(lastAnalysisDate));
    SnapshotDto pastAnalysis = dbClient.snapshotDao().insert(dbSession, newAnalysis(project).setCreatedAt(previousAnalysisDate).setLast(false));
    dbClient.snapshotDao().insert(dbSession, newAnalysis(project).setCreatedAt(oldAnalysisDate).setLast(false));
    db.commit();

    // project
    insertMeasure("PROJECT_M1", lastAnalysis.getUuid(), project.uuid(), NCLOC_METRIC_ID);
    insertMeasure("PROJECT_M2", pastAnalysis.getUuid(), project.uuid(), NCLOC_METRIC_ID);
    insertMeasure("PROJECT_M3", "OLD_ANALYSIS_UUID", project.uuid(), NCLOC_METRIC_ID);
    db.commit();

    // Measures of project for last and previous analyses
    List<MeasureDto> result = underTest.selectPastMeasures(db.getSession(),
      new PastMeasureQuery(project.uuid(), singletonList(NCLOC_METRIC_ID), previousAnalysisDate, lastAnalysisDate + 1_000L));

    assertThat(result).hasSize(2).extracting(MeasureDto::getData).containsOnly("PROJECT_M1", "PROJECT_M2");
  }

  private void verifyMeasures(MeasureQuery.Builder query, String... expectedIds) {
    List<MeasureDto> measures = underTest.selectByQuery(db.getSession(), query.build());
    assertThat(measures).extracting(MeasureDto::getData).containsOnly(expectedIds);
  }

  private void verifyZeroMeasures(MeasureQuery.Builder query) {
    assertThat(underTest.selectByQuery(db.getSession(), query.build())).isEmpty();
  }

  private void insertMeasure(String id, String analysisUuid, String componentUuid, int metricId) {
    MeasureDto measure = MeasureTesting.newMeasure()
      .setAnalysisUuid(analysisUuid)
      .setComponentUuid(componentUuid)
      .setMetricId(metricId)
      // as ids can't be forced when inserting measures, the field "data"
      // is used to store a virtual id. It is used then in assertions.
      .setData(id);
    db.getDbClient().measureDao().insert(db.getSession(), measure);
  }

  private SnapshotDto insertAnalysis(String projectUuid, boolean isLast) {
    return db.getDbClient().snapshotDao().insert(db.getSession(), SnapshotTesting.newSnapshot()
      .setUuid(Uuids.createFast())
      .setComponentUuid(projectUuid)
      .setLast(isLast));
  }

}
