/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.ibatis.session.ResultHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.component.ProjectData;
import org.sonar.db.metric.MetricDto;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.measure.MeasureTesting.newMeasure;

class MeasureDaoIT {

  @RegisterExtension
  public final DbTester db = DbTester.create(System2.INSTANCE);

  private final MeasureDao underTest = db.getDbClient().measureDao();

  @Test
  void insert_measure() {
    MeasureDto dto = newMeasure();
    assertThat(dto.getJsonValueHash()).isNull();

    int count = underTest.insert(db.getSession(), dto);

    assertThat(dto.getJsonValueHash()).isNotNull();
    assertThat(count).isEqualTo(1);
    verifyTableSize(1);
    verifyPersisted(dto);
  }

  @Test
  void update_measure() {
    MeasureDto dto = newMeasure();
    underTest.insert(db.getSession(), dto);

    dto.addValue("metric1", "value1");
    dto.computeJsonValueHash();
    int count = underTest.update(db.getSession(), dto);

    assertThat(count).isEqualTo(1);
    verifyTableSize(1);
    verifyPersisted(dto);
  }

  @Test
  void insertOrUpdate_inserts_or_updates_measure() {
    // insert
    MeasureDto dto = newMeasure();
    int count = underTest.insertOrUpdate(db.getSession(), dto);
    assertThat(count).isEqualTo(1);
    verifyTableSize(1);
    verifyPersisted(dto);

    // update
    String key = dto.getMetricValues().keySet().stream().findFirst().orElseThrow();
    dto.addValue(key, getDoubleValue());
    count = underTest.insertOrUpdate(db.getSession(), dto);
    assertThat(count).isEqualTo(1);
    verifyTableSize(1);
    verifyPersisted(dto);
  }

  @Test
  void insertOrUpdate_merges_measures() {
    // insert
    Double value2 = getDoubleValue();
    MeasureDto dto = newMeasure();
    dto.getMetricValues().clear();
    dto.addValue("key1", getDoubleValue())
      .addValue("key2", value2);
    int count = underTest.insert(db.getSession(), dto);
    verifyPersisted(dto);
    verifyTableSize(1);
    assertThat(count).isEqualTo(1);

    // update key1 value, remove key2 (must not disappear from DB) and add key3
    Double value1 = getDoubleValue();
    Double value3 = getDoubleValue();
    dto.addValue("key1", value1)
      .addValue("key3", value3)
      .getMetricValues().remove("key2");
    count = underTest.insertOrUpdate(db.getSession(), dto);
    assertThat(count).isEqualTo(1);
    verifyTableSize(1);

    assertThat(underTest.selectByComponentUuid(db.getSession(), dto.getComponentUuid()))
      .hasValueSatisfying(selected -> {
        assertThat(selected.getComponentUuid()).isEqualTo(dto.getComponentUuid());
        assertThat(selected.getBranchUuid()).isEqualTo(dto.getBranchUuid());
        assertThat(selected.getMetricValues()).contains(
          entry("key1", value1),
          entry("key2", value2),
          entry("key3", value3));
        assertThat(selected.getJsonValueHash()).isEqualTo(dto.computeJsonValueHash());
      });
  }

  @Test
  void selectByComponentUuid() {
    MeasureDto measure1 = newMeasure();
    MeasureDto measure2 = newMeasure();
    underTest.insert(db.getSession(), measure1);
    underTest.insert(db.getSession(), measure2);

    assertThat(underTest.selectByComponentUuid(db.getSession(), measure1.getComponentUuid()))
      .hasValueSatisfying(selected -> assertThat(selected).usingRecursiveComparison().isEqualTo(measure1));

    assertThat(underTest.selectByComponentUuid(db.getSession(), "unknown-component")).isEmpty();
  }

  @Test
  void selectByComponentUuidAndMetricKeys() {
    ComponentDto branch1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto branch2 = db.components().insertPrivateProject().getMainBranchComponent();
    String metricKey1 = "metric1";
    String metricKey2 = "metric2";
    String metricKey3 = "metric3";
    String value = "foo";
    db.measures().insertMeasure(branch1, m -> m.addValue(metricKey1, value).addValue(metricKey2, value).addValue(metricKey3, value));
    db.measures().insertMeasure(branch2, m -> m.addValue(metricKey1, value));

    assertThat(underTest.selectByComponentUuidAndMetricKeys(db.getSession(), branch1.uuid(), List.of(metricKey1, metricKey2)))
      .hasValueSatisfying(selected -> {
        assertThat(selected.getComponentUuid()).isEqualTo(branch1.uuid());
        assertThat(selected.getMetricValues()).containsOnlyKeys(metricKey1, metricKey2);
      });

    assertThat(underTest.selectByComponentUuidAndMetricKeys(db.getSession(), "unknown-component", List.of(metricKey1))).isEmpty();
    assertThat(underTest.selectByComponentUuidAndMetricKeys(db.getSession(), branch1.uuid(), List.of("random-metric"))).isEmpty();
  }

