/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.resources;

import java.util.List;
import java.util.NoSuchElementException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.Metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.DIRECTORIES;
import static org.sonar.api.measures.CoreMetrics.NCLOC;
import static org.sonar.api.measures.CoreMetrics.getMetric;
import static org.sonar.api.measures.CoreMetrics.getMetrics;

public class CoreMetricsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void read_metrics_from_class_reflection() {
    List<Metric> metrics = getMetrics();
    assertThat(metrics.size()).isGreaterThan(100);
    assertThat(metrics).contains(NCLOC, DIRECTORIES);
  }

  @Test
  public void get_metric_by_key() throws Exception {
    Metric metric = getMetric("ncloc");
    assertThat(metric.getKey()).isEqualTo("ncloc");
  }

  @Test
  public void fail_get_unknown_metric_by_key() throws Exception {
    expectedException.expect(NoSuchElementException.class);
    getMetric("unknown");
  }
}
