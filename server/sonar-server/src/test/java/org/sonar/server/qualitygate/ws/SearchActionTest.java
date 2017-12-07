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
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualitygate.QgateProjectFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualitygates.SearchResponse;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.sonar.api.utils.System2.INSTANCE;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ORGANIZATION;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PAGE;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PAGE_SIZE;

public class SearchActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);

  private QgateProjectFinder projectFinder = new QgateProjectFinder(dbClient, userSession);
  private QualityGatesWsSupport wsSupport = new QualityGatesWsSupport(dbClient, userSession, defaultOrganizationProvider);

  private SearchAction underTest = new SearchAction(dbClient, projectFinder, wsSupport);
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void definition() {
    WebService.Action action = ws.getDef();

    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isFalse();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("gateId", true),
        tuple("query", false),
        tuple("organization", false),
        tuple("selected", false),
        tuple("page", false),
        tuple("pageSize", false));
  }

  @Test
  public void search_projects_of_a_quality_gate_from_an_organization() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(organization);
    associateProjectToQualityGate(project, qualityGate);
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);

    SearchResponse response = ws.newRequest()
      .setParam(PARAM_GATE_ID, valueOf(qualityGate.getId()))
      .setParam(PARAM_ORGANIZATION, valueOf(organization.getKey()))
      .executeProtobuf(SearchResponse.class);

    assertThat(response).isNotNull();
    assertThat(response.getMore()).isFalse();
    assertThat(response.getResultsCount()).isEqualTo(1);
    assertThat(response.getResults(0).getId()).isEqualTo(project.getId());
    assertThat(response.getResults(0).getName().substring(4)).isEqualTo(project.getKey().substring(3));
  }

  @Test
  public void search_on_default_organization_when_none_is_provided() {
    OrganizationDto defaultOrganization = db.getDefaultOrganization();

    ComponentDto project = db.components().insertPublicProject(defaultOrganization);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(defaultOrganization);
    associateProjectToQualityGate(project, qualityGate);
    userSession.addPermission(ADMINISTER_QUALITY_GATES, defaultOrganization);

    SearchResponse response = ws.newRequest()
      .setParam(PARAM_GATE_ID, valueOf(qualityGate.getId()))
      .executeProtobuf(SearchResponse.class);

    assertThat(response).isNotNull();
    assertThat(response.getMore()).isFalse();
    assertThat(response.getResultsCount()).isEqualTo(1);
    assertThat(response.getResults(0).getId()).isEqualTo(project.getId());
    assertThat(response.getResults(0).getName().substring(4)).isEqualTo(project.getKey().substring(3));
  }

  @Test
  public void fail_when_quality_gates_does_not_belong_to_organization() {
    OrganizationDto organization = db.organizations().insert();
    OrganizationDto otherOrganization = db.organizations().insert();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(otherOrganization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Quality gate '%s' does not exists.", qualityGate.getId()));

    ws.newRequest()
      .setParam(PARAM_GATE_ID, valueOf(qualityGate.getId()))
      .setParam(PARAM_ORGANIZATION, valueOf(organization.getKey()))
      .executeProtobuf(SearchResponse.class);

  }

  @Test
  public void more_is_true_when_not_all_project_fit_in_page_size() {
    OrganizationDto organization = db.organizations().insert();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(organization);
    for (int i = 0; i < 20; i++) {
      ComponentDto project1 = db.components().insertPublicProject();
      associateProjectToQualityGate(project1, qualityGate);
    }

    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);

    SearchResponse response = ws.newRequest()
      .setParam(PARAM_GATE_ID, valueOf(qualityGate.getId()))
      .setParam(PARAM_ORGANIZATION, valueOf(organization.getKey()))
      .setParam(PARAM_PAGE_SIZE, valueOf(10))
      .setParam(PARAM_PAGE, valueOf(1))
      .executeProtobuf(SearchResponse.class);

    assertThat(response).isNotNull();
    assertThat(response.getMore()).isTrue();
    assertThat(response.getResultsCount()).isEqualTo(10);
  }

  private void associateProjectToQualityGate(ComponentDto componentDto, QualityGateDto qualityGateDto) {
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto()
      .setKey("sonar.qualitygate")
      .setResourceId(componentDto.getId())
      .setValue(valueOf(qualityGateDto.getId())));
    db.commit();
  }
}