  @Test
  void selectByComponentUuidsAndMetricKeys() {
    ComponentDto branch1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto branch2 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto branch3 = db.components().insertPrivateProject().getMainBranchComponent();
    String metricKey1 = "metric1";
    String metricKey2 = "metric2";
    String metricKey3 = "metric3";
    String value = "foo";
    db.measures().insertMeasure(branch1, m -> m.addValue(metricKey1, value).addValue(metricKey2, value).addValue(metricKey3, value));
    db.measures().insertMeasure(branch2, m -> m.addValue(metricKey1, value));
    db.measures().insertMeasure(branch3, m -> m.addValue(metricKey1, value));

    List<MeasureDto> measures = underTest.selectByComponentUuidsAndMetricKeys(db.getSession(), List.of(branch1.uuid(), branch2.uuid()),
      List.of(metricKey1, metricKey2));
    assertThat(measures).hasSize(2);
    assertThat(measures.stream().filter(m -> m.getComponentUuid().equals(branch1.uuid())).map(MeasureDto::getMetricValues).findFirst())
      .hasValueSatisfying(metricValues -> assertThat(metricValues).containsOnlyKeys(metricKey1, metricKey2));
    assertThat(measures.stream().filter(m -> m.getComponentUuid().equals(branch2.uuid())).map(MeasureDto::getMetricValues).findFirst())
      .hasValueSatisfying(metricValues -> assertThat(metricValues).containsOnlyKeys(metricKey1));

    assertThat(underTest.selectByComponentUuidsAndMetricKeys(db.getSession(), List.of("unknown-component"), List.of(metricKey1))).isEmpty();
    assertThat(underTest.selectByComponentUuidsAndMetricKeys(db.getSession(), List.of(branch1.uuid()), List.of("random-metric"))).isEmpty();
  }

  @Test
  void selectByComponentUuidsAndMetricKeys_does_not_use_db_when_no_components() {
    String metricKey = randomAlphabetic(7);
    newMeasure().addValue(metricKey, randomAlphabetic(11));

    DbSession dbSession = mock(DbSession.class);
    assertThat(underTest.selectByComponentUuidsAndMetricKeys(dbSession, emptyList(), singletonList(metricKey)))
      .isEmpty();
    verifyNoInteractions(dbSession);
  }

  @Test
  void selectByComponentUuidsAndMetricKeys_does_not_use_db_when_no_metrics() {
    DbSession dbSession = mock(DbSession.class);
    assertThat(underTest.selectByComponentUuidsAndMetricKeys(dbSession, singletonList("nonexistent"), emptyList()))
      .isEmpty();
    verifyNoInteractions(dbSession);
  }

  @Test
  void selectByComponentUuidsAndMetricKeys_with_single_component_and_single_metric() {
    String metricKey = randomAlphabetic(7);
    String value = randomAlphabetic(11);
    MeasureDto measure = newMeasure().addValue(metricKey, value);
    underTest.insert(db.getSession(), measure);

    List<MeasureDto> measureDtos = underTest.selectByComponentUuidsAndMetricKeys(
      db.getSession(), singletonList(measure.getComponentUuid()), singletonList(metricKey));
    assertThat(measureDtos).hasSize(1);
    assertThat(measureDtos.get(0).getMetricValues()).isEqualTo(Map.of(metricKey, value));
  }

  @Test
  void selectByComponentUuidsAndMetricKeys_with_nonexistent_component_returns_empty() {
    String metricKey = randomAlphabetic(7);
    String value = randomAlphabetic(11);
    MeasureDto measure = newMeasure().addValue(metricKey, value);
    underTest.insert(db.getSession(), measure);

    assertThat(underTest.selectByComponentUuidsAndMetricKeys(
      db.getSession(), singletonList("nonexistent"), singletonList(metricKey))).isEmpty();

    assertThat(underTest.selectByComponentUuidsAndMetricKeys(
      db.getSession(), singletonList(measure.getComponentUuid()), singletonList(metricKey))).isNotEmpty();
  }

