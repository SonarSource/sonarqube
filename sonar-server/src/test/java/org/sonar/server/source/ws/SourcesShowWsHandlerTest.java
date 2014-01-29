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

package org.sonar.server.source.ws;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.server.ws.WsTester;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.source.SourceService;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SourcesShowWsHandlerTest {

  @Mock
  SourceService sourceService;

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    tester = new WsTester(new SourcesWs(new SourcesShowWsHandler(sourceService)));
  }

  @Test
  public void show_source() throws Exception {
    String componentKey = "org.apache.struts:struts:Dispatcher";
    when(sourceService.getSourcesByComponent(eq(componentKey), anyInt(), anyInt())).thenReturn(newArrayList(
      "/*",
      " * Header",
      " */",
      "",
      "public class <span class=\"sym-31 sym\">HelloWorld</span> {",
      "}"
    ));

    WsTester.TestRequest request = tester.newRequest("show").setParam("key", componentKey);
    request.execute().assertJson(getClass(), "show_source.json");
  }

  @Test
  public void fail_to_show_source_if_no_source_found() throws Exception {
    String componentKey = "org.apache.struts:struts:Dispatcher";
    when(sourceService.getSourcesByComponent(anyString(), anyInt(), anyInt())).thenReturn(null);

    try {
      WsTester.TestRequest request = tester.newRequest("show").setParam("key", componentKey);
      request.execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
  }

  @Test
  public void show_source_with_from_and_to_params() throws Exception {
    String componentKey = "org.apache.struts:struts:Dispatcher";
    when(sourceService.getSourcesByComponent(componentKey, 3, 5)).thenReturn(newArrayList(
      " */",
      "",
      "public class <span class=\"sym-31 sym\">HelloWorld</span> {"
    ));
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", componentKey).setParam("from", "3").setParam("to", "5");
    request.execute().assertJson(getClass(), "show_source_with_params_from_and_to.json");
  }

  @Test
  public void show_source_always_should_not_begin_with_from_0() throws Exception {
    String componentKey = "org.apache.struts:struts:Dispatcher";
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", componentKey).setParam("from", "0").setParam("to", "5");
    request.execute();
    verify(sourceService).getSourcesByComponent(componentKey, 1, 5);
  }

  @Test
  public void show_source_with_scm() throws Exception {
    String componentKey = "org.apache.struts:struts:Dispatcher";
    when(sourceService.getSourcesByComponent(eq(componentKey), anyInt(), anyInt())).thenReturn(newArrayList(
      "public class <span class=\"sym-31 sym\">HelloWorld</span> {}"
    ));

    when(sourceService.getScmAuthorData(componentKey)).thenReturn("1=julien;");
    when(sourceService.getScmDateData(componentKey)).thenReturn("1=2013-03-13T16:22:31+0100;");

    WsTester.TestRequest request = tester.newRequest("show").setParam("key", componentKey);
    request.execute().assertJson(getClass(), "show_source_with_scm.json");
  }

  @Test
  public void show_source_with_scm_with_from_and_to_params() throws Exception {
    String componentKey = "org.apache.struts:struts:Dispatcher";
    when(sourceService.getSourcesByComponent(componentKey, 3, 5)).thenReturn(newArrayList(
      " */",
      "",
      "public class <span class=\"sym-31 sym\">HelloWorld</span> {"
    ));
    when(sourceService.getScmAuthorData(componentKey))
      .thenReturn("1=julien;2=simon;3=julien;4=simon;5=jean;6=julien");
    when(sourceService.getScmDateData(componentKey))
      .thenReturn("1=2013-03-13T16:22:31+0100;2=2013-03-14T16:22:31+0100;3=2013-03-13T16:22:31+0100;4=2013-03-14T16:22:31+0100;5=2013-03-15T16:22:31+0100;6=2013-03-13T16:22:31+0100;");

    WsTester.TestRequest request = tester.newRequest("show").setParam("key", componentKey).setParam("from", "3").setParam("to", "5");
    request.execute().assertJson(getClass(), "show_source_with_scm_with_from_and_to_params.json");
  }

  @Test
  public void show_source_with_scm_without_repeating_same_lines() throws Exception {
    String componentKey = "org.apache.struts:struts:Dispatcher";
    when(sourceService.getSourcesByComponent(eq(componentKey), anyInt(), anyInt())).thenReturn(newArrayList(
      " */",
      "",
      "public class <span class=\"sym-31 sym\">HelloWorld</span> {"
    ));
    when(sourceService.getScmAuthorData(componentKey))
      .thenReturn("1=julien;2=julien;3=simon");
    when(sourceService.getScmDateData(componentKey))
      .thenReturn("1=2013-03-13T16:22:31+0100;2=2013-03-13T16:22:31+0100;3=2013-03-14T16:22:31+0100;");
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", componentKey);
    request.execute().assertJson(getClass(), "show_source_with_scm_without_repeating_same_lines.json");
  }
}
