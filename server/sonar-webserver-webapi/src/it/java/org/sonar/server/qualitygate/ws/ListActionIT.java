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

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentMatcher;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.ai.code.assurance.AiCodeAssuranceEntitlement;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.qualitygate.QualityGateCaycChecker;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonar.server.qualitygate.QualityGateModeChecker;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualitygates.ListWsResponse.QualityGate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.qualitygate.QualityGateCaycStatus.COMPLIANT;
import static org.sonar.server.qualitygate.QualityGateCaycStatus.NON_COMPLIANT;
import static org.sonar.server.qualitygate.QualityGateCaycStatus.OVER_COMPLIANT;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.Qualitygates.ListWsResponse;

class ListActionIT {

  @RegisterExtension
  UserSessionRule userSession = UserSessionRule.standalone();
  @RegisterExtension
  DbTester db = DbTester.create();

  private final DbClient dbClient = db.getDbClient();
  private final QualityGateFinder qualityGateFinder = new QualityGateFinder(dbClient);

  private final QualityGateCaycChecker qualityGateCaycChecker = mock(QualityGateCaycChecker.class);
  private final QualityGateModeChecker qualityGateModeChecker = mock(QualityGateModeChecker.class);
  private final AiCodeAssuranceEntitlement aiCodeAssuranceEntitlement = mock(AiCodeAssuranceEntitlement.class);
  private final QualityGatesWsSupport wsSupport = new QualityGatesWsSupport(db.getDbClient(), userSession, TestComponentFinder.from(db));

  private final WsActionTester ws = new WsActionTester(new ListAction(db.getDbClient(), wsSupport, qualityGateFinder,
    qualityGateCaycChecker, qualityGateModeChecker, new QualityGateActionsSupport(wsSupport, aiCodeAssuranceEntitlement)));

  @BeforeEach
  void setUp() {
    when(qualityGateCaycChecker.checkCaycCompliant(any(Collection.class), any(List.class))).thenReturn(COMPLIANT);
    doReturn(new QualityGateModeChecker.QualityModeResult(false, false))
      .when(qualityGateModeChecker).getUsageOfModeMetrics(any(List.class));

  }

