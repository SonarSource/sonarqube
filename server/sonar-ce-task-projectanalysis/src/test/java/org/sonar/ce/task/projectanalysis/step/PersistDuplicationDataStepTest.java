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
package org.sonar.ce.task.projectanalysis.step;

import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.MutableAnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.duplication.DuplicationRepositoryRule;
import org.sonar.ce.task.projectanalysis.duplication.TextBlock;
import org.sonar.ce.task.projectanalysis.measure.MeasureToMeasureDto;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.DUPLICATIONS_DATA_KEY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;

public class PersistDuplicationDataStepTest extends BaseStepTest {

  private static final int ROOT_REF = 1;
  private static final String PROJECT_KEY = "PROJECT_KEY";
  private static final String PROJECT_UUID = "u1";

  private static final int FILE_1_REF = 2;
  private static final String FILE_1_KEY = "FILE_1_KEY";
  private static final String FILE_1_UUID = "u2";

  private static final int FILE_2_REF = 3;
  private static final String FILE_2_KEY = "FILE_2_KEY";
  private static final String FILE_2_UUID = "u3";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(
      builder(PROJECT, ROOT_REF).setKey(PROJECT_KEY).setUuid(PROJECT_UUID)
        .addChildren(
          builder(FILE, FILE_1_REF).setKey(FILE_1_KEY).setUuid(FILE_1_UUID)
            .build(),
          builder(FILE, FILE_2_REF).setKey(FILE_2_KEY).setUuid(FILE_2_UUID)
            .build())
        .build());

  @Rule
  public MutableAnalysisMetadataHolderRule analysisMetadataHolder = new MutableAnalysisMetadataHolderRule();
  @Rule
  public DuplicationRepositoryRule duplicationRepository = DuplicationRepositoryRule.create(treeRootHolder);
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule();

  @Before
  public void setUp() throws Exception {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(DUPLICATIONS_DATA_KEY).setValueType(Metric.ValueType.STRING.name()));
    insertComponent(PROJECT_KEY, PROJECT_UUID);
    insertComponent(FILE_1_KEY, FILE_1_UUID);
    insertComponent(FILE_2_KEY, FILE_2_UUID);
    db.commit();
    metricRepository.add(metric.getId(), new Metric.Builder(DUPLICATIONS_DATA_KEY, DUPLICATIONS_DATA_KEY, Metric.ValueType.STRING).create());
  }

  @Override
  protected ComputationStep step() {
    return underTest();
  }

  @Test
  public void nothing_to_persist_when_no_duplication() {
    TestComputationStepContext context = new TestComputationStepContext();

    underTest().execute(context);

    assertThatNothingPersisted();
    verifyStatistics(context, 0);
  }

  @Test
  public void compute_duplications_on_same_file() {
    duplicationRepository.addDuplication(FILE_1_REF, new TextBlock(1, 5), new TextBlock(6, 10));
    TestComputationStepContext context = new TestComputationStepContext();

    underTest().execute(context);

    assertThat(selectMeasureData(FILE_1_UUID)).hasValue("<duplications><g><b s=\"1\" l=\"5\" t=\"false\" r=\"" + FILE_1_KEY + "\"/><b s=\"6\" l=\"5\" t=\"false\" r=\""
        + FILE_1_KEY + "\"/></g></duplications>");
    assertThat(selectMeasureData(FILE_2_UUID)).isEmpty();
    assertThat(selectMeasureData(PROJECT_UUID)).isEmpty();
  }

  @Test
  public void compute_duplications_on_different_files() {
    duplicationRepository.addDuplication(FILE_1_REF, new TextBlock(1, 5), FILE_2_REF, new TextBlock(6, 10));
    TestComputationStepContext context = new TestComputationStepContext();

    underTest().execute(context);

    assertThat(selectMeasureData(FILE_1_UUID)).hasValue(
      "<duplications><g><b s=\"1\" l=\"5\" t=\"false\" r=\"" + FILE_1_KEY + "\"/><b s=\"6\" l=\"5\" t=\"false\" r=\""
        + FILE_2_KEY + "\"/></g></duplications>");
    assertThat(selectMeasureData(FILE_2_UUID)).isEmpty();
    assertThat(selectMeasureData(PROJECT_UUID)).isEmpty();
  }

  @Test
  public void compute_duplications_on_unchanged_file() {
    duplicationRepository.addExtendedProjectDuplication(FILE_1_REF, new TextBlock(1, 5), FILE_2_REF, new TextBlock(6, 10));
    TestComputationStepContext context = new TestComputationStepContext();

    underTest().execute(context);

    assertThat(selectMeasureData(FILE_1_UUID)).hasValue(
      "<duplications><g><b s=\"1\" l=\"5\" t=\"false\" r=\"" + FILE_1_KEY + "\"/><b s=\"6\" l=\"5\" t=\"true\" r=\""
        + FILE_2_KEY + "\"/></g></duplications>");
    assertThat(selectMeasureData(FILE_2_UUID)).isEmpty();
    assertThat(selectMeasureData(PROJECT_UUID)).isEmpty();
  }

  @Test
  public void compute_duplications_on_different_projects() {
    String fileKeyFromOtherProject = "PROJECT2_KEY:file2";
    duplicationRepository.addCrossProjectDuplication(FILE_1_REF, new TextBlock(1, 5), fileKeyFromOtherProject, new TextBlock(6, 10));
    TestComputationStepContext context = new TestComputationStepContext();

    underTest().execute(context);

    assertThat(selectMeasureData(FILE_1_UUID)).hasValue(
      "<duplications><g><b s=\"1\" l=\"5\" t=\"false\" r=\"" + FILE_1_KEY + "\"/><b s=\"6\" l=\"5\" t=\"false\" r=\""
        + fileKeyFromOtherProject + "\"/></g></duplications>");
    assertThat(selectMeasureData(FILE_2_UUID)).isEmpty();
    assertThat(selectMeasureData(PROJECT_UUID)).isEmpty();
  }

  private PersistDuplicationDataStep underTest() {
    return new PersistDuplicationDataStep(db.getDbClient(), treeRootHolder, metricRepository, duplicationRepository,
      new MeasureToMeasureDto(analysisMetadataHolder, treeRootHolder));
  }

  private void assertThatNothingPersisted() {
    assertThat(db.countRowsOfTable(db.getSession(), "live_measures")).isEqualTo(0);
  }

  private Optional<String> selectMeasureData(String componentUuid) {
      return db.getDbClient().liveMeasureDao().selectMeasure(db.getSession(), componentUuid, "duplications_data")
      .map(LiveMeasureDto::getTextValue);
  }

  private ComponentDto insertComponent(String key, String uuid) {
    ComponentDto componentDto = new ComponentDto()
      .setOrganizationUuid("org1")
      .setDbKey(key)
      .setUuid(uuid)
      .setUuidPath(uuid + ".")
      .setRootUuid(uuid)
      .setProjectUuid(uuid);
    db.getDbClient().componentDao().insert(db.getSession(), componentDto);
    return componentDto;
  }

  private static void verifyStatistics(TestComputationStepContext context, int expectedInsertsOrUpdates) {
    context.getStatistics().assertValue("insertsOrUpdates", expectedInsertsOrUpdates);
  }
}
