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

import java.util.Date;
import java.util.Locale;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.Page;
import org.sonar.api.web.ResourceLanguage;
import org.sonar.api.web.ResourceQualifier;
import org.sonar.api.web.ResourceScope;
import org.sonar.api.web.UserRole;
import org.sonar.api.web.View;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.dashboard.ActiveDashboardDto;
import org.sonar.db.dashboard.DashboardDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotTesting;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ui.Views;
import org.sonar.server.ws.WsTester;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComponentNavigationActionTest {

  System2 system = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system);

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private DbClient dbClient = dbTester.getDbClient();

  private I18n i18n;

  private ResourceTypes resourceTypes;

  @Before
  public void before() {
    dbTester.truncateTables();

    i18n = mock(I18n.class);
    when(i18n.message(any(Locale.class), any(String.class), any(String.class)))
      .thenAnswer(new Answer<String>() {
        @Override
        public String answer(InvocationOnMock invocation) throws Throwable {
          return invocation.getArgumentAt(2, String.class);
        }
      });

    resourceTypes = mock(ResourceTypes.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_missing_parameters() throws Exception {
    WsTester wsTester = newdWsTester();
    wsTester.newGetRequest("api/navigation", "component").execute();
  }

  @Test(expected = NotFoundException.class)
  public void fail_on_unexistent_key() throws Exception {
    WsTester wsTester = newdWsTester();
    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "polop").execute();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_on_missing_permission() throws Exception {
    dbClient.componentDao().insert(dbTester.getSession(), ComponentTesting.newProjectDto("abcd").setKey("polop"));
    dbTester.getSession().commit();

    WsTester wsTester = newdWsTester();
    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "polop").execute();
  }

  @Test
  public void no_snapshot_anonymous() throws Exception {
    dbClient.componentDao().insert(dbTester.getSession(), ComponentTesting.newProjectDto("abcd")
      .setKey("polop").setName("Polop"));
    dbTester.getSession().commit();

    userSessionRule.addProjectUuidPermissions(UserRole.USER, "abcd");

    WsTester wsTester = newdWsTester();
    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "polop").execute().assertJson(getClass(), "no_snapshot.json");
  }

  @Test
  public void no_snapshot_connected_user_and_favorite() throws Exception {
    int userId = 42;
    ComponentDto project = ComponentTesting.newProjectDto("abcd")
      .setKey("polop").setName("Polop");
    dbClient.componentDao().insert(dbTester.getSession(), project);
    dbClient.propertiesDao().insertProperty(dbTester.getSession(), new PropertyDto().setKey("favourite").setResourceId(project.getId()).setUserId((long) userId));
    dbTester.getSession().commit();

    userSessionRule.login("obiwan").setUserId(userId).addProjectUuidPermissions(UserRole.USER, "abcd");

    WsTester wsTester = newdWsTester();
    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "polop").execute().assertJson(getClass(), "no_snapshot_user_favourite.json");
  }

  @Test
  public void with_snapshot_and_connected_user() throws Exception {
    Date snapshotDate = DateUtils.parseDateTime("2015-04-22T11:44:00+0200");

    int userId = 42;
    ComponentDto project = ComponentTesting.newProjectDto("abcd")
      .setKey("polop").setName("Polop");
    dbClient.componentDao().insert(dbTester.getSession(), project);
    dbClient.snapshotDao().insert(dbTester.getSession(), new SnapshotDto().setCreatedAt(snapshotDate.getTime()).setVersion("3.14")
      .setLast(true).setQualifier(project.qualifier()).setComponentId(project.getId()).setRootProjectId(project.getId()).setScope(project.scope()));
    dbTester.getSession().commit();

    userSessionRule.login("obiwan").setUserId(userId).addProjectUuidPermissions(UserRole.USER, "abcd");

    WsTester wsTester = newdWsTester();
    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "polop").execute().assertJson(getClass(), "with_snapshot_and_connected_user.json");
  }

  @Test
  public void with_dashboards() throws Exception {
    dbClient.componentDao().insert(dbTester.getSession(), ComponentTesting.newProjectDto("abcd")
      .setKey("polop").setName("Polop"));
    DashboardDto dashboard = new DashboardDto().setGlobal(false).setName("Anon Dashboard").setShared(true).setColumnLayout("100%");
    dbClient.dashboardDao().insert(dashboard);
    dbClient.activeDashboardDao().insert(new ActiveDashboardDto().setDashboardId(dashboard.getId()));
    dbTester.getSession().commit();

    userSessionRule.addProjectUuidPermissions(UserRole.USER, "abcd");

    WsTester wsTester = newdWsTester();
    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "polop").execute().assertJson(getClass(), "with_dashboards.json");
  }

  @Test
  public void with_default_dashboards() throws Exception {
    dbClient.componentDao().insert(dbTester.getSession(), ComponentTesting.newProjectDto("abcd")
      .setKey("polop").setName("Polop"));
    DashboardDto dashboard = new DashboardDto().setGlobal(false).setName("Anon Dashboard").setShared(true).setColumnLayout("100%");
    dbClient.dashboardDao().insert(dashboard);
    dbClient.activeDashboardDao().insert(new ActiveDashboardDto().setDashboardId(dashboard.getId()));
    dbTester.getSession().commit();

    userSessionRule.login("obiwan").addProjectUuidPermissions(UserRole.USER, "abcd");

    WsTester wsTester = newdWsTester();
    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "polop").execute().assertJson(getClass(), "with_default_dashboards.json");
  }

  @Test
  public void with_extensions() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto("abcd")
      .setKey("polop").setName("Polop").setLanguage("xoo");
    dbClient.componentDao().insert(dbTester.getSession(), project);
    dbClient.snapshotDao().insert(dbTester.getSession(), SnapshotTesting.newSnapshotForProject(project));
    dbTester.getSession().commit();

    userSessionRule.addProjectUuidPermissions(UserRole.USER, "abcd");

    WsTester wsTester = newdWsTester(createViews());
    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "polop").execute().assertJson(getClass(), "with_extensions.json");
  }

  @Test
  public void admin_with_extensions() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto("abcd")
      .setKey("polop").setName("Polop").setLanguage("xoo");
    dbClient.componentDao().insert(dbTester.getSession(), project);
    dbClient.snapshotDao().insert(dbTester.getSession(), SnapshotTesting.newSnapshotForProject(project));
    dbTester.getSession().commit();

    userSessionRule
      .addProjectUuidPermissions(UserRole.USER, "abcd")
      .addProjectUuidPermissions(UserRole.ADMIN, "abcd");

    WsTester wsTester = newdWsTester(createViews());
    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "polop").execute().assertJson(getClass(), "admin_with_extensions.json");
  }

  @Test
  public void with_admin_rights() throws Exception {
    final String language = "xoo";
    int userId = 42;
    dbClient.componentDao().insert(dbTester.getSession(), ComponentTesting.newProjectDto("abcd")
      .setKey("polop").setName("Polop").setLanguage(language));
    dbTester.getSession().commit();

    userSessionRule.login("obiwan").setUserId(userId)
      .addProjectUuidPermissions(UserRole.USER, "abcd")
      .addProjectUuidPermissions(UserRole.ADMIN, "abcd");

    @NavigationSection(NavigationSection.RESOURCE_CONFIGURATION)
    @ResourceScope(Scopes.PROJECT)
    @ResourceQualifier(Qualifiers.PROJECT)
    @ResourceLanguage(language)
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
    Page page1 = new FirstPage();

    @NavigationSection(NavigationSection.RESOURCE_CONFIGURATION)
    @ResourceScope(Scopes.PROJECT)
    @ResourceQualifier(Qualifiers.PROJECT)
    @ResourceLanguage(language)
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
    Page page2 = new SecondPage();

    WsTester wsTester = newdWsTester(new Page[]{page1, page2});
    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "polop").execute().assertJson(getClass(), "with_admin_rights.json");
  }

  @Test
  public void with_component_which_has_all_properties() throws Exception {
    int userId = 42;
    ComponentDto project = ComponentTesting.newProjectDto("abcd")
      .setKey("polop").setName("Polop");
    dbClient.componentDao().insert(dbTester.getSession(), project);
    dbTester.getSession().commit();

    userSessionRule.login("obiwan").setUserId(userId)
      .addProjectUuidPermissions(UserRole.USER, "abcd")
      .addProjectUuidPermissions(UserRole.ADMIN, "abcd");

    ResourceType projectResourceType = ResourceType.builder(project.qualifier())
      .setProperty("comparable", true)
      .setProperty("configurable", true)
      .setProperty("hasRolePolicy", true)
      .setProperty("modifiable_history", true)
      .setProperty("updatable_key", true)
      .setProperty("deletable", true)
      .build();
    when(resourceTypes.get(project.qualifier()))
      .thenReturn(projectResourceType);

    WsTester wsTester = newdWsTester();
    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "polop").execute().assertJson(getClass(), "with_all_properties.json");
  }

  @Test
  public void on_module() throws Exception {
    int userId = 42;
    ComponentDto project = ComponentTesting.newProjectDto("abcd")
      .setKey("polop").setName("Polop");
    ComponentDto module = ComponentTesting.newModuleDto("bcde", project)
      .setKey("palap").setName("Palap");
    dbClient.componentDao().insert(dbTester.getSession(), project, module);
    dbTester.getSession().commit();

    userSessionRule.login("obiwan").setUserId(userId)
      .addProjectUuidPermissions(UserRole.USER, "abcd")
      .addProjectUuidPermissions(UserRole.ADMIN, "abcd");

    WsTester wsTester = newdWsTester();
    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "palap").execute().assertJson(getClass(), "on_module.json");
  }

  @Test
  public void with_quality_profile_admin_rights() throws Exception {
    final String language = "xoo";
    int userId = 42;
    dbClient.componentDao().insert(dbTester.getSession(), ComponentTesting.newProjectDto("abcd")
      .setKey("polop").setName("Polop").setLanguage(language));
    dbTester.getSession().commit();

    userSessionRule.login("obiwan").setUserId(userId)
      .addProjectUuidPermissions(UserRole.USER, "abcd")
      .setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    @NavigationSection(NavigationSection.RESOURCE_CONFIGURATION)
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
    WsTester wsTester = newdWsTester(new FirstPage());
    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "polop").execute().assertJson(getClass(), "quality_profile_admin.json");
  }

  @Test
  public void bread_crumbs_on_several_levels() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto("abcd")
      .setKey("polop").setName("Polop");
    ComponentDto module = ComponentTesting.newModuleDto("bcde", project)
      .setKey("palap").setName("Palap");
    ComponentDto directory = ComponentTesting.newDirectory(module, "src/main/xoo");
    ComponentDto file = ComponentTesting.newFileDto(module, "cdef").setName("Source.xoo")
      .setKey("palap:src/main/xoo/Source.xoo")
      .setPath(directory.path());
    dbClient.componentDao().insert(dbTester.getSession(), project, module, directory, file);

    SnapshotDto projectSnapshot = SnapshotTesting.newSnapshotForProject(project);
    dbClient.snapshotDao().insert(dbTester.getSession(), projectSnapshot);
    SnapshotDto moduleSnapshot = SnapshotTesting.createForComponent(module, projectSnapshot);
    dbClient.snapshotDao().insert(dbTester.getSession(), moduleSnapshot);
    SnapshotDto directorySnapshot = SnapshotTesting.createForComponent(directory, moduleSnapshot);
    dbClient.snapshotDao().insert(dbTester.getSession(), directorySnapshot);
    dbClient.snapshotDao().insert(dbTester.getSession(), SnapshotTesting.createForComponent(file, directorySnapshot));

    dbTester.getSession().commit();

    userSessionRule.addProjectUuidPermissions(UserRole.USER, "abcd");

    WsTester wsTester = newdWsTester();
    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "palap:src/main/xoo/Source.xoo").execute().assertJson(getClass(), "breadcrumbs.json");
  }

  private WsTester newdWsTester(View... views) {
    return new WsTester(new NavigationWs(new ComponentNavigationAction(dbClient, new Views(userSessionRule, views), i18n, resourceTypes, userSessionRule,
      new ComponentFinder(dbClient))));
  }

  private View[] createViews() {
    @NavigationSection(NavigationSection.RESOURCE)
    @ResourceScope(Scopes.PROJECT)
    @ResourceQualifier(Qualifiers.PROJECT)
    @ResourceLanguage("xoo")
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
    Page page1 = new FirstPage();

    @NavigationSection(NavigationSection.RESOURCE)
    @ResourceScope(Scopes.PROJECT)
    @ResourceQualifier(Qualifiers.PROJECT)
    @ResourceLanguage("xoo")
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
    Page page2 = new SecondPage();

    @NavigationSection(NavigationSection.RESOURCE)
    @ResourceScope(Scopes.PROJECT)
    @ResourceQualifier(Qualifiers.PROJECT)
    @ResourceLanguage("xoo")
    @UserRole(UserRole.ADMIN)
    class AdminPage implements Page {
      @Override
      public String getTitle() {
        return "Admin Page";
      }

      @Override
      public String getId() {
        return "/admin/page";
      }
    }
    Page adminPage = new AdminPage();
    return new Page[]{page1, page2, adminPage};
  }
}
