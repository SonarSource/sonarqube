/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.issue.ws;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.IssueQueryService;
import org.sonar.server.issue.IssueService;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ComponentTagsActionTest {

  @Mock
  private IssueService service;

  @Mock
  private IssueQueryService queryService;

  private ComponentTagsAction componentTagsAction;

  private WsTester tester;

  @Before
  public void setUp() {
    componentTagsAction = new ComponentTagsAction(service, queryService);
    tester = new WsTester(new IssuesWs(componentTagsAction));
  }

  @Test
  public void should_define() {
    Action action = tester.controller("api/issues").action("component_tags");
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.handler()).isEqualTo(componentTagsAction);
    assertThat(action.params()).hasSize(3);

    Param query = action.param("componentUuid");
    assertThat(query.isRequired()).isTrue();
    assertThat(query.description()).isNotEmpty();
    assertThat(query.exampleValue()).isNotEmpty();
    Param createdAfter = action.param("createdAfter");
    assertThat(createdAfter.isRequired()).isFalse();
    assertThat(createdAfter.description()).isNotEmpty();
    assertThat(createdAfter.exampleValue()).isNotEmpty();
    Param pageSize = action.param("ps");
    assertThat(pageSize.isRequired()).isFalse();
    assertThat(pageSize.defaultValue()).isEqualTo("10");
    assertThat(pageSize.description()).isNotEmpty();
    assertThat(pageSize.exampleValue()).isNotEmpty();
  }

  @Test
  public void should_return_empty_list() throws Exception {
    tester.newGetRequest("api/issues", "component_tags").setParam("componentUuid", "polop").execute().assertJson("{\"tags\":[]}");
  }

  @Test
  public void should_return_tag_list() throws Exception {
    Map<String, Long> tags = ImmutableMap.<String, Long>builder()
      .put("convention", 2771L)
      .put("brain-overload", 998L)
      .put("cwe", 89L)
      .put("bug", 32L)
      .put("cert", 2L)
      .build();
    IssueQuery query = mock(IssueQuery.class);
    ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
    when(queryService.createFromMap(captor.capture())).thenReturn(query);
    when(service.listTagsForComponent(query, 5)).thenReturn(tags);

    tester.newGetRequest("api/issues", "component_tags").setParam("componentUuid", "polop").setParam("ps", "5").execute()
      .assertJson(getClass(), "component-tags.json");
    assertThat(captor.getValue())
      .containsEntry("componentUuids", "polop")
      .containsEntry("resolved", false);
    verify(service).listTagsForComponent(query, 5);
  }

  @Test
  public void should_return_tag_list_with_created_after() throws Exception {
    Map<String, Long> tags = ImmutableMap.<String, Long>builder()
      .put("convention", 2771L)
      .put("brain-overload", 998L)
      .put("cwe", 89L)
      .put("bug", 32L)
      .put("cert", 2L)
      .build();
    IssueQuery query = mock(IssueQuery.class);
    ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
    when(queryService.createFromMap(captor.capture())).thenReturn(query);
    when(service.listTagsForComponent(query, 5)).thenReturn(tags);

    String componentUuid = "polop";
    String createdAfter = "2011-04-25";
    tester.newGetRequest("api/issues", "component_tags")
      .setParam("componentUuid", componentUuid)
      .setParam("createdAfter", createdAfter)
      .setParam("ps", "5")
      .execute()
      .assertJson(getClass(), "component-tags.json");
    assertThat(captor.getValue())
      .containsEntry("componentUuids", componentUuid)
      .containsEntry("resolved", false)
      .containsEntry("createdAfter", createdAfter);
    verify(service).listTagsForComponent(query, 5);
  }
}
