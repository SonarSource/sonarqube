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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypeTree;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.Page;
import org.sonar.api.web.UserRole;
import org.sonar.api.web.View;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ui.Views;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;

public class GlobalNavigationActionTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private Settings settings = new MapSettings();

  private WsActionTester ws;

  @Test
  public void empty_call() throws Exception {
    init();

    executeAndVerify("empty.json");
  }

  @Test
  public void with_root_qualifiers() throws Exception {
    init(new View[] {}, new ResourceTypeTree[] {
      ResourceTypeTree.builder()
        .addType(ResourceType.builder("POL").build())
        .addType(ResourceType.builder("LOP").build())
        .addRelations("POL", "LOP")
        .build(),
      ResourceTypeTree.builder()
        .addType(ResourceType.builder("PAL").build())
        .addType(ResourceType.builder("LAP").build())
        .addRelations("PAL", "LAP")
        .build()
    });

    executeAndVerify("with_qualifiers.json");
  }

  @Test
  public void only_logo() throws Exception {
    init();
    settings.setProperty("sonar.lf.logoUrl", "http://some-server.tld/logo.png");
    settings.setProperty("sonar.lf.logoWidthPx", "123");

    executeAndVerify("only_logo.json");
  }

  @Test
  public void nominal_call_for_anonymous() throws Exception {
    init(createViews(), new ResourceTypeTree[] {});
    addNominalSettings();

    executeAndVerify("anonymous.json");
  }

  @Test
  public void nominal_call_for_user() throws Exception {
    init(createViews(), new ResourceTypeTree[] {});
    addNominalSettings();

    userSessionRule.login("obiwan");

    executeAndVerify("user.json");
  }

  @Test
  public void nominal_call_for_admin() throws Exception {
    init(createViews(), new ResourceTypeTree[] {});
    addNominalSettings();

    userSessionRule.login("obiwan").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    executeAndVerify("admin.json");
  }

  @Test
  public void nominal_call_for_user_without_configured_dashboards() throws Exception {
    init(createViews(), new ResourceTypeTree[] {});
    addNominalSettings();

    userSessionRule.login("anakin");

    executeAndVerify("anonymous.json");
  }

  private void init() {
    init(new View[] {}, new ResourceTypeTree[] {});
  }

  private void init(View[] views, ResourceTypeTree[] resourceTypeTrees) {
    ws = new WsActionTester(new GlobalNavigationAction(new Views(userSessionRule, views), settings, new ResourceTypes(resourceTypeTrees)));
  }

  private void executeAndVerify(String json) {
    JsonAssert.assertJson(ws.newRequest().execute().getInput()).isSimilarTo(getClass().getResource(GlobalNavigationActionTest.class.getSimpleName() + "/" + json));
  }

  private void addNominalSettings() {
    settings.setProperty("sonar.lf.logoUrl", "http://some-server.tld/logo.png");
    settings.setProperty("sonar.lf.logoWidthPx", "123");
  }

  private View[] createViews() {
    Page page = new Page() {
      @Override
      public String getTitle() {
        return "My Plugin Page";
      }

      @Override
      public String getId() {
        return "my_plugin_page";
      }
    };

    Page controller = new Page() {
      @Override
      public String getTitle() {
        return "My Rails App";
      }

      @Override
      public String getId() {
        return "/my_rails_app";
      }
    };

    @NavigationSection(NavigationSection.HOME)
    @UserRole(GlobalPermissions.SYSTEM_ADMIN)
    class AdminPage implements Page {
      @Override
      public String getTitle() {
        return "Admin Page";
      }

      @Override
      public String getId() {
        return "admin_page";
      }
    }
    return new View[] {page, controller, new AdminPage()};
  }
}
