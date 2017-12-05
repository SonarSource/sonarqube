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
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualitygate.QualityGateUpdater;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_NAME;

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

  private CopyAction underTest = new CopyAction(dbClient, userSession, defaultOrganizationProvider, qualityGateUpdater);
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
        tuple("name", true));
  }

  @Test
  public void copy() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, defaultOrganizationProvider.get().getUuid());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = db.measures().insertMetric();
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric);

    ws.newRequest()
      .setParam(PARAM_ID, qualityGate.getId().toString())
      .setParam(PARAM_NAME, "new-name")
      .execute();

    QualityGateDto actual = db.getDbClient().qualityGateDao().selectByName(dbSession, "new-name");
    assertThat(actual).isNotNull();
    assertThat(actual.isBuiltIn()).isFalse();
    assertThat(actual.getId()).isNotEqualTo(qualityGate.getId());
    assertThat(actual.getUuid()).isNotEqualTo(qualityGate.getUuid());

    assertThat(db.getDbClient().gateConditionDao().selectForQualityGate(dbSession, qualityGate.getId()))
      .extracting(c-> (int) c.getMetricId(), QualityGateConditionDto::getPeriod, QualityGateConditionDto::getWarningThreshold, QualityGateConditionDto::getErrorThreshold)
      .containsExactlyInAnyOrder(tuple(metric.getId(), condition.getPeriod(), condition.getWarningThreshold(), condition.getErrorThreshold()));
  }

  @Test
  public void fail_when_missing_administer_quality_gate_permission() {
    userSession.addPermission(ADMINISTER_QUALITY_PROFILES, defaultOrganizationProvider.get().getUuid());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam(PARAM_ID, qualityGate.getId().toString())
      .setParam(PARAM_NAME, "new-name")
      .execute();
  }

  @Test
  public void copy_of_builtin_should_not_be_builtin() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, defaultOrganizationProvider.get().getUuid());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qualityGateDto -> qualityGateDto.setBuiltIn(true));

    ws.newRequest()
      .setParam(PARAM_ID, qualityGate.getId().toString())
      .setParam(PARAM_NAME, "new-name")
      .execute();

    QualityGateDto actual = db.getDbClient().qualityGateDao().selectByName(dbSession, "new-name");
    assertThat(actual.isBuiltIn()).isFalse();
  }

  @Test
  public void fail_when_id_parameter_is_missing() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, defaultOrganizationProvider.get().getUuid());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'id' parameter is missing");

    ws.newRequest()
      .setParam(PARAM_NAME, "new-name")
      .execute();
  }

  @Test
  public void fail_when_quality_gate_id_is_not_found() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, defaultOrganizationProvider.get().getUuid());

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No quality gate has been found for id 123");

    ws.newRequest()
      .setParam(PARAM_ID, "123")
      .setParam(PARAM_NAME, "new-name")
      .execute();
  }

  @Test
  public void fail_when_name_parameter_is_missing() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, defaultOrganizationProvider.get().getUuid());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'name' parameter is missing");

    ws.newRequest()
      .setParam(PARAM_ID, qualityGate.getId().toString())
      .execute();
  }

  @Test
  public void fail_when_name_parameter_match_existing_quality_gate() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, defaultOrganizationProvider.get().getUuid());
    QualityGateDto existingQualityGate = db.qualityGates().insertQualityGate();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Name has already been taken");

    ws.newRequest()
      .setParam(PARAM_ID, qualityGate.getId().toString())
      .setParam(PARAM_NAME, existingQualityGate.getName())
      .execute();
  }
}
