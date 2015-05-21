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

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.core.qualitygate.db.QualityGateConditionDao;
import org.sonar.jpa.dao.MeasuresDao;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RegisterMetricsTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldSaveIfNew() {
    setupData("shouldSaveIfNew");

    Metric metric1 = new Metric.Builder("new1", "short1", Metric.ValueType.FLOAT)
      .setDescription("desc1")
      .setDirection(1)
      .setQualitative(true)
      .setDomain("domain1")
      .setUserManaged(false)
      .create();
    Metric metric2 = new Metric.Builder("new2", "short2", Metric.ValueType.FLOAT)
      .setDescription("desc2")
      .setDirection(1)
      .setQualitative(true)
      .setDomain("domain2")
      .setUserManaged(false)
      .create();

    RegisterMetrics synchronizer = new RegisterMetrics(new MeasuresDao(getSession()), mock(QualityGateConditionDao.class), new Metrics[0]);
    synchronizer.register(Arrays.asList(metric1, metric2));
    checkTables("shouldSaveIfNew", "metrics");
  }

  @Test
  public void shouldUpdateIfAlreadyExists() {
    setupData("shouldUpdateIfAlreadyExists");

    RegisterMetrics synchronizer = new RegisterMetrics(new MeasuresDao(getSession()), mock(QualityGateConditionDao.class), new Metrics[0]);
    synchronizer.register(Lists.<Metric>newArrayList(new Metric.Builder("key", "new short name", Metric.ValueType.FLOAT)
      .setDescription("new description")
      .setDirection(-1)
      .setQualitative(true)
      .setDomain("new domain")
      .setUserManaged(false)
      .create()));

    checkTables("shouldUpdateIfAlreadyExists", "metrics");
  }

  @Test
  public void shouldAddUserManagesMetric() {
    Metrics metrics = mock(Metrics.class);
    when(metrics.getMetrics()).thenReturn(Lists.<Metric>newArrayList(new Metric.Builder("key", "new short name", Metric.ValueType.FLOAT)
      .setDescription("new description")
      .setDirection(-1)
      .setQualitative(true)
      .setDomain("new domain")
      .setUserManaged(true)
      .create()));

    MeasuresDao measuresDao = new MeasuresDao(getSession());
    RegisterMetrics loader = new RegisterMetrics(measuresDao, mock(QualityGateConditionDao.class), new Metrics[] {metrics});
    List<Metric> result = loader.getMetricsRepositories();

    assertThat(result).hasSize(1);
  }

  @Test
  public void shouldNotUpdateUserManagesMetricIfAlreadyExists() {
    setupData("shouldNotUpdateUserManagesMetricIfAlreadyExists");

    Metrics metrics = mock(Metrics.class);
    when(metrics.getMetrics()).thenReturn(Lists.<Metric>newArrayList(new Metric.Builder("key", "new short name", Metric.ValueType.FLOAT)
      .setDescription("new description")
      .setDirection(-1)
      .setQualitative(true)
      .setDomain("new domain")
      .setUserManaged(true)
      .create()));

    MeasuresDao measuresDao = new MeasuresDao(getSession());
    RegisterMetrics loader = new RegisterMetrics(measuresDao, mock(QualityGateConditionDao.class), new Metrics[] {metrics});
    List<Metric> result = loader.getMetricsRepositories();

    assertThat(result).isEmpty();
  }

  @Test
  public void shouldEnableOnlyLoadedMetrics() {
    setupData("shouldEnableOnlyLoadedMetrics");

    MeasuresDao measuresDao = new MeasuresDao(getSession());
    RegisterMetrics loader = new RegisterMetrics(measuresDao, mock(QualityGateConditionDao.class), new Metrics[0]);
    loader.start();

    assertThat(measuresDao.getMetric("deprecated").getEnabled()).isFalse();
    assertThat(measuresDao.getMetric(CoreMetrics.COMPLEXITY_KEY).getEnabled()).isTrue();
  }

  @Test
  public void clean_quality_gate_conditions() {
    QualityGateConditionDao conditionDao = mock(QualityGateConditionDao.class);
    RegisterMetrics loader = new RegisterMetrics(new MeasuresDao(getSession()), conditionDao, new Metrics[0]);
    loader.cleanAlerts();
    verify(conditionDao).deleteConditionsWithInvalidMetrics();
  }
}
