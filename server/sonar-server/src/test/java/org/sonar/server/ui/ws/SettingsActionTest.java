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
package org.sonar.server.ui.ws;

import java.util.Locale;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.i18n.I18n;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.Page;
import org.sonar.api.web.View;
import org.sonar.core.config.WebConstants;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ui.Views;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SettingsActionTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private Settings settings = new MapSettings();

  private I18n i18n = mock(I18n.class);

  private WsActionTester ws;

  @Before
  public void before() {
    when(i18n.message(any(Locale.class), anyString(), anyString())).thenAnswer(invocation -> invocation.getArgumentAt(2, String.class));
  }

  @Test
  public void empty() throws Exception {
    init();
    userSessionRule.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    executeAndVerify("empty.json");
  }

  @Test
  public void with_provisioning() throws Exception {
    init();
    userSessionRule.setGlobalPermissions(GlobalPermissions.PROVISIONING);

    executeAndVerify("with_provisioning.json");
  }

  @Test
  public void with_views() throws Exception {
    init(createViews());
    userSessionRule.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    executeAndVerify("with_views.json");
  }

  @Test
  public void with_update_center() throws Exception {
    init();
    settings.setProperty(WebConstants.SONAR_UPDATECENTER_ACTIVATE, true);
    userSessionRule.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    executeAndVerify("with_update_center.json");
  }

  @Test
  public void with_views_and_update_center_but_not_admin() throws Exception {
    init(createViews());
    settings.setProperty(WebConstants.SONAR_UPDATECENTER_ACTIVATE, true);

    executeAndVerify("empty.json");
  }

  private void init(View... views) {
    ws = new WsActionTester(new SettingsAction(settings, new Views(userSessionRule, views), i18n, userSessionRule));
  }

  private void executeAndVerify(String json) {
    JsonAssert.assertJson(ws.newRequest().execute().getInput()).isSimilarTo(getClass().getResource("SettingsActionTest/" + json));
  }

  private View[] createViews() {

    @NavigationSection(NavigationSection.CONFIGURATION)
    class FirstPage implements Page {
      @Override
      public String getTitle() {
        return "First Page";
      }

      @Override
      public String getId() {
        return "first_page";
      }
    }

    @NavigationSection(NavigationSection.CONFIGURATION)
    class SecondPage implements Page {
      @Override
      public String getTitle() {
        return "Second Page";
      }

      @Override
      public String getId() {
        return "second_page";
      }
    }

    Page page = new FirstPage();
    Page controller = new SecondPage();
    return new View[] {page, controller};
  }
}
