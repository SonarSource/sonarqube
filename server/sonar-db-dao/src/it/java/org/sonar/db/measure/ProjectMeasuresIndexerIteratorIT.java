/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.ProjectMeasuresIndexerIterator.ProjectMeasures;
import org.sonar.db.metric.MetricDto;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sonar.api.measures.Metric.Level.ERROR;
import static org.sonar.api.measures.Metric.Level.OK;
import static org.sonar.api.measures.Metric.ValueType.DATA;
import static org.sonar.api.measures.Metric.ValueType.DISTRIB;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.LEVEL;
import static org.sonar.api.measures.Metric.ValueType.STRING;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;

class ProjectMeasuresIndexerIteratorIT {

  @RegisterExtension
  private final DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = dbTester.getDbClient();
  private final DbSession dbSession = dbTester.getSession();

  @Test
  void return_project_measure() {
    ProjectData projectData = dbTester.components().insertPrivateProject(
      c -> c.setKey("Project-Key").setName("Project Name"),
      p -> p.setTags(newArrayList("platform", "java")));
    ComponentDto project = projectData.getMainBranchComponent();

    SnapshotDto analysis = dbTester.components().insertSnapshot(project);
    MetricDto metric1 = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setKey("ncloc"));
    MetricDto metric2 = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setKey("coverage"));
    dbTester.measures().insertLiveMeasure(project, metric1, m -> m.setValue(10d));
    dbTester.measures().insertLiveMeasure(project, metric2, m -> m.setValue(20d));

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById).hasSize(1);
    ProjectMeasures doc = docsById.get(projectData.projectUuid());
    assertThat(doc).isNotNull();
    assertThat(doc.getProject().getUuid()).isEqualTo(projectData.projectUuid());
    assertThat(doc.getProject().getKey()).isEqualTo("Project-Key");
    assertThat(doc.getProject().getName()).isEqualTo("Project Name");
    assertThat(doc.getProject().getQualifier()).isEqualTo("TRK");
    assertThat(doc.getProject().getTags()).containsExactly("platform", "java");
    assertThat(doc.getProject().getAnalysisDate()).isNotNull().isEqualTo(analysis.getCreatedAt());
    assertThat(doc.getMeasures().getNumericMeasures()).containsOnly(entry(metric1.getKey(), 10d), entry(metric2.getKey(), 20d));
  }

  @Test
  void return_application_measure() {
    ProjectData projectData = dbTester.components().insertPrivateApplication(c -> c.setKey("App-Key").setName("App Name"));
    ComponentDto project = projectData.getMainBranchComponent();

    SnapshotDto analysis = dbTester.components().insertSnapshot(project);
    MetricDto metric1 = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setKey("ncloc"));
    MetricDto metric2 = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setKey("coverage"));
    dbTester.measures().insertLiveMeasure(project, metric1, m -> m.setValue(10d));
    dbTester.measures().insertLiveMeasure(project, metric2, m -> m.setValue(20d));

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById).hasSize(1);
    ProjectMeasures doc = docsById.get(projectData.projectUuid());
    assertThat(doc).isNotNull();
    assertThat(doc.getProject().getUuid()).isEqualTo(projectData.projectUuid());
    assertThat(doc.getProject().getKey()).isEqualTo("App-Key");
    assertThat(doc.getProject().getName()).isEqualTo("App Name");
    assertThat(doc.getProject().getAnalysisDate()).isNotNull().isEqualTo(analysis.getCreatedAt());
    assertThat(doc.getMeasures().getNumericMeasures()).containsOnly(entry(metric1.getKey(), 10d), entry(metric2.getKey(), 20d));
  }

  @Test
  void return_project_measure_having_leak() {
    ProjectData projectData = dbTester.components().insertPrivateProject(
      c -> c.setKey("Project-Key").setName("Project Name"),
      p -> p.setTagsString("platform,java"));
    ComponentDto project = projectData.getMainBranchComponent();
    MetricDto metric = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setKey("new_lines"));
    dbTester.measures().insertLiveMeasure(project, metric, m -> m.setValue(10d));

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById.get(projectData.projectUuid()).getMeasures().getNumericMeasures()).containsOnly(entry("new_lines", 10d));
  }

  @Test
  void return_quality_gate_status_measure() {
    ComponentDto project1 = dbTester.components().insertPrivateProject().getMainBranchComponent();
    ProjectData projectData2 = dbTester.components().insertPrivateProject();
    ComponentDto project2 = projectData2.getMainBranchComponent();
    ProjectData projectData3 = dbTester.components().insertPrivateProject();
    ComponentDto project3 = projectData3.getMainBranchComponent();
    MetricDto metric = dbTester.measures().insertMetric(m -> m.setValueType(LEVEL.name()).setKey("alert_status"));
    dbTester.measures().insertLiveMeasure(project2, metric, m -> m.setValue(null).setData(OK.name()));
    dbTester.measures().insertLiveMeasure(project3, metric, m -> m.setValue(null).setData(ERROR.name()));

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById.get(projectData2.projectUuid()).getMeasures().getQualityGateStatus()).isEqualTo("OK");
    assertThat(docsById.get(projectData3.projectUuid()).getMeasures().getQualityGateStatus()).isEqualTo("ERROR");
  }

  @Test
  void does_not_fail_when_quality_gate_has_no_value() {
    ProjectData projectData = dbTester.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    MetricDto metric = dbTester.measures().insertMetric(m -> m.setValueType(LEVEL.name()).setKey("alert_status"));
    dbTester.measures().insertLiveMeasure(project, metric, m -> m.setValue(null).setData((String) null));

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById.get(projectData.projectUuid()).getMeasures().getNumericMeasures()).isEmpty();
  }

  @Test
  void return_language_distribution_measure_from_biggest_branch() {
    ProjectData projectData = dbTester.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    MetricDto languagesDistributionMetric = dbTester.measures().insertMetric(m -> m.setValueType(DATA.name()).setKey(
      "ncloc_language_distribution"));
    MetricDto nclocMetric = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setKey("ncloc"));

    dbTester.measures().insertLiveMeasure(project, languagesDistributionMetric, m -> m.setValue(null).setData("<null>=2;java=6;xoo=18"));
    dbTester.measures().insertLiveMeasure(project, nclocMetric, m -> m.setValue(26d));

    ComponentDto branch = dbTester.components().insertProjectBranch(project);
    dbTester.measures().insertLiveMeasure(branch, languagesDistributionMetric, m -> m.setValue(null).setData("<null>=4;java=12;xoo=36"));
    dbTester.measures().insertLiveMeasure(branch, nclocMetric, m -> m.setValue(52d));

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById.get(projectData.projectUuid()).getMeasures().getNclocByLanguages())
      .containsOnly(entry("<null>", 4), entry("java", 12), entry("xoo", 36));
  }

  @Test
  void does_not_return_none_numeric_metrics() {
    ProjectData projectData = dbTester.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    MetricDto dataMetric = dbTester.measures().insertMetric(m -> m.setValueType(DATA.name()).setKey("data"));
    MetricDto distribMetric = dbTester.measures().insertMetric(m -> m.setValueType(DISTRIB.name()).setKey("distrib"));
    MetricDto stringMetric = dbTester.measures().insertMetric(m -> m.setValueType(STRING.name()).setKey("string"));
    dbTester.measures().insertLiveMeasure(project, dataMetric, m -> m.setData("dat"));
    dbTester.measures().insertLiveMeasure(project, distribMetric, m -> m.setData("dis"));
    dbTester.measures().insertLiveMeasure(project, stringMetric, m -> m.setData("str"));

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById.get(projectData.projectUuid()).getMeasures().getNumericMeasures()).isEmpty();
  }

  @Test
  void does_not_return_disabled_metrics() {
    ProjectData projectData = dbTester.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    MetricDto disabledMetric =
      dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setEnabled(false).setHidden(false).setKey("disabled"));
    dbTester.measures().insertLiveMeasure(project, disabledMetric, m -> m.setValue(10d));

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById.get(projectData.projectUuid()).getMeasures().getNumericMeasures()).isEmpty();
  }

  @Test
  void ignore_measure_that_does_not_have_value() {
    MetricDto metric1 = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setKey("coverage"));
    MetricDto metric2 = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setKey("ncloc"));
    MetricDto leakMetric = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setKey("new_lines"));
    ProjectData projectData = dbTester.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();

    dbTester.measures().insertLiveMeasure(project, metric1, m -> m.setValue(10d));
    dbTester.measures().insertLiveMeasure(project, leakMetric, m -> m.setValue(20d));
    dbTester.measures().insertLiveMeasure(project, metric2, m -> m.setValue(null));

    Map<String, Double> numericMeasures =
      createResultSetAndReturnDocsById().get(projectData.projectUuid()).getMeasures().getNumericMeasures();
    assertThat(numericMeasures).containsOnly(entry(metric1.getKey(), 10d), entry(leakMetric.getKey(), 20d));
  }

  @Test
  void ignore_numeric_measure_that_has_text_value_but_not_numeric_value() {
    MetricDto metric1 = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setKey("coverage"));
    MetricDto metric2 = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setKey("ncloc"));
    ProjectData projectData = dbTester.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    dbTester.measures().insertLiveMeasure(project, metric1, m -> m.setValue(10d).setData((String) null));
    dbTester.measures().insertLiveMeasure(project, metric2, m -> m.setValue(null).setData("foo"));

    Map<String, Double> numericMeasures =
      createResultSetAndReturnDocsById().get(projectData.projectUuid()).getMeasures().getNumericMeasures();
    assertThat(numericMeasures).containsOnly(entry("coverage", 10d));
  }

  @Test
  void return_many_project_measures() {
    ComponentDto project1 = dbTester.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project2 = dbTester.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project3 = dbTester.components().insertPrivateProject().getMainBranchComponent();
    dbTester.components().insertSnapshot(project1);
    dbTester.components().insertSnapshot(project2);
    dbTester.components().insertSnapshot(project3);

    assertThat(createResultSetAndReturnDocsById()).hasSize(3);
  }

  @Test
  void return_project_without_analysis() {
    ProjectData projectData = dbTester.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    dbClient.snapshotDao().insert(dbSession, newAnalysis(project).setLast(false));
    dbSession.commit();

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById).hasSize(1);
    ProjectMeasures doc = docsById.get(projectData.projectUuid());
    assertThat(doc.getProject().getAnalysisDate()).isNull();
    assertThat(doc.getProject().getCreationDate()).isEqualTo(projectData.getProjectDto().getCreatedAt());
  }

  @Test
  void return_only_docs_from_given_project() {
    ProjectData projectData1 = dbTester.components().insertPrivateProject();
    ComponentDto project1 = projectData1.getMainBranchComponent();
    ComponentDto project2 = dbTester.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project3 = dbTester.components().insertPrivateProject().getMainBranchComponent();
    SnapshotDto analysis1 = dbTester.components().insertSnapshot(project1);
    SnapshotDto analysis2 = dbTester.components().insertSnapshot(project2);
    SnapshotDto analysis3 = dbTester.components().insertSnapshot(project3);

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById(projectData1.projectUuid());

    assertThat(docsById).hasSize(1);
    ProjectMeasures doc = docsById.get(projectData1.projectUuid());
    assertThat(doc).isNotNull();
    assertThat(doc.getProject().getUuid()).isEqualTo(projectData1.projectUuid());
    assertThat(doc.getProject().getKey()).isNotNull().isEqualTo(project1.getKey());
    assertThat(doc.getProject().getName()).isNotNull().isEqualTo(project1.name());
    assertThat(doc.getProject().getAnalysisDate()).isNotNull().isEqualTo(analysis1.getCreatedAt());
  }

  @Test
  void return_nothing_on_unknown_project() {
    ComponentDto project = dbTester.components().insertPrivateProject().getMainBranchComponent();
    dbTester.components().insertSnapshot(project);

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById("UNKNOWN");

    assertThat(docsById).isEmpty();
  }

  @Test
  void non_main_branches_are_not_indexed() {
    ProjectData projectData = dbTester.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    MetricDto metric = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setKey("ncloc"));
    dbTester.measures().insertLiveMeasure(project, metric, m -> m.setValue(10d));

    ComponentDto branch = dbTester.components().insertProjectBranch(project, b -> b.setKey("feature/foo"));
    dbTester.measures().insertLiveMeasure(branch, metric, m -> m.setValue(20d));

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById).hasSize(1).containsOnlyKeys(projectData.projectUuid());
    assertThat(docsById.get(projectData.projectUuid()).getMeasures().getNumericMeasures()).containsEntry(metric.getKey(), 10d);
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
}
