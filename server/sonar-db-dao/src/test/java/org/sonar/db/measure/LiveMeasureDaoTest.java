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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.metric.MetricDto;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.measure.MeasureTesting.newLiveMeasure;

public class LiveMeasureDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private LiveMeasureDao underTest = db.getDbClient().liveMeasureDao();
  private MetricDto metric;

  @Before
  public void setUp() throws Exception {
    metric = db.measures().insertMetric();
  }

  @Test
  public void test_selectByComponentUuidsAndMetricIds() {
    LiveMeasureDto measure1 = newLiveMeasure().setMetricId(metric.getId());
    LiveMeasureDto measure2 = newLiveMeasure().setMetricId(metric.getId());
    underTest.insert(db.getSession(), measure1);
    underTest.insert(db.getSession(), measure2);

    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricIds(db.getSession(),
      asList(measure1.getComponentUuid(), measure2.getComponentUuid()), singletonList(metric.getId()));
    assertThat(selected)
      .extracting(LiveMeasureDto::getComponentUuid, LiveMeasureDto::getProjectUuid, LiveMeasureDto::getMetricId, LiveMeasureDto::getValue, LiveMeasureDto::getDataAsString)
      .containsExactlyInAnyOrder(
        tuple(measure1.getComponentUuid(), measure1.getProjectUuid(), measure1.getMetricId(), measure1.getValue(), measure1.getDataAsString()),
        tuple(measure2.getComponentUuid(), measure2.getProjectUuid(), measure2.getMetricId(), measure2.getValue(), measure2.getDataAsString()));

    assertThat(underTest.selectByComponentUuidsAndMetricIds(db.getSession(), emptyList(), singletonList(metric.getId()))).isEmpty();
    assertThat(underTest.selectByComponentUuidsAndMetricIds(db.getSession(), singletonList(measure1.getComponentUuid()), emptyList())).isEmpty();
  }

  @Test
  public void selectByComponentUuidsAndMetricIds_returns_empty_list_if_metric_does_not_match() {
    LiveMeasureDto measure = newLiveMeasure().setMetricId(metric.getId());
    underTest.insert(db.getSession(), measure);

    int otherMetricId = metric.getId() + 100;
    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricIds(db.getSession(), singletonList(measure.getComponentUuid()), singletonList(otherMetricId));

    assertThat(selected).isEmpty();
  }

  @Test
  public void selectByComponentUuidsAndMetricIds_returns_empty_list_if_component_does_not_match() {
    LiveMeasureDto measure = newLiveMeasure();
    underTest.insert(db.getSession(), measure);

    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricIds(db.getSession(), singletonList("_missing_"), singletonList(measure.getMetricId()));

    assertThat(selected).isEmpty();
  }

  @Test
  public void test_selectByComponentUuidsAndMetricKeys() {
    LiveMeasureDto measure1 = newLiveMeasure().setMetricId(metric.getId());
    LiveMeasureDto measure2 = newLiveMeasure().setMetricId(metric.getId());
    underTest.insert(db.getSession(), measure1);
    underTest.insert(db.getSession(), measure2);

    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricKeys(db.getSession(), asList(measure1.getComponentUuid(), measure2.getComponentUuid()),
      singletonList(metric.getKey()));
    assertThat(selected)
      .extracting(LiveMeasureDto::getComponentUuid, LiveMeasureDto::getProjectUuid, LiveMeasureDto::getMetricId, LiveMeasureDto::getValue, LiveMeasureDto::getDataAsString)
      .containsExactlyInAnyOrder(
        tuple(measure1.getComponentUuid(), measure1.getProjectUuid(), measure1.getMetricId(), measure1.getValue(), measure1.getDataAsString()),
        tuple(measure2.getComponentUuid(), measure2.getProjectUuid(), measure2.getMetricId(), measure2.getValue(), measure2.getDataAsString()));

    assertThat(underTest.selectByComponentUuidsAndMetricKeys(db.getSession(), emptyList(), singletonList(metric.getKey()))).isEmpty();
    assertThat(underTest.selectByComponentUuidsAndMetricKeys(db.getSession(), singletonList(measure1.getComponentUuid()), emptyList())).isEmpty();
  }

  @Test
  public void selectByComponentUuidsAndMetricKeys_returns_empty_list_if_metric_does_not_match() {
    LiveMeasureDto measure = newLiveMeasure().setMetricId(metric.getId());
    underTest.insert(db.getSession(), measure);

    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricKeys(db.getSession(), singletonList(measure.getComponentUuid()), singletonList("_other_"));

    assertThat(selected).isEmpty();
  }

  @Test
  public void selectByComponentUuidsAndMetricKeys_returns_empty_list_if_component_does_not_match() {
    LiveMeasureDto measure = newLiveMeasure().setMetricId(metric.getId());
    underTest.insert(db.getSession(), measure);

    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricKeys(db.getSession(), singletonList("_missing_"), singletonList(metric.getKey()));

    assertThat(selected).isEmpty();
  }

  @Test
  public void test_selectMeasure() {
    MetricDto metric = db.measures().insertMetric();
    LiveMeasureDto stored = newLiveMeasure().setMetricId(metric.getId());
    underTest.insert(db.getSession(), stored);

    // metric exists but not component
    assertThat(underTest.selectMeasure(db.getSession(), "_missing_", metric.getKey())).isEmpty();

    // component exists but not metric
    assertThat(underTest.selectMeasure(db.getSession(), stored.getComponentUuid(), "_missing_")).isEmpty();

    // component and metric don't match
    assertThat(underTest.selectMeasure(db.getSession(), "_missing_", "_missing_")).isEmpty();

    // matches
    assertThat(underTest.selectMeasure(db.getSession(), stored.getComponentUuid(), metric.getKey()).get())
      .isEqualToComparingFieldByField(stored);
  }

  @Test
  public void selectTreeByQuery() {
    List<LiveMeasureDto> results = new ArrayList<>();
    MetricDto metric = db.measures().insertMetric();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    underTest.insert(db.getSession(), newLiveMeasure(file, metric).setValue(3.14));

    underTest.selectTreeByQuery(db.getSession(), project,
      MeasureTreeQuery.builder()
        .setMetricIds(singleton(metric.getId()))
        .setStrategy(MeasureTreeQuery.Strategy.LEAVES).build(),
      context -> results.add(context.getResultObject()));

    assertThat(results).hasSize(1);
    LiveMeasureDto result = results.get(0);
    assertThat(result.getComponentUuid()).isEqualTo(file.uuid());
    assertThat(result.getMetricId()).isEqualTo(metric.getId());
    assertThat(result.getValue()).isEqualTo(3.14);
  }

  @Test
  public void selectTreeByQuery_with_empty_results() {
    List<LiveMeasureDto> results = new ArrayList<>();
    underTest.selectTreeByQuery(db.getSession(), newPrivateProjectDto(db.getDefaultOrganization()),
      MeasureTreeQuery.builder().setStrategy(MeasureTreeQuery.Strategy.LEAVES).build(),
      context -> results.add(context.getResultObject()));

    assertThat(results).isEmpty();
  }

  @Test
  public void selectMeasure_map_fields() {
    MetricDto metric = db.measures().insertMetric();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    underTest.insert(db.getSession(), newLiveMeasure(file, metric).setValue(3.14).setVariation(0.1).setData("text_value"));

    LiveMeasureDto result = underTest.selectMeasure(db.getSession(), file.uuid(), metric.getKey()).orElseThrow(() -> new IllegalArgumentException("Measure not found"));

    assertThat(result).as("Fail to map fields of %s", result.toString()).extracting(
      LiveMeasureDto::getProjectUuid, LiveMeasureDto::getComponentUuid, LiveMeasureDto::getMetricId, LiveMeasureDto::getValue, LiveMeasureDto::getVariation,
      LiveMeasureDto::getDataAsString, LiveMeasureDto::getTextValue)
      .contains(project.uuid(), file.uuid(), metric.getId(), 3.14, 0.1, "text_value", "text_value");
  }

  @Test
  public void insert_data() {
    byte[] data = "text_value".getBytes(StandardCharsets.UTF_8);
    MetricDto metric = db.measures().insertMetric();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    LiveMeasureDto measure = newLiveMeasure(file, metric).setData(data);

    underTest.insert(db.getSession(), measure);


    LiveMeasureDto result = underTest.selectMeasure(db.getSession(), file.uuid(), metric.getKey()).orElseThrow(() -> new IllegalArgumentException("Measure not found"));
    assertThat(new String(result.getData(), StandardCharsets.UTF_8)).isEqualTo("text_value");
    assertThat(result.getDataAsString()).isEqualTo("text_value");
  }

  @Test
  public void test_insertOrUpdate() {
    // insert
    LiveMeasureDto dto = newLiveMeasure();
    underTest.insertOrUpdate(db.getSession(), dto, "foo");
    verifyPersisted(dto);
    verifyTableSize(1);

    // update
    dto.setValue(dto.getValue() + 1);
    dto.setVariation(dto.getVariation() + 10);
    dto.setData(dto.getDataAsString() + "_new");
    underTest.insertOrUpdate(db.getSession(), dto, "foo");
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  public void deleteByProjectUuidExcludingMarker() {
    LiveMeasureDto measure1 = newLiveMeasure().setProjectUuid("P1");
    LiveMeasureDto measure2 = newLiveMeasure().setProjectUuid("P1");
    LiveMeasureDto measure3DifferentMarker = newLiveMeasure().setProjectUuid("P1");
    LiveMeasureDto measure4NoMarker = newLiveMeasure().setProjectUuid("P1");
    LiveMeasureDto measure5OtherProject = newLiveMeasure().setProjectUuid("P2");
    underTest.insertOrUpdate(db.getSession(), measure1, "foo");
    underTest.insertOrUpdate(db.getSession(), measure2, "foo");
    underTest.insertOrUpdate(db.getSession(), measure3DifferentMarker, "bar");
    underTest.insertOrUpdate(db.getSession(), measure4NoMarker, null);
    underTest.insertOrUpdate(db.getSession(), measure5OtherProject, "foo");

    underTest.deleteByProjectUuidExcludingMarker(db.getSession(), "P1", "foo");

    verifyTableSize(3);
    verifyPersisted(measure1);
    verifyPersisted(measure2);
    verifyPersisted(measure5OtherProject);
  }

  private void verifyTableSize(int expectedSize) {
    assertThat(db.countRowsOfTable(db.getSession(), "live_measures")).isEqualTo(expectedSize);
  }

  private void verifyPersisted(LiveMeasureDto dto) {
    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricIds(db.getSession(), singletonList(dto.getComponentUuid()), singletonList(dto.getMetricId()));
    assertThat(selected).hasSize(1);
    assertThat(selected.get(0)).isEqualToComparingFieldByField(dto);
  }
}
