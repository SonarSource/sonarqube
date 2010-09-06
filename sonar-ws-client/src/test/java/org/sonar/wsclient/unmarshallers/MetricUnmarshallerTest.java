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
package org.sonar.wsclient.unmarshallers;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.wsclient.services.Metric;

import java.io.IOException;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class MetricUnmarshallerTest {

  @Test
  public void toModel() throws IOException {
    Metric metric = new MetricUnmarshaller().toModel("[]");
    assertThat(metric, nullValue());

    metric = new MetricUnmarshaller().toModel(loadFile("/metrics/one_metric.json"));
    assertThat(metric.getKey(), is("ncloc"));
    assertThat(metric.getName(), is("Lines of code"));
    assertThat(metric.getDescription(), is("Non Commenting Lines of Code"));
    assertThat(metric.getType(), is("INT"));
    assertTrue(metric.getHidden());
  }



  @Test
  public void toModels() throws IOException {
    Collection<Metric> metrics = new MetricUnmarshaller().toModels("[]");
    assertThat(metrics.size(), is(0));

    metrics = new MetricUnmarshaller().toModels(loadFile("/metrics/one_metric.json"));
    assertThat(metrics.size(), is(1));

    metrics = new MetricUnmarshaller().toModels(loadFile("/metrics/many_metrics.json"));
    assertThat(metrics.size(), is(10));
  }

  private static String loadFile(String path) throws IOException {
    return IOUtils.toString(MetricUnmarshallerTest.class.getResourceAsStream(path));
  }
}
