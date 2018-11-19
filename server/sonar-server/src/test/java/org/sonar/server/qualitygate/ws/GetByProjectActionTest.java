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

import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.WsQualityGates;
import org.sonarqube.ws.WsQualityGates.GetByProjectWsResponse;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.test.JsonAssert.assertJson;

public class GetByProjectActionTest {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private ComponentDbTester componentDb = new ComponentDbTester(db);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private WsActionTester ws = new WsActionTester(
    new GetByProjectAction(userSession, dbClient, TestComponentFinder.from(db), new QualityGateFinder(dbClient)));

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.description()).isNotEmpty();
    assertThat(def.isInternal()).isFalse();
    assertThat(def.since()).isEqualTo("6.1");
    assertThat(def.changelog()).extracting(Change::getVersion, Change::getDescription).containsExactlyInAnyOrder(
      tuple("6.6", "The parameter 'projectId' has been removed"),
      tuple("6.6", "The parameter 'projectKey' has been renamed to 'project'"),
      tuple("6.6", "This webservice is now part of the public API")
    );
    assertThat(def.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("project");

    WebService.Param projectKey = def.param("project");
    assertThat(projectKey.description()).isNotEmpty();
    assertThat(projectKey.isRequired()).isTrue();
    assertThat(projectKey.exampleValue()).isNotEmpty();
  }

  @Test
  public void json_example() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project = componentDb.insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto));
    QualityGateDto qualityGate = insertQualityGate("My team QG");
    associateProjectToQualityGate(project.getId(), qualityGate.getId());
    logInAsProjectUser(project);

    String result = ws.newRequest().setParam("project", project.getKey()).execute().getInput();

    assertJson(result)
      .ignoreFields("id")
      .isSimilarTo(getClass().getResource("get_by_project-example.json"));
  }

  @Test
  public void empty_response() {
    ComponentDto project = componentDb.insertPrivateProject();
    insertQualityGate("Another QG");
    logInAsProjectUser(project);

    String result = ws.newRequest().setParam("project", project.getKey()).execute().getInput();

    assertThat(result).isEqualToIgnoringWhitespace("{}");
  }

  @Test
  public void default_quality_gate() {
    ComponentDto project = componentDb.insertComponent(ComponentTesting.newPrivateProjectDto(db.organizations().insert()));
    QualityGateDto dbQualityGate = insertQualityGate("Sonar way");
    setDefaultQualityGate(dbQualityGate.getId());
    logInAsProjectUser(project);

    GetByProjectWsResponse result = callByKey(project.getKey());

    WsQualityGates.QualityGate qualityGate = result.getQualityGate();
    assertThat(Long.valueOf(qualityGate.getId())).isEqualTo(dbQualityGate.getId());
    assertThat(qualityGate.getName()).isEqualTo(dbQualityGate.getName());
    assertThat(qualityGate.getDefault()).isTrue();
  }

  @Test
  public void project_quality_gate_over_default() {
    ComponentDto project = componentDb.insertComponent(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()));
    QualityGateDto defaultDbQualityGate = insertQualityGate("Sonar way");
    QualityGateDto dbQualityGate = insertQualityGate("My team QG");
    setDefaultQualityGate(defaultDbQualityGate.getId());
    associateProjectToQualityGate(project.getId(), dbQualityGate.getId());
    logInAsProjectUser(project);

    GetByProjectWsResponse result = callByKey(project.getKey());

    WsQualityGates.QualityGate qualityGate = result.getQualityGate();
    assertThat(qualityGate.getName()).isEqualTo(dbQualityGate.getName());
    assertThat(qualityGate.getDefault()).isFalse();
  }

  @Test
  public void get_by_project_key() {
    ComponentDto project = componentDb.insertComponent(ComponentTesting.newPrivateProjectDto(db.organizations().insert()));
    QualityGateDto dbQualityGate = insertQualityGate("My team QG");
    associateProjectToQualityGate(project.getId(), dbQualityGate.getId());
    logInAsProjectUser(project);

    GetByProjectWsResponse result = callByKey(project.getDbKey());

    assertThat(result.getQualityGate().getName()).isEqualTo(dbQualityGate.getName());
  }

  @Test
  public void get_with_project_admin_permission() {
    ComponentDto project = componentDb.insertPrivateProject();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    QualityGateDto dbQualityGate = insertQualityGate("Sonar way");
    setDefaultQualityGate(dbQualityGate.getId());

    GetByProjectWsResponse result = callByKey(project.getKey());

    assertThat(result.getQualityGate().getName()).isEqualTo(dbQualityGate.getName());
  }

  @Test
  public void get_with_project_user_permission() {
    ComponentDto project = componentDb.insertPrivateProject();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    QualityGateDto dbQualityGate = insertQualityGate("Sonar way");
    setDefaultQualityGate(dbQualityGate.getId());

    GetByProjectWsResponse result = callByKey(project.getKey());

    assertThat(result.getQualityGate().getName()).isEqualTo(dbQualityGate.getName());
  }

  @Test
  public void fail_when_insufficient_permission() {
    ComponentDto project = componentDb.insertComponent(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()));
    userSession.logIn();
    QualityGateDto dbQualityGate = insertQualityGate("Sonar way");
    setDefaultQualityGate(dbQualityGate.getId());

    expectedException.expect(ForbiddenException.class);

    callByKey(project.getKey());
  }

  @Test
  public void fail_when_project_does_not_exist() {
    expectedException.expect(NotFoundException.class);

    callByKey("Unknown");
  }

  @Test
  public void fail_when_no_parameter() {
    expectedException.expect(IllegalArgumentException.class);

    call(null);
  }

  @Test
  public void fail_when_using_branch_db_key() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organization);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component key '%s' not found", branch.getDbKey()));

    call(branch.getDbKey());
  }

  private GetByProjectWsResponse callByKey(String projectKey) {
    return call(projectKey);
  }

  private GetByProjectWsResponse call(@Nullable String projectKey) {
    TestRequest request = ws.newRequest();
    if (projectKey != null) {
      request.setParam("project", projectKey);
    }
    return request.executeProtobuf(GetByProjectWsResponse.class);
  }

  private QualityGateDto insertQualityGate(String name) {
    QualityGateDto qualityGate = dbClient.qualityGateDao().insert(dbSession, new QualityGateDto().setName(name));
    db.commit();
    return qualityGate;
  }

  private void associateProjectToQualityGate(long componentId, long qualityGateId) {
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto()
      .setKey("sonar.qualitygate")
      .setResourceId(componentId)
      .setValue(String.valueOf(qualityGateId)));
    db.commit();
  }

  private void setDefaultQualityGate(long qualityGateId) {
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto()
      .setKey("sonar.qualitygate")
      .setValue(String.valueOf(qualityGateId)));
    db.commit();
  }

  private void logInAsProjectUser(ComponentDto project) {
    userSession.logIn().addProjectPermission(UserRole.USER, project);
  }
}
