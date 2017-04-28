/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.project.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.BillingValidationsProxy;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;

public class BulkDeleteActionTest {

  private static final String ACTION = "bulk_delete";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ComponentCleanerService componentCleanerService = mock(ComponentCleanerService.class);
  private WsTester ws;
  private DbClient dbClient = db.getDbClient();
  private BulkDeleteAction underTest = new BulkDeleteAction(componentCleanerService, dbClient, userSession, new ProjectsWsSupport(dbClient, mock(BillingValidationsProxy.class)));
  private OrganizationDto org1;
  private OrganizationDto org2;

  @Before
  public void setUp() {
    ws = new WsTester(new ProjectsWs(underTest));
    org1 = db.organizations().insert();
    org2 = db.organizations().insert();
  }

  @Test
  public void system_administrator_deletes_projects_by_uuids_in_all_organizations() throws Exception {
    userSession.logIn().setSystemAdministrator();
    ComponentDto toDeleteInOrg1 = db.components().insertPrivateProject(org1);
    ComponentDto toDeleteInOrg2 = db.components().insertPrivateProject(org2);
    ComponentDto toKeep = db.components().insertPrivateProject(org2);

    WsTester.Result result = ws.newPostRequest("api/projects", ACTION)
      .setParam("ids", toDeleteInOrg1.uuid() + "," + toDeleteInOrg2.uuid())
      .execute();
    result.assertNoContent();

    verifyDeleted(toDeleteInOrg1, toDeleteInOrg2);
  }

  @Test
  public void system_administrator_deletes_projects_by_keys_in_all_organizations() throws Exception {
    userSession.logIn().setSystemAdministrator();
    ComponentDto toDeleteInOrg1 = db.components().insertPrivateProject(org1);
    ComponentDto toDeleteInOrg2 = db.components().insertPrivateProject(org2);
    ComponentDto toKeep = db.components().insertPrivateProject(org2);

    WsTester.Result result = ws.newPostRequest("api/projects", ACTION)
      .setParam("keys", toDeleteInOrg1.key() + "," + toDeleteInOrg2.key())
      .execute();
    result.assertNoContent();

    verifyDeleted(toDeleteInOrg1, toDeleteInOrg2);
  }

  @Test
  public void projects_that_dont_exist_are_ignored_and_dont_break_bulk_deletion() throws Exception {
    userSession.logIn().setSystemAdministrator();
    ComponentDto toDelete1 = db.components().insertPrivateProject(org1);
    ComponentDto toDelete2 = db.components().insertPrivateProject(org1);

    WsTester.Result result = ws.newPostRequest("api/projects", ACTION)
      .setParam("keys", toDelete1.key() + ",missing," + toDelete2.key() + ",doesNotExist")
      .execute();
    result.assertNoContent();

    verifyDeleted(toDelete1, toDelete2);
  }

  @Test
  public void throw_ForbiddenException_if_organization_administrator_does_not_set_organization_parameter() throws Exception {
    userSession.logIn().addPermission(ADMINISTER, org1);
    ComponentDto project = db.components().insertPrivateProject(org1);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    ws.newPostRequest("api/projects", ACTION)
      .setParam("keys", project.key())
      .execute();

    verifyNoDeletions();
  }

  @Test
  public void organization_administrator_deletes_projects_by_keys_in_his_organization() throws Exception {
    userSession.logIn().addPermission(ADMINISTER, org1);
    ComponentDto toDelete = db.components().insertPrivateProject(org1);
    ComponentDto cantBeDeleted = db.components().insertPrivateProject(org2);

    WsTester.Result result = ws.newPostRequest("api/projects", ACTION)
      .setParam("organization", org1.getKey())
      .setParam("keys", toDelete.key() + "," + cantBeDeleted.key())
      .execute();
    result.assertNoContent();

    verifyDeleted(toDelete);
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() throws Exception {
    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    ws.newPostRequest("api/projects", ACTION)
      .setParam("ids", "whatever-the-uuid").execute();

    verifyNoDeletions();
  }

  @Test
  public void throw_ForbiddenException_if_param_organization_is_not_set_and_not_system_administrator() throws Exception {
    userSession.logIn().setNonSystemAdministrator();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    ws.newPostRequest("api/projects", ACTION)
      .setParam("ids", "whatever-the-uuid").execute();

    verifyNoDeletions();
  }

  @Test
  public void throw_ForbiddenException_if_param_organization_is_set_but_not_organization_administrator() throws Exception {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    ws.newPostRequest("api/projects", ACTION)
      .setParam("organization", org1.getKey())
      .setParam("ids", "whatever-the-uuid")
      .execute();

    verifyNoDeletions();
  }

  private void verifyDeleted(ComponentDto... projects) {
    ArgumentCaptor<ComponentDto> argument = ArgumentCaptor.forClass(ComponentDto.class);
    verify(componentCleanerService, times(projects.length)).delete(any(DbSession.class), argument.capture());

    for (ComponentDto project : projects) {
      assertThat(argument.getAllValues()).extracting(ComponentDto::uuid).contains(project.uuid());
    }
  }

  private void verifyNoDeletions() {
    verifyZeroInteractions(componentCleanerService);
  }
}