  @Test
  void list_quality_gates() {
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
  void test_built_in_flag() {
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
  void test_ai_code_supported_flag() {
    when(aiCodeAssuranceEntitlement.isEnabled()).thenReturn(true);
    QualityGateDto qualityGateWithAiCodeSupported = db.qualityGates().insertQualityGate(qg -> qg.setAiCodeSupported(true));
    QualityGateDto qualityGateWithoutAiCodeSupported = db.qualityGates().insertQualityGate(qg -> qg.setAiCodeSupported(false));
    db.qualityGates().setDefaultQualityGate(qualityGateWithAiCodeSupported);

    ListWsResponse response = ws.newRequest()
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getQualitygatesList())
      .extracting(QualityGate::getName, QualityGate::getIsAiCodeSupported)
      .containsExactlyInAnyOrder(
        tuple(qualityGateWithAiCodeSupported.getName(), true),
        tuple(qualityGateWithoutAiCodeSupported.getName(), false));
  }

  @Test
  void test_caycStatus_flag() {
    QualityGateDto qualityGate1 = db.qualityGates().insertQualityGate();
    QualityGateConditionDto condition1 = db.qualityGates().addCondition(qualityGate1, db.measures().insertMetric());
    QualityGateDto qualityGate2 = db.qualityGates().insertQualityGate();
    QualityGateConditionDto condition2 = db.qualityGates().addCondition(qualityGate2, db.measures().insertMetric());
    QualityGateDto qualityGate3 = db.qualityGates().insertQualityGate();
    QualityGateConditionDto condition3 = db.qualityGates().addCondition(qualityGate3, db.measures().insertMetric());
    doReturn(COMPLIANT).when(qualityGateCaycChecker).checkCaycCompliant(argThat(hasCondition(condition1)), any(List.class));
    doReturn(NON_COMPLIANT).when(qualityGateCaycChecker).checkCaycCompliant(argThat(hasCondition(condition2)), any(List.class));
    doReturn(OVER_COMPLIANT).when(qualityGateCaycChecker).checkCaycCompliant(argThat(hasCondition(condition3)), any(List.class));

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
  void execute_shouldReturnExpectedModeFlags() {
    QualityGateDto qualityGate1 = db.qualityGates().insertQualityGate();
    MetricDto metric1 = db.measures().insertMetric();
    db.qualityGates().addCondition(qualityGate1, metric1);
    QualityGateDto qualityGate2 = db.qualityGates().insertQualityGate();
    MetricDto metric2 = db.measures().insertMetric();
    db.qualityGates().addCondition(qualityGate2, metric2);

    doReturn(new QualityGateModeChecker.QualityModeResult(true, false))
      .when(qualityGateModeChecker).getUsageOfModeMetrics(argThat(hasMetric(metric1)));
    doReturn(new QualityGateModeChecker.QualityModeResult(false, true))
      .when(qualityGateModeChecker).getUsageOfModeMetrics(argThat(hasMetric(metric2)));

    db.qualityGates().setDefaultQualityGate(qualityGate1);

    ListWsResponse response = ws.newRequest()
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getQualitygatesList())
      .extracting(QualityGate::getName, QualityGate::getHasMQRConditions, QualityGate::getHasStandardConditions)
      .containsExactlyInAnyOrder(
        tuple(qualityGate1.getName(), true, false),
        tuple(qualityGate2.getName(), false, true));
  }

  private ArgumentMatcher<Collection<QualityGateConditionDto>> hasCondition(QualityGateConditionDto condition) {
    return collection -> collection.stream().anyMatch(e -> e.getUuid().equals(condition.getUuid()));
  }

  private ArgumentMatcher<List<MetricDto>> hasMetric(MetricDto metricDto) {
    return collection -> collection.stream().anyMatch(e -> e.getUuid().equals(metricDto.getUuid()));
  }

  @Test
  void no_default_quality_gate() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    assertThatThrownBy(() -> ws.newRequest()
      .executeProtobuf(ListWsResponse.class))
      .isInstanceOf(IllegalStateException.class);

  }

