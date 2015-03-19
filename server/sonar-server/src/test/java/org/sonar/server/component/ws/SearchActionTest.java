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

package org.sonar.server.component.ws;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.web.UserRole;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.user.AuthorizationDao;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.component.db.ComponentIndexDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.failBecauseExceptionWasNotThrown;
import static org.mockito.Mockito.mock;

@Category(DbTests.class)
public class SearchActionTest {

  @ClassRule
  public static DbTester dbTester = new DbTester();

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    dbTester.truncateTables();
    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(),
      new ComponentDao(), new AuthorizationDao(dbTester.myBatis()), new ComponentIndexDao()
      );
    tester = new WsTester(new ComponentsWs(mock(ComponentAppAction.class), new SearchAction(dbClient)));
  }

  @Test
  public void return_projects_from_view() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    MockUserSession.set().setLogin("john").addProjectUuidPermissions(UserRole.USER, "EFGH");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "search").setParam("componentUuid", "EFGH").setParam("q", "st");
    request.execute().assertJson(getClass(), "return_projects_from_view.json");
  }

  @Test
  public void return_projects_from_subview() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    MockUserSession.set().setLogin("john").addComponentUuidPermission(UserRole.USER, "EFGH", "FGHI");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "search").setParam("componentUuid", "FGHI").setParam("q", "st");
    request.execute().assertJson(getClass(), "return_projects_from_subview.json");
  }

  @Test
  public void return_only_authorized_projects_from_view() throws Exception {
    dbTester.prepareDbUnit(getClass(), "return_only_authorized_projects_from_view.xml");
    MockUserSession.set().setLogin("john").addProjectUuidPermissions(UserRole.USER, "EFGH");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "search").setParam("componentUuid", "EFGH").setParam("q", "st");
    request.execute().assertJson(getClass(), "return_only_authorized_projects_from_view.json");
  }

  @Test
  public void return_paged_result() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    MockUserSession.set().setLogin("john").addProjectUuidPermissions(UserRole.USER, "EFGH");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "search").setParam("componentUuid", "EFGH").setParam("q", "st").setParam("p", "2").setParam("ps", "1");
    request.execute().assertJson(getClass(), "return_paged_result.json");
  }

  @Test
  public void return_only_first_page() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    MockUserSession.set().setLogin("john").addProjectUuidPermissions(UserRole.USER, "EFGH");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "search").setParam("componentUuid", "EFGH").setParam("q", "st").setParam("p", "1").setParam("ps", "1");
    request.execute().assertJson(getClass(), "return_only_first_page.json");
  }

  @Test
  public void fail_when_search_param_is_too_short() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    MockUserSession.set().setLogin("john").addProjectUuidPermissions(UserRole.USER, "EFGH");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "search").setParam("componentUuid", "EFGH").setParam("q", "s");

    try {
      request.execute();
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Minimum search is 2 characters");
    }
  }
}
