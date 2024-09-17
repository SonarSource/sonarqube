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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.metric.MetricDto;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.api.measures.Metric.ValueType.DATA;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.measure.MeasureTesting.newLiveMeasure;

class LiveMeasureDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final LiveMeasureDao underTest = db.getDbClient().liveMeasureDao();
  private MetricDto metric;

  private int branchId = 0;

  @BeforeEach
  void setUp() {
    metric = db.measures().insertMetric();
  }

  @Test
  void selectByComponentUuidsAndMetricUuids() {
    LiveMeasureDto measure1 = newLiveMeasure().setMetricUuid(metric.getUuid());
    LiveMeasureDto measure2 = newLiveMeasure().setMetricUuid(metric.getUuid());
    underTest.insert(db.getSession(), measure1);
    underTest.insert(db.getSession(), measure2);

    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricUuids(db.getSession(),
      asList(measure1.getComponentUuid(), measure2.getComponentUuid()), singletonList(metric.getUuid()));
    assertThat(selected)
      .extracting(LiveMeasureDto::getComponentUuid, LiveMeasureDto::getProjectUuid, LiveMeasureDto::getMetricUuid,
        LiveMeasureDto::getValue, LiveMeasureDto::getDataAsString)
      .containsExactlyInAnyOrder(
        tuple(measure1.getComponentUuid(), measure1.getProjectUuid(), measure1.getMetricUuid(), measure1.getValue(),
          measure1.getDataAsString()),
        tuple(measure2.getComponentUuid(), measure2.getProjectUuid(), measure2.getMetricUuid(), measure2.getValue(),
          measure2.getDataAsString()));

    assertThat(underTest.selectByComponentUuidsAndMetricUuids(db.getSession(), emptyList(), singletonList(metric.getUuid()))).isEmpty();
    assertThat(underTest.selectByComponentUuidsAndMetricUuids(db.getSession(), singletonList(measure1.getComponentUuid()), emptyList())).isEmpty();
  }

  @Test
  void selectByComponentUuidsAndMetricUuids_returns_empty_list_if_metric_does_not_match() {
    LiveMeasureDto measure = newLiveMeasure().setMetricUuid(metric.getUuid());
    underTest.insert(db.getSession(), measure);

    String otherMetricUuid = metric.getUuid() + "other";
    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricUuids(db.getSession(),
      singletonList(measure.getComponentUuid()), singletonList(otherMetricUuid));

    assertThat(selected).isEmpty();
  }

  @Test
  void selectByComponentUuidsAndMetricUuids_returns_empty_list_if_component_does_not_match() {
    LiveMeasureDto measure = newLiveMeasure();
    underTest.insert(db.getSession(), measure);

    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricUuids(db.getSession(), singletonList("_missing_"),
      singletonList(measure.getMetricUuid()));

    assertThat(selected).isEmpty();
  }

  @Test
  void selectByComponentUuidAndMetricKey() {
    LiveMeasureDto measure = newLiveMeasure().setMetricUuid(metric.getUuid());
    underTest.insert(db.getSession(), measure);

    Optional<LiveMeasureDto> selected = underTest.selectMeasure(db.getSession(), measure.getComponentUuid(), metric.getKey());

    assertThat(selected).isNotEmpty();
    assertThat(selected.get()).isEqualToComparingFieldByField(measure);
  }

  @Test
  void selectByComponentUuidAndMetricKey_return_empty_if_component_does_not_match() {
    LiveMeasureDto measure = newLiveMeasure().setMetricUuid(metric.getUuid());
    underTest.insert(db.getSession(), measure);

    assertThat(underTest.selectMeasure(db.getSession(), "_missing_", metric.getKey())).isEmpty();
  }

  @Test
  void selectByComponentUuidAndMetricKey_return_empty_if_metric_does_not_match() {
    LiveMeasureDto measure = newLiveMeasure().setMetricUuid(metric.getUuid());
    underTest.insert(db.getSession(), measure);

    assertThat(underTest.selectMeasure(db.getSession(), measure.getComponentUuid(), "_missing_")).isEmpty();
  }

  @Test
  void selectMeasure() {
    MetricDto metric = db.measures().insertMetric();
    LiveMeasureDto stored = newLiveMeasure().setMetricUuid(metric.getUuid());
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
  void selectMeasure_map_fields() {
    MetricDto metric = db.measures().insertMetric();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    underTest.insert(db.getSession(), newLiveMeasure(file, metric).setValue(3.14).setData("text_value"));

    LiveMeasureDto result =
      underTest.selectMeasure(db.getSession(), file.uuid(), metric.getKey()).orElseThrow(() -> new IllegalArgumentException("Measure not " +
        "found"));

    assertThat(result).as("Fail to map fields of %s", result.toString()).extracting(
        LiveMeasureDto::getProjectUuid, LiveMeasureDto::getComponentUuid, LiveMeasureDto::getMetricUuid, LiveMeasureDto::getValue,
        LiveMeasureDto::getDataAsString, LiveMeasureDto::getTextValue)
      .contains(project.uuid(), file.uuid(), metric.getUuid(), 3.14, "text_value", "text_value");
  }

  @Test
  void insert_data() {
    byte[] data = "text_value".getBytes(StandardCharsets.UTF_8);
    MetricDto metric = db.measures().insertMetric();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    LiveMeasureDto measure = newLiveMeasure(file, metric).setData(data);

    underTest.insert(db.getSession(), measure);

    LiveMeasureDto result =
      underTest.selectMeasure(db.getSession(), file.uuid(), metric.getKey()).orElseThrow(() -> new IllegalArgumentException("Measure not " +
        "found"));
    assertThat(new String(result.getData(), StandardCharsets.UTF_8)).isEqualTo("text_value");
    assertThat(result.getDataAsString()).isEqualTo("text_value");
  }

  @Test
  void insertOrUpdate() {
    // insert
    LiveMeasureDto dto = newLiveMeasure();
    underTest.insertOrUpdate(db.getSession(), dto);
    verifyPersisted(dto);
    verifyTableSize(1);

    // update
    dto.setValue(dto.getValue() + 1);
    dto.setData(dto.getDataAsString() + "_new");
    underTest.insertOrUpdate(db.getSession(), dto);
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  void deleteByComponentUuidExcludingMetricUuids() {
    LiveMeasureDto measure1 = newLiveMeasure().setComponentUuid("C1").setMetricUuid("1");
    LiveMeasureDto measure2 = newLiveMeasure().setComponentUuid("C1").setMetricUuid("2");
    LiveMeasureDto measure3 = newLiveMeasure().setComponentUuid("C1").setMetricUuid("3");
    LiveMeasureDto measureOtherComponent = newLiveMeasure().setComponentUuid("C2").setMetricUuid("3");
    underTest.insertOrUpdate(db.getSession(), measure1);
    underTest.insertOrUpdate(db.getSession(), measure2);
    underTest.insertOrUpdate(db.getSession(), measure3);
    underTest.insertOrUpdate(db.getSession(), measureOtherComponent);

    underTest.deleteByComponentUuidExcludingMetricUuids(db.getSession(), "C1", Arrays.asList("1", "2"));

    verifyTableSize(3);
    verifyPersisted(measure1);
    verifyPersisted(measure2);
    verifyPersisted(measureOtherComponent);
  }

  @Test
  void deleteByComponentUuidExcludingMetricUuids_with_empty_metrics() {
    LiveMeasureDto measure1 = newLiveMeasure().setComponentUuid("C1").setMetricUuid("1");
    LiveMeasureDto measure2 = newLiveMeasure().setComponentUuid("C1").setMetricUuid("2");
    LiveMeasureDto measureOnOtherComponent = newLiveMeasure().setComponentUuid("C2").setMetricUuid("2");
    underTest.insertOrUpdate(db.getSession(), measure1);
    underTest.insertOrUpdate(db.getSession(), measure2);
    underTest.insertOrUpdate(db.getSession(), measureOnOtherComponent);

    underTest.deleteByComponentUuidExcludingMetricUuids(db.getSession(), "C1", Collections.emptyList());

    verifyTableSize(1);
    verifyPersisted(measureOnOtherComponent);
  }

  @Test
  void upsert_inserts_or_updates_row() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }

    // insert
    LiveMeasureDto dto = newLiveMeasure();
    int count = underTest.upsert(db.getSession(), dto);
    verifyPersisted(dto);
    verifyTableSize(1);
    assertThat(count).isOne();

    // update
    dto.setValue(dto.getValue() + 1);
    dto.setData(dto.getDataAsString() + "_new");
    count = underTest.upsert(db.getSession(), dto);
    assertThat(count).isOne();
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  void upsert_does_not_update_row_if_values_are_not_changed() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }

    LiveMeasureDto dto = newLiveMeasure();
    underTest.upsert(db.getSession(), dto);

    // update
    int count = underTest.upsert(db.getSession(), dto);
    assertThat(count).isZero();
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  void upsert_updates_row_if_lob_data_is_changed() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }

    LiveMeasureDto dto = newLiveMeasure().setData(RandomStringUtils.random(10_000));
    underTest.upsert(db.getSession(), dto);

    // update
    dto.setData(RandomStringUtils.random(dto.getDataAsString().length() + 10));
    int count = underTest.upsert(db.getSession(), dto);
    assertThat(count).isOne();
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  void upsert_does_not_update_row_if_lob_data_is_not_changed() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }
    LiveMeasureDto dto = newLiveMeasure().setData(RandomStringUtils.random(10_000));
    underTest.upsert(db.getSession(), dto);

    // update
    int count = underTest.upsert(db.getSession(), dto);
    assertThat(count).isZero();
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  void upsert_updates_row_if_lob_data_is_removed() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }

    LiveMeasureDto dto = newLiveMeasure().setData(RandomStringUtils.random(10_000));
    underTest.upsert(db.getSession(), dto);

    // update
    dto.setData((String) null);
    int count = underTest.upsert(db.getSession(), dto);
    assertThat(count).isOne();
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  void upsert_updates_row_if_value_is_changed() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }
    LiveMeasureDto dto = newLiveMeasure().setValue(40.0);
    underTest.upsert(db.getSession(), dto);

    // update
    dto.setValue(50.0);
    int count = underTest.upsert(db.getSession(), dto);
    assertThat(count).isOne();
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  void upsert_updates_row_if_value_is_removed() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }
    LiveMeasureDto dto = newLiveMeasure().setValue(40.0);
    underTest.upsert(db.getSession(), dto);

    // update
    dto.setValue(null);
    int count = underTest.upsert(db.getSession(), dto);
    assertThat(count).isOne();
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  void upsert_updates_row_if_value_is_added() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }
    LiveMeasureDto dto = newLiveMeasure().setValue(null);
    underTest.upsert(db.getSession(), dto);

    // update
    dto.setValue(40.0);
    int count = underTest.upsert(db.getSession(), dto);
    assertThat(count).isOne();
    verifyPersisted(dto);
    verifyTableSize(1);
  }

  @Test
  void upsert_multiple_rows() {
    if (!db.getDbClient().getDatabase().getDialect().supportsUpsert()) {
      return;
    }

    // insert 30
    List<LiveMeasureDto> inserted = new ArrayList<>();
    IntStream.range(0, 30).forEach(i -> inserted.add(newLiveMeasure()));
    for (LiveMeasureDto dto : inserted) {
      underTest.upsert(db.getSession(), dto);
    }
    verifyTableSize(30);

    // update 10 with new values, update 5 without any change and insert new 50
    List<LiveMeasureDto> upserted = new ArrayList<>();
    IntStream.range(0, 10).forEach(i -> {
      LiveMeasureDto d = inserted.get(i);
      upserted.add(d.setValue(d.getValue() + 123));
    });
    upserted.addAll(inserted.subList(10, 15));
    IntStream.range(0, 50).forEach(i -> upserted.add(newLiveMeasure()));
    for (LiveMeasureDto dto : upserted) {
      underTest.upsert(db.getSession(), dto);
    }
    db.getSession().commit();
    verifyTableSize(80);
  }

  private void verifyTableSize(int expectedSize) {
    assertThat(db.countRowsOfTable(db.getSession(), "live_measures")).isEqualTo(expectedSize);
  }

  private void verifyPersisted(LiveMeasureDto dto) {
    List<LiveMeasureDto> selected = underTest.selectByComponentUuidsAndMetricUuids(db.getSession(), singletonList(dto.getComponentUuid())
      , singletonList(dto.getMetricUuid()));
    assertThat(selected).hasSize(1);
    assertThat(selected.get(0)).isEqualToComparingOnlyGivenFields(dto,
      // do not compare the field "uuid", which is used only for insert, not select
      "componentUuid", "projectUuid", "metricUuid", "value", "textValue", "data");
  }
}
