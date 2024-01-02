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

import java.util.Collection;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ID;

public class DeleteConditionActionIT {

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final WsActionTester ws = new WsActionTester(
    new DeleteConditionAction(db.getDbClient(), new QualityGatesWsSupport(db.getDbClient(), userSession, TestComponentFinder.from(db))));

  @Test
  public void definition() {
    WebService.Action action = ws.getDef();
    assertThat(action.since()).isEqualTo("4.3");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("id", true));
  }

  @Test
  public void delete_condition() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = db.measures().insertMetric();
    QualityGateConditionDto qualityGateCondition = db.qualityGates().addCondition(qualityGate, metric);

    ws.newRequest()
      .setParam(PARAM_ID, qualityGateCondition.getUuid())
      .execute();

    assertThat(searchConditionsOf(qualityGate)).isEmpty();
  }

  @Test
  public void no_content() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = db.measures().insertMetric();
    QualityGateConditionDto qualityGateCondition = db.qualityGates().addCondition(qualityGate, metric);

    TestResponse result = ws.newRequest()
      .setParam(PARAM_ID, qualityGateCondition.getUuid())
      .execute();

    assertThat(result.getStatus()).isEqualTo(HTTP_NO_CONTENT);
  }

  @Test
  public void user_with_permission_can_call_endpoint() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = db.measures().insertMetric();
    QualityGateConditionDto qualityGateCondition = db.qualityGates().addCondition(qualityGate, metric);
    UserDto user = db.users().insertUser();
    db.qualityGates().addUserPermission(qualityGate, user);
    userSession.logIn(user);

    TestResponse response = ws.newRequest()
      .setParam(PARAM_ID, qualityGateCondition.getUuid())
      .execute();

    assertThat(response.getStatus()).isEqualTo(HTTP_NO_CONTENT);
  }

  @Test
  public void user_with_group_permission_can_call_endpoint() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = db.measures().insertMetric();
    QualityGateConditionDto qualityGateCondition = db.qualityGates().addCondition(qualityGate, metric);
    UserDto user = db.users().insertUser();
    GroupDto group = db.users().insertGroup();
    db.qualityGates().addGroupPermission(qualityGate, group);
    userSession.logIn(user).setGroups(group);

    TestResponse response = ws.newRequest()
      .setParam(PARAM_ID, qualityGateCondition.getUuid())
      .execute();

    assertThat(response.getStatus()).isEqualTo(HTTP_NO_CONTENT);
  }

  @Test
  public void fail_if_built_in_quality_gate() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setBuiltIn(true));
    MetricDto metric = db.measures().insertMetric();
    QualityGateConditionDto qualityGateCondition = db.qualityGates().addCondition(qualityGate, metric);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_ID, qualityGateCondition.getUuid())
      .execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(format("Operation forbidden for built-in Quality Gate '%s'", qualityGate.getName()));
  }

  @Test
  public void fail_if_not_quality_gate_administrator() {
    userSession.addPermission(ADMINISTER_QUALITY_PROFILES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = db.measures().insertMetric();
    QualityGateConditionDto qualityGateCondition = db.qualityGates().addCondition(qualityGate, metric);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_ID, qualityGateCondition.getUuid())
      .execute())
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_condition_uuid_is_not_found() {
    userSession.addPermission(ADMINISTER_QUALITY_PROFILES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = db.measures().insertMetric();
    QualityGateConditionDto qualityGateCondition = db.qualityGates().addCondition(qualityGate, metric);
    String unknownConditionUuid = "unknown";

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_ID, unknownConditionUuid)
      .execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("No quality gate condition with uuid '" + unknownConditionUuid + "'");
  }

  @Test
  public void fail_when_condition_match_unknown_quality_gate() {
    userSession.addPermission(ADMINISTER_QUALITY_PROFILES);
    QualityGateConditionDto condition = new QualityGateConditionDto().setUuid("uuid").setMetricUuid("metric").setQualityGateUuid("123");
    db.getDbClient().gateConditionDao().insert(condition, db.getSession());
    db.commit();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_ID, condition.getUuid())
      .execute())
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining(format("Condition '%s' is linked to an unknown quality gate '%s'", condition.getUuid(), 123L));
  }

  private Collection<QualityGateConditionDto> searchConditionsOf(QualityGateDto qualityGate) {
    return db.getDbClient().gateConditionDao().selectForQualityGate(db.getSession(), qualityGate.getUuid());
  }
}
