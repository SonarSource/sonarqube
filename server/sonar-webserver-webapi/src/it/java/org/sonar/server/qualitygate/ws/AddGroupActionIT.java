/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
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
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;

@RunWith(DataProviderRunner.class)
public class AddGroupActionIT {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final DbClient dbClient = db.getDbClient();
  private final QualityGatesWsSupport wsSupport = new QualityGatesWsSupport(dbClient, userSession, TestComponentFinder.from(db));
  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();
  private final WsActionTester ws = new WsActionTester(new AddGroupAction(dbClient, uuidFactory, wsSupport));

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("add_group");
    assertThat(def.isPost()).isTrue();
    assertThat(def.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("groupName", "gateName");
  }

  @Test
  public void add_group() {
    QualityGateDto qualityGateDto = db.qualityGates().insertQualityGate();
    GroupDto group = db.users().insertDefaultGroup();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    TestResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGateDto.getName())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute();

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(dbClient.qualityGateGroupPermissionsDao().exists(db.getSession(), qualityGateDto, group)).isTrue();
  }

  @Test
  public void does_nothing_when_group_can_already_edit_qualityGateDto() {
    QualityGateDto qualityGateDto = db.qualityGates().insertQualityGate();
    GroupDto group = db.users().insertDefaultGroup();

    db.qualityGates().addGroupPermission(qualityGateDto, group);
    assertThat(dbClient.qualityGateGroupPermissionsDao().exists(db.getSession(), qualityGateDto, group)).isTrue();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGateDto.getName())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute();

    assertThat(dbClient.qualityGateGroupPermissionsDao().exists(db.getSession(), qualityGateDto, group)).isTrue();
  }

  @Test
  public void quality_gate_administers_can_add_group() {
    QualityGateDto qualityGateDto = db.qualityGates().insertQualityGate();
    GroupDto group = db.users().insertDefaultGroup();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGateDto.getName())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute();

    assertThat(dbClient.qualityGateGroupPermissionsDao().exists(db.getSession(), qualityGateDto, group)).isTrue();
  }

  @Test
  public void quality_gate_editors_can_add_group() {
    QualityGateDto qualityGateDto = db.qualityGates().insertQualityGate();

    GroupDto originalGroup = db.users().insertDefaultGroup();
    UserDto userAllowedToEditQualityGate = db.users().insertUser();
    db.users().insertMember(originalGroup, userAllowedToEditQualityGate);

    db.qualityGates().addGroupPermission(qualityGateDto, originalGroup);
    userSession.logIn(userAllowedToEditQualityGate).setGroups(originalGroup);
    GroupDto newGroup = db.users().insertGroup();

    ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGateDto.getName())
      .setParam(PARAM_GROUP_NAME, newGroup.getName())
      .execute();

    assertThat(dbClient.qualityGateGroupPermissionsDao().exists(db.getSession(), qualityGateDto, originalGroup)).isTrue();
  }

  @Test
  public void fail_when_group_does_not_exist() {
    QualityGateDto qualityGateDto = db.qualityGates().insertQualityGate();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    TestRequest request = ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGateDto.getName())
      .setParam(PARAM_GROUP_NAME, "unknown");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Group with name 'unknown' is not found");
  }

  @Test
  public void fail_when_qualityGateDto_does_not_exist() {
    GroupDto group = db.users().insertDefaultGroup();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    TestRequest request = ws.newRequest()
      .setParam(PARAM_GATE_NAME, "unknown")
      .setParam(PARAM_GROUP_NAME, group.getName());

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("No quality gate has been found for name unknown");
  }

  @Test
  public void fail_when_not_enough_permission() {
    QualityGateDto qualityGateDto = db.qualityGates().insertQualityGate();
    GroupDto group = db.users().insertDefaultGroup();

    TestRequest request = ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGateDto.getName())
      .setParam(PARAM_GROUP_NAME, group.getName());

    assertThatThrownBy(request::execute).isInstanceOf(ForbiddenException.class);
  }
}
