/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
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
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ID;

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
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.since()).isEqualTo("4.3");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("id");

    WebService.Param id = definition.param("id");
    assertThat(id.isRequired()).isTrue();
  }

  @Test
  public void delete_quality_gate() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    Long qualityGateId = qualityGate.getId();
    assertThat(db.getDbClient().qualityGateDao().selectById(dbSession, qualityGateId)).isNotNull();

    ws.newRequest()
      .setParam(PARAM_ID, valueOf(qualityGateId))
      .execute();

    assertThat(db.getDbClient().qualityGateDao().selectById(dbSession, qualityGateId)).isNull();
  }

  @Test
  public void delete_quality_gate_if_non_default_when_a_default_exist() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("To Delete"));
    Long toDeleteQualityGateId = qualityGate.getId();
    assertThat(db.getDbClient().qualityGateDao().selectById(dbSession, toDeleteQualityGateId)).isNotNull();

    QualityGateDto defaultQualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("Default"));
    db.qualityGates().setDefaultQualityGate(defaultQualityGate);

    ws.newRequest()
      .setParam(PARAM_ID, valueOf(toDeleteQualityGateId))
      .execute();

    assertThat(db.getDbClient().qualityGateDao().selectById(dbSession, toDeleteQualityGateId)).isNull();
  }

  @Test
  public void does_not_delete_built_in_quality_gate() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setBuiltIn(true));
    Long qualityGateId = qualityGate.getId();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("Operation forbidden for built-in Quality Gate '%s'", qualityGate.getName()));

    ws.newRequest()
      .setParam(PARAM_ID, valueOf(qualityGateId))
      .execute();

    assertThat(db.getDbClient().qualityGateDao().selectById(dbSession, qualityGateId)).isNotNull();
  }

  @Test
  public void fail_when_invalid_id() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());

    String invalidId = "invalid-id";

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("The 'id' parameter cannot be parsed as a long value: %s", invalidId));

    ws.newRequest()
      .setParam(PARAM_ID, valueOf(invalidId))
      .execute();
  }

  @Test
  public void fail_when_missing_id() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);

    ws.newRequest()
      .setParam(PARAM_ID, valueOf(EMPTY))
      .execute();
  }

  @Test
  public void fail_to_delete_default_quality_gate() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("To Delete"));
    db.qualityGates().setDefaultQualityGate(qualityGate);
    Long qualityGateId = qualityGate.getId();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The default quality gate cannot be removed");

    ws.newRequest()
      .setParam(PARAM_ID, valueOf(qualityGateId))
      .execute();
  }

  @Test
  public void fail_on_unknown_quality_gate() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());

    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setParam(PARAM_ID, "123")
      .execute();
  }

  @Test
  public void fail_when_not_quality_gates_administer() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_PROFILES, db.getDefaultOrganization());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("old name"));

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam(PARAM_ID, qualityGate.getId().toString())
      .execute();
  }

}
