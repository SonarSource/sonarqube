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
package org.sonar.server.rule.ws;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WsTester;
import org.sonar.core.rule.RuleTagDto;
import org.sonar.server.rule.RuleTags;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RuleTagsWsTest {

  @Mock
  RuleTags ruleTags;

  WsTester tester;

  @Before
  public void setUp() {
    tester = new WsTester(new RuleTagsWs(ruleTags));
  }

  @Test
  public void define_ws() throws Exception {
    WebService.Controller controller = tester.controller("api/rule_tags");
    assertThat(controller).isNotNull();
    assertThat(controller.path()).isEqualTo("api/rule_tags");
    assertThat(controller.description()).isNotEmpty();

    WebService.Action search = controller.action("list");
    assertThat(search).isNotNull();
    assertThat(search.key()).isEqualTo("list");
    assertThat(search.handler()).isNotNull();
    assertThat(search.since()).isEqualTo("4.2");
    assertThat(search.isPost()).isFalse();

    WebService.Action create = controller.action("create");
    assertThat(create).isNotNull();
    assertThat(create.key()).isEqualTo("create");
    assertThat(create.handler()).isNotNull();
    assertThat(create.since()).isEqualTo("4.2");
    assertThat(create.isPost()).isTrue();
    assertThat(create.params()).hasSize(1);
    assertThat(create.param("tag")).isNotNull();
  }

  @Test
  public void list_tags() throws Exception {
    when(ruleTags.listAllTags()).thenReturn(ImmutableList.of("tag1", "tag2", "tag3"));
    tester.newRequest("list").execute().assertJson(getClass(), "list.json");
    verify(ruleTags).listAllTags();
  }

  @Test
  public void create_ok() throws Exception {
    String tag = "newtag";
    Long tagId = 42L;
    RuleTagDto newTag = new RuleTagDto().setId(tagId).setTag(tag);
    when(ruleTags.create("newtag")).thenReturn(newTag);

    WsTester.TestRequest request = tester.newRequest("create").setParam("tag", tag);
    request.execute().assertJson(getClass(), "create_ok.json");
    verify(ruleTags).create(tag);
  }

  @Test
  public void create_missing_parameter() throws Exception {
    WsTester.TestRequest request = tester.newRequest("create");
    try {
      request.execute();
      fail();
    } catch (IllegalArgumentException e) {
      verifyZeroInteractions(ruleTags);
    }
  }
}
