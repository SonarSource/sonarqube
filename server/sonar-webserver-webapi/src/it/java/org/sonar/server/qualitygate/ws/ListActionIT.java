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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.qualitygate.QualityGateCaycChecker;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualitygates.ListWsResponse.QualityGate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.qualitygate.QualityGateCaycStatus.COMPLIANT;
import static org.sonar.server.qualitygate.QualityGateCaycStatus.NON_COMPLIANT;
import static org.sonar.server.qualitygate.QualityGateCaycStatus.OVER_COMPLIANT;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.Qualitygates.ListWsResponse;

public class ListActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final DbClient dbClient = db.getDbClient();
  private final QualityGateFinder qualityGateFinder = new QualityGateFinder(dbClient);

  private final QualityGateCaycChecker qualityGateCaycChecker = mock(QualityGateCaycChecker.class);

  private final WsActionTester ws = new WsActionTester(new ListAction(db.getDbClient(),
    new QualityGatesWsSupport(dbClient, userSession, TestComponentFinder.from(db)), qualityGateFinder, qualityGateCaycChecker));

  @Before
  public void setUp() {
    when(qualityGateCaycChecker.checkCaycCompliant(any(), any())).thenReturn(COMPLIANT);
  }

  @Test
  public void list_quality_gates() {
    QualityGateDto defaultQualityGate = db.qualityGates().insertQualityGate();
    QualityGateDto otherQualityGate = db.qualityGates().insertQualityGate();
    db.qualityGates().setDefaultQualityGate(defaultQualityGate);

    ListWsResponse response = ws.newRequest()
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getQualitygatesList())
      .extracting(QualityGate::getName, QualityGate::getIsDefault)
      .containsExactlyInAnyOrder(
        tuple(defaultQualityGate.getName(), true),
        tuple(otherQualityGate.getName(), false));
  }

  @Test
  public void test_built_in_flag() {
    QualityGateDto qualityGate1 = db.qualityGates().insertQualityGate(qualityGate -> qualityGate.setBuiltIn(true));
    QualityGateDto qualityGate2 = db.qualityGates().insertQualityGate(qualityGate -> qualityGate.setBuiltIn(false));
    db.qualityGates().setDefaultQualityGate(qualityGate1);

    ListWsResponse response = ws.newRequest()
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getQualitygatesList())
      .extracting(QualityGate::getName, QualityGate::getIsBuiltIn)
      .containsExactlyInAnyOrder(
        tuple(qualityGate1.getName(), true),
        tuple(qualityGate2.getName(), false));
  }

  @Test
  public void test_caycStatus_flag() {
    QualityGateDto qualityGate1 = db.qualityGates().insertQualityGate();
    QualityGateDto qualityGate2 = db.qualityGates().insertQualityGate();
    QualityGateDto qualityGate3 = db.qualityGates().insertQualityGate();
    when(qualityGateCaycChecker.checkCaycCompliant(any(DbSession.class), eq(qualityGate1.getUuid()))).thenReturn(COMPLIANT);
    when(qualityGateCaycChecker.checkCaycCompliant(any(DbSession.class), eq(qualityGate2.getUuid()))).thenReturn(NON_COMPLIANT);
    when(qualityGateCaycChecker.checkCaycCompliant(any(DbSession.class), eq(qualityGate3.getUuid()))).thenReturn(OVER_COMPLIANT);

    db.qualityGates().setDefaultQualityGate(qualityGate1);

    ListWsResponse response = ws.newRequest()
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getQualitygatesList())
      .extracting(QualityGate::getName, QualityGate::getCaycStatus)
      .containsExactlyInAnyOrder(
        tuple(qualityGate1.getName(), COMPLIANT.toString()),
        tuple(qualityGate2.getName(), NON_COMPLIANT.toString()),
        tuple(qualityGate3.getName(), OVER_COMPLIANT.toString()));
  }

  @Test
  public void no_default_quality_gate() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    assertThatThrownBy(() -> ws.newRequest()
      .executeProtobuf(ListWsResponse.class))
      .isInstanceOf(IllegalStateException.class);

  }

  @Test
  public void actions_with_quality_gate_administer_permission() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto defaultQualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("Default").setBuiltIn(false));
    QualityGateDto builtInQualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("Sonar way").setBuiltIn(true));
    QualityGateDto otherQualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("Sonar way - Without Coverage").setBuiltIn(false));
    db.qualityGates().setDefaultQualityGate(defaultQualityGate);

    ListWsResponse response = ws.newRequest().executeProtobuf(ListWsResponse.class);

    assertThat(response.getActions())
      .extracting(ListWsResponse.RootActions::getCreate)
      .isEqualTo(true);
    assertThat(response.getQualitygatesList())
      .extracting(QualityGate::getName,
        qg -> qg.getActions().getRename(), qg -> qg.getActions().getDelete(), qg -> qg.getActions().getManageConditions(),
        qp -> qp.getActions().getCopy(), qp -> qp.getActions().getSetAsDefault(), qp -> qp.getActions().getAssociateProjects(),
        qp -> qp.getActions().getDelegate())
      .containsExactlyInAnyOrder(
        tuple(defaultQualityGate.getName(), true, false, true, true, false, false, true),
        tuple(builtInQualityGate.getName(), false, false, false, true, true, true, false),
        tuple(otherQualityGate.getName(), true, true, true, true, true, true, true));
  }

  @Test
  public void actions_with_quality_gate_delegate_permission() {
    QualityGateDto defaultQualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("Sonar way"));
    QualityGateDto otherQualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("Sonar way - Without Coverage"));
    UserDto user = db.users().insertUser();
    db.qualityGates().addUserPermission(defaultQualityGate, user);
    db.qualityGates().addUserPermission(otherQualityGate, user);
    userSession.logIn(user);
    db.qualityGates().setDefaultQualityGate(defaultQualityGate);

    ListWsResponse response = ws.newRequest().executeProtobuf(ListWsResponse.class);

    assertThat(response.getActions())
      .extracting(ListWsResponse.RootActions::getCreate)
      .isEqualTo(false);
    assertThat(response.getQualitygatesList())
      .extracting(QualityGate::getName,
        qg -> qg.getActions().getRename(), qg -> qg.getActions().getDelete(), qg -> qg.getActions().getManageConditions(),
        qp -> qp.getActions().getCopy(), qp -> qp.getActions().getSetAsDefault(), qp -> qp.getActions().getAssociateProjects(),
        qp -> qp.getActions().getDelegate())
      .containsExactlyInAnyOrder(
        tuple(defaultQualityGate.getName(), false, false, true, false, false, false, true),
        tuple(otherQualityGate.getName(), false, false, true, false, false, false, true));
  }

  @Test
  public void actions_without_quality_gate_administer_permission() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_PROFILES);
    QualityGateDto defaultQualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("Sonar way").setBuiltIn(true));
    QualityGateDto otherQualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("Sonar way - Without Coverage").setBuiltIn(false));
    db.qualityGates().setDefaultQualityGate(defaultQualityGate);

    ListWsResponse response = ws.newRequest().executeProtobuf(ListWsResponse.class);

    assertThat(response.getActions())
      .extracting(ListWsResponse.RootActions::getCreate)
      .isEqualTo(false);
    assertThat(response.getQualitygatesList())
      .extracting(QualityGate::getName,
        qg -> qg.getActions().getRename(), qg -> qg.getActions().getDelete(), qg -> qg.getActions().getManageConditions(),
        qp -> qp.getActions().getCopy(), qp -> qp.getActions().getSetAsDefault(), qp -> qp.getActions().getAssociateProjects(),
        qp -> qp.getActions().getDelegate())
      .containsExactlyInAnyOrder(
        tuple(defaultQualityGate.getName(), false, false, false, false, false, false, false),
        tuple(otherQualityGate.getName(), false, false, false, false, false, false, false));
  }

  @Test
  public void json_example() {
    userSession.logIn("admin").addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto defaultQualityGate = db.qualityGates().insertQualityGate(qualityGate -> qualityGate.setName("Sonar way").setBuiltIn(true));
    QualityGateDto otherQualityGate = db.qualityGates().insertQualityGate(qualityGate -> qualityGate.setName("Sonar way - Without Coverage").setBuiltIn(false));
    db.qualityGates().setDefaultQualityGate(defaultQualityGate);
    when(qualityGateCaycChecker.checkCaycCompliant(any(), eq(defaultQualityGate.getUuid()))).thenReturn(COMPLIANT);
    when(qualityGateCaycChecker.checkCaycCompliant(any(), eq(otherQualityGate.getUuid()))).thenReturn(NON_COMPLIANT);

    String response = ws.newRequest().execute().getInput();

    assertJson(response).ignoreFields("default")
      .isSimilarTo(getClass().getResource("list-example.json"));
  }

  @Test
  public void verify_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action.since()).isEqualTo("4.3");
    assertThat(action.key()).isEqualTo("list");
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.changelog()).isNotEmpty();
    assertThat(action.params()).extracting(WebService.Param::key, WebService.Param::isRequired).isEmpty();
  }

}
