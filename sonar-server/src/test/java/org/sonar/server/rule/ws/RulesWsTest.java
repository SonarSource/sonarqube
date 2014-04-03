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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WsTester;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class RulesWsTest {

  @Mock
  RuleSearchWsHandler searchHandler;

  @Mock
  RuleShowWsHandler showHandler;

  @Mock
  AddTagsWsHandler addTagsWsHandler;

  @Mock
  RemoveTagsWsHandler removeTagsWsHandler;

  WsTester tester;

  @Before
  public void setUp() {
    tester = new WsTester(new RulesWs(searchHandler, showHandler, addTagsWsHandler, removeTagsWsHandler));
  }

  @Test
  public void define_ws() throws Exception {
    WebService.Controller controller = tester.controller("api/rules");
    assertThat(controller).isNotNull();
    assertThat(controller.path()).isEqualTo("api/rules");
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(4);

    WebService.Action list = controller.action("list");
    assertThat(list).isNotNull();
    assertThat(list.handler()).isNotNull();
    assertThat(list.since()).isEqualTo("4.3");
    assertThat(list.isPost()).isFalse();
    assertThat(list.isInternal()).isFalse();
    assertThat(list.params()).hasSize(11);

    WebService.Action show = controller.action("show");
    assertThat(show).isNotNull();
    assertThat(show.handler()).isNotNull();
    assertThat(show.since()).isEqualTo("4.2");
    assertThat(show.isPost()).isFalse();
    assertThat(show.isInternal()).isFalse();

    WebService.Action addTags = controller.action("add_tags");
    assertThat(addTags).isNotNull();
    assertThat(addTags.handler()).isNotNull();
    assertThat(addTags.since()).isEqualTo("4.2");
    assertThat(addTags.isPost()).isTrue();
    assertThat(addTags.isInternal()).isFalse();
    assertThat(addTags.params()).hasSize(2);

    WebService.Action removeTags = controller.action("remove_tags");
    assertThat(removeTags).isNotNull();
    assertThat(removeTags.handler()).isNotNull();
    assertThat(removeTags.since()).isEqualTo("4.2");
    assertThat(removeTags.isPost()).isTrue();
    assertThat(removeTags.isInternal()).isFalse();
    assertThat(removeTags.params()).hasSize(2);
  }
}
