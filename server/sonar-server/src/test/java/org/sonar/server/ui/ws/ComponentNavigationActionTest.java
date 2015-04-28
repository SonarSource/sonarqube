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
package org.sonar.server.ui.ws;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
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
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.dashboard.ActiveDashboardDao;
import org.sonar.core.dashboard.ActiveDashboardDto;
import org.sonar.core.dashboard.DashboardDao;
import org.sonar.core.dashboard.DashboardDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.component.db.SnapshotDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.measure.persistence.MeasureDao;
import org.sonar.server.ui.Views;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.db.UserDao;
import org.sonar.server.ws.WsTester;

import java.util.Date;
import java.util.Locale;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComponentNavigationActionTest {

  @ClassRule
  public static final DbTester dbTester = new DbTester();

  private DbSession session;

  private WsTester wsTester;

  private UserDao userDao;

  private DashboardDao dashboardDao;

  private ActiveDashboardDao activeDashboardDao;

  private DbClient dbClient;

  private I18n i18n;

  private ResourceTypes resourceTypes;

  @Before
  public void before() throws Exception {
    dbTester.truncateTables();

    System2 system = mock(System2.class);
    userDao = new UserDao(dbTester.myBatis(), system);
    dashboardDao = new DashboardDao(dbTester.myBatis());
    activeDashboardDao = new ActiveDashboardDao(dbTester.myBatis());
    dbClient = new DbClient(
      dbTester.database(), dbTester.myBatis(), userDao, dashboardDao, activeDashboardDao,
      new ComponentDao(system), new SnapshotDao(system), new PropertiesDao(dbTester.myBatis()),
      new MeasureDao());

    i18n = mock(I18n.class);
    when(i18n.message(any(Locale.class), any(String.class), any(String.class)))
      .thenAnswer(new Answer<String>() {
        @Override
        public String answer(InvocationOnMock invocation) throws Throwable {
          return invocation.getArgumentAt(2, String.class);
        }
      });

    resourceTypes = mock(ResourceTypes.class);

    session = dbClient.openSession(false);
  }

  @After
  public void after() throws Exception {
    session.close();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_missing_parameters() throws Exception {
    wsTester = new WsTester(new NavigationWs(new ComponentNavigationAction(null, null, null, null, null)));

    wsTester.newGetRequest("api/navigation", "component").execute();
  }

  @Test(expected = NotFoundException.class)
  public void fail_on_unexistent_key() throws Exception {
    wsTester = new WsTester(new NavigationWs(new ComponentNavigationAction(dbClient, null, null, null, null)));

    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "polop").execute();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_on_missing_permission() throws Exception {
    dbClient.componentDao().insert(session, ComponentTesting.newProjectDto("abcd").setKey("polop"));
    session.commit();

    MockUserSession.set();
    wsTester = new WsTester(new NavigationWs(new ComponentNavigationAction(dbClient, null, null, null, null)));

    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "polop").execute();
  }

  @Test
  public void no_snapshot_anonymous() throws Exception {
    dbClient.componentDao().insert(session, ComponentTesting.newProjectDto("abcd")
      .setKey("polop").setName("Polop"));
    session.commit();

    MockUserSession.set().addProjectUuidPermissions(UserRole.USER, "abcd");
    wsTester = new WsTester(new NavigationWs(new ComponentNavigationAction(dbClient, activeDashboardDao,
      new Views(), i18n, resourceTypes)));

    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "polop").execute().assertJson(getClass(), "no_snapshot.json");
  }

  @Test
  public void no_snapshot_connected_user_and_favorite() throws Exception {
    int userId = 42;
    ComponentDto project = dbClient.componentDao().insert(session, ComponentTesting.newProjectDto("abcd")
      .setKey("polop").setName("Polop"));
    dbClient.propertiesDao().setProperty(new PropertyDto().setKey("favourite").setResourceId(project.getId()).setUserId((long) userId), session);
    session.commit();

    MockUserSession.set().setLogin("obiwan").setUserId(userId).addProjectUuidPermissions(UserRole.USER, "abcd");

    wsTester = new WsTester(new NavigationWs(new ComponentNavigationAction(dbClient, activeDashboardDao,
      new Views(), i18n, resourceTypes)));

    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "polop").execute().assertJson(getClass(), "no_snapshot_user_favourite.json");
  }

  @Test
  public void with_snapshot_and_connected_user() throws Exception {
    Date snapshotDate = DateUtils.parseDateTime("2015-04-22T11:44:00+0200");

    int userId = 42;
    ComponentDto project = dbClient.componentDao().insert(session, ComponentTesting.newProjectDto("abcd")
      .setKey("polop").setName("Polop"));
    dbClient.snapshotDao().insert(session, new SnapshotDto().setCreatedAt(snapshotDate.getTime()).setVersion("3.14")
      .setLast(true).setQualifier(project.qualifier()).setResourceId(project.getId()).setRootProjectId(project.getId()).setScope(project.scope()));
    session.commit();

    MockUserSession.set().setLogin("obiwan").setUserId(userId).addProjectUuidPermissions(UserRole.USER, "abcd");

    wsTester = new WsTester(new NavigationWs(new ComponentNavigationAction(dbClient, activeDashboardDao,
      new Views(), i18n, resourceTypes)));

    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "polop").execute().assertJson(getClass(), "with_snapshot_and_connected_user.json");
  }

  @Test
  public void with_dashboards() throws Exception {
    dbClient.componentDao().insert(session, ComponentTesting.newProjectDto("abcd")
      .setKey("polop").setName("Polop"));
    DashboardDto dashboard = new DashboardDto().setGlobal(false).setName("Anon Dashboard").setShared(true).setColumnLayout("100%");
    dashboardDao.insert(dashboard);
    activeDashboardDao.insert(new ActiveDashboardDto().setDashboardId(dashboard.getId()));
    session.commit();

    MockUserSession.set().addProjectUuidPermissions(UserRole.USER, "abcd");

    wsTester = new WsTester(new NavigationWs(new ComponentNavigationAction(dbClient, activeDashboardDao,
      new Views(), i18n, resourceTypes)));

    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "polop").execute().assertJson(getClass(), "with_dashboards.json");
  }

  @Test
  public void with_default_dashboards() throws Exception {
    dbClient.componentDao().insert(session, ComponentTesting.newProjectDto("abcd")
      .setKey("polop").setName("Polop"));
    DashboardDto dashboard = new DashboardDto().setGlobal(false).setName("Anon Dashboard").setShared(true).setColumnLayout("100%");
    dashboardDao.insert(dashboard);
    activeDashboardDao.insert(new ActiveDashboardDto().setDashboardId(dashboard.getId()));
    session.commit();

    MockUserSession.set().setLogin("obiwan").addProjectUuidPermissions(UserRole.USER, "abcd");

    wsTester = new WsTester(new NavigationWs(new ComponentNavigationAction(dbClient, activeDashboardDao,
      new Views(), i18n, resourceTypes)));

    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "polop").execute().assertJson(getClass(), "with_default_dashboards.json");
  }

  @Test
  public void with_extensions() throws Exception {
    final String language = "xoo";
    ComponentDto project = dbClient.componentDao().insert(session, ComponentTesting.newProjectDto("abcd")
      .setKey("polop").setName("Polop").setLanguage(language));
    dbClient.snapshotDao().insert(session, new SnapshotDto()
      .setLast(true).setQualifier(project.qualifier()).setResourceId(project.getId()).setRootProjectId(project.getId()).setScope(project.scope()));
    session.commit();

    MockUserSession.set().addProjectUuidPermissions(UserRole.USER, "abcd");

    @NavigationSection(NavigationSection.RESOURCE)
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

    @NavigationSection(NavigationSection.RESOURCE)
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

    wsTester = new WsTester(new NavigationWs(new ComponentNavigationAction(dbClient, activeDashboardDao,
      new Views(new Page[] {page1, page2}), i18n, resourceTypes)));

    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "polop").execute().assertJson(getClass(), "with_extensions.json");
  }

  @Test
  public void with_admin_rights() throws Exception {
    final String language = "xoo";
    int userId = 42;
    dbClient.componentDao().insert(session, ComponentTesting.newProjectDto("abcd")
      .setKey("polop").setName("Polop").setLanguage(language));
    session.commit();

    MockUserSession.set().setLogin("obiwan").setUserId(userId)
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

    wsTester = new WsTester(new NavigationWs(new ComponentNavigationAction(dbClient, activeDashboardDao,
      new Views(new Page[] {page1, page2}), i18n, resourceTypes)));

    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "polop").execute().assertJson(getClass(), "with_admin_rights.json");
  }

  @Test
  public void with_component_which_has_all_properties() throws Exception {
    int userId = 42;
    ComponentDto project = ComponentTesting.newProjectDto("abcd")
      .setKey("polop").setName("Polop");
    dbClient.componentDao().insert(session, project);
    session.commit();

    MockUserSession.set().setLogin("obiwan").setUserId(userId)
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

    wsTester = new WsTester(new NavigationWs(new ComponentNavigationAction(dbClient, activeDashboardDao,
      new Views(), i18n, resourceTypes)));

    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "polop").execute().assertJson(getClass(), "with_all_properties.json");
  }

  @Test
  public void on_module() throws Exception {
    int userId = 42;
    ComponentDto project = ComponentTesting.newProjectDto("abcd")
      .setKey("polop").setName("Polop");
    ComponentDto module = ComponentTesting.newModuleDto("bcde", project)
      .setKey("palap").setName("Palap");
    dbClient.componentDao().insert(session, project, module);
    session.commit();

    MockUserSession.set().setLogin("obiwan").setUserId(userId)
      .addProjectUuidPermissions(UserRole.USER, "abcd")
      .addProjectUuidPermissions(UserRole.ADMIN, "abcd");

    wsTester = new WsTester(new NavigationWs(new ComponentNavigationAction(dbClient, activeDashboardDao,
      new Views(), i18n, resourceTypes)));

    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "palap").execute().assertJson(getClass(), "on_module.json");
  }

  @Test
  public void with_quality_profile_admin_rights() throws Exception {
    final String language = "xoo";
    int userId = 42;
    dbClient.componentDao().insert(session, ComponentTesting.newProjectDto("abcd")
      .setKey("polop").setName("Polop").setLanguage(language));
    session.commit();

    MockUserSession.set().setLogin("obiwan").setUserId(userId)
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
    Page page = new FirstPage();

    wsTester = new WsTester(new NavigationWs(new ComponentNavigationAction(dbClient, activeDashboardDao,
      new Views(new Page[] {page}), i18n, resourceTypes)));

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
    dbClient.componentDao().insert(session, project, module, directory, file);

    SnapshotDto projectSnapshot = dbClient.snapshotDao().insert(session, new SnapshotDto()
      .setLast(true)
      .setQualifier(project.qualifier())
      .setResourceId(project.getId())
      .setRootProjectId(project.getId())
      .setScope(project.scope()));
    SnapshotDto moduleSnapshot = dbClient.snapshotDao().insert(session, new SnapshotDto()
      .setLast(true)
      .setQualifier(module.qualifier())
      .setResourceId(module.getId())
      .setRootProjectId(project.getId())
      .setScope(module.scope())
      .setParentId(projectSnapshot.getId()));
    SnapshotDto directorySnapshot = dbClient.snapshotDao().insert(session, new SnapshotDto()
      .setLast(true)
      .setQualifier(directory.qualifier())
      .setResourceId(directory.getId())
      .setRootProjectId(project.getId())
      .setScope(directory.scope())
      .setParentId(moduleSnapshot.getId()));
    dbClient.snapshotDao().insert(session, new SnapshotDto()
      .setLast(true)
      .setQualifier(file.qualifier())
      .setResourceId(file.getId())
      .setRootProjectId(project.getId())
      .setScope(file.scope())
      .setParentId(directorySnapshot.getId()));

    session.commit();

    MockUserSession.set().addProjectUuidPermissions(UserRole.USER, "abcd");

    wsTester = new WsTester(new NavigationWs(new ComponentNavigationAction(dbClient, activeDashboardDao,
      new Views(), i18n, resourceTypes)));

    wsTester.newGetRequest("api/navigation", "component").setParam("componentKey", "palap:src/main/xoo/Source.xoo").execute().assertJson(getClass(), "breadcrumbs.json");
  }
}
