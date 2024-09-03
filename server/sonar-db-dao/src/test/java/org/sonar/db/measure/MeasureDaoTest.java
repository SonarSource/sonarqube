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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.sonar.db.measure.MeasureTesting.newMeasure;

class MeasureDaoTest {

  @RegisterExtension
  public final DbTester db = DbTester.create(System2.INSTANCE);

  private final MeasureDao underTest = db.getDbClient().measureDao();

  @Test
  void insert_measure() {
    MeasureDto dto = newMeasure();
    int count = underTest.insert(db.getSession(), dto);
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

    assertThat(underTest.selectMeasure(db.getSession(), dto.getComponentUuid()))
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
  void select_measure() {
    MeasureDto measure1 = newMeasure();
    MeasureDto measure2 = newMeasure();
    underTest.insert(db.getSession(), measure1);
    underTest.insert(db.getSession(), measure2);

    assertThat(underTest.selectMeasure(db.getSession(), measure1.getComponentUuid()))
      .hasValueSatisfying(selected -> assertThat(selected).usingRecursiveComparison().isEqualTo(measure1));
    assertThat(underTest.selectMeasure(db.getSession(), "unknown-component")).isEmpty();
  }

  @Test
  void selectMeasure_with_single_metric() {
    String metricKey = "metric1";
    String value = "foo";
    MeasureDto measure1 = newMeasure().addValue(metricKey, value);
    MeasureDto measure2 = newMeasure();
    underTest.insert(db.getSession(), measure1);
    underTest.insert(db.getSession(), measure2);

    assertThat(underTest.selectMeasure(db.getSession(), measure1.getComponentUuid(), metricKey))
      .hasValueSatisfying(selected -> assertThat(selected.getMetricValues()).containsOnly(entry(metricKey, value)));

    assertThat(underTest.selectMeasure(db.getSession(), "unknown-component", metricKey)).isEmpty();
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
  void select_branch_measure_hashes() {
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

    assertThat(underTest.selectBranchMeasureHashes(db.getSession(), "b1"))
      .containsOnly(new MeasureHash("c1", hash1), new MeasureHash("c2", hash2));
  }

  private void verifyTableSize(int expectedSize) {
    assertThat(db.countRowsOfTable(db.getSession(), "measures")).isEqualTo(expectedSize);
  }

  private void verifyPersisted(MeasureDto dto) {
    assertThat(underTest.selectMeasure(db.getSession(), dto.getComponentUuid())).hasValueSatisfying(selected -> {
      assertThat(selected).usingRecursiveComparison().isEqualTo(dto);
    });
  }

  private static double getDoubleValue() {
    return RandomUtils.nextInt(100);
  }
}
