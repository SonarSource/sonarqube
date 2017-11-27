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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualitygate.QualityGates;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_ID;

public class DestroyActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private TestDefaultOrganizationProvider organizationProvider = TestDefaultOrganizationProvider.from(db);
  private QualityGates qualityGates = new QualityGates(dbClient, userSession, organizationProvider);
  private WsActionTester ws;
  private DestroyAction underTest;

  @Before
  public void setUp() {
    underTest = new DestroyAction(qualityGates);
    ws = new WsActionTester(underTest);

  }

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
  public void should_delete_quality_gate() {
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
  public void should_delete_quality_gate_even_if_default() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("To Delete"));
    Long qualityGateId = qualityGate.getId();
    db.properties().insertProperty(new PropertyDto().setKey("sonar.qualitygate").setValue(Long.toString(qualityGateId)));
    assertThat(db.getDbClient().qualityGateDao().selectById(dbSession, qualityGateId)).isNotNull();

    ws.newRequest()
      .setParam(PARAM_ID, valueOf(qualityGateId))
      .execute();

    assertThat(db.getDbClient().qualityGateDao().selectById(dbSession, qualityGateId)).isNull();
  }

  @Test
  public void should_delete_quality_gate_if_non_default_when_a_default_exist() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("To Delete"));
    Long toDeleteQualityGateId = qualityGate.getId();
    assertThat(db.getDbClient().qualityGateDao().selectById(dbSession, toDeleteQualityGateId)).isNotNull();

    QualityGateDto defaultqualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("Default"));
    Long defaultQualityGateId = defaultqualityGate.getId();
    db.properties().insertProperty(new PropertyDto().setKey("sonar.qualitygate").setValue(Long.toString(defaultQualityGateId)));
    assertThat(db.getDbClient().qualityGateDao().selectById(dbSession, defaultQualityGateId)).isNotNull();

    ws.newRequest()
      .setParam(PARAM_ID, valueOf(toDeleteQualityGateId))
      .execute();

    assertThat(db.getDbClient().qualityGateDao().selectById(dbSession, toDeleteQualityGateId)).isNull();
    assertThat(db.getDbClient().qualityGateDao().selectById(dbSession, defaultQualityGateId)).isNotNull();
  }

}