  @Test
  void selectByComponentUuidsAndMetricKeys_with_nonexistent_metric_returns_empty() {
    String metricKey = randomAlphabetic(7);
    String value = randomAlphabetic(11);
    MeasureDto measure = newMeasure().addValue(metricKey, value);
    underTest.insert(db.getSession(), measure);

    assertThat(underTest.selectByComponentUuidsAndMetricKeys(
      db.getSession(), singletonList(measure.getComponentUuid()), singletonList("nonexistent"))).isEmpty();

    assertThat(underTest.selectByComponentUuidsAndMetricKeys(
      db.getSession(), singletonList(measure.getComponentUuid()), singletonList(metricKey))).isNotEmpty();

    MeasureDto m = newMeasure().addValue("foo", "bar");
    underTest.insert(db.getSession(), m);
  }

  @Test
  void selectByComponentUuidsAndMetricKeys_with_many_components_and_many_metrics() {
    String metric1 = "metric1";
    String metric2 = "metric2";
    String nonRequestedMetric = "nonRequestedMetric";

    String component1 = "component1";
    String component1measure1 = "component1measure1";
    underTest.insert(db.getSession(),
      newMeasure().setComponentUuid(component1)
        .addValue(metric1, component1measure1)
        .addValue(nonRequestedMetric, randomAlphabetic(7)));

    String component2 = "component2";
    String component2measure1 = "component2measure1";
    String component2measure2 = "component2measure2";
    underTest.insert(db.getSession(),
      newMeasure().setComponentUuid(component2)
        .addValue(metric1, component2measure1)
        .addValue(metric2, component2measure2)
        .addValue(nonRequestedMetric, randomAlphabetic(7)));

    String nonRequestedComponent = "nonRequestedComponent";
    underTest.insert(db.getSession(),
      newMeasure().setComponentUuid(nonRequestedComponent).addValue(metric1, randomAlphabetic(7)));

    List<MeasureDto> measureDtos = underTest.selectByComponentUuidsAndMetricKeys(
      db.getSession(), Arrays.asList(component1, component2), Arrays.asList(metric1, metric2));

    assertThat(measureDtos.stream().map(MeasureDto::getComponentUuid))
      .containsExactlyInAnyOrder(component1, component2);

    assertThat(measureDtos).flatExtracting(m -> m.getMetricValues().entrySet().stream()
        .map(entry -> tuple(m.getComponentUuid(), entry.getKey(), entry.getValue()))
        .toList())
      .containsExactlyInAnyOrder(
        tuple(component1, metric1, component1measure1),
        tuple(component2, metric1, component2measure1),
        tuple(component2, metric2, component2measure2)
      );
  }

  @Test
  void select_measure_hashes_for_branch() {
    MeasureDto measure1 = new MeasureDto()
      .setComponentUuid("c1")
      .setBranchUuid("b1")
      .addValue("metric1", "value1");
    MeasureDto measure2 = new MeasureDto()
      .setComponentUuid("c2")
      .setBranchUuid("b1")
      .addValue("metric2", "value2");
    MeasureDto measure3 = new MeasureDto()
      .setComponentUuid("c3")
      .setBranchUuid("b3")
      .addValue("metric3", "value3");
    long hash1 = measure1.computeJsonValueHash();
    long hash2 = measure2.computeJsonValueHash();
    measure3.computeJsonValueHash();

    underTest.insert(db.getSession(), measure1);
    underTest.insert(db.getSession(), measure2);
    underTest.insert(db.getSession(), measure3);

    assertThat(underTest.selectMeasureHashesForBranch(db.getSession(), "b1"))
      .containsOnly(new MeasureHash("c1", hash1), new MeasureHash("c2", hash2));
  }

