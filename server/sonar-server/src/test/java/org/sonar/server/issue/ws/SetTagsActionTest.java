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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
public class SetTagsActionTest {

  @Mock
  IssueService service;
  SetTagsAction sut;
  WsTester tester;

  @Before
  public void setUp() {
    sut = new SetTagsAction(service);
    tester = new WsTester(new IssuesWs(sut));
  }

  @Test
  public void should_define() {
    Action action = tester.controller("api/issues").action("set_tags");
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExampleAsString()).isNull();
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.handler()).isEqualTo(sut);
    assertThat(action.params()).hasSize(2);

    Param query = action.param("key");
    assertThat(query.isRequired()).isTrue();
    assertThat(query.description()).isNotEmpty();
    assertThat(query.exampleValue()).isNotEmpty();
    Param pageSize = action.param("tags");
    assertThat(pageSize.isRequired()).isFalse();
    assertThat(pageSize.defaultValue()).isNull();
    assertThat(pageSize.description()).isNotEmpty();
    assertThat(pageSize.exampleValue()).isNotEmpty();
  }

  @Test
  public void should_set_tags() throws Exception {
    when(service.setTags("polop", ImmutableList.of("palap"))).thenReturn(ImmutableSet.of("palap"));
    tester.newPostRequest("api/issues", "set_tags").setParam("key", "polop").setParam("tags", "palap").execute()
      .assertJson("{\"tags\":[\"palap\"]}");
    verify(service).setTags("polop", ImmutableList.of("palap"));
  }
}
