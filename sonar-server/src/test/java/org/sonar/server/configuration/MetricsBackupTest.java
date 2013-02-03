/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.server.configuration;

import org.junit.Test;
import org.sonar.jpa.dao.MeasuresDao;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import org.sonar.api.measures.Metric;

import java.util.Arrays;
import java.util.Collection;

import static org.fest.assertions.Assertions.assertThat;

public class MetricsBackupTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldExportMetrics() {
    MetricsBackup metricsBackup = new MetricsBackup();
    SonarConfig sonarConfig = new SonarConfig();
    Collection<Metric> metrics = createMetrics();
    metricsBackup.exportXml(sonarConfig, metrics);

    assertThat(sonarConfig.getMetrics()).isEqualTo(metrics);
  }

  @Test
  public void shouldImportMetrics() {
    MeasuresDao measuresDao = new MeasuresDao(getSession());
    Collection<Metric> oldMetrics = createMetrics();
    measuresDao.registerMetrics(oldMetrics);

    MetricsBackup metricsBackup = new MetricsBackup(getSession());
    SonarConfig sonarConfig = new SonarConfig();

    Collection<Metric> importedMetrics = createNewMetrics();
    sonarConfig.setMetrics(importedMetrics);
    metricsBackup.importXml(sonarConfig);

    Collection<Metric> allMetrics = measuresDao.getMetrics();
    assertThat(allMetrics).hasSize(4);

    Collection<Metric> enabledMetrics = measuresDao.getEnabledMetrics();
    assertThat(enabledMetrics).hasSize(3);

  }

  private Collection<Metric> createMetrics() {
    Metric m1 = new Metric("metric1");
    m1.setDescription("old desc metric1");
    m1.setEnabled(true);
    m1.setOrigin(Metric.Origin.GUI);

    Metric m2 = new Metric("metric2");
    m2.setDescription("old desc metric2");
    m2.setEnabled(true);
    m2.setOrigin(Metric.Origin.WS);

    Metric m3 = new Metric("metric3");
    m3.setDescription("desc metric3");
    m3.setEnabled(true);
    m3.setOrigin(Metric.Origin.WS);

    return Arrays.asList(m1, m2, m3);
  }

  private Collection<Metric> createNewMetrics() {
    Metric m1 = new Metric("metric1");
    m1.setDescription("new desc metric1");
    m1.setOrigin(Metric.Origin.WS);

    Metric m2 = new Metric("metric2");
    m2.setDescription("new desc metric2");
    m2.setOrigin(Metric.Origin.WS);

    Metric m3 = new Metric("new metric");
    m3.setOrigin(Metric.Origin.WS);

    return Arrays.asList(m1, m2, m3);
  }
}