  @Test
  void selectTreeByQuery_return_leaves_and_base_component() {
    List<MeasureDto> results = new ArrayList<>();
    MetricDto metric1 = db.measures().insertMetric();
    MetricDto metric2 = db.measures().insertMetric();

    ComponentDto branch1 = db.components().insertPrivateProject().getMainBranchComponent();
    MeasureDto measureOnProject1 = newMeasureForMetrics(branch1, metric1, metric2);

    ComponentDto file11 = db.components().insertComponent(newFileDto(branch1));
    ComponentDto file12 = db.components().insertComponent(newFileDto(branch1));
    MeasureDto measureOnFile11 = newMeasureForMetrics(file11, metric1, metric2);
    MeasureDto measureOnFile12 = newMeasureForMetrics(file12, metric1, metric2);

    ComponentDto branch2 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file21 = db.components().insertComponent(newFileDto(branch2));
    newMeasureForMetrics(file21, metric1, metric2);

    underTest.selectTreeByQuery(db.getSession(), branch1,
      MeasureTreeQuery.builder()
        .setStrategy(MeasureTreeQuery.Strategy.LEAVES).build(),
      context -> results.add(context.getResultObject()));

    assertThat(results)
      .hasSize(3)
      .extracting(MeasureDto::getComponentUuid, measureDto -> measureDto.getMetricValues().get(metric1.getKey()))
      .contains(
        tuple(branch1.uuid(), measureOnProject1.getDouble(metric1.getKey())),
        tuple(file11.uuid(), measureOnFile11.getDouble(metric1.getKey())),
        tuple(file12.uuid(), measureOnFile12.getDouble(metric1.getKey()))
      );
  }

  @Test
  void selectTreeByQuery_return_children_and_several_measures() {
    List<MeasureDto> results = new ArrayList<>();
    MetricDto metric1 = db.measures().insertMetric();
    MetricDto metric2 = db.measures().insertMetric();

    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();

    ComponentDto dir = db.components().insertComponent(newDirectory(branch, RandomStringUtils.randomAlphabetic(15)));
    MeasureDto measureOnDirectory = newMeasureForMetrics(dir, metric1, metric2);

    ComponentDto file1 = db.components().insertComponent(newFileDto(dir));
    newMeasureForMetrics(file1, metric1, metric2);

    underTest.selectTreeByQuery(db.getSession(), branch,
      MeasureTreeQuery.builder()
        .setStrategy(MeasureTreeQuery.Strategy.CHILDREN).build(),
      context -> results.add(context.getResultObject()));

    assertThat(results)
      .hasSize(1)
      .extracting(MeasureDto::getComponentUuid, measureDto -> measureDto.getMetricValues().get(metric1.getKey()),
        measureDto -> measureDto.getMetricValues().get(metric2.getKey()))
      .contains(tuple(dir.uuid(), measureOnDirectory.getDouble(metric1.getKey()), measureOnDirectory.getDouble(metric2.getKey())));
  }

  @Test
  void selectTreeByQuery_return_leaves_filtered_by_qualifier() {
    List<MeasureDto> results = new ArrayList<>();
    MetricDto metric1 = db.measures().insertMetric();
    MetricDto metric2 = db.measures().insertMetric();

    ComponentDto branch1 = db.components().insertPrivateProject().getMainBranchComponent();
    newMeasureForMetrics(branch1, metric1, metric2);

    ComponentDto file = db.components().insertComponent(newFileDto(branch1));
    ComponentDto uts = db.components().insertComponent(newFileDto(branch1).setQualifier(ComponentQualifiers.UNIT_TEST_FILE));
    MeasureDto measureOnFile11 = newMeasureForMetrics(file, metric1, metric2);
    newMeasureForMetrics(uts, metric1, metric2);

    underTest.selectTreeByQuery(db.getSession(), branch1,
      MeasureTreeQuery.builder()
        .setQualifiers(Collections.singletonList(ComponentQualifiers.FILE))
        .setStrategy(MeasureTreeQuery.Strategy.LEAVES).build(),
      context -> results.add(context.getResultObject()));

    assertThat(results)
      .hasSize(1)
      .extracting(MeasureDto::getComponentUuid, measureDto -> measureDto.getMetricValues().get(metric1.getKey()))
      .contains(
        tuple(file.uuid(), measureOnFile11.getDouble(metric1.getKey()))
      );
  }

