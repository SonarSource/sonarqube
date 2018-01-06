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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.QGateWithOrgDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonar.server.qualitygate.QualityGateUpdater;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualitygates.QualityGate;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_NAME;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ORGANIZATION;

public class CopyActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private QualityGateUpdater qualityGateUpdater = new QualityGateUpdater(dbClient, UuidFactoryFast.getInstance());
  private QualityGateFinder qualityGateFinder = new QualityGateFinder(dbClient);
  private QualityGatesWsSupport wsSupport = new QualityGatesWsSupport(dbClient, userSession, defaultOrganizationProvider);

  private CopyAction underTest = new CopyAction(dbClient, userSession, qualityGateUpdater, qualityGateFinder, wsSupport);
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void definition() {
    WebService.Action action = ws.getDef();

    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isTrue();
    assertThat(action.responseExampleAsString()).isNullOrEmpty();
    assertThat(action.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("id", true),
        tuple("organization", false),
        tuple("name", true));
  }

  @Test
  public void copy() {

    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);

    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    MetricDto metric = db.measures().insertMetric();
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric);

    ws.newRequest()
      .setParam(PARAM_ID, qualityGate.getId().toString())
      .setParam(PARAM_NAME, "new-name")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();

    QGateWithOrgDto actual = db.getDbClient().qualityGateDao().selectByOrganizationAndName(dbSession, organization, "new-name");
    assertThat(actual).isNotNull();
    assertThat(actual.isBuiltIn()).isFalse();
    assertThat(actual.getId()).isNotEqualTo(qualityGate.getId());
    assertThat(actual.getUuid()).isNotEqualTo(qualityGate.getUuid());

    assertThat(db.getDbClient().gateConditionDao().selectForQualityGate(dbSession, qualityGate.getId()))
      .extracting(c-> (int) c.getMetricId(), QualityGateConditionDto::getPeriod, QualityGateConditionDto::getWarningThreshold, QualityGateConditionDto::getErrorThreshold)
      .containsExactlyInAnyOrder(tuple(metric.getId(), condition.getPeriod(), condition.getWarningThreshold(), condition.getErrorThreshold()));
  }

  @Test
  public void default_organization_is_used_when_no_organization_parameter(){
    OrganizationDto defaultOrganization = db.getDefaultOrganization();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, defaultOrganization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(defaultOrganization);

    ws.newRequest()
      .setParam(PARAM_ID, qualityGate.getId().toString())
      .setParam(PARAM_NAME, "new-name")
      .execute();

    QGateWithOrgDto actual = db.getDbClient().qualityGateDao().selectByOrganizationAndName(dbSession, defaultOrganization, "new-name");
    assertThat(actual).isNotNull();
    assertThat(actual.getOrganizationUuid()).isEqualTo(defaultOrganization.getUuid());
  }

  @Test
  public void copy_of_builtin_should_not_be_builtin() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization, qualityGateDto -> qualityGateDto.setBuiltIn(true));

    ws.newRequest()
      .setParam(PARAM_ID, qualityGate.getId().toString())
      .setParam(PARAM_NAME, "new-name")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();

    QualityGateDto actual = db.getDbClient().qualityGateDao().selectByName(dbSession, "new-name");
    assertThat(actual).isNotNull();
    assertThat(actual.isBuiltIn()).isFalse();
  }

  @Test
  public void response_contains_quality_gate() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, defaultOrganizationProvider.get().getUuid());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());

    QualityGate response = ws.newRequest()
      .setParam(PARAM_ID, qualityGate.getId().toString())
      .setParam(PARAM_NAME, "new-name")
      .executeProtobuf(QualityGate.class);

    assertThat(response).isNotNull();
    assertThat(response.getId()).isNotEqualTo(qualityGate.getId());
    assertThat(response.getName()).isEqualTo("new-name");
  }

  @Test
  public void quality_gates_can_have_the_same_name_in_different_organization() {
    OrganizationDto organization1 = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization1);
    QualityGateDto qualityGate1 = db.qualityGates().insertQualityGate(organization1);

    OrganizationDto organization2 = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization2);
    QualityGateDto qualityGate2 = db.qualityGates().insertQualityGate(organization2);

    assertThat(qualityGate1.getName()).isNotEqualTo(qualityGate2.getName());

    ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization2.getKey())
      .setParam(PARAM_ID, qualityGate2.getId().toString())
      .setParam(PARAM_NAME, qualityGate1.getName())
      .execute();

    QGateWithOrgDto actual = db.getDbClient().qualityGateDao().selectByOrganizationAndName(dbSession, organization2, qualityGate1.getName());
    assertThat(actual).isNotNull();
  }

  @Test
  public void quality_gate_from_external_organization_can_not_be_copied(){
    OrganizationDto organization1 = db.organizations().insert();
    QualityGateDto qualityGate1 = db.qualityGates().insertQualityGate(organization1);

    OrganizationDto organization2 = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization2);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("No quality gate has been found for id %s in organization %s", qualityGate1.getId(), organization2.getName()));

    ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization2.getKey())
      .setParam(PARAM_ID, qualityGate1.getId().toString())
      .setParam(PARAM_NAME, "new-name")
      .execute();
  }

  @Test
  public void fail_when_missing_administer_quality_gate_permission() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_PROFILES, organization);

    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam(PARAM_ID, qualityGate.getId().toString())
      .setParam(PARAM_NAME, "new-name")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_id_parameter_is_missing() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'id' parameter is missing");

    ws.newRequest()
      .setParam(PARAM_NAME, "new-name")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_quality_gate_id_is_not_found() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format(
      "No quality gate has been found for id 123 in organization %s", organization.getName()));

    ws.newRequest()
      .setParam(PARAM_ID, "123")
      .setParam(PARAM_NAME, "new-name")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_name_parameter_is_missing() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'name' parameter is missing");

    ws.newRequest()
      .setParam(PARAM_ID, qualityGate.getId().toString())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_name_parameter_is_empty() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'name' parameter is empty");

    ws.newRequest()
      .setParam(PARAM_ID, qualityGate.getId().toString())
      .setParam(PARAM_NAME, "")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_name_parameter_match_existing_quality_gate_in_the_same_organization() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);
    QualityGateDto existingQualityGate = db.qualityGates().insertQualityGate(organization);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Name has already been taken");

    ws.newRequest()
      .setParam(PARAM_ID, qualityGate.getId().toString())
      .setParam(PARAM_NAME, existingQualityGate.getName())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }
}
