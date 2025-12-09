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
package org.sonar.db.metric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.sonar.db.metric.MetricTesting.newMetricDto;

class MetricDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final DbSession dbSession = db.getSession();
  private final MetricDao underTest = new MetricDao();

  @Test
  void select_by_key_enabled_metric() {
    MetricDto expected = db.measures().insertMetric(t -> t.setEnabled(true));

    assertEquals(expected, underTest.selectByKey(dbSession, expected.getKey()));
  }

  @Test
  void select_by_key_disabled_metric() {
    MetricDto expected = db.measures().insertMetric(t -> t.setEnabled(false));

    assertEquals(expected, underTest.selectByKey(dbSession, expected.getKey()));
  }

  @Test
  void select_or_fail_by_key() {
    assertThatThrownBy(() -> underTest.selectOrFailByKey(dbSession, "unknown"))
      .isInstanceOf(RowNotFoundException.class);
  }

  @Test
  void find_all_enabled() {
    List<MetricDto> enabledMetrics = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> MetricTesting.newMetricDto().setEnabled(true))
      .toList();
    List<MetricDto> disabledMetrics = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> MetricTesting.newMetricDto().setEnabled(false))
      .toList();

    List<MetricDto> all = new ArrayList<>(enabledMetrics);
    all.addAll(disabledMetrics);
    Collections.shuffle(all);
    all.forEach(metricDto -> db.getDbClient().metricDao().insert(db.getSession(), metricDto));
    db.getSession().commit();

    assertThat(underTest.selectEnabled(dbSession))
      .extracting(MetricDto::getUuid)
      .containsOnly(enabledMetrics.stream().map(MetricDto::getUuid).toArray(String[]::new));
  }

  @Test
  void find_all() {
    List<MetricDto> enabledMetrics = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> MetricTesting.newMetricDto().setEnabled(true))
      .toList();
    List<MetricDto> disabledMetrics = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> MetricTesting.newMetricDto().setEnabled(false))
      .toList();

    List<MetricDto> all = new ArrayList<>(enabledMetrics);
    all.addAll(disabledMetrics);
    Collections.shuffle(all);
    all.forEach(metricDto -> db.getDbClient().metricDao().insert(db.getSession(), metricDto));
    db.getSession().commit();

    assertThat(underTest.selectEnabled(dbSession))
      .extracting(MetricDto::getUuid)
      .containsOnly(enabledMetrics.stream().map(MetricDto::getUuid).toArray(String[]::new));
  }

  @Test
  void insert() {
    underTest.insert(dbSession, new MetricDto()
      .setUuid(Uuids.createFast())
      .setKey("coverage")
      .setShortName("Coverage")
      .setDescription("Coverage by unit tests")
      .setDomain("Tests")
      .setValueType("PERCENT")
      .setQualitative(true)

      .setWorstValue(0d)
      .setBestValue(100d)
      .setOptimizedBestValue(true)
      .setDirection(1)
      .setHidden(true)
      .setDeleteHistoricalData(true)
      .setEnabled(true));

    MetricDto result = underTest.selectByKey(dbSession, "coverage");
    assertThat(result.getUuid()).isNotNull();
    assertThat(result.getKey()).isEqualTo("coverage");
    assertThat(result.getShortName()).isEqualTo("Coverage");
    assertThat(result.getDescription()).isEqualTo("Coverage by unit tests");
    assertThat(result.getDomain()).isEqualTo("Tests");
    assertThat(result.getValueType()).isEqualTo("PERCENT");
    assertThat(result.getDirection()).isOne();
    assertThat(result.isQualitative()).isTrue();
    assertThat(result.getWorstValue()).isEqualTo(0d);
    assertThat(result.getBestValue()).isEqualTo(100d);
    assertThat(result.isOptimizedBestValue()).isTrue();
    assertThat(result.isDeleteHistoricalData()).isTrue();
    assertThat(result.isHidden()).isTrue();
    assertThat(result.isEnabled()).isTrue();
  }

  @Test
  void insert_metrics() {
    underTest.insert(dbSession, new MetricDto()
        .setUuid(Uuids.createFast())
        .setKey("coverage")
        .setShortName("Coverage")
        .setDescription("Coverage by unit tests")
        .setDomain("Tests")
        .setValueType("PERCENT")
        .setQualitative(true)

        .setWorstValue(0d)
        .setBestValue(100d)
        .setOptimizedBestValue(true)
        .setDirection(1)
        .setHidden(true)
        .setDeleteHistoricalData(true)
        .setEnabled(true),
      new MetricDto()
        .setUuid(Uuids.createFast())
        .setKey("ncloc")
        .setShortName("ncloc")
        .setDescription("ncloc")
        .setDomain("Tests")
        .setValueType("INT")
        .setQualitative(true)

        .setWorstValue(0d)
        .setBestValue(100d)
        .setOptimizedBestValue(true)
        .setDirection(1)
        .setHidden(true)
        .setDeleteHistoricalData(true)
        .setEnabled(true));
    dbSession.commit();

    assertThat(db.countRowsOfTable("metrics")).isEqualTo(2);
  }

  @Test
  void selectByUuid() {
    MetricDto metric = underTest.insert(dbSession, newMetricDto());

    MetricDto result = underTest.selectByUuid(dbSession, metric.getUuid());

    assertThat(result).isNotNull();
  }

  @Test
  void selectByUuids() {
    MetricDto metric1 = underTest.insert(dbSession, newMetricDto());
    MetricDto metric2 = underTest.insert(dbSession, newMetricDto());

    List<MetricDto> result = underTest.selectByUuids(dbSession, newHashSet(metric1.getUuid(), metric2.getUuid()));

    assertThat(result).hasSize(2);
  }

  @Test
  void update() {
    MetricDto metric = underTest.insert(dbSession, newMetricDto().setKey("first-key"));

    underTest.update(dbSession, metric.setKey("second-key"));

    MetricDto result = underTest.selectByKey(dbSession, "second-key");
    assertThat(result).isNotNull();
  }

  @Test
  void countEnabled() {
    underTest.insert(dbSession, newMetricDto().setEnabled(true));
    underTest.insert(dbSession, newMetricDto().setEnabled(true));
    underTest.insert(dbSession, newMetricDto().setEnabled(false));

    int result = underTest.countEnabled(dbSession);

    assertThat(result).isEqualTo(2);
  }

  @Test
  void selectByKeys() {
    underTest.insert(dbSession, newMetricDto().setKey("first-key"));
    underTest.insert(dbSession, newMetricDto().setKey("second-key"));
    underTest.insert(dbSession, newMetricDto().setKey("third-key"));

    List<MetricDto> result = underTest.selectByKeys(dbSession, Arrays.asList("first-key", "second-key", "third-key"));

    assertThat(result).hasSize(3)
      .extracting("key").containsOnly("first-key", "second-key", "third-key");
  }

  @Test
  void disableByKey() {
    underTest.insert(dbSession, newMetricDto().setKey("metric-key").setEnabled(true));

    boolean updated = underTest.disableByKey(dbSession, "metric-key");
    assertThat(updated).isTrue();

    MetricDto result = underTest.selectByKey(dbSession, "metric-key");
    assertThat(result.isEnabled()).isFalse();

    // disable again -> zero rows are touched
    updated = underTest.disableByKey(dbSession, "metric-key");
    assertThat(updated).isFalse();
  }

  @Test
  void selectOrFailByKey() {
    underTest.insert(dbSession, newMetricDto().setKey("metric-key"));

    MetricDto result = underTest.selectOrFailByKey(dbSession, "metric-key");

    assertThat(result).isNotNull();
    assertThat(result.getKey()).isEqualTo("metric-key");
  }

  @Test
  void selectEnabled_with_paging() {
    underTest.insert(dbSession, newMetricDto().setEnabled(true));
    underTest.insert(dbSession, newMetricDto().setEnabled(true));
    underTest.insert(dbSession, newMetricDto().setEnabled(true));
    underTest.insert(dbSession, newMetricDto().setEnabled(false));

    List<MetricDto> result = underTest.selectEnabled(dbSession, 1, 2);

    assertThat(result).hasSize(2);
  }

  private void assertEquals(MetricDto expected, MetricDto result) {
    assertThat(result.getKey()).isEqualTo(expected.getKey());
    assertThat(result.getShortName()).isEqualTo(expected.getShortName());
    assertThat(result.getDescription()).isEqualTo(expected.getDescription());
    assertThat(result.getDomain()).isEqualTo(expected.getDomain());
    assertThat(result.getValueType()).isEqualTo(expected.getValueType());
    assertThat(result.getDirection()).isEqualTo(expected.getDirection());
    assertThat(result.isQualitative()).isEqualTo(expected.isQualitative());
    assertThat(result.getWorstValue()).isCloseTo(expected.getWorstValue(), within(0.001d));
    assertThat(result.getBestValue()).isCloseTo(expected.getBestValue(), within(0.001d));
    assertThat(result.isOptimizedBestValue()).isEqualTo(expected.isOptimizedBestValue());
    assertThat(result.isDeleteHistoricalData()).isEqualTo(expected.isDeleteHistoricalData());
    assertThat(result.isHidden()).isEqualTo(expected.isHidden());
    assertThat(result.isEnabled()).isEqualTo(expected.isEnabled());
    assertThat(result.getDecimalScale()).isEqualTo(expected.getDecimalScale());
  }
}
