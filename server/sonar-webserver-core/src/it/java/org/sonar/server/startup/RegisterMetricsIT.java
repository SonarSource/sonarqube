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
package org.sonar.server.startup;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RegisterMetricsIT {

  public static final int SOON_TO_BE_REMOVED_COMPLEXITY_METRICS_COUNT = 7;
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final UuidFactory uuidFactory = new SequenceUuidFactory();
  private final DbClient dbClient = dbTester.getDbClient();
  private final RegisterMetrics register = new RegisterMetrics(dbClient, uuidFactory);

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

    register.register(asList(m1, custom));

    Map<String, MetricDto> metricsByKey = selectAllMetrics();
    assertThat(metricsByKey).hasSize(2);
    assertEquals(m1, metricsByKey.get("m1"));
    assertEquals(custom, metricsByKey.get("custom"));
  }

  /**
   * Update existing metrics
   */
  @Test
  public void update_metrics() {
    dbTester.measures().insertMetric(t -> t.setKey("m1")
      .setShortName("name")
      .setValueType(Metric.ValueType.INT.name())
      .setDescription("old desc")
      .setDomain("old domain")
      .setShortName("old short name")
      .setQualitative(false)
      .setEnabled(true)
      .setOptimizedBestValue(false)
      .setDirection(1)
      .setHidden(false));

    Metric m1 = new Metric.Builder("m1", "New name", Metric.ValueType.FLOAT)
      .setDescription("new description")
      .setDirection(-1)
      .setQualitative(true)
      .setDomain("new domain")
      .setUserManaged(false)
      .setDecimalScale(3)
      .setHidden(true)
      .create();
    register.register(asList(m1));

    Map<String, MetricDto> metricsByKey = selectAllMetrics();
    assertThat(metricsByKey).hasSize(1);
    assertEquals(m1, metricsByKey.get("m1"));
  }

  @Test
  public void disable_undefined_metrics() {
    Random random = new Random();
    int count = 1 + random.nextInt(10);
    IntStream.range(0, count)
      .forEach(t -> dbTester.measures().insertMetric(m -> m.setEnabled(random.nextBoolean())));

    register.register(Collections.emptyList());

    assertThat(selectAllMetrics().values().stream())
      .extracting(MetricDto::isEnabled)
      .containsOnly(IntStream.range(0, count).mapToObj(t -> false).toArray(Boolean[]::new));
  }

  @Test
  public void enable_disabled_metrics() {
    MetricDto enabledMetric = dbTester.measures().insertMetric(t -> t.setEnabled(true));
    MetricDto disabledMetric = dbTester.measures().insertMetric(t -> t.setEnabled(false));

    register.register(asList(builderOf(enabledMetric).create(), builderOf(disabledMetric).create()));

    assertThat(selectAllMetrics().values())
      .extracting(MetricDto::isEnabled)
      .containsOnly(true, true);
  }

  @Test
  public void insert_core_metrics_without_removed_metric() {
    register.start();

    assertThat(dbTester.countRowsOfTable("metrics"))
      .isEqualTo(CoreMetrics.getMetrics().size()
        // Metric CoreMetrics.WONT_FIX_ISSUES was renamed to CoreMetrics.ACCEPTED_ISSUES in 10.3.
        // We don't want to insert it anymore
        - 1
        // SONAR-12647 We are exclusing complexity metrics, they will be removed from the plugin API soon
        - SOON_TO_BE_REMOVED_COMPLEXITY_METRICS_COUNT);
  }

  @Test
  public void fail_if_duplicated_plugin_metrics() {
    Metrics plugin1 = new TestMetrics(new Metric.Builder("m1", "In first plugin", Metric.ValueType.FLOAT).create());
    Metrics plugin2 = new TestMetrics(new Metric.Builder("m1", "In second plugin", Metric.ValueType.FLOAT).create());

    assertThatThrownBy(() -> new RegisterMetrics(dbClient, uuidFactory, new Metrics[] {plugin1, plugin2}).start())
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void fail_if_plugin_duplicates_core_metric() {
    Metrics plugin = new TestMetrics(new Metric.Builder("ncloc", "In plugin", Metric.ValueType.FLOAT).create());

    assertThatThrownBy(() -> new RegisterMetrics(dbClient, uuidFactory, new Metrics[] {plugin}).start())
      .isInstanceOf(IllegalStateException.class);
  }

  private static class TestMetrics implements Metrics {
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
      .collect(Collectors.toMap(MetricDto::getKey, Function.identity()));
  }

  private void assertEquals(Metric expected, MetricDto actual) {
    assertThat(actual.getKey()).isEqualTo(expected.getKey());
    assertThat(actual.getShortName()).isEqualTo(expected.getName());
    assertThat(actual.getValueType()).isEqualTo(expected.getType().name());
    assertThat(actual.getDescription()).isEqualTo(expected.getDescription());
    assertThat(actual.getDirection()).isEqualTo(expected.getDirection());
    assertThat(actual.isQualitative()).isEqualTo(expected.getQualitative());
  }

  private static Metric.Builder builderOf(MetricDto enabledMetric) {
    return new Metric.Builder(enabledMetric.getKey(), enabledMetric.getShortName(), Metric.ValueType.valueOf(enabledMetric.getValueType()))
      .setDescription(enabledMetric.getDescription())
      .setDirection(enabledMetric.getDirection())
      .setQualitative(enabledMetric.isQualitative())
      .setQualitative(enabledMetric.isQualitative())
      .setDomain(enabledMetric.getDomain())
      .setHidden(enabledMetric.isHidden());
  }
}
