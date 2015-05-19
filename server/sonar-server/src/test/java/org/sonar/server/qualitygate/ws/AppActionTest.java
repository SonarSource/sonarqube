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
package org.sonar.server.qualitygate.ws;

import com.google.common.collect.ImmutableList;
import org.json.simple.JSONValue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.api.i18n.I18n;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.core.timemachine.Periods;
import org.sonar.server.qualitygate.QgateProjectFinder;
import org.sonar.server.qualitygate.QualityGates;
import org.sonar.server.ws.WsTester;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AppActionTest {

  @Mock
  private QualityGates qGates;

  @Mock
  private Periods periods;

  @Mock
  private I18n i18n;

  WsTester tester;

  @Before
  public void setUp() {
    tester = new WsTester(new QGatesWs(
      new ListAction(qGates), new ShowAction(qGates), new SearchAction(mock(QgateProjectFinder.class)),
      new CreateAction(qGates), new CopyAction(qGates), new DestroyAction(qGates), new RenameAction(qGates),
      new SetAsDefaultAction(qGates), new UnsetDefaultAction(qGates),
      new CreateConditionAction(qGates), new UpdateConditionAction(qGates), new DeleteConditionAction(qGates),
      new SelectAction(qGates), new DeselectAction(qGates), new AppAction(qGates, periods, i18n)));
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void should_initialize_app() throws Exception {
    doAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        return (String) invocation.getArguments()[1];
      }
    }).when(i18n).message(any(Locale.class), any(String.class), any(String.class));

    Metric metric = mock(Metric.class);
    when(metric.getId()).thenReturn(42);
    when(metric.getKey()).thenReturn("metric");
    when(metric.getName()).thenReturn("Metric");
    when(metric.getType()).thenReturn(ValueType.BOOL);
    when(metric.getDomain()).thenReturn("General");
    when(metric.isHidden()).thenReturn(false);
    when(qGates.gateMetrics()).thenReturn(ImmutableList.of(metric));

    String json = tester.newGetRequest("api/qualitygates", "app").execute().outputAsString();

    Map responseJson = (Map) JSONValue.parse(json);
    assertThat((Boolean) responseJson.get("edit")).isFalse();
    Collection<Map> periods = (Collection<Map>) responseJson.get("periods");
    assertThat(periods).hasSize(5);
    Collection<Map> metrics = (Collection<Map>) responseJson.get("metrics");
    assertThat(metrics).hasSize(1);
    Map metricMap = metrics.iterator().next();
    assertThat(metricMap.get("id").toString()).isEqualTo("42");
    assertThat(metricMap.get("key")).isEqualTo("metric");
    assertThat(metricMap.get("name")).isEqualTo("Metric");
    assertThat(metricMap.get("type")).isEqualTo("BOOL");
    assertThat(metricMap.get("domain")).isEqualTo("General");
    assertThat(metricMap.get("hidden")).isEqualTo(false);
  }
}
