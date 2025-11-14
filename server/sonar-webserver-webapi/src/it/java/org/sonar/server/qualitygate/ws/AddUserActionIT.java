/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_NAME;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_LOGIN;

public class AddUserActionIT {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final DbClient dbClient = db.getDbClient();
  private final QualityGatesWsSupport wsSupport = new QualityGatesWsSupport(dbClient, userSession, TestComponentFinder.from(db));
  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();
  private final WsActionTester ws = new WsActionTester(new AddUserAction(dbClient, uuidFactory, wsSupport));

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("add_user");
    assertThat(def.isPost()).isTrue();
    assertThat(def.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("login", "gateName");
  }

  @Test
  public void add_user() {
    QualityGateDto qualityGateDto = db.qualityGates().insertQualityGate();
    UserDto user = db.users().insertUser();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    TestResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGateDto.getName())
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(dbClient.qualityGateUserPermissionDao().exists(db.getSession(), qualityGateDto, user)).isTrue();
  }

  @Test
  public void does_nothing_when_user_can_already_edit_qualityGateDto() {
    QualityGateDto qualityGateDto = db.qualityGates().insertQualityGate();
    UserDto user = db.users().insertUser();
    db.qualityGates().addUserPermission(qualityGateDto, user);
    assertThat(dbClient.qualityGateUserPermissionDao().exists(db.getSession(), qualityGateDto, user)).isTrue();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGateDto.getName())
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();

    assertThat(dbClient.qualityGateUserPermissionDao().exists(db.getSession(), qualityGateDto, user)).isTrue();
  }

  @Test
  public void quality_gate_administers_can_add_user() {
    QualityGateDto qualityGateDto = db.qualityGates().insertQualityGate();
    UserDto user = db.users().insertUser();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGateDto.getName())
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();

    assertThat(dbClient.qualityGateUserPermissionDao().exists(db.getSession(), qualityGateDto, user)).isTrue();
  }

  @Test
  public void quality_gate_editors_can_add_user() {
    QualityGateDto qualityGateDto = db.qualityGates().insertQualityGate();
    UserDto user = db.users().insertUser();
    UserDto userAllowedToEditQualityGate = db.users().insertUser();
    db.qualityGates().addUserPermission(qualityGateDto, userAllowedToEditQualityGate);
    userSession.logIn(userAllowedToEditQualityGate);

    ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGateDto.getName())
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();

    assertThat(dbClient.qualityGateUserPermissionDao().exists(db.getSession(), qualityGateDto, user)).isTrue();
  }

  @Test
  public void fail_when_user_does_not_exist() {
    QualityGateDto qualityGateDto = db.qualityGates().insertQualityGate();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    TestRequest request = ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGateDto.getName())
      .setParam(PARAM_LOGIN, "unknown");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("User with login 'unknown' is not found");

  }

  @Test
  public void fail_when_qualityGateDto_does_not_exist() {
    UserDto user = db.users().insertUser();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    TestRequest request = ws.newRequest()
      .setParam(PARAM_GATE_NAME, "unknown")
      .setParam(PARAM_LOGIN, user.getLogin());

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("No quality gate has been found for name unknown");
  }

  @Test
  public void fail_when_not_enough_permission() {
    QualityGateDto qualityGateDto = db.qualityGates().insertQualityGate();
    UserDto user = db.users().insertUser();

    TestRequest request = ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGateDto.getName())
      .setParam(PARAM_LOGIN, user.getLogin());

    assertThatThrownBy(request::execute).isInstanceOf(ForbiddenException.class);
  }
}
