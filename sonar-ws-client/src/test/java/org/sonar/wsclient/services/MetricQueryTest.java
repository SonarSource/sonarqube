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
package org.sonar.wsclient.services;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class MetricQueryTest extends QueryTestCase {

  @Test
  public void all() {
    assertThat(MetricQuery.all().getUrl(), is("/api/metrics?"));
    assertThat(MetricQuery.all().getModelClass().getName(), is(Metric.class.getName()));
  }

  @Test
  public void byKey() {
    assertThat(MetricQuery.byKey("ncloc").getUrl(), is("/api/metrics/ncloc?"));
    assertThat(MetricQuery.byKey("ncloc").getModelClass().getName(), is(Metric.class.getName()));
  }

}
