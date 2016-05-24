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
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.mockito.Mockito.mock;


public class SearchViewComponentsActionTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  WsTester ws;

  @Before
  public void setUp() {
    ws = new WsTester(new ComponentsWs(mock(AppAction.class), new SearchViewComponentsAction(db.getDbClient(), userSessionRule, new ComponentFinder(db.getDbClient()))));
  }

  @Test
  public void return_projects_from_view() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");
    userSessionRule.login("john").addProjectUuidPermissions(UserRole.USER, "EFGH");

    WsTester.TestRequest request = newRequest().setParam("componentId", "EFGH").setParam("q", "st");
    request.execute().assertJson(getClass(), "return_projects_from_view.json");
  }

  @Test
  public void return_projects_from_subview() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");
    userSessionRule.login("john").addComponentUuidPermission(UserRole.USER, "EFGH", "FGHI");

    WsTester.TestRequest request = newRequest().setParam("componentId", "FGHI").setParam("q", "st");
    request.execute().assertJson(getClass(), "return_projects_from_subview.json");
  }

  @Test
  public void return_only_authorized_projects_from_view() throws Exception {
    db.prepareDbUnit(getClass(), "return_only_authorized_projects_from_view.xml");
    userSessionRule.login("john").addProjectUuidPermissions(UserRole.USER, "EFGH");

    WsTester.TestRequest request = newRequest().setParam("componentId", "EFGH").setParam("q", "st");
    request.execute().assertJson(getClass(), "return_only_authorized_projects_from_view.json");
  }

  @Test
  public void return_paged_result() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");
    userSessionRule.login("john").addProjectUuidPermissions(UserRole.USER, "EFGH");

    WsTester.TestRequest request = newRequest().setParam("componentId", "EFGH").setParam("q", "st").setParam(Param.PAGE, "2")
      .setParam(Param.PAGE_SIZE, "1");
    request.execute().assertJson(getClass(), "return_paged_result.json");
  }

  @Test
  public void return_only_first_page() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");
    userSessionRule.login("john").addProjectUuidPermissions(UserRole.USER, "EFGH");

    WsTester.TestRequest request = newRequest().setParam("componentId", "EFGH").setParam("q", "st").setParam(Param.PAGE, "1")
      .setParam(Param.PAGE_SIZE, "1");
    request.execute().assertJson(getClass(), "return_only_first_page.json");
  }

  @Test
  public void fail_when_search_param_is_too_short() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Minimum search is 2 characters");

    db.prepareDbUnit(getClass(), "shared.xml");
    userSessionRule.login("john").addProjectUuidPermissions(UserRole.USER, "EFGH");

    WsTester.TestRequest request = newRequest().setParam("componentId", "EFGH").setParam("q", "s");
    request.execute();
  }

  @Test
  public void fail_when_project_uuid_does_not_exists() throws Exception {
    thrown.expect(NotFoundException.class);
    thrown.expectMessage("Component id 'UNKNOWN' not found");

    db.prepareDbUnit(getClass(), "shared.xml");
    userSessionRule.login("john").addProjectUuidPermissions(UserRole.USER, "EFGH");

    WsTester.TestRequest request = newRequest().setParam("componentId", "UNKNOWN").setParam("q", "st");
    request.execute();
  }

  private WsTester.TestRequest newRequest() {
    return ws.newGetRequest("api/components", "search_view_components");
  }
}
