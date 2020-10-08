/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.metric.ws;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.measures.MetricFinder;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.test.JsonAssert.assertJson;

public class UserMetricsActionTest {

  MetricFinder metrics = mock(MetricFinder.class);

  WsActionTester ws = new WsActionTester(new UserMetricsAction(metrics));

  @Test
  public void test_definition() {
    assertThat(ws.getDef().key()).isEqualTo("user_metrics");
    assertThat(ws.getDef().isInternal()).isTrue();
    assertThat(ws.getDef().responseExampleAsString()).isNotEmpty();
    assertThat(ws.getDef().params()).isEmpty();
  }

  @Test
  public void should_list_manual_metrics() {
    Metric m1 = mock(Metric.class);
    when(m1.getUserManaged()).thenReturn(true);
    when(m1.getKey()).thenReturn("m1");
    when(m1.getName()).thenReturn("Metric 1");
    when(m1.getType()).thenReturn(ValueType.STRING);
    Metric m2 = mock(Metric.class);
    when(m2.getUserManaged()).thenReturn(false);
    Metric m3 = mock(Metric.class);
    when(m3.getUserManaged()).thenReturn(true);
    when(m3.getKey()).thenReturn("m3");
    when(m3.getName()).thenReturn("Metric 3");
    when(m3.getType()).thenReturn(ValueType.STRING);
    Metric m4 = mock(Metric.class);
    when(m4.getUserManaged()).thenReturn(true);
    when(m4.getKey()).thenReturn("m3");
    when(m4.getName()).thenReturn("Metric 4");
    when(m4.getType()).thenReturn(ValueType.INT);
    when(metrics.findAll()).thenReturn(Lists.newArrayList(m1, m2, m3, m4));

    assertJson(ws.newRequest().execute().getInput()).isSimilarTo(getClass().getResource("UserMetricsActionTest/app.json"));
  }
}
