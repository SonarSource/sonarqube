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
package org.sonar.db.metric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.sonar.db.metric.MetricTesting.newMetricDto;

public class MetricDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbSession dbSession = db.getSession();
  private MetricDao underTest = new MetricDao();

  @Test
  public void select_by_key_enabled_metric() {
    MetricDto expected = db.measures().insertMetric(t -> t.setEnabled(true).setUserManaged(false));

    assertEquals(expected, underTest.selectByKey(dbSession, expected.getKey()));
  }

  @Test
  public void select_by_key_disabled_metric() {
    MetricDto expected = db.measures().insertMetric(t -> t.setEnabled(false).setUserManaged(false));

    assertEquals(expected, underTest.selectByKey(dbSession, expected.getKey()));
  }

  @Test
  public void select_by_key_manual_metric() {
    MetricDto expected = db.measures().insertMetric(t -> t.setUserManaged(true));

    assertEquals(expected, underTest.selectByKey(dbSession, expected.getKey()));
  }

  @Test
  public void select_or_fail_by_key() {
    expectedException.expect(RowNotFoundException.class);

    underTest.selectOrFailByKey(dbSession, "unknown");
  }

  @Test
  public void find_all_enabled() {
    List<MetricDto> enabledMetrics = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> MetricTesting.newMetricDto().setEnabled(true))
      .collect(Collectors.toList());
    List<MetricDto> disabledMetrics = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> MetricTesting.newMetricDto().setEnabled(false))
      .collect(Collectors.toList());

    List<MetricDto> all = new ArrayList<>(enabledMetrics);
    all.addAll(disabledMetrics);
    Collections.shuffle(all);
    all.forEach(metricDto -> db.getDbClient().metricDao().insert(db.getSession(), metricDto));
    db.getSession().commit();

    assertThat(underTest.selectEnabled(dbSession))
      .extracting(MetricDto::getId)
      .containsOnly(enabledMetrics.stream().map(MetricDto::getId).toArray(Integer[]::new));
  }

  @Test
  public void find_all() {
    List<MetricDto> enabledMetrics = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> MetricTesting.newMetricDto().setEnabled(true))
      .collect(Collectors.toList());
    List<MetricDto> disabledMetrics = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> MetricTesting.newMetricDto().setEnabled(false))
      .collect(Collectors.toList());

    List<MetricDto> all = new ArrayList<>(enabledMetrics);
    all.addAll(disabledMetrics);
    Collections.shuffle(all);
    all.forEach(metricDto -> db.getDbClient().metricDao().insert(db.getSession(), metricDto));
    db.getSession().commit();

    assertThat(underTest.selectEnabled(dbSession))
      .extracting(MetricDto::getId)
      .containsOnly(enabledMetrics.stream().map(MetricDto::getId).toArray(Integer[]::new));
  }

  @Test
  public void insert() {
    underTest.insert(dbSession, new MetricDto()
      .setKey("coverage")
      .setShortName("Coverage")
      .setDescription("Coverage by unit tests")
      .setDomain("Tests")
      .setValueType("PERCENT")
      .setQualitative(true)
      .setUserManaged(true)
      .setWorstValue(0d)
      .setBestValue(100d)
      .setOptimizedBestValue(true)
      .setDirection(1)
      .setHidden(true)
      .setDeleteHistoricalData(true)
      .setEnabled(true));

    MetricDto result = underTest.selectByKey(dbSession, "coverage");
    assertThat(result.getId()).isNotNull();
    assertThat(result.getKey()).isEqualTo("coverage");
    assertThat(result.getShortName()).isEqualTo("Coverage");
    assertThat(result.getDescription()).isEqualTo("Coverage by unit tests");
    assertThat(result.getDomain()).isEqualTo("Tests");
    assertThat(result.getValueType()).isEqualTo("PERCENT");
    assertThat(result.getDirection()).isEqualTo(1);
    assertThat(result.isQualitative()).isTrue();
    assertThat(result.isUserManaged()).isTrue();
    assertThat(result.getWorstValue()).isEqualTo(0d);
    assertThat(result.getBestValue()).isEqualTo(100d);
    assertThat(result.isOptimizedBestValue()).isTrue();
    assertThat(result.isDeleteHistoricalData()).isTrue();
    assertThat(result.isHidden()).isTrue();
    assertThat(result.isEnabled()).isTrue();
  }

  @Test
  public void insert_metrics() {
    underTest.insert(dbSession, new MetricDto()
      .setKey("coverage")
      .setShortName("Coverage")
      .setDescription("Coverage by unit tests")
      .setDomain("Tests")
      .setValueType("PERCENT")
      .setQualitative(true)
      .setUserManaged(true)
      .setWorstValue(0d)
      .setBestValue(100d)
      .setOptimizedBestValue(true)
      .setDirection(1)
      .setHidden(true)
      .setDeleteHistoricalData(true)
      .setEnabled(true),
      new MetricDto()
        .setKey("ncloc")
        .setShortName("ncloc")
        .setDescription("ncloc")
        .setDomain("Tests")
        .setValueType("INT")
        .setQualitative(true)
        .setUserManaged(true)
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
  public void selectById() {
    MetricDto metric = underTest.insert(dbSession, newMetricDto());

    MetricDto result = underTest.selectById(dbSession, metric.getId());

    assertThat(result).isNotNull();
  }

  @Test
  public void selectByIds() {
    MetricDto metric1 = underTest.insert(dbSession, newMetricDto());
    MetricDto metric2 = underTest.insert(dbSession, newMetricDto());

    List<MetricDto> result = underTest.selectByIds(dbSession, newHashSet(metric1.getId(), metric2.getId()));

    assertThat(result).hasSize(2);
  }

  @Test
  public void update() {
    MetricDto metric = underTest.insert(dbSession, newMetricDto().setKey("first-key"));

    underTest.update(dbSession, metric.setKey("second-key"));

    MetricDto result = underTest.selectByKey(dbSession, "second-key");
    assertThat(result).isNotNull();
  }

  @Test
  public void countEnabled() {
    underTest.insert(dbSession, newMetricDto().setEnabled(true).setUserManaged(true));
    underTest.insert(dbSession, newMetricDto().setEnabled(true).setUserManaged(true));
    underTest.insert(dbSession, newMetricDto().setEnabled(false));

    int result = underTest.countEnabled(dbSession, true);

    assertThat(result).isEqualTo(2);
  }

  @Test
  public void selectDomains() {
    underTest.insert(dbSession, newMetricDto().setDomain("first-domain").setEnabled(true));
    underTest.insert(dbSession, newMetricDto().setDomain("second-domain").setEnabled(true));
    underTest.insert(dbSession, newMetricDto().setDomain("second-domain").setEnabled(true));
    underTest.insert(dbSession, newMetricDto().setDomain("third-domain").setEnabled(true));

    List<String> domains = underTest.selectEnabledDomains(dbSession);

    assertThat(domains).hasSize(3).containsOnly("first-domain", "second-domain", "third-domain");
  }

  @Test
  public void selectByKeys() {
    underTest.insert(dbSession, newMetricDto().setKey("first-key"));
    underTest.insert(dbSession, newMetricDto().setKey("second-key"));
    underTest.insert(dbSession, newMetricDto().setKey("third-key"));

    List<MetricDto> result = underTest.selectByKeys(dbSession, Arrays.asList("first-key", "second-key", "third-key"));

    assertThat(result).hasSize(3)
      .extracting("key").containsOnly("first-key", "second-key", "third-key");
  }

  @Test
  public void disableByIds() {
    MetricDto metric1 = underTest.insert(dbSession, newMetricDto().setEnabled(true).setUserManaged(true));
    MetricDto metric2 = underTest.insert(dbSession, newMetricDto().setEnabled(true).setUserManaged(true));

    underTest.disableCustomByIds(dbSession, Arrays.asList(metric1.getId(), metric2.getId()));

    List<MetricDto> result = underTest.selectByIds(dbSession, newHashSet(metric1.getId(), metric2.getId()));
    assertThat(result).hasSize(2);
    assertThat(result).extracting("enabled").containsOnly(false);
  }

  @Test
  public void disableByKey() {
    underTest.insert(dbSession, newMetricDto().setKey("metric-key").setEnabled(true).setUserManaged(true));

    boolean updated = underTest.disableCustomByKey(dbSession, "metric-key");
    assertThat(updated).isTrue();

    MetricDto result = underTest.selectByKey(dbSession, "metric-key");
    assertThat(result.isEnabled()).isFalse();

    // disable again -> zero rows are touched
    updated = underTest.disableCustomByKey(dbSession, "metric-key");
    assertThat(updated).isFalse();
  }

  @Test
  public void selectOrFailByKey() {
    underTest.insert(dbSession, newMetricDto().setKey("metric-key"));

    MetricDto result = underTest.selectOrFailByKey(dbSession, "metric-key");

    assertThat(result).isNotNull();
    assertThat(result.getKey()).isEqualTo("metric-key");
  }

  @Test
  public void selectEnabled_with_paging_and_custom() {
    underTest.insert(dbSession, newMetricDto().setUserManaged(true).setEnabled(true));
    underTest.insert(dbSession, newMetricDto().setUserManaged(true).setEnabled(true));
    underTest.insert(dbSession, newMetricDto().setUserManaged(true).setEnabled(true));
    underTest.insert(dbSession, newMetricDto().setUserManaged(false).setEnabled(true));
    underTest.insert(dbSession, newMetricDto().setUserManaged(true).setEnabled(false));

    List<MetricDto> result = underTest.selectEnabled(dbSession, true, 0, 100);

    assertThat(result).hasSize(3);
  }

  @Test
  public void selectAvailableByComponentUuid() {
    underTest.insert(dbSession, newMetricDto().setUserManaged(true).setEnabled(true).setKey("metric-key"));
    underTest.insert(dbSession, newMetricDto().setUserManaged(false).setEnabled(true).setKey("another-metric-key"));
    underTest.insert(dbSession, newMetricDto().setUserManaged(true).setEnabled(false).setKey("third-metric-key"));

    List<MetricDto> result = underTest.selectAvailableCustomMetricsByComponentUuid(dbSession, "project-uuid");

    assertThat(result).hasSize(1)
      .extracting("key").containsOnly("metric-key");
  }

  private void assertEquals(MetricDto expected, MetricDto result) {
    assertThat(result.getKey()).isEqualTo(expected.getKey());
    assertThat(result.getShortName()).isEqualTo(expected.getShortName());
    assertThat(result.getDescription()).isEqualTo(expected.getDescription());
    assertThat(result.getDomain()).isEqualTo(expected.getDomain());
    assertThat(result.getValueType()).isEqualTo(expected.getValueType());
    assertThat(result.getDirection()).isEqualTo(expected.getDirection());
    assertThat(result.isQualitative()).isEqualTo(expected.isQualitative());
    assertThat(result.isUserManaged()).isEqualTo(expected.isUserManaged());
    assertThat(result.getWorstValue()).isCloseTo(expected.getWorstValue(), within(0.001d));
    assertThat(result.getBestValue()).isCloseTo(expected.getBestValue(), within(0.001d));
    assertThat(result.isOptimizedBestValue()).isEqualTo(expected.isOptimizedBestValue());
    assertThat(result.isDeleteHistoricalData()).isEqualTo(expected.isDeleteHistoricalData());
    assertThat(result.isHidden()).isEqualTo(expected.isHidden());
    assertThat(result.isEnabled()).isEqualTo(expected.isEnabled());
    assertThat(result.getDecimalScale()).isEqualTo(expected.getDecimalScale());
    assertThat(result.isUserManaged()).isEqualTo(expected.isUserManaged());
  }
}
