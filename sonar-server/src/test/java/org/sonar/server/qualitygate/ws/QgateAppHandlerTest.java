/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.json.simple.JSONValue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.api.i18n.I18n;
import org.sonar.api.server.ws.WsTester;
import org.sonar.core.timemachine.Periods;
import org.sonar.server.qualitygate.QualityGates;

import java.util.*;
import java.util.Map.Entry;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;

@RunWith(MockitoJUnitRunner.class)
public class QgateAppHandlerTest {

  @Mock
  private QualityGates qGates;

  @Mock
  private Periods periods;

  @Mock
  private I18n i18n;

  WsTester tester;

  @Before
  public void setUp() {
    tester = new WsTester(new QualityGatesWs(qGates, null, new QgateAppHandler(qGates, periods, i18n)));
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
    String json = tester.newRequest("app").execute().outputAsString();
    Map responseJson = (Map) JSONValue.parse(json);
    assertThat((Boolean) responseJson.get("edit")).isFalse();
    Collection<Map> periods = (Collection<Map>) responseJson.get("periods");
    assertThat(periods).hasSize(3);
    Map messages = (Map) responseJson.get("messages");
    assertThat(messages).isNotNull().isNotEmpty().hasSize(45);
    for (Entry message: (Set<Entry>) messages.entrySet()) {
      assertThat(message.getKey()).isEqualTo(message.getValue());
    }
  }
}
