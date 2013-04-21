/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.resources;

import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CoreMetricsTest {
  @Test
  public void shouldReadMetricsFromClassReflection() {
    List<Metric> metrics = CoreMetrics.getMetrics();
    assertTrue(metrics.size() > 10);
    assertTrue(metrics.contains(CoreMetrics.NCLOC));
    assertTrue(metrics.contains(CoreMetrics.DIRECTORIES));
  }

  @Test
  public void shouldExcludeDeprecatedIsoMetrics() {
    List<Metric> metrics = CoreMetrics.getMetrics();
    assertFalse(metrics.contains(CoreMetrics.USABILITY));
    assertFalse(metrics.contains(CoreMetrics.EFFICIENCY));
    assertFalse(metrics.contains(CoreMetrics.RELIABILITY));
    assertFalse(metrics.contains(CoreMetrics.PORTABILITY));
    assertFalse(metrics.contains(CoreMetrics.MAINTAINABILITY));
  }
}
