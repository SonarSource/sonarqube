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
package org.sonar.server.issue.ws;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.server.issue.IssueService;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IssueTagsActionTest {

  @Mock
  private IssueService service;

  private TagsAction tagsAction;

  private WsTester tester;

  @Before
  public void setUp() {
    tagsAction = new TagsAction(service);
    tester = new WsTester(new IssuesWs(tagsAction));
  }

  @Test
  public void should_define() {
    Action action = tester.controller("api/issues").action("tags");
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.handler()).isEqualTo(tagsAction);
    assertThat(action.params()).hasSize(2);

    Param query = action.param("q");
    assertThat(query.isRequired()).isFalse();
    assertThat(query.description()).isNotEmpty();
    assertThat(query.exampleValue()).isNotEmpty();
    Param pageSize = action.param("ps");
    assertThat(pageSize.isRequired()).isFalse();
    assertThat(pageSize.defaultValue()).isEqualTo("10");
    assertThat(pageSize.description()).isNotEmpty();
    assertThat(pageSize.exampleValue()).isNotEmpty();
  }

  @Test
  public void should_return_empty_list() throws Exception {
    tester.newGetRequest("api/issues", "tags").execute().assertJson("{\"tags\":[]}");
  }

  @Test
  public void should_return_tag_list() throws Exception {
    when(service.listTags("polop", 5)).thenReturn(Lists.newArrayList("tag1", "tag2", "tag3", "tag4", "tag5"));
    tester.newGetRequest("api/issues", "tags").setParam("q", "polop").setParam("ps", "5").execute()
      .assertJson("{\"tags\":[\"tag1\", \"tag2\", \"tag3\", \"tag4\", \"tag5\"]}");
    verify(service).listTags("polop", 5);
  }
}
