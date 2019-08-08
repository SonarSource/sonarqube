/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.QGateWithOrgDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ORGANIZATION;

public class DeleteConditionActionTest {

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private TestDefaultOrganizationProvider organizationProvider = TestDefaultOrganizationProvider.from(db);

  private WsActionTester ws = new WsActionTester(
    new DeleteConditionAction(db.getDbClient(), new QualityGatesWsSupport(db.getDbClient(), userSession, organizationProvider)));

  @Test
  public void definition() {
    WebService.Action action = ws.getDef();
    assertThat(action.since()).isEqualTo("4.3");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("id", true),
        tuple("organization", false));
  }

  @Test
  public void delete_condition() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    MetricDto metric = db.measures().insertMetric();
    QualityGateConditionDto qualityGateCondition = db.qualityGates().addCondition(qualityGate, metric);

    ws.newRequest()
      .setParam(PARAM_ID, valueOf(qualityGateCondition.getId()))
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();

    assertThat(searchConditionsOf(qualityGate)).isEmpty();
  }

  @Test
  public void default_organization_is_used_when_no_organization_parameter() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    MetricDto metric = db.measures().insertMetric();
    QualityGateConditionDto qualityGateCondition = db.qualityGates().addCondition(qualityGate, metric);

    ws.newRequest()
      .setParam(PARAM_ID, valueOf(qualityGateCondition.getId()))
      .execute();

    assertThat(searchConditionsOf(qualityGate)).isEmpty();
  }

  @Test
  public void no_content() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    MetricDto metric = db.measures().insertMetric();
    QualityGateConditionDto qualityGateCondition = db.qualityGates().addCondition(qualityGate, metric);

    TestResponse result = ws.newRequest()
      .setParam(PARAM_ID, valueOf(qualityGateCondition.getId()))
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();

    assertThat(result.getStatus()).isEqualTo(HTTP_NO_CONTENT);
  }

  @Test
  public void fail_if_built_in_quality_gate() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization, qg -> qg.setBuiltIn(true));
    MetricDto metric = db.measures().insertMetric();
    QualityGateConditionDto qualityGateCondition = db.qualityGates().addCondition(qualityGate, metric);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("Operation forbidden for built-in Quality Gate '%s'", qualityGate.getName()));

    ws.newRequest()
      .setParam(PARAM_ID, valueOf(qualityGateCondition.getId()))
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void fail_if_not_quality_gate_administrator() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_PROFILES, organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    MetricDto metric = db.measures().insertMetric();
    QualityGateConditionDto qualityGateCondition = db.qualityGates().addCondition(qualityGate, metric);

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam(PARAM_ID, valueOf(qualityGateCondition.getId()))
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void fail_if_condition_id_is_not_found() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_PROFILES, organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    MetricDto metric = db.measures().insertMetric();
    QualityGateConditionDto qualityGateCondition = db.qualityGates().addCondition(qualityGate, metric);
    long unknownConditionId = qualityGateCondition.getId() + 42L;

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No quality gate condition with id '" + unknownConditionId + "'");

    ws.newRequest()
      .setParam(PARAM_ID, valueOf(unknownConditionId))
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_condition_match_unknown_quality_gate() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_PROFILES, organization);
    QualityGateConditionDto condition = new QualityGateConditionDto().setQualityGateId(123L);
    db.getDbClient().gateConditionDao().insert(condition, db.getSession());
    db.commit();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(format("Condition '%s' is linked to an unknown quality gate '%s'", condition.getId(), 123L));

    ws.newRequest()
      .setParam(PARAM_ID, valueOf(condition.getId()))
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_condition_match_quality_gate_on_other_organization() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_PROFILES, organization);
    OrganizationDto otherOrganization = db.organizations().insert();
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(otherOrganization);
    MetricDto metric = db.measures().insertMetric();
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(format("Condition '%s' is linked to an unknown quality gate '%s'", condition.getId(), qualityGate.getId()));

    ws.newRequest()
      .setParam(PARAM_ID, valueOf(condition.getId()))
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  private Collection<QualityGateConditionDto> searchConditionsOf(QualityGateDto qualityGate) {
    return db.getDbClient().gateConditionDao().selectForQualityGate(db.getSession(), qualityGate.getId());
  }
}
