/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_ID;

public class DeleteConditionActionTest {

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private TestDefaultOrganizationProvider organizationProvider = TestDefaultOrganizationProvider.from(db);

  private WsActionTester ws = new WsActionTester(new DeleteConditionAction(userSession, db.getDbClient(), new QualityGatesWsSupport(db.getDbClient(), organizationProvider)));

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.since()).isEqualTo("4.3");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.params()).extracting(Param::key).containsExactlyInAnyOrder("id");

    Param id = definition.param("id");
    assertThat(id.isRequired()).isTrue();
  }

  @Test
  public void delete_condition() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = db.measures().insertMetric();
    QualityGateConditionDto qualityGateCondition = db.qualityGates().addCondition(qualityGate, metric);

    call(qualityGateCondition.getId());

    assertThat(searchConditionsOf(qualityGate)).isEmpty();
  }

  @Test
  public void no_content() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = db.measures().insertMetric();
    QualityGateConditionDto qualityGateCondition = db.qualityGates().addCondition(qualityGate, metric);

    TestResponse result = call(qualityGateCondition.getId());

    assertThat(result.getStatus()).isEqualTo(HTTP_NO_CONTENT);
  }

  @Test
  public void fail_if_not_quality_gate_administrator() {
    userSession.addPermission(ADMINISTER, db.getDefaultOrganization());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = db.measures().insertMetric();
    QualityGateConditionDto qualityGateCondition = db.qualityGates().addCondition(qualityGate, metric);

    expectedException.expect(ForbiddenException.class);

    call(qualityGateCondition.getId());
  }

  @Test
  public void fail_if_condition_id_is_not_found() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = db.measures().insertMetric();
    QualityGateConditionDto qualityGateCondition = db.qualityGates().addCondition(qualityGate, metric);
    long unknownConditionId = qualityGateCondition.getId() + 42L;

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No quality gate condition with id '" + unknownConditionId + "'");

    call(unknownConditionId);
  }

  private TestResponse call(long qualityGateConditionId) {
    return ws.newRequest()
      .setParam(PARAM_ID, String.valueOf(qualityGateConditionId))
      .execute();
  }

  private Collection<QualityGateConditionDto> searchConditionsOf(QualityGateDto qualityGate) {
    return db.getDbClient().gateConditionDao().selectForQualityGate(db.getSession(), qualityGate.getId());
  }
}
