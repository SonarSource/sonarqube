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

import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.MeasureDao;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.metric.MetricRepositoryRule;
import org.sonar.server.db.DbClient;
import org.sonar.server.metric.persistence.MetricDao;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY;

@Category(DbTests.class)
public class PersistMeasuresStepTest extends BaseStepTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";
  private static final String STRING_METRIC_KEY = "string-metric-key";
  private static final String DOUBLE_METRIC_KEY = "double-metric-key";
  private static final String OPTIMIZED_METRIC_KEY = "optimized-metric-key";

  private static final Metric STRING_METRIC = new Metric.Builder(STRING_METRIC_KEY, "String metric", Metric.ValueType.STRING).create();
  private static final Metric DOUBLE_METRIC = new Metric.Builder(DOUBLE_METRIC_KEY, "Double metric", Metric.ValueType.FLOAT).create();

  private static final int PROJECT_REF = 1;
  private static final int FILE_REF = 2;

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule();

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  DbClient dbClient;
  DbSession session;
  DbIdsRepository dbIdsRepository = new DbIdsRepository();
  RuleDto rule;
  ComponentDto projectDto;
  ComponentDto fileDto;

  PersistMeasuresStep sut;

  @Before
  public void setUp() {
    dbTester.truncateTables();

    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new MeasureDao(), new ComponentDao(), new MetricDao(), new RuleDao(System2.INSTANCE));
    session = dbClient.openSession(false);

    session.commit();

    sut = new PersistMeasuresStep(dbClient, metricRepository, dbIdsRepository, treeRootHolder, measureRepository);

    projectDto = addComponent("project-key");
    fileDto = addComponent("file-key");

    Component file = DumbComponent.builder(Component.Type.FILE, FILE_REF).setUuid("CDEF").setKey("MODULE_KEY:file").build();
    Component project = DumbComponent.builder(Component.Type.PROJECT, PROJECT_REF).setUuid("ABCD").setKey(PROJECT_KEY).addChildren(file).build();
    treeRootHolder.setRoot(project);

    dbIdsRepository.setComponentId(project, projectDto.getId());
    dbIdsRepository.setSnapshotId(project, 3L);
    dbIdsRepository.setComponentId(file, fileDto.getId());
    dbIdsRepository.setSnapshotId(file, 4L);
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void insert_measures_from_report() {
    metricRepository.add(1, STRING_METRIC);
    metricRepository.add(2, DOUBLE_METRIC);

    measureRepository.addRawMeasure(PROJECT_REF, STRING_METRIC_KEY,
      Measure.newMeasureBuilder().setVariations(
        MeasureVariations.newMeasureVariationsBuilder()
          .setVariation(1, 1.1d)
          .setVariation(2, 2.2d)
          .setVariation(3, 3.3d)
          .setVariation(4, 4.4d)
          .setVariation(5, 5.5d)
          .build()
      ).create("measure-data")
    );
    measureRepository.addRawMeasure(FILE_REF, DOUBLE_METRIC_KEY,
      Measure.newMeasureBuilder().setVariations(
        MeasureVariations.newMeasureVariationsBuilder()
          .setVariation(1, 1.1d)
          .setVariation(2, 2.2d)
          .setVariation(3, 3.3d)
          .setVariation(4, 4.4d)
          .setVariation(5, 5.5d)
          .build()
      ).create(123.123d)
    );

    sut.execute();
    session.commit();

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(2);

    List<Map<String, Object>> dtos = retrieveDtos();

    Map<String, Object> dto = dtos.get(0);
    assertThat(dto.get("snapshotId")).isEqualTo(3L);
    assertThat(dto.get("componentId")).isEqualTo(projectDto.getId());
    assertThat(dto.get("metricId")).isEqualTo(1L);
    assertThat(dto.get("textValue")).isEqualTo("measure-data");
    assertThat(dto.get("severity")).isNull();

    dto = dtos.get(PROJECT_REF);
    assertThat(dto.get("snapshotId")).isEqualTo(4L);
    assertThat(dto.get("componentId")).isEqualTo(fileDto.getId());
    assertThat(dto.get("metricId")).isEqualTo(2L);
    assertThat(dto.get("value")).isEqualTo(123.1d);
    assertThat(dto.get("severity")).isNull();
  }

  private List<Map<String, Object>> retrieveDtos() {
    return dbTester.select(
      "select snapshot_id as \"snapshotId\", project_id as \"componentId\", metric_id as \"metricId\", rule_id as \"ruleId\", value as \"value\", text_value as \"textValue\", " +
        "rule_priority as \"severity\" from project_measures");
  }

  @Test
  public void bestValue_measure_of_bestValueOptimized_metrics_are_not_persisted() {
    metricRepository.add(1, new Metric.Builder(OPTIMIZED_METRIC_KEY, "Optimized metric", Metric.ValueType.BOOL).setOptimizedBestValue(true).setBestValue(1d).create());

    measureRepository.addRawMeasure(FILE_REF, OPTIMIZED_METRIC_KEY, Measure.newMeasureBuilder().create(true));

    sut.execute();
    session.commit();

    assertThat(retrieveDtos()).isEmpty();
  }

  @Test
  public void empty_values_are_not_persisted() {
    metricRepository.add(1, STRING_METRIC);
    metricRepository.add(2, DOUBLE_METRIC);

    measureRepository.addRawMeasure(FILE_REF, STRING_METRIC_KEY, Measure.newMeasureBuilder().createNoValue());
    measureRepository.addRawMeasure(FILE_REF, DOUBLE_METRIC_KEY, Measure.newMeasureBuilder().createNoValue());

    sut.execute();
    session.commit();

    assertThat(retrieveDtos()).isEmpty();
  }

  @Test
  public void do_not_insert_file_complexity_distribution_metric_on_files() {
    metricRepository.add(1, FILE_COMPLEXITY_DISTRIBUTION);

    measureRepository.addRawMeasure(PROJECT_REF, FILE_COMPLEXITY_DISTRIBUTION_KEY, Measure.newMeasureBuilder().create("0=1;2=10"));
    measureRepository.addRawMeasure(FILE_REF, FILE_COMPLEXITY_DISTRIBUTION_KEY, Measure.newMeasureBuilder().create("0=1;2=10"));

    sut.execute();

    session.commit();

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(1);

    List<Map<String, Object>> dtos = retrieveDtos();

    Map<String, Object> dto = dtos.get(0);
    assertThat(dto.get("snapshotId")).isEqualTo(3L);
    assertThat(dto.get("componentId")).isEqualTo(projectDto.getId());
    assertThat(dto.get("textValue")).isEqualTo("0=1;2=10");
  }

  @Test
  public void do_not_insert_function_complexity_distribution_metric_on_files() {
    metricRepository.add(1, FUNCTION_COMPLEXITY_DISTRIBUTION);

    measureRepository.addRawMeasure(PROJECT_REF, FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, Measure.newMeasureBuilder().create("0=1;2=10"));
    measureRepository.addRawMeasure(FILE_REF, FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, Measure.newMeasureBuilder().create("0=1;2=10"));

    sut.execute();

    session.commit();

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(1);

    List<Map<String, Object>> dtos = retrieveDtos();

    Map<String, Object> dto = dtos.get(0);
    assertThat(dto.get("snapshotId")).isEqualTo(3L);
    assertThat(dto.get("componentId")).isEqualTo(projectDto.getId());
    assertThat(dto.get("textValue")).isEqualTo("0=1;2=10");
  }

  private ComponentDto addComponent(String key) {
    ComponentDto componentDto = new ComponentDto().setKey(key).setUuid(Uuids.create());
    dbClient.componentDao().insert(session, componentDto);
    session.commit();
    return componentDto;
  }

  @Override
  protected ComputationStep step() {
    return sut;
  }
}
