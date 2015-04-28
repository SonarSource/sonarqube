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
import org.sonar.api.config.Settings;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypeTree;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.utils.System2;
import org.sonar.api.web.Page;
import org.sonar.api.web.View;
import org.sonar.core.dashboard.ActiveDashboardDao;
import org.sonar.core.dashboard.ActiveDashboardDto;
import org.sonar.core.dashboard.DashboardDao;
import org.sonar.core.dashboard.DashboardDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.user.UserDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.ui.Views;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.db.UserDao;
import org.sonar.server.ws.WsTester;

import static org.mockito.Mockito.mock;

public class GlobalNavigationActionTest {

  @ClassRule
  public static final DbTester dbTester = new DbTester();

  private DbSession session;

  private WsTester wsTester;

  private UserDao userDao;

  private DashboardDao dashboardDao;

  private ActiveDashboardDao activeDashboardDao;

  @Before
  public void before() throws Exception {
    dbTester.truncateTables();

    userDao = new UserDao(dbTester.myBatis(), mock(System2.class));
    dashboardDao = new DashboardDao(dbTester.myBatis());
    activeDashboardDao = new ActiveDashboardDao(dbTester.myBatis());
    DbClient dbClient = new DbClient(
      dbTester.database(), dbTester.myBatis(), userDao, dashboardDao, activeDashboardDao);

    session = dbClient.openSession(false);
  }

  @After
  public void after() throws Exception {
    session.close();
  }

  @Test
  public void empty_call() throws Exception {
    wsTester = new WsTester(new NavigationWs(new GlobalNavigationAction(activeDashboardDao, new Views(), new Settings(), new ResourceTypes())));

    MockUserSession.set();

    wsTester.newGetRequest("api/navigation", "global").execute().assertJson(getClass(), "empty.json");
  }

  @Test
  public void with_root_qualifiers() throws Exception {
    ResourceTypes resourceTypes = new ResourceTypes(
      new ResourceTypeTree[] {
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
    wsTester = new WsTester(new NavigationWs(new GlobalNavigationAction(activeDashboardDao, new Views(), new Settings(), resourceTypes)));

    MockUserSession.set();

    wsTester.newGetRequest("api/navigation", "global").execute().assertJson(getClass(), "with_qualifiers.json");
  }

  @Test
  public void only_logo() throws Exception {
    wsTester = new WsTester(new NavigationWs(new GlobalNavigationAction(activeDashboardDao, new Views(),
      new Settings()
        .setProperty("sonar.lf.logoUrl", "http://some-server.tld/logo.png")
        .setProperty("sonar.lf.logoWidthPx", "123"),
      new ResourceTypes())));

    MockUserSession.set();

    wsTester.newGetRequest("api/navigation", "global").execute().assertJson(getClass(), "only_logo.json");
  }

  @Test
  public void nominal_call_for_anonymous() throws Exception {
    nominalSetup();

    MockUserSession.set();

    wsTester.newGetRequest("api/navigation", "global").execute().assertJson(getClass(), "anonymous.json");
  }

  @Test
  public void nominal_call_for_user() throws Exception {
    nominalSetup();

    MockUserSession.set().setLogin("obiwan");

    wsTester.newGetRequest("api/navigation", "global").execute().assertJson(getClass(), "user.json");
  }

  @Test
  public void nominal_call_for_user_without_configured_dashboards() throws Exception {
    nominalSetup();

    MockUserSession.set().setLogin("anakin");

    wsTester.newGetRequest("api/navigation", "global").execute().assertJson(getClass(), "anonymous.json");
  }

  private void nominalSetup() {
    createAndConfigureDashboardForUser();
    createAndConfigureDashboardForAnonymous();

    session.commit();

    Settings settings = new Settings()
      .setProperty("sonar.lf.logoUrl", "http://some-server.tld/logo.png")
      .setProperty("sonar.lf.logoWidthPx", "123");
    wsTester = new WsTester(new NavigationWs(new GlobalNavigationAction(activeDashboardDao, createViews(), settings, new ResourceTypes())));
  }

  private void createAndConfigureDashboardForUser() {
    UserDto user = createUser("obiwan", 42L);
    userDao.insert(session, user);
    DashboardDto defaultDashboardForUser = createDashboard(1L, "Default Dashboard for User", true);
    dashboardDao.insert(defaultDashboardForUser);
    activeDashboardDao.insert(createActiveDashboard(user.getId(), defaultDashboardForUser.getId(), 1));
  }

  private void createAndConfigureDashboardForAnonymous() {
    DashboardDto defaultDashboardForAnonymous = createDashboard(2L, "Default Dashboard for Anonymous", true);
    dashboardDao.insert(defaultDashboardForAnonymous);
    activeDashboardDao.insert(createActiveDashboard(null, defaultDashboardForAnonymous.getId(), 1));
  }

  private UserDto createUser(String login, Long userId) {
    return new UserDto()
      .setActive(true)
      .setId(userId)
      .setLogin(login);
  }

  private DashboardDto createDashboard(long id, String name, boolean global) {
    return new DashboardDto()
      .setGlobal(global)
      .setId(id)
      .setName(name)
      .setShared(true);
  }

  private ActiveDashboardDto createActiveDashboard(Long userId, Long dashboardId, int orderIndex) {
    return new ActiveDashboardDto()
      .setOrderIndex(orderIndex)
      .setDashboardId(dashboardId)
      .setUserId(userId);
  }

  private Views createViews() {
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
    return new Views(new View[] {page, controller});
  }
}
