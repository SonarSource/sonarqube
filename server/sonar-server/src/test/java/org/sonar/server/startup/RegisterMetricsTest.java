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
package org.sonar.server.startup;

import java.util.Collections;
import java.util.List;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.qualitygate.db.QualityGateConditionDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.metric.persistence.MetricDao;
import org.sonar.test.DbTests;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class RegisterMetricsTest {

  @ClassRule
  public static DbTester dbTester = new DbTester();

  /**
   * Insert new metrics, including custom metrics
   */
  @Test
  public void insert_new_metrics() {
    dbTester.prepareDbUnit(getClass(), "insert_new_metrics.xml");

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

    RegisterMetrics register = new RegisterMetrics(dbClient());
    register.register(asList(m1, custom));
    dbTester.assertDbUnit(getClass(), "insert_new_metrics-result.xml", "metrics");
  }

  /**
   * Update existing metrics, except if custom metric
   */
  @Test
  public void update_non_custom_metrics() {
    dbTester.prepareDbUnit(getClass(), "update_non_custom_metrics.xml");

    RegisterMetrics register = new RegisterMetrics(dbClient());
    Metric m1 = new Metric.Builder("m1", "New name", Metric.ValueType.FLOAT)
      .setDescription("new description")
      .setDirection(-1)
      .setQualitative(true)
      .setDomain("new domain")
      .setUserManaged(false)
      .setHidden(true)
      .create();
    Metric custom = new Metric.Builder("custom", "New custom", Metric.ValueType.FLOAT)
      .setDescription("New description of custom metric")
      .setUserManaged(true)
      .create();
    register.register(asList(m1, custom));

    dbTester.assertDbUnit(getClass(), "update_non_custom_metrics-result.xml", "metrics");
  }

  @Test
  public void disable_undefined_metrics() {
    dbTester.prepareDbUnit(getClass(), "disable_undefined_metrics.xml");

    RegisterMetrics register = new RegisterMetrics(dbClient());
    register.register(Collections.<Metric>emptyList());

    dbTester.assertDbUnit(getClass(), "disable_undefined_metrics-result.xml", "metrics");
  }

  @Test
  public void insert_core_metrics() {
    dbTester.truncateTables();

    RegisterMetrics register = new RegisterMetrics(dbClient());
    register.start();

    assertThat(dbTester.countRowsOfTable("metrics")).isEqualTo(CoreMetrics.getMetrics().size());
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_duplicated_plugin_metrics() throws Exception {
    Metrics plugin1 = new TestMetrics(new Metric.Builder("m1", "In first plugin", Metric.ValueType.FLOAT).create());
    Metrics plugin2 = new TestMetrics(new Metric.Builder("m1", "In second plugin", Metric.ValueType.FLOAT).create());

    new RegisterMetrics(dbClient(), new Metrics[]{plugin1, plugin2}).start();
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_plugin_duplicates_core_metric() throws Exception {
    Metrics plugin = new TestMetrics(new Metric.Builder("ncloc", "In plugin", Metric.ValueType.FLOAT).create());

    new RegisterMetrics(dbClient(), new Metrics[]{plugin}).start();
  }

  private DbClient dbClient() {
    return new DbClient(dbTester.database(), dbTester.myBatis(), new MetricDao(), new QualityGateConditionDao(dbTester.myBatis()));
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
}
