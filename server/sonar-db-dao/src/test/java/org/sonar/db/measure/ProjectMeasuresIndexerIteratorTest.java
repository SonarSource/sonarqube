/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.ProjectMeasuresIndexerIterator.ProjectMeasures;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.metric.MetricTesting;
import org.sonar.db.organization.OrganizationDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sonar.api.measures.Metric.Level.ERROR;
import static org.sonar.api.measures.Metric.Level.OK;
import static org.sonar.api.measures.Metric.Level.WARN;
import static org.sonar.api.measures.Metric.ValueType.DATA;
import static org.sonar.api.measures.Metric.ValueType.DISTRIB;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.LEVEL;
import static org.sonar.api.measures.Metric.ValueType.STRING;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;

public class ProjectMeasuresIndexerIteratorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();

  @Test
  public void return_project_measure() {
    MetricDto metric1 = insertIntMetric("ncloc");
    MetricDto metric2 = insertIntMetric("coverage");
    ComponentDto project = ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization()).setKey("Project-Key").setName("Project Name").setTagsString("platform,java");
    SnapshotDto analysis = dbTester.components().insertProjectAndSnapshot(project);
    insertMeasure(project, analysis, metric1, 10d);
    insertMeasure(project, analysis, metric2, 20d);

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById).hasSize(1);
    ProjectMeasures doc = docsById.get(project.uuid());
    assertThat(doc).isNotNull();
    assertThat(doc.getProject().getUuid()).isEqualTo(project.uuid());
    assertThat(doc.getProject().getKey()).isEqualTo("Project-Key");
    assertThat(doc.getProject().getName()).isEqualTo("Project Name");
    assertThat(doc.getProject().getTags()).containsExactly("platform", "java");
    assertThat(doc.getProject().getAnalysisDate()).isNotNull().isEqualTo(analysis.getCreatedAt());
    assertThat(doc.getMeasures().getNumericMeasures()).containsOnly(entry("ncloc", 10d), entry("coverage", 20d));
  }

  @Test
  public void return_project_measure_having_leak() throws Exception {
    MetricDto metric = insertIntMetric("new_lines");
    ComponentDto project = ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization());
    SnapshotDto analysis = dbTester.components().insertProjectAndSnapshot(project);
    insertMeasureOnLeak(project, analysis, metric, 10d);

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById.get(project.uuid()).getMeasures().getNumericMeasures()).containsOnly(entry("new_lines", 10d));
  }

  @Test
  public void return_quality_gate_status_measure() throws Exception {
    MetricDto metric = insertMetric("alert_status", LEVEL);
    insertProjectAndMeasure("project1", metric, WARN.name());
    insertProjectAndMeasure("project2", metric, OK.name());
    insertProjectAndMeasure("project3", metric, ERROR.name());

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById.get("project1").getMeasures().getQualityGateStatus()).isEqualTo("WARN");
    assertThat(docsById.get("project2").getMeasures().getQualityGateStatus()).isEqualTo("OK");
    assertThat(docsById.get("project3").getMeasures().getQualityGateStatus()).isEqualTo("ERROR");
  }

  @Test
  public void does_not_fail_when_quality_gate_has_no_value() throws Exception {
    MetricDto metric = insertMetric("alert_status", LEVEL);
    insertProjectAndMeasure("project", metric, null);

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById.get("project").getMeasures().getNumericMeasures()).isEmpty();
  }

  @Test
  public void return_language_distribution_measure() throws Exception {
    MetricDto metric = insertMetric("ncloc_language_distribution", DATA);
    ComponentDto project = ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization());
    SnapshotDto analysis = dbTester.components().insertProjectAndSnapshot(project);
    insertMeasure(project, analysis, metric, "<null>=2;java=6;xoo=18");

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById.get(project.uuid()).getMeasures().getLanguages()).containsOnly("<null>", "java", "xoo");
  }

  @Test
  public void does_not_return_none_numeric_metrics() throws Exception {
    MetricDto dataMetric = insertMetric("data", DATA);
    MetricDto distribMetric = insertMetric("distrib", DISTRIB);
    MetricDto stringMetric = insertMetric("string", STRING);
    ComponentDto project = ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization());
    SnapshotDto analysis = dbTester.components().insertProjectAndSnapshot(project);
    insertMeasure(project, analysis, dataMetric, "dat");
    insertMeasure(project, analysis, distribMetric, "dis");
    insertMeasure(project, analysis, stringMetric, "str");

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById.get(project.uuid()).getMeasures().getNumericMeasures()).isEmpty();
  }

  @Test
  public void does_not_return_disabled_metrics() throws Exception {
    MetricDto disabledMetric = insertMetric("disabled", false, false, INT);
    ComponentDto project = ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization());
    SnapshotDto analysis = dbTester.components().insertProjectAndSnapshot(project);
    insertMeasure(project, analysis, disabledMetric, 10d);

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById.get(project.uuid()).getMeasures().getNumericMeasures()).isEmpty();
  }

  @Test
  public void fail_when_measure_return_no_value() throws Exception {
    MetricDto metric = insertIntMetric("new_lines");
    ComponentDto project = ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization());
    SnapshotDto analysis = dbTester.components().insertProjectAndSnapshot(project);
    insertMeasure(project, analysis, metric, 10d);

    expectedException.expect(IllegalStateException.class);
    createResultSetAndReturnDocsById();
  }

  @Test
  public void return_many_project_measures() {
    dbTester.components().insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization()));
    dbTester.components().insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization()));
    dbTester.components().insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization()));

    assertThat(createResultSetAndReturnDocsById()).hasSize(3);
  }

  @Test
  public void return_project_without_analysis() throws Exception {
    ComponentDto project = dbTester.components().insertComponent(ComponentTesting.newPrivateProjectDto(dbTester.organizations().insert()));
    dbClient.snapshotDao().insert(dbSession, newAnalysis(project).setLast(false));
    dbSession.commit();

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById).hasSize(1);
    ProjectMeasures doc = docsById.get(project.uuid());
    assertThat(doc.getProject().getAnalysisDate()).isNull();
  }

  @Test
  public void does_not_return_non_active_projects() throws Exception {
    // Disabled project
    dbTester.components().insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization()).setEnabled(false));
    // Disabled project with analysis
    ComponentDto project = dbTester.components().insertComponent(ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization()).setEnabled(false));
    dbClient.snapshotDao().insert(dbSession, newAnalysis(project));

    // A view
    dbTester.components().insertProjectAndSnapshot(newView(dbTester.getDefaultOrganization()));

    dbSession.commit();

    assertResultSetIsEmpty();
  }

  @Test
  public void return_only_docs_from_given_project() throws Exception {
    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto);
    SnapshotDto analysis = dbTester.components().insertProjectAndSnapshot(project);
    dbTester.components().insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(organizationDto));
    dbTester.components().insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(organizationDto));

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById(project.uuid());

    assertThat(docsById).hasSize(1);
    ProjectMeasures doc = docsById.get(project.uuid());
    assertThat(doc).isNotNull();
    assertThat(doc.getProject().getUuid()).isEqualTo(project.uuid());
    assertThat(doc.getProject().getKey()).isNotNull().isEqualTo(project.getKey());
    assertThat(doc.getProject().getName()).isNotNull().isEqualTo(project.name());
    assertThat(doc.getProject().getAnalysisDate()).isNotNull().isEqualTo(analysis.getCreatedAt());
  }

  @Test
  public void return_nothing_on_unknown_project() throws Exception {
    dbTester.components().insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(dbTester.getDefaultOrganization()));

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById("UNKNOWN");

    assertThat(docsById).isEmpty();
  }

  private Map<String, ProjectMeasures> createResultSetAndReturnDocsById() {
    return createResultSetAndReturnDocsById(null);
  }

  private Map<String, ProjectMeasures> createResultSetAndReturnDocsById(@Nullable String projectUuid) {
    ProjectMeasuresIndexerIterator it = ProjectMeasuresIndexerIterator.create(dbTester.getSession(), projectUuid);
    Map<String, ProjectMeasures> docsById = Maps.uniqueIndex(it, pm -> pm.getProject().getUuid());
    it.close();
    return docsById;
  }

  private void assertResultSetIsEmpty() {
    assertThat(createResultSetAndReturnDocsById()).isEmpty();
  }

  private MetricDto insertIntMetric(String metricKey) {
    return insertMetric(metricKey, true, false, INT);
  }

  private MetricDto insertMetric(String metricKey, Metric.ValueType type) {
    return insertMetric(metricKey, true, false, type);
  }

  private MetricDto insertMetric(String metricKey, boolean enabled, boolean hidden, Metric.ValueType type) {
    MetricDto metric = dbClient.metricDao().insert(dbSession,
      MetricTesting.newMetricDto()
        .setKey(metricKey)
        .setEnabled(enabled)
        .setHidden(hidden)
        .setValueType(type.name()));
    dbSession.commit();
    return metric;
  }

  private MeasureDto insertProjectAndMeasure(String projectUuid, MetricDto metric, String value) {
    ComponentDto project = newPrivateProjectDto(dbTester.getDefaultOrganization(), projectUuid);
    SnapshotDto analysis1 = dbTester.components().insertProjectAndSnapshot(project);
    return insertMeasure(project, analysis1, metric, value);
  }

  private MeasureDto insertMeasure(ComponentDto project, SnapshotDto analysis, MetricDto metric, double value) {
    return insertMeasure(project, analysis, metric, value, null);
  }

  private MeasureDto insertMeasureOnLeak(ComponentDto project, SnapshotDto analysis, MetricDto metric, double value) {
    return insertMeasure(project, analysis, metric, null, value);
  }

  private MeasureDto insertMeasure(ComponentDto project, SnapshotDto analysis, MetricDto metric, String value) {
    return insertMeasure(MeasureTesting.newMeasureDto(metric, project, analysis).setData(value));
  }

  private MeasureDto insertMeasure(ComponentDto project, SnapshotDto analysis, MetricDto metric, @Nullable Double value, @Nullable Double leakValue) {
    return insertMeasure(MeasureTesting.newMeasureDto(metric, project, analysis).setValue(value).setVariation(leakValue));
  }

  private MeasureDto insertMeasure(MeasureDto measure) {
    dbClient.measureDao().insert(dbSession, measure);
    dbSession.commit();
    return measure;
  }

}
