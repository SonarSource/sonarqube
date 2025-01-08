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

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualitygates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_NAME;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.MediaTypes.JSON;

public class SearchGroupsActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final QualityGatesWsSupport wsSupport = new QualityGatesWsSupport(db.getDbClient(), userSession, TestComponentFinder.from(db));

  private final WsActionTester ws = new WsActionTester(new SearchGroupsAction(db.getDbClient(), wsSupport));

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("search_groups");
    assertThat(def.isPost()).isFalse();
    assertThat(def.params()).extracting(WebService.Param::key)
      .containsExactlyInAnyOrder("gateName", "selected", "q", "p", "ps");
  }

  @Test
  public void test_example() {
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    GroupDto group1 = db.users().insertGroup(newGroupDto().setName("users").setDescription("Users"));
    GroupDto group2 = db.users().insertGroup(newGroupDto().setName("administrators").setDescription("Administrators"));
    db.qualityGates().addGroupPermission(gate, group1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES);

    String result = ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(WebService.Param.SELECTED, "all")
      .setMediaType(JSON)
      .execute()
      .getInput();

    assertJson(ws.getDef().responseExampleAsString()).isSimilarTo(result);
  }

  @Test
  public void search_all_groups() {
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.qualityGates().addGroupPermission(gate, group1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES);

    Qualitygates.SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(WebService.Param.SELECTED, "all")
      .executeProtobuf(Qualitygates.SearchGroupsResponse.class);

    assertThat(response.getGroupsList())
      .extracting(Qualitygates.SearchGroupsResponse.Group::getName,
        Qualitygates.SearchGroupsResponse.Group::getDescription, Qualitygates.SearchGroupsResponse.Group::getSelected)
      .containsExactlyInAnyOrder(
        Assertions.tuple(group1.getName(), group1.getDescription(), true),
        Assertions.tuple(group2.getName(), group2.getDescription(), false));
  }

  @Test
  public void search_selected_groups() {
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.qualityGates().addGroupPermission(gate, group1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES);

    Qualitygates.SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(WebService.Param.SELECTED, "selected")
      .executeProtobuf(Qualitygates.SearchGroupsResponse.class);

    assertThat(response.getGroupsList())
      .extracting(Qualitygates.SearchGroupsResponse.Group::getName,
        Qualitygates.SearchGroupsResponse.Group::getDescription, Qualitygates.SearchGroupsResponse.Group::getSelected)
      .containsExactlyInAnyOrder(
        Assertions.tuple(group1.getName(), group1.getDescription(), true));
  }

  @Test
  public void search_deselected_groups() {
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.qualityGates().addGroupPermission(gate, group1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES);

    Qualitygates.SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(WebService.Param.SELECTED, "deselected")
      .executeProtobuf(Qualitygates.SearchGroupsResponse.class);

    assertThat(response.getGroupsList())
      .extracting(Qualitygates.SearchGroupsResponse.Group::getName,
        Qualitygates.SearchGroupsResponse.Group::getDescription, Qualitygates.SearchGroupsResponse.Group::getSelected)
      .containsExactlyInAnyOrder(
        Assertions.tuple(group2.getName(), group2.getDescription(), false));
  }

  @Test
  public void search_by_name() {
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    GroupDto group1 = db.users().insertGroup("sonar-users-project");
    GroupDto group2 = db.users().insertGroup("sonar-users-qgate");
    GroupDto group3 = db.users().insertGroup("sonar-admin");
    db.qualityGates().addGroupPermission(gate, group1);
    db.qualityGates().addGroupPermission(gate, group2);
    db.qualityGates().addGroupPermission(gate, group3);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES);

    Qualitygates.SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(TEXT_QUERY, "UsErS")
      .setParam(WebService.Param.SELECTED, "all")
      .executeProtobuf(Qualitygates.SearchGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting(Qualitygates.SearchGroupsResponse.Group::getName)
      .containsExactlyInAnyOrder(group1.getName(), group2.getName());
  }

  @Test
  public void group_without_description() {
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    GroupDto group = db.users().insertGroup(newGroupDto().setDescription(null));
    db.qualityGates().addGroupPermission(gate, group);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES);

    Qualitygates.SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(WebService.Param.SELECTED, "all")
      .executeProtobuf(Qualitygates.SearchGroupsResponse.class);

    assertThat(response.getGroupsList())
      .extracting(Qualitygates.SearchGroupsResponse.Group::getName, Qualitygates.SearchGroupsResponse.Group::hasDescription)
      .containsExactlyInAnyOrder(Assertions.tuple(group.getName(), false));
  }

  @Test
  public void paging_search() {
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    GroupDto group3 = db.users().insertGroup("group3");
    GroupDto group1 = db.users().insertGroup("group1");
    GroupDto group2 = db.users().insertGroup("group2");
    db.qualityGates().addGroupPermission(gate, group1);
    db.qualityGates().addGroupPermission(gate, group2);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES);

    assertThat(ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(WebService.Param.SELECTED, "all")
      .setParam(PAGE, "1")
      .setParam(PAGE_SIZE, "1")
      .executeProtobuf(Qualitygates.SearchGroupsResponse.class).getGroupsList())
      .extracting(Qualitygates.SearchGroupsResponse.Group::getName)
      .containsExactly(group1.getName());

    assertThat(ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(WebService.Param.SELECTED, "all")
      .setParam(PAGE, "3")
      .setParam(PAGE_SIZE, "1")
      .executeProtobuf(Qualitygates.SearchGroupsResponse.class).getGroupsList())
      .extracting(Qualitygates.SearchGroupsResponse.Group::getName)
      .containsExactly(group3.getName());

    assertThat(ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(WebService.Param.SELECTED, "all")
      .setParam(PAGE, "1")
      .setParam(PAGE_SIZE, "10")
      .executeProtobuf(Qualitygates.SearchGroupsResponse.class).getGroupsList())
      .extracting(Qualitygates.SearchGroupsResponse.Group::getName)
      .containsExactly(group1.getName(), group2.getName(), group3.getName());
  }

  @Test
  public void uses_global_permission() {
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    GroupDto group = db.users().insertGroup();
    db.qualityGates().addGroupPermission(gate, group);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES);

    Qualitygates.SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(WebService.Param.SELECTED, "all")
      .executeProtobuf(Qualitygates.SearchGroupsResponse.class);

    assertThat(response.getGroupsList())
      .extracting(Qualitygates.SearchGroupsResponse.Group::getName)
      .containsExactlyInAnyOrder(group.getName());
  }

  @Test
  public void qg_administers_can_search_groups() {
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    GroupDto group = db.users().insertGroup();
    db.qualityGates().addGroupPermission(gate, group);
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    Qualitygates.SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(WebService.Param.SELECTED, "all")
      .executeProtobuf(Qualitygates.SearchGroupsResponse.class);

    assertThat(response.getGroupsList())
      .extracting(Qualitygates.SearchGroupsResponse.Group::getName)
      .containsExactlyInAnyOrder(group.getName());
  }

  @Test
  public void qg_editors_can_search_groups() {
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    GroupDto group = db.users().insertGroup();
    db.qualityGates().addGroupPermission(gate, group);
    UserDto userAllowedToEditQualityGate = db.users().insertUser();
    db.qualityGates().addUserPermission(gate, userAllowedToEditQualityGate);
    userSession.logIn(userAllowedToEditQualityGate);

    Qualitygates.SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(WebService.Param.SELECTED, "all")
      .executeProtobuf(Qualitygates.SearchGroupsResponse.class);

    assertThat(response.getGroupsList())
      .extracting(Qualitygates.SearchGroupsResponse.Group::getName)
      .containsExactlyInAnyOrder(group.getName());
  }

  @Test
  public void fail_when_qgate_does_not_exist() {
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_GATE_NAME, "unknown")
      .execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("No quality gate has been found for name unknown");
  }

  @Test
  public void fail_when_not_enough_permission() {
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    userSession.logIn(db.users().insertUser());

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .execute())
      .isInstanceOf(ForbiddenException.class);
  }

}
