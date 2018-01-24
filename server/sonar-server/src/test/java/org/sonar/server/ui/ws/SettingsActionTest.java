/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.ui.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.web.page.Page;
import org.sonar.api.web.page.PageDefinition;
import org.sonar.core.platform.PluginRepository;
import org.sonar.process.ProcessProperties;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ui.PageRepository;
import org.sonar.server.ws.WsActionTester;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.test.JsonAssert.assertJson;

public class SettingsActionTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private MapSettings settings = new MapSettings();

  private WsActionTester ws;

  @Test
  public void empty() {
    init();
    logInAsSystemAdministrator();

    executeAndVerify("empty.json");
  }

  @Test
  public void returns_page_settings() {
    init(createPages());
    logInAsSystemAdministrator();

    executeAndVerify("with_pages.json");
  }

  @Test
  public void returns_update_center_settings() {
    init();
    settings.setProperty(ProcessProperties.Property.SONAR_UPDATECENTER_ACTIVATE.getKey(), true);
    logInAsSystemAdministrator();

    executeAndVerify("with_update_center.json");
  }

  @Test
  public void request_succeeds_but_settings_are_not_returned_when_user_is_not_system_administrator() {
    init(createPages());
    settings.setProperty(ProcessProperties.Property.SONAR_UPDATECENTER_ACTIVATE.getKey(), true);
    userSessionRule.logIn().setNonSystemAdministrator();

    executeAndVerify("empty.json");
  }

  private void init(Page... pages) {
    PluginRepository pluginRepository = mock(PluginRepository.class);
    when(pluginRepository.hasPlugin(anyString())).thenReturn(true);
    PageRepository pageRepository = new PageRepository(pluginRepository, new PageDefinition[] {context -> {
      for (Page page : pages) {
        context.addPage(page);
      }
    }});
    ws = new WsActionTester(new SettingsAction(pageRepository, settings.asConfig(), userSessionRule));
    pageRepository.start();
  }

  private void executeAndVerify(String json) {
    assertJson(ws.newRequest().execute().getInput()).isSimilarTo(getClass().getResource("SettingsActionTest/" + json));
  }

  private Page[] createPages() {
    Page firstPage = Page.builder("my_plugin/first_page").setName("First Page").setAdmin(true).build();
    Page secondPage = Page.builder("my_plugin/second_page").setName("Second Page").setAdmin(true).build();

    return new Page[] {firstPage, secondPage};
  }

  private void logInAsSystemAdministrator() {
    userSessionRule.logIn().setSystemAdministrator();
  }
}
