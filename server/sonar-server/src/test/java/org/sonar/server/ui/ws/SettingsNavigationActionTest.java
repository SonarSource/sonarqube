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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.config.Settings;
import org.sonar.api.i18n.I18n;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.Page;
import org.sonar.api.web.View;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.plugins.UpdateCenterClient;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ui.Views;
import org.sonar.server.ws.WsTester;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SettingsNavigationActionTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private WsTester wsTester;

  private Settings settings;

  private I18n i18n;

  @Before
  public void before() {
    settings = new Settings();
    i18n = mock(I18n.class);
    when(i18n.message(any(Locale.class), anyString(), anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        return invocation.getArgumentAt(2, String.class);
      }
      
    });
  }

  @Test
  public void empty() throws Exception {
    userSessionRule.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    wsTester = new WsTester(new NavigationWs(new SettingsNavigationAction(settings, new Views(userSessionRule), i18n, userSessionRule)));

    wsTester.newGetRequest("api/navigation", "settings").execute().assertJson(getClass(), "empty.json");
  }

  @Test
  public void with_provisioning() throws Exception {
    userSessionRule.setGlobalPermissions(GlobalPermissions.PROVISIONING);
    wsTester = new WsTester(new NavigationWs(new SettingsNavigationAction(settings, new Views(userSessionRule), i18n, userSessionRule)));

    wsTester.newGetRequest("api/navigation", "settings").execute().assertJson(getClass(), "with_provisioning.json");
  }

  @Test
  public void with_views() throws Exception {
    userSessionRule.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    wsTester = new WsTester(new NavigationWs(new SettingsNavigationAction(settings, createViews(), i18n, userSessionRule)));

    wsTester.newGetRequest("api/navigation", "settings").execute().assertJson(getClass(), "with_views.json");
  }

  @Test
  public void with_update_center() throws Exception {
    settings.setProperty(UpdateCenterClient.ACTIVATION_PROPERTY, true);
    userSessionRule.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    wsTester = new WsTester(new NavigationWs(new SettingsNavigationAction(settings, new Views(userSessionRule), i18n, userSessionRule)));

    wsTester.newGetRequest("api/navigation", "settings").execute().assertJson(getClass(), "with_update_center.json");
  }

  @Test
  public void with_views_and_update_center_but_not_admin() throws Exception {
    settings.setProperty(UpdateCenterClient.ACTIVATION_PROPERTY, true);
    wsTester = new WsTester(new NavigationWs(new SettingsNavigationAction(settings, createViews(), i18n, userSessionRule)));

    wsTester.newGetRequest("api/navigation", "settings").execute().assertJson(getClass(), "empty.json");
  }

  private Views createViews() {


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
        return "/second/page";
      }
    }

    Page page = new FirstPage();
    Page controller = new SecondPage();
    return new Views(userSessionRule, new View[] {page, controller});
  }
}
