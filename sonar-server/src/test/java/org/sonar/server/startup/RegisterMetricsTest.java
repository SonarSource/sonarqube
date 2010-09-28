/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.startup;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.jpa.dao.MeasuresDao;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RegisterMetricsTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldSaveIfNew() {
    setupData("shouldSaveIfNew");

    Metric metric1 = new Metric("new1", "short1", "desc1", Metric.ValueType.FLOAT, 1, true, "domain1", false);
    Metric metric2 = new Metric("new2", "short2", "desc2", Metric.ValueType.FLOAT, 1, true, "domain2", false);
    RegisterMetrics synchronizer = new RegisterMetrics(getSession(), new MeasuresDao(getSession()), null);
    synchronizer.register(Arrays.asList(metric1, metric2));
    checkTables("shouldSaveIfNew", "metrics");
  }

  @Test
  public void shouldUpdateIfAlreadyExists() {
    setupData("shouldUpdateIfAlreadyExists");

    final List<Metric> metrics = new ArrayList<Metric>();
    metrics.add(new Metric("key", "new short name", "new description", Metric.ValueType.FLOAT, -1, true, "new domain", false));
    RegisterMetrics synchronizer = new RegisterMetrics(getSession(), new MeasuresDao(getSession()), null);
    synchronizer.register(metrics);

    checkTables("shouldUpdateIfAlreadyExists", "metrics");
  }

  @Test
  public void enableOnlyLoadedMetrics() throws SQLException {
    setupData("enableOnlyLoadedMetrics");

    RegisterMetrics loader = new RegisterMetrics(getSession(), new MeasuresDao(getSession()), null);
    loader.start();

    assertFalse(getDao().getMeasuresDao().getMetric("deprecated").getEnabled());
    assertTrue(getDao().getMeasuresDao().getMetric(CoreMetrics.COMPLEXITY).getEnabled());
  }

  @Test
  public void cleanAlerts() throws SQLException {
    setupData("cleanAlerts");

    RegisterMetrics loader = new RegisterMetrics(getSession(), new MeasuresDao(getSession()), null);
    loader.cleanAlerts();

    checkTables("cleanAlerts", "metrics", "alerts");
  }
}
