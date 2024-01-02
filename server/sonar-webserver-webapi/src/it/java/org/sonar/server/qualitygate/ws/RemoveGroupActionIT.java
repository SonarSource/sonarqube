/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;

public class RemoveGroupActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final DbClient dbClient = db.getDbClient();
  private final QualityGatesWsSupport wsSupport = new QualityGatesWsSupport(dbClient, userSession, TestComponentFinder.from(db));
  private final WsActionTester ws = new WsActionTester(new RemoveGroupAction(dbClient, wsSupport));

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("remove_group");
    assertThat(def.isPost()).isTrue();
    assertThat(def.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("groupName", "gateName");
  }

  @Test
  public void remove_group() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    GroupDto group = db.users().insertGroup();
    db.qualityGates().addGroupPermission(qualityGate, group);
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    TestResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGate.getName())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute();

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(dbClient.qualityGateGroupPermissionsDao().exists(db.getSession(), qualityGate, group)).isFalse();
  }

  @Test
  public void does_nothing_when_group_cannot_edit_gate() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    GroupDto group = db.users().insertGroup();

    assertThat(dbClient.qualityGateGroupPermissionsDao().exists(db.getSession(), qualityGate, group)).isFalse();

    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGate.getName())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute();

    assertThat(dbClient.qualityGateGroupPermissionsDao().exists(db.getSession(), qualityGate, group)).isFalse();
  }

  @Test
  public void qg_administrators_can_remove_group() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    GroupDto group = db.users().insertGroup();
    db.qualityGates().addGroupPermission(qualityGate, group);
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGate.getName())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute();

    assertThat(dbClient.qualityGateGroupPermissionsDao().exists(db.getSession(), qualityGate, group)).isFalse();
  }

  @Test
  public void qg_editors_can_remove_group() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    GroupDto group = db.users().insertGroup();
    db.qualityGates().addGroupPermission(qualityGate, group);
    UserDto userAllowedToEditGate = db.users().insertUser();
    db.qualityGates().addUserPermission(qualityGate, userAllowedToEditGate);
    userSession.logIn(userAllowedToEditGate);

    ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGate.getName())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute();

    assertThat(dbClient.qualityGateGroupPermissionsDao().exists(db.getSession(), qualityGate, group)).isFalse();
  }

  @Test
  public void fail_when_group_does_not_exist() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGate.getName())
      .setParam(PARAM_GROUP_NAME, "unknown")
      .execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Group with name 'unknown' is not found");
  }

  @Test
  public void fail_when_qgate_does_not_exist() {
    GroupDto group = db.users().insertGroup();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_GATE_NAME, "unknown")
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining(String.format("No quality gate has been found for name unknown"));
  }

  @Test
  public void fail_when_qg_is_built_in() {
    GroupDto group = db.users().insertGroup();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setBuiltIn(true));
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGate.getName())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(String.format("Operation forbidden for built-in Quality Gate '%s'", qualityGate.getName()));
  }

  @Test
  public void fail_when_not_enough_permission() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    GroupDto group = db.users().insertGroup();
    userSession.logIn(db.users().insertUser()).addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGate.getName())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute())
      .isInstanceOf(ForbiddenException.class);
  }
}
