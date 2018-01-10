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

import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.ProjectMeasuresIndexerIterator.ProjectMeasures;
import org.sonar.db.metric.MetricDto;
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
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization, p -> p.setDbKey("Project-Key").setName("Project Name").setTagsString("platform,java"));
    SnapshotDto analysis = dbTester.components().insertSnapshot(project);
    MetricDto metric1 = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setKey("ncloc"));
    MetricDto metric2 = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setKey("coverage"));
    dbTester.measures().insertLiveMeasure(project, metric1, m -> m.setValue(10d));
    dbTester.measures().insertLiveMeasure(project, metric2, m -> m.setValue(20d));

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById).hasSize(1);
    ProjectMeasures doc = docsById.get(project.uuid());
    assertThat(doc).isNotNull();
    assertThat(doc.getProject().getUuid()).isEqualTo(project.uuid());
    assertThat(doc.getProject().getKey()).isEqualTo("Project-Key");
    assertThat(doc.getProject().getName()).isEqualTo("Project Name");
    assertThat(doc.getProject().getTags()).containsExactly("platform", "java");
    assertThat(doc.getProject().getAnalysisDate()).isNotNull().isEqualTo(analysis.getCreatedAt());
    assertThat(doc.getMeasures().getNumericMeasures()).containsOnly(entry(metric1.getKey(), 10d), entry(metric2.getKey(), 20d));
  }

  @Test
  public void return_project_measure_having_leak() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization, p -> p.setDbKey("Project-Key").setName("Project Name").setTagsString("platform,java"));
    MetricDto metric = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setKey("new_lines"));
    dbTester.measures().insertLiveMeasure(project, metric, m -> m.setVariation(10d));

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById.get(project.uuid()).getMeasures().getNumericMeasures()).containsOnly(entry("new_lines", 10d));
  }

  @Test
  public void return_quality_gate_status_measure() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project1 = dbTester.components().insertPrivateProject(organization);
    ComponentDto project2 = dbTester.components().insertPrivateProject(organization);
    ComponentDto project3 = dbTester.components().insertPrivateProject(organization);
    MetricDto metric = dbTester.measures().insertMetric(m -> m.setValueType(LEVEL.name()).setKey("alert_status"));
    dbTester.measures().insertLiveMeasure(project1, metric, m -> m.setValue(null).setData(WARN.name()));
    dbTester.measures().insertLiveMeasure(project2, metric, m -> m.setValue(null).setData(OK.name()));
    dbTester.measures().insertLiveMeasure(project3, metric, m -> m.setValue(null).setData(ERROR.name()));

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById.get(project1.uuid()).getMeasures().getQualityGateStatus()).isEqualTo("WARN");
    assertThat(docsById.get(project2.uuid()).getMeasures().getQualityGateStatus()).isEqualTo("OK");
    assertThat(docsById.get(project3.uuid()).getMeasures().getQualityGateStatus()).isEqualTo("ERROR");
  }

  @Test
  public void does_not_fail_when_quality_gate_has_no_value() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    MetricDto metric = dbTester.measures().insertMetric(m -> m.setValueType(LEVEL.name()).setKey("alert_status"));
    dbTester.measures().insertLiveMeasure(project, metric, m -> m.setValue(null).setVariation(null).setData((String) null));

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById.get(project.uuid()).getMeasures().getNumericMeasures()).isEmpty();
  }

  @Test
  public void return_language_distribution_measure() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    MetricDto metric = dbTester.measures().insertMetric(m -> m.setValueType(DATA.name()).setKey("ncloc_language_distribution"));
    dbTester.measures().insertLiveMeasure(project, metric, m -> m.setValue(null).setData("<null>=2;java=6;xoo=18"));

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById.get(project.uuid()).getMeasures().getNclocByLanguages())
      .containsOnly(entry("<null>", 2), entry("java", 6), entry("xoo", 18));
  }

  @Test
  public void does_not_return_none_numeric_metrics() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    MetricDto dataMetric = dbTester.measures().insertMetric(m -> m.setValueType(DATA.name()).setKey("data"));
    MetricDto distribMetric = dbTester.measures().insertMetric(m -> m.setValueType(DISTRIB.name()).setKey("distrib"));
    MetricDto stringMetric = dbTester.measures().insertMetric(m -> m.setValueType(STRING.name()).setKey("string"));
    dbTester.measures().insertLiveMeasure(project, dataMetric, m -> m.setData("dat"));
    dbTester.measures().insertLiveMeasure(project, distribMetric, m -> m.setData("dis"));
    dbTester.measures().insertLiveMeasure(project, stringMetric, m -> m.setData("str"));

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById.get(project.uuid()).getMeasures().getNumericMeasures()).isEmpty();
  }

  @Test
  public void does_not_return_disabled_metrics() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    MetricDto disabledMetric = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setEnabled(false).setHidden(false).setKey("disabled"));
    dbTester.measures().insertLiveMeasure(project, disabledMetric, m -> m.setValue(10d));

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById.get(project.uuid()).getMeasures().getNumericMeasures()).isEmpty();
  }

  @Test
  public void ignore_measure_that_does_not_have_value() {
    OrganizationDto organization = dbTester.organizations().insert();
    MetricDto metric1 = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setKey("coverage"));
    MetricDto metric2 = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setKey("ncloc"));
    MetricDto leakMetric = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setKey("new_lines"));
    ComponentDto project = dbTester.components().insertPrivateProject(organization);

    dbTester.measures().insertLiveMeasure(project, metric1, m -> m.setValue(10d));
    dbTester.measures().insertLiveMeasure(project, leakMetric, m -> m.setValue(null).setVariation(20d));
    dbTester.measures().insertLiveMeasure(project, metric2, m -> m.setValue(null).setVariation(null));

    Map<String, Double> numericMeasures = createResultSetAndReturnDocsById().get(project.uuid()).getMeasures().getNumericMeasures();
    assertThat(numericMeasures).containsOnly(entry(metric1.getKey(), 10d), entry(leakMetric.getKey(), 20d));
  }

  @Test
  public void ignore_numeric_measure_that_has_text_value_but_not_numeric_value() {
    OrganizationDto organization = dbTester.organizations().insert();
    MetricDto metric1 = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setKey("coverage"));
    MetricDto metric2 = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setKey("ncloc"));
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    dbTester.measures().insertLiveMeasure(project, metric1, m -> m.setValue(10d).setData((String) null));
    dbTester.measures().insertLiveMeasure(project, metric2, m -> m.setValue(null).setData("foo"));

    Map<String, Double> numericMeasures = createResultSetAndReturnDocsById().get(project.uuid()).getMeasures().getNumericMeasures();
    assertThat(numericMeasures).containsOnly(entry("coverage", 10d));
  }

  @Test
  public void return_many_project_measures() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project1 = dbTester.components().insertPrivateProject(organization);
    ComponentDto project2 = dbTester.components().insertPrivateProject(organization);
    ComponentDto project3 = dbTester.components().insertPrivateProject(organization);
    dbTester.components().insertSnapshot(project1);
    dbTester.components().insertSnapshot(project2);
    dbTester.components().insertSnapshot(project3);

    assertThat(createResultSetAndReturnDocsById()).hasSize(3);
  }

  @Test
  public void return_project_without_analysis() {
    ComponentDto project = dbTester.components().insertComponent(ComponentTesting.newPrivateProjectDto(dbTester.organizations().insert()));
    dbClient.snapshotDao().insert(dbSession, newAnalysis(project).setLast(false));
    dbSession.commit();

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById).hasSize(1);
    ProjectMeasures doc = docsById.get(project.uuid());
    assertThat(doc.getProject().getAnalysisDate()).isNull();
  }

  @Test
  public void does_not_return_non_active_projects() {
    OrganizationDto organization = dbTester.organizations().insert();
    // Disabled project
    dbTester.components().insertProjectAndSnapshot(ComponentTesting.newPrivateProjectDto(organization).setEnabled(false));
    // Disabled project with analysis
    ComponentDto project = dbTester.components().insertComponent(ComponentTesting.newPrivateProjectDto(organization).setEnabled(false));
    dbClient.snapshotDao().insert(dbSession, newAnalysis(project));

    // A view
    dbTester.components().insertProjectAndSnapshot(newView(organization));

    dbSession.commit();

    assertThat(createResultSetAndReturnDocsById()).isEmpty();
  }

  @Test
  public void return_only_docs_from_given_project() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project1 = dbTester.components().insertPrivateProject(organization);
    ComponentDto project2 = dbTester.components().insertPrivateProject(organization);
    ComponentDto project3 = dbTester.components().insertPrivateProject(organization);
    SnapshotDto analysis1 = dbTester.components().insertSnapshot(project1);
    SnapshotDto analysis2 = dbTester.components().insertSnapshot(project2);
    SnapshotDto analysis3 = dbTester.components().insertSnapshot(project3);

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById(project1.uuid());

    assertThat(docsById).hasSize(1);
    ProjectMeasures doc = docsById.get(project1.uuid());
    assertThat(doc).isNotNull();
    assertThat(doc.getProject().getUuid()).isEqualTo(project1.uuid());
    assertThat(doc.getProject().getKey()).isNotNull().isEqualTo(project1.getDbKey());
    assertThat(doc.getProject().getName()).isNotNull().isEqualTo(project1.name());
    assertThat(doc.getProject().getAnalysisDate()).isNotNull().isEqualTo(analysis1.getCreatedAt());
  }

  @Test
  public void return_nothing_on_unknown_project() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    dbTester.components().insertSnapshot(project);

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById("UNKNOWN");

    assertThat(docsById).isEmpty();
  }

  @Test
  public void non_main_branches_are_not_indexed() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    MetricDto metric = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setKey("ncloc"));
    dbTester.measures().insertLiveMeasure(project, metric, m -> m.setValue(10d));

    ComponentDto branch = dbTester.components().insertProjectBranch(project, b -> b.setKey("feature/foo"));
    dbTester.measures().insertLiveMeasure(branch, metric, m -> m.setValue(20d));

    Map<String, ProjectMeasures> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById).hasSize(1).containsOnlyKeys(project.uuid());
    assertThat(docsById.get(project.uuid()).getMeasures().getNumericMeasures().get(metric.getKey())).isEqualTo(10d);
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
