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
package org.sonar.server.component.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.index.ComponentIndex;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComponentsWsTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  WebService.Controller controller;

  @Before
  public void setUp() {
    Languages languages = mock(Languages.class, RETURNS_DEEP_STUBS);
    when(languages.all()).thenReturn(new Language[0]);

    WsTester tester = new WsTester(new ComponentsWs(
      new AppAction(mock(DbClient.class), userSessionRule, mock(ComponentFinder.class)),
      new SearchViewComponentsAction(mock(DbClient.class), userSessionRule, mock(ComponentFinder.class)),
      new SuggestionsAction(mock(DbClient.class), mock(ComponentIndex.class)),
      new SearchAction(mock(DbClient.class), mock(ResourceTypes.class), mock(I18n.class), userSessionRule, languages)));
    controller = tester.controller("api/components");
  }

  @Test
  public void define_controller() {
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.since()).isEqualTo("4.2");
    assertThat(controller.actions()).hasSize(4);
  }

  @Test
  public void define_suggestions_action() {
    WebService.Action action = controller.action("suggestions");
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isFalse();
    assertThat(action.handler()).isNotNull();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(1);
  }

  @Test
  public void define_app_action() {
    WebService.Action action = controller.action("app");
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isFalse();
    assertThat(action.handler()).isNotNull();
    assertThat(action.params()).hasSize(2);
  }

  @Test
  public void define_search_view_components_action() {
    WebService.Action action = controller.action("search_view_components");

    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isFalse();
    assertThat(action.handler()).isNotNull();
    assertThat(action.params()).hasSize(4);
  }

}
