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
package org.sonar.server.startup;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

public class RegisterMetricsTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = dbTester.getDbClient();

  /**
   * Insert new metrics, including custom metrics
   */
  @Test
  public void insert_new_metrics() {
    Metric m1 = new Metric.Builder("m1", "One", Metric.ValueType.FLOAT)
      .setDescription("desc1")
      .setDirection(1)
      .setQualitative(true)
      .setDomain("domain1")
      .setUserManaged(false)
      .create();
    Metric custom = new Metric.Builder("custom", "Custom", Metric.ValueType.FLOAT)
      .setDescription("This is a custom metric")
      .setUserManaged(true)
      .create();

    RegisterMetrics register = new RegisterMetrics(dbClient);
    register.register(asList(m1, custom));

    Map<String, MetricDto> metricsByKey = selectAllMetrics();
    assertThat(metricsByKey).hasSize(2);
    assertEquals(m1, metricsByKey.get("m1"));
    assertEquals(custom, metricsByKey.get("custom"));
  }

  /**
   * Update existing metrics, except if custom metric
   */
  @Test
  public void update_non_custom_metrics() {
    dbTester.measures().insertMetric(t -> t.setKey("m1")
      .setShortName("name")
      .setValueType(Metric.ValueType.INT.name())
      .setDescription("old desc")
      .setDomain("old domain")
      .setShortName("old short name")
      .setQualitative(false)
      .setUserManaged(false)
      .setEnabled(true)
      .setOptimizedBestValue(false)
      .setDirection(1)
      .setHidden(false));
    MetricDto customMetric = dbTester.measures().insertMetric(t -> t.setKey("custom")
      .setValueType(Metric.ValueType.FLOAT.name())
      .setDescription("old desc")
      .setShortName("Custom")
      .setQualitative(false)
      .setUserManaged(true)
      .setEnabled(true)
      .setOptimizedBestValue(false)
      .setDirection(0)
      .setHidden(false)
      .setDecimalScale(1));

    RegisterMetrics register = new RegisterMetrics(dbClient);
    Metric m1 = new Metric.Builder("m1", "New name", Metric.ValueType.FLOAT)
      .setDescription("new description")
      .setDirection(-1)
      .setQualitative(true)
      .setDomain("new domain")
      .setUserManaged(false)
      .setDecimalScale(3)
      .setHidden(true)
      .create();
    Metric custom = new Metric.Builder("custom", "New custom", Metric.ValueType.FLOAT)
      .setDescription("New description of custom metric")
      .setUserManaged(true)
      .create();
    register.register(asList(m1, custom));

    Map<String, MetricDto> metricsByKey = selectAllMetrics();
    assertThat(metricsByKey).hasSize(2);
    assertEquals(m1, metricsByKey.get("m1"));
    MetricDto actual = metricsByKey.get("custom");
    assertThat(actual.getKey()).isEqualTo(custom.getKey());
    assertThat(actual.getShortName()).isEqualTo(customMetric.getShortName());
    assertThat(actual.getValueType()).isEqualTo(customMetric.getValueType());
    assertThat(actual.getDescription()).isEqualTo(customMetric.getDescription());
    assertThat(actual.getDirection()).isEqualTo(customMetric.getDirection());
    assertThat(actual.isQualitative()).isEqualTo(customMetric.isQualitative());
    assertThat(actual.isUserManaged()).isEqualTo(customMetric.isUserManaged());
  }

  @Test
  public void disable_undefined_metrics() {
    Random random = new Random();
    int count = 1 + random.nextInt(10);
    IntStream.range(0, count)
      .forEach(t -> dbTester.measures().insertMetric(m -> m.setEnabled(random.nextBoolean()).setUserManaged(false)));

    RegisterMetrics register = new RegisterMetrics(dbClient);
    register.register(Collections.emptyList());

    assertThat(selectAllMetrics().values().stream())
      .extracting(MetricDto::isEnabled)
      .containsOnly(IntStream.range(0, count).mapToObj(t -> false).toArray(Boolean[]::new));
  }

  @Test
  public void enable_disabled_metrics() {
    MetricDto enabledMetric = dbTester.measures().insertMetric(t -> t.setEnabled(true).setUserManaged(false));
    MetricDto disabledMetric = dbTester.measures().insertMetric(t -> t.setEnabled(false).setUserManaged(false));

    RegisterMetrics register = new RegisterMetrics(dbClient);
    register.register(asList(builderOf(enabledMetric).create(), builderOf(disabledMetric).create()));

    assertThat(selectAllMetrics().values())
      .extracting(MetricDto::isEnabled)
      .containsOnly(true, true);
  }

  @Test
  public void does_not_enable_disabled_custom_metrics() {
    MetricDto enabledMetric = dbTester.measures().insertMetric(t -> t.setEnabled(true).setUserManaged(true));
    MetricDto disabledMetric = dbTester.measures().insertMetric(t -> t.setEnabled(false).setUserManaged(true));

    RegisterMetrics register = new RegisterMetrics(dbClient);
    register.register(asList(builderOf(enabledMetric).create(), builderOf(disabledMetric).create()));

    assertThat(selectAllMetrics().values())
      .extracting(MetricDto::getKey, MetricDto::isEnabled)
      .containsOnly(
        tuple(enabledMetric.getKey(), true),
        tuple(disabledMetric.getKey(), false));
  }

  @Test
  public void insert_core_metrics() {
    RegisterMetrics register = new RegisterMetrics(dbClient);
    register.start();

    assertThat(dbTester.countRowsOfTable("metrics")).isEqualTo(CoreMetrics.getMetrics().size());
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_duplicated_plugin_metrics() {
    Metrics plugin1 = new TestMetrics(new Metric.Builder("m1", "In first plugin", Metric.ValueType.FLOAT).create());
    Metrics plugin2 = new TestMetrics(new Metric.Builder("m1", "In second plugin", Metric.ValueType.FLOAT).create());

    new RegisterMetrics(dbClient, new Metrics[] {plugin1, plugin2}).start();
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_plugin_duplicates_core_metric() {
    Metrics plugin = new TestMetrics(new Metric.Builder("ncloc", "In plugin", Metric.ValueType.FLOAT).create());

    new RegisterMetrics(dbClient, new Metrics[] {plugin}).start();
  }

  private class TestMetrics implements Metrics {
    private final List<Metric> metrics;

    public TestMetrics(Metric... metrics) {
      this.metrics = asList(metrics);
    }

    @Override
    public List<Metric> getMetrics() {
      return metrics;
    }
  }

  private Map<String, MetricDto> selectAllMetrics() {
    return dbTester.getDbClient().metricDao().selectAll(dbTester.getSession())
      .stream()
      .collect(uniqueIndex(MetricDto::getKey));
  }

  private void assertEquals(Metric expected, MetricDto actual) {
    assertThat(actual.getKey()).isEqualTo(expected.getKey());
    assertThat(actual.getShortName()).isEqualTo(expected.getName());
    assertThat(actual.getValueType()).isEqualTo(expected.getType().name());
    assertThat(actual.getDescription()).isEqualTo(expected.getDescription());
    assertThat(actual.getDirection()).isEqualTo(expected.getDirection());
    assertThat(actual.isQualitative()).isEqualTo(expected.getQualitative());
    assertThat(actual.isUserManaged()).isEqualTo(expected.getUserManaged());
  }

  private static Metric.Builder builderOf(MetricDto enabledMetric) {
    return new Metric.Builder(enabledMetric.getKey(), enabledMetric.getShortName(), Metric.ValueType.valueOf(enabledMetric.getValueType()))
      .setDescription(enabledMetric.getDescription())
      .setDirection(enabledMetric.getDirection())
      .setQualitative(enabledMetric.isQualitative())
      .setQualitative(enabledMetric.isQualitative())
      .setDomain(enabledMetric.getDomain())
      .setUserManaged(enabledMetric.isUserManaged())
      .setHidden(enabledMetric.isHidden());
  }
}
