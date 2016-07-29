/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualitygate.QualityGates;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsQualityGates;
import org.sonarqube.ws.WsQualityGates.GetByProjectWsResponse;

import static com.google.common.base.Throwables.propagate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_PROJECT_KEY;

public class GetByProjectActionTest {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  ComponentDbTester componentDb = new ComponentDbTester(db);
  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();

  private WsActionTester ws = new WsActionTester(
    new GetByProjectAction(userSession, dbClient, new ComponentFinder(dbClient),
      new QualityGates(dbClient, mock(MetricFinder.class), mock(UserSession.class), mock(Settings.class))));

  @Test
  public void json_example() {
    ComponentDto project = componentDb.insertComponent(newProjectDto());
    QualityGateDto qualityGate = insertQualityGate("My team QG");
    associateProjectToQualityGate(project.getId(), qualityGate.getId());

    String result = ws.newRequest().setParam(PARAM_PROJECT_ID, project.uuid()).execute().getInput();

    assertJson(result)
      .ignoreFields("id")
      .isSimilarTo(getClass().getResource("get_by_project-example.json"));
  }

  @Test
  public void empty_response() {
    ComponentDto project = componentDb.insertComponent(newProjectDto());
    insertQualityGate("Another QG");

    String result = ws.newRequest().setParam(PARAM_PROJECT_ID, project.uuid()).execute().getInput();

    assertThat(result).isEqualToIgnoringWhitespace("{}");
  }

  @Test
  public void default_quality_gate() {
    ComponentDto project = componentDb.insertComponent(newProjectDto());
    QualityGateDto dbQualityGate = insertQualityGate("Sonar way");
    setDefaultQualityGate(dbQualityGate.getId());

    GetByProjectWsResponse result = callByUuid(project.uuid());

    WsQualityGates.QualityGate qualityGate = result.getQualityGate();
    assertThat(Long.valueOf(qualityGate.getId())).isEqualTo(dbQualityGate.getId());
    assertThat(qualityGate.getName()).isEqualTo(dbQualityGate.getName());
    assertThat(qualityGate.getDefault()).isTrue();
  }

  @Test
  public void project_quality_gate_over_default() {
    ComponentDto project = componentDb.insertComponent(newProjectDto());
    QualityGateDto defaultDbQualityGate = insertQualityGate("Sonar way");
    QualityGateDto dbQualityGate = insertQualityGate("My team QG");
    setDefaultQualityGate(defaultDbQualityGate.getId());
    associateProjectToQualityGate(project.getId(), dbQualityGate.getId());

    GetByProjectWsResponse result = callByUuid(project.uuid());

    WsQualityGates.QualityGate qualityGate = result.getQualityGate();
    assertThat(qualityGate.getName()).isEqualTo(dbQualityGate.getName());
    assertThat(qualityGate.getDefault()).isFalse();
  }

  @Test
  public void get_by_project_key() {
    ComponentDto project = componentDb.insertComponent(newProjectDto());
    QualityGateDto dbQualityGate = insertQualityGate("My team QG");
    associateProjectToQualityGate(project.getId(), dbQualityGate.getId());

    GetByProjectWsResponse result = callByKey(project.key());

    assertThat(result.getQualityGate().getName()).isEqualTo(dbQualityGate.getName());
  }

  @Test
  public void get_with_project_admin_permission() {
    ComponentDto project = componentDb.insertComponent(newProjectDto());
    userSession.anonymous().addProjectUuidPermissions(UserRole.USER, project.uuid());
    QualityGateDto dbQualityGate = insertQualityGate("Sonar way");
    setDefaultQualityGate(dbQualityGate.getId());

    GetByProjectWsResponse result = callByUuid(project.uuid());

    assertThat(result.getQualityGate().getName()).isEqualTo(dbQualityGate.getName());
  }

  @Test
  public void get_with_project_browse_permission() {
    ComponentDto project = componentDb.insertComponent(newProjectDto());
    userSession.anonymous().addProjectUuidPermissions(UserRole.ADMIN, project.uuid());
    QualityGateDto dbQualityGate = insertQualityGate("Sonar way");
    setDefaultQualityGate(dbQualityGate.getId());

    GetByProjectWsResponse result = callByUuid(project.uuid());

    assertThat(result.getQualityGate().getName()).isEqualTo(dbQualityGate.getName());
  }

  @Test
  public void fail_when_insufficient_permission() {
    expectedException.expect(ForbiddenException.class);

    ComponentDto project = componentDb.insertComponent(newProjectDto());
    userSession.anonymous().setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    QualityGateDto dbQualityGate = insertQualityGate("Sonar way");
    setDefaultQualityGate(dbQualityGate.getId());

    callByUuid(project.uuid());
  }

  @Test
  public void fail_when_project_does_not_exist() {
    expectedException.expect(NotFoundException.class);

    callByUuid("Unknown");
  }

  @Test
  public void fail_when_no_parameter() {
    expectedException.expect(IllegalArgumentException.class);

    call(null, null);
  }

  @Test
  public void fail_when_project_uuid_and_key_provided() {
    expectedException.expect(IllegalArgumentException.class);

    call("uuid", "key");
  }

  private GetByProjectWsResponse callByUuid(String projectUuid) {
    return call(projectUuid, null);
  }

  private GetByProjectWsResponse callByKey(String projectKey) {
    return call(null, projectKey);
  }

  private GetByProjectWsResponse call(@Nullable String projectUuid, @Nullable String projectKey) {
    TestRequest request = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF);

    if (projectUuid != null) {
      request.setParam(PARAM_PROJECT_ID, projectUuid);
    }

    if (projectKey != null) {
      request.setParam(PARAM_PROJECT_KEY, projectKey);
    }

    InputStream response = request.execute().getInputStream();

    try {
      return GetByProjectWsResponse.parseFrom(response);
    } catch (IOException e) {
      throw propagate(e);
    }
  }

  private QualityGateDto insertQualityGate(String name) {
    QualityGateDto qualityGate = dbClient.qualityGateDao().insert(dbSession, new QualityGateDto().setName(name));
    db.commit();
    return qualityGate;
  }

  private void associateProjectToQualityGate(long componentId, long qualityGateId) {
    dbClient.propertiesDao().insertProperty(dbSession, new PropertyDto()
      .setKey("sonar.qualitygate")
      .setResourceId(componentId)
      .setValue(String.valueOf(qualityGateId)));
    db.commit();
  }

  private void setDefaultQualityGate(long qualityGateId) {
    dbClient.propertiesDao().insertProperty(dbSession, new PropertyDto()
      .setKey("sonar.qualitygate")
      .setValue(String.valueOf(qualityGateId)));
    db.commit();
  }
}
