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
package org.sonar.server.qualitygate.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualitygate.QualityGateUpdater;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.WsQualityGates.CreateWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;

public class CreateActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private CreateAction underTest = new CreateAction(dbClient, userSession, new QualityGateUpdater(dbClient), defaultOrganizationProvider);
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void create_quality_gate() throws Exception {
    logInAsQualityGateAdmin();

    CreateWsResponse response = executeRequest("Default");

    assertThat(response.getName()).isEqualTo("Default");
    assertThat(response.getId()).isNotNull();
    dbSession.commit();
    QualityGateDto qualityGateDto = dbClient.qualityGateDao().selectByName(dbSession, "Default");
    assertThat(qualityGateDto).isNotNull();
  }

  @Test
  public void throw_ForbiddenException_if_not_gate_administrator() throws Exception {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    executeRequest("Default");
  }

  @Test
  public void throw_ForbiddenException_if_not_gate_administrator_of_default_organization() throws Exception {
    // as long as organizations don't support Quality gates, the global permission
    // is defined on the default organization
    OrganizationDto org = db.organizations().insert();
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES, org);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    executeRequest("Default");
  }

  @Test
  public void test_ws_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isTrue();
    assertThat(action.responseExampleAsString()).isNull();
    assertThat(action.params()).hasSize(1);
  }

  private CreateWsResponse executeRequest(String name) {
    return ws.newRequest()
      .setParam("name", name)
      .executeProtobuf(CreateWsResponse.class);
  }

  private void logInAsQualityGateAdmin() {
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
  }

}