  @Test
  void selectTreeByQuery_return_leaves_filtered_by_name_or_key() {
    List<MeasureDto> results = new ArrayList<>();
    MetricDto metric1 = db.measures().insertMetric();
    MetricDto metric2 = db.measures().insertMetric();

    ComponentDto matchingBranch =
      db.components().insertPrivateProject(project -> project.setName("matchingBranch")).getMainBranchComponent();
    MeasureDto measureOnProject = newMeasureForMetrics(matchingBranch, metric1, metric2);

    ComponentDto fileWithMatchingName = db.components().insertComponent(newFileDto(matchingBranch).setName("matchingName"));
    ComponentDto fileWithMatchingKey = db.components().insertComponent(newFileDto(matchingBranch).setName("anotherName").setKey("matching"
    ));
    ComponentDto fileNotMatching = db.components().insertComponent(newFileDto(matchingBranch).setName("anotherName").setKey("anotherKee"));

    MeasureDto measureOnMatchingName = newMeasureForMetrics(fileWithMatchingName, metric1, metric2);
    MeasureDto measureOnMatchingKee = newMeasureForMetrics(fileWithMatchingKey, metric1, metric2);
    newMeasureForMetrics(fileNotMatching, metric1, metric2);

    underTest.selectTreeByQuery(db.getSession(), matchingBranch,
      MeasureTreeQuery.builder()
        .setNameOrKeyQuery("matching")
        .setStrategy(MeasureTreeQuery.Strategy.LEAVES).build(),
      context -> results.add(context.getResultObject()));

    assertThat(results)
      .hasSize(3)
      .extracting(MeasureDto::getComponentUuid, measureDto -> measureDto.getMetricValues().get(metric1.getKey()))
      .contains(
        tuple(matchingBranch.uuid(), measureOnProject.getDouble(metric1.getKey())),
        tuple(fileWithMatchingName.uuid(), measureOnMatchingName.getDouble(metric1.getKey())),
        tuple(fileWithMatchingKey.uuid(), measureOnMatchingKee.getDouble(metric1.getKey()))
      );
  }

  @Test
  void selectTreeByQuery_with_empty_results() {
    List<MeasureDto> results = new ArrayList<>();
    underTest.selectTreeByQuery(db.getSession(), newPrivateProjectDto(),
      MeasureTreeQuery.builder().setStrategy(MeasureTreeQuery.Strategy.LEAVES).build(),
      context -> results.add(context.getResultObject()));

    assertThat(results).isEmpty();
  }

  @Test
  void selectTreeByQuery_does_not_use_db_when_query_returns_empty() {
    DbSession dbSession = mock(DbSession.class);
    MeasureTreeQuery query = mock(MeasureTreeQuery.class);
    when(query.returnsEmpty()).thenReturn(true);

    List<MeasureDto> results = new ArrayList<>();
    ResultHandler<MeasureDto> resultHandler = context -> results.add(context.getResultObject());
    underTest.selectTreeByQuery(dbSession, new ComponentDto(), query, resultHandler);

    assertThat(results).isEmpty();
    verifyNoInteractions(dbSession);
  }

  @Test
  void findNclocOfBiggestBranch() {
    ProjectData projectData = db.components().insertPrivateProject();
    BranchDto branch1 = projectData.getMainBranchDto();
    BranchDto branch2 = db.components().insertProjectBranch(projectData.getProjectDto());

    // Insert measures for each branch and for a random component on branch1
    MetricDto metric = db.measures().insertMetric(metricDto -> metricDto.setKey(CoreMetrics.NCLOC_KEY));
    MeasureDto measure1 = newMeasure(branch1, metric, 3);
    MeasureDto measure2 = newMeasure(branch2, metric, 4);
    MeasureDto measure3 = newMeasure(db.components().insertFile(branch1), metric, 6);

    underTest.insertOrUpdate(db.getSession(), measure1);
    underTest.insertOrUpdate(db.getSession(), measure2);
    underTest.insertOrUpdate(db.getSession(), measure3);

    assertThat(underTest.findNclocOfBiggestBranch(db.getSession(), List.of(branch1.getUuid(), branch2.getUuid())))
      .isEqualTo(4);
  }

  private MeasureDto newMeasureForMetrics(ComponentDto componentDto, MetricDto... metrics) {
    return db.measures().insertMeasure(componentDto,
      m -> Arrays.stream(metrics).forEach(metric -> m.addValue(metric.getKey(), RandomUtils.nextInt(50))));
  }

  private void verifyTableSize(int expectedSize) {
    assertThat(db.countRowsOfTable(db.getSession(), "measures")).isEqualTo(expectedSize);
  }

  private void verifyPersisted(MeasureDto dto) {
    assertThat(underTest.selectByComponentUuid(db.getSession(), dto.getComponentUuid())).hasValueSatisfying(selected -> {
      assertThat(selected).usingRecursiveComparison().isEqualTo(dto);
    });
  }

  private static double getDoubleValue() {
    return RandomUtils.nextInt(100);
  }
}
