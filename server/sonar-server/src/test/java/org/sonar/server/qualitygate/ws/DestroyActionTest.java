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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.QGateWithOrgDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ORGANIZATION;

public class DestroyActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private TestDefaultOrganizationProvider organizationProvider = TestDefaultOrganizationProvider.from(db);
  private QualityGateFinder qualityGateFinder = new QualityGateFinder(dbClient);
  private QualityGatesWsSupport wsSupport = new QualityGatesWsSupport(db.getDbClient(), userSession, organizationProvider);

  private DbSession dbSession = db.getSession();
  private DestroyAction underTest = new DestroyAction(dbClient, wsSupport, qualityGateFinder);
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void delete_quality_gate() {
    OrganizationDto organization = db.organizations().insert();
    db.qualityGates().createDefaultQualityGate(organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);

    ws.newRequest()
      .setParam(PARAM_ID, qualityGate.getId().toString())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();

    assertThat(db.getDbClient().qualityGateDao().selectByOrganizationAndId(dbSession, organization, qualityGate.getId())).isNull();
  }

  @Test
  public void delete_quality_gate_if_non_default_when_a_default_exist() {
    OrganizationDto organization = db.organizations().insert();
    db.qualityGates().createDefaultQualityGate(organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);

    ws.newRequest()
      .setParam(PARAM_ID, valueOf(qualityGate.getId()))
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();

    assertThat(db.getDbClient().qualityGateDao().selectByOrganizationAndId(dbSession, organization, qualityGate.getId())).isNull();
  }

  @Test
  public void delete_quality_gate_and_any_association_to_any_project() {
    OrganizationDto organization = db.organizations().insert();
    db.qualityGates().createDefaultQualityGate(organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    ComponentDto prj1 = db.components().insertPublicProject(organization);
    ComponentDto prj2 = db.components().insertPublicProject(organization);
    db.qualityGates().associateProjectToQualityGate(prj1, qualityGate);
    db.qualityGates().associateProjectToQualityGate(prj2, qualityGate);
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);

    ws.newRequest()
      .setParam(PARAM_ID, valueOf(qualityGate.getId()))
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();

    assertThat(db.getDbClient().projectQgateAssociationDao().selectQGateUuidByComponentUuid(dbSession, prj1.uuid()))
      .isEmpty();
    assertThat(db.getDbClient().projectQgateAssociationDao().selectQGateUuidByComponentUuid(dbSession, prj2.uuid()))
      .isEmpty();

    assertThat(db.getDbClient().projectQgateAssociationDao().selectQGateUuidByComponentUuid(dbSession, prj1.uuid()))
      .isEmpty();
    assertThat(db.getDbClient().projectQgateAssociationDao().selectQGateUuidByComponentUuid(dbSession, prj2.uuid()))
      .isEmpty();
  }

  @Test
  public void does_not_delete_built_in_quality_gate() {
    OrganizationDto organization = db.organizations().insert();
    db.qualityGates().createDefaultQualityGate(organization);
    QualityGateDto builtInQualityGate = db.qualityGates().insertBuiltInQualityGate();
    db.getDbClient().qualityGateDao().associate(db.getSession(), Uuids.createFast(), organization, builtInQualityGate);
    db.commit();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("Operation forbidden for built-in Quality Gate '%s'", builtInQualityGate.getName()));

    ws.newRequest()
      .setParam(PARAM_ID, valueOf(builtInQualityGate.getId()))
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void default_organization_is_used_when_no_organization_parameter() {
    OrganizationDto defaultOrganization = db.getDefaultOrganization();
    db.qualityGates().createDefaultQualityGate(defaultOrganization);
    userSession.addPermission(ADMINISTER_QUALITY_GATES, defaultOrganization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(defaultOrganization);
    OrganizationDto otherOrganization = db.organizations().insert();
    QGateWithOrgDto otherQualityGate = db.qualityGates().insertQualityGate(otherOrganization);

    ws.newRequest()
      .setParam(PARAM_ID, valueOf(qualityGate.getId()))
      .execute();

    assertThat(db.getDbClient().qualityGateDao().selectByOrganizationAndId(dbSession, defaultOrganization, qualityGate.getId())).isNull();
  }

  @Test
  public void fail_when_invalid_id() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);

    String invalidId = "invalid-id";

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("The 'id' parameter cannot be parsed as a long value: %s", invalidId));

    ws.newRequest()
      .setParam(PARAM_ID, valueOf(invalidId))
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_missing_id() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);

    expectedException.expect(IllegalArgumentException.class);

    ws.newRequest()
      .setParam(PARAM_ID, valueOf(EMPTY))
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void fail_to_delete_default_quality_gate() {
    OrganizationDto organization = db.organizations().insert();
    QualityGateDto defaultQualityGate = db.qualityGates().createDefaultQualityGate(organization);
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The default quality gate cannot be removed");

    ws.newRequest()
      .setParam(PARAM_ID, valueOf(defaultQualityGate.getId()))
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void fail_on_unknown_quality_gate() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);

    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setParam(PARAM_ID, "123")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_not_quality_gates_administer() {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    db.qualityGates().createDefaultQualityGate(organization);
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_PROFILES, organization);

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam(PARAM_ID, qualityGate.getId().toString())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void definition() {
    WebService.Action action = ws.getDef();

    assertThat(action.since()).isEqualTo("4.3");
    assertThat(action.isPost()).isTrue();

    assertThat(action.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("id", true),
        tuple("organization", false));
  }

}