  @Test
  void actions_with_quality_gate_administer_permission() {
    when(aiCodeAssuranceEntitlement.isEnabled()).thenReturn(true);
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto defaultQualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("Default").setBuiltIn(false));
    QualityGateDto builtInQualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("Sonar way").setBuiltIn(true));
    QualityGateDto otherQualityGate =
      db.qualityGates().insertQualityGate(qg -> qg.setName("Sonar way - Without Coverage").setBuiltIn(false));
    db.qualityGates().setDefaultQualityGate(defaultQualityGate);

    ListWsResponse response = ws.newRequest().executeProtobuf(ListWsResponse.class);

    assertThat(response.getActions())
      .extracting(ListWsResponse.RootActions::getCreate)
      .isEqualTo(true);
    assertThat(response.getQualitygatesList())
      .extracting(QualityGate::getName,
        qg -> qg.getActions().getRename(), qg -> qg.getActions().getDelete(), qg -> qg.getActions().getManageConditions(),
        qp -> qp.getActions().getCopy(), qp -> qp.getActions().getSetAsDefault(), qp -> qp.getActions().getAssociateProjects(),
        qp -> qp.getActions().getDelegate(), qg -> qg.getActions().getManageAiCodeAssurance())
      .containsExactlyInAnyOrder(
        tuple(defaultQualityGate.getName(), true, false, true, true, false, false, true, true),
        tuple(builtInQualityGate.getName(), false, false, false, true, true, true, false, false),
        tuple(otherQualityGate.getName(), true, true, true, true, true, true, true, true));
  }

  @Test
  void getManageAiCodeAssurance_action_not_available_when_feature_disabled() {
    when(aiCodeAssuranceEntitlement.isEnabled()).thenReturn(false);
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto defaultQualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("Default").setBuiltIn(false));
    db.qualityGates().setDefaultQualityGate(defaultQualityGate);

    ListWsResponse response = ws.newRequest().executeProtobuf(ListWsResponse.class);

    assertThat(response.getQualitygatesList()).hasSize(1);
    assertThat(response.getQualitygatesList().get(0).getActions().getManageAiCodeAssurance()).isFalse();
    assertThat(response.getQualitygatesList())
      .extracting(QualityGate::getName, QualityGate::getIsAiCodeSupported)
      .containsExactly(tuple(defaultQualityGate.getName(), false));
  }

  @Test
  void actions_with_quality_gate_delegate_permission() {
    when(aiCodeAssuranceEntitlement.isEnabled()).thenReturn(true);
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
        qp -> qp.getActions().getDelegate(), qg -> qg.getActions().getManageAiCodeAssurance())
      .containsExactlyInAnyOrder(
        tuple(defaultQualityGate.getName(), false, false, true, false, false, false, true, false),
        tuple(otherQualityGate.getName(), false, false, true, false, false, false, true, false));
  }

  @Test
  void actions_without_quality_gate_administer_permission() {
    when(aiCodeAssuranceEntitlement.isEnabled()).thenReturn(true);
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_PROFILES);
    QualityGateDto defaultQualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("Sonar way").setBuiltIn(true));
    QualityGateDto otherQualityGate =
      db.qualityGates().insertQualityGate(qg -> qg.setName("Sonar way - Without Coverage").setBuiltIn(false));
    db.qualityGates().setDefaultQualityGate(defaultQualityGate);

    ListWsResponse response = ws.newRequest().executeProtobuf(ListWsResponse.class);

    assertThat(response.getActions())
      .extracting(ListWsResponse.RootActions::getCreate)
      .isEqualTo(false);
    assertThat(response.getQualitygatesList())
      .extracting(QualityGate::getName,
        qg -> qg.getActions().getRename(), qg -> qg.getActions().getDelete(), qg -> qg.getActions().getManageConditions(),
        qp -> qp.getActions().getCopy(), qp -> qp.getActions().getSetAsDefault(), qp -> qp.getActions().getAssociateProjects(),
        qp -> qp.getActions().getDelegate(), qg -> qg.getActions().getManageAiCodeAssurance())
      .containsExactlyInAnyOrder(
        tuple(defaultQualityGate.getName(), false, false, false, false, false, false, false, false),
        tuple(otherQualityGate.getName(), false, false, false, false, false, false, false, false));
  }

  @Test
  void json_example() {
    when(aiCodeAssuranceEntitlement.isEnabled()).thenReturn(true);
    userSession.logIn("admin").addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto defaultQualityGate =
      db.qualityGates().insertQualityGate(qualityGate -> qualityGate.setName("Sonar way").setBuiltIn(true));
    QualityGateConditionDto condition1 = db.qualityGates().addCondition(defaultQualityGate, db.measures().insertMetric());
    QualityGateDto otherQualityGate = db.qualityGates().insertQualityGate(qualityGate -> qualityGate.setName("Sonar way - Without " +
      "Coverage").setBuiltIn(false));
    QualityGateConditionDto condition2 = db.qualityGates().addCondition(otherQualityGate, db.measures().insertMetric());
    db.qualityGates().setDefaultQualityGate(defaultQualityGate);
    doReturn(COMPLIANT).when(qualityGateCaycChecker).checkCaycCompliant(argThat(hasCondition(condition1)), any(List.class));
    doReturn(NON_COMPLIANT).when(qualityGateCaycChecker).checkCaycCompliant(argThat(hasCondition(condition2)), any(List.class));

    String response = ws.newRequest().execute().getInput();

    assertJson(response).ignoreFields("default")
      .isSimilarTo(getClass().getResource("list-example.json"));
  }

  @Test
  void verify_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action.since()).isEqualTo("4.3");
    assertThat(action.key()).isEqualTo("list");
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.changelog()).isNotEmpty();
    assertThat(action.params()).extracting(WebService.Param::key, WebService.Param::isRequired).isEmpty();
  }

}
