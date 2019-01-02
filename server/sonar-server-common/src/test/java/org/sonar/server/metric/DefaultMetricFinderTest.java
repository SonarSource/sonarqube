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
package org.sonar.server.metric;

import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.metric.MetricTesting.newMetricDto;


public class DefaultMetricFinderTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DefaultMetricFinder underTest = new DefaultMetricFinder(db.getDbClient());

  @Test
  public void findAll_enabled() {
    db.getDbClient().metricDao().insert(db.getSession(), newMetricDto());
    db.getDbClient().metricDao().insert(db.getSession(), newMetricDto());
    db.getDbClient().metricDao().insert(db.getSession(), newMetricDto().setEnabled(false));
    db.commit();

    assertThat(underTest.findAll()).hasSize(2);
  }

  @Test
  public void findAll_by_keys() {
    db.getDbClient().metricDao().insert(db.getSession(), newMetricDto().setKey("ncloc"));
    db.getDbClient().metricDao().insert(db.getSession(), newMetricDto().setKey("foo"));
    db.getDbClient().metricDao().insert(db.getSession(), newMetricDto().setKey("coverage"));
    db.commit();

    assertThat(underTest.findAll(Arrays.asList("ncloc", "foo"))).extracting(Metric::getKey).containsExactlyInAnyOrder("ncloc", "foo")
      .doesNotContain("coverage");

  }

  @Test
  public void findById() {
    MetricDto firstMetric = db.getDbClient().metricDao().insert(db.getSession(), newMetricDto());
    MetricDto secondMetric = db.getDbClient().metricDao().insert(db.getSession(), newMetricDto());
    db.commit();

    assertThat(underTest.findById(firstMetric.getId())).extracting(Metric::getKey).containsExactly(firstMetric.getKey());
  }

  @Test
  public void findByKey() {
    MetricDto firstMetric = db.getDbClient().metricDao().insert(db.getSession(), newMetricDto());
    MetricDto secondMetric = db.getDbClient().metricDao().insert(db.getSession(), newMetricDto());
    db.commit();

    assertThat(underTest.findByKey(secondMetric.getKey())).extracting(Metric::getKey).containsExactly(secondMetric.getKey());
  }
}
