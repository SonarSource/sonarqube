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
package org.sonar.server.project.ws;

import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.server.project.ws.BulkDeleteAction.PARAM_IDS;
import static org.sonar.server.project.ws.BulkDeleteAction.PARAM_KEYS;

public class BulkDeleteActionTest {

  private static final String ACTION = "bulk_delete";

  private System2 system2 = System2.INSTANCE;

  @Rule
  public DbTester db = DbTester.create(system2);

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ComponentDbTester componentDbTester = new ComponentDbTester(db);
  private ComponentCleanerService componentCleanerService = mock(ComponentCleanerService.class);
  private WsTester ws;
  private DbClient dbClient = db.getDbClient();

  @Before
  public void setUp() {
    ws = new WsTester(new ProjectsWs(
      new BulkDeleteAction(
        componentCleanerService,
        dbClient,
        userSessionRule)));
  }

  @Test
  public void delete_projects_by_uuids() throws Exception {
    userSessionRule.logIn().setRoot();
    ComponentDto p1 = componentDbTester.insertProject();
    ComponentDto p2 = componentDbTester.insertProject();

    WsTester.Result result = ws.newPostRequest("api/projects", ACTION).setParam(PARAM_IDS, p1.uuid() + "," + p2.uuid()).execute();
    result.assertNoContent();

    verifyDeleted(p1, p2);
  }

  @Test
  public void delete_projects_by_keys() throws Exception {
    userSessionRule.logIn().setRoot();
    ComponentDto p1 = componentDbTester.insertProject();
    ComponentDto p2 = componentDbTester.insertProject();

    WsTester.Result result = ws.newPostRequest("api/projects", ACTION)
      .setParam(PARAM_KEYS, p1.key() + "," + p2.key()).execute();
    result.assertNoContent();

    verifyDeleted(p1, p2);
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() throws Exception {
    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    ws.newPostRequest("api/projects", ACTION).setParam(PARAM_IDS, "whatever-the-uuid").execute();
  }

  @Test
  public void throw_ForbiddenException_if_not_root_administrator() throws Exception {
    userSessionRule.logIn().setNonRoot();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    ws.newPostRequest("api/projects", ACTION).setParam(PARAM_IDS, "whatever-the-uuid").execute();
  }

  private void verifyDeleted(ComponentDto... projects) {
    ArgumentCaptor<List<ComponentDto>> argument = (ArgumentCaptor<List<ComponentDto>>) ((ArgumentCaptor) ArgumentCaptor.forClass(List.class));
    verify(componentCleanerService).delete(any(DbSession.class), argument.capture());

    assertThat(argument.getValue()).containsOnly(projects);
  }
}
