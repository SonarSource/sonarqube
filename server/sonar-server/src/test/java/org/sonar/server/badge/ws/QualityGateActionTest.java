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
package org.sonar.server.badge.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.measures.Metric.Level;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.Metric.Level.ERROR;
import static org.sonar.api.measures.Metric.Level.OK;
import static org.sonar.api.measures.Metric.Level.WARN;
import static org.sonar.api.measures.Metric.ValueType.LEVEL;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.BranchType.LONG;
import static org.sonar.db.component.BranchType.SHORT;

public class QualityGateActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private MapSettings mapSettings = new MapSettings().setProperty("sonar.sonarcloud.enabled", false);

  private WsActionTester ws = new WsActionTester(
    new QualityGateAction(db.getDbClient(),
      new ProjectBadgesSupport(userSession, db.getDbClient(), new ComponentFinder(db.getDbClient(), null)),
      new SvgGenerator(mapSettings.asConfig())));

  @Test
  public void quality_gate_passed() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto metric = createQualityGateMetric();
    db.measures().insertLiveMeasure(project, metric, m -> m.setData(OK.name()));

    String response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute().getInput();

    checkResponse(response, OK);
  }

  @Test
  public void quality_gate_warn() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto metric = createQualityGateMetric();
    db.measures().insertLiveMeasure(project, metric, m -> m.setData(WARN.name()));

    String response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute().getInput();

    checkResponse(response, WARN);
  }

  @Test
  public void quality_gate_failed() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto metric = createQualityGateMetric();
    db.measures().insertLiveMeasure(project, metric, m -> m.setData(ERROR.name()));

    String response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute().getInput();

    checkResponse(response, ERROR);
  }

  @Test
  public void quality_gate_on_long_living_branch() {
    ComponentDto project = db.components().insertMainBranch(p -> p.setPrivate(false));
    userSession.registerComponents(project);
    MetricDto metric = createQualityGateMetric();
    db.measures().insertLiveMeasure(project, metric, m -> m.setData(OK.name()));
    ComponentDto longBranch = db.components().insertProjectBranch(project, b -> b.setBranchType(LONG));
    db.measures().insertLiveMeasure(longBranch, metric, m -> m.setData(WARN.name()));

    String response = ws.newRequest()
      .setParam("project", longBranch.getKey())
      .setParam("branch", longBranch.getBranch())
      .execute().getInput();

    checkResponse(response, WARN);
  }

  @Test
  public void quality_gate_on_application() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto application = db.components().insertPublicApplication(organization);
    userSession.registerComponents(application);
    MetricDto metric = createQualityGateMetric();
    db.measures().insertLiveMeasure(application, metric, m -> m.setData(WARN.name()));

    String response = ws.newRequest()
      .setParam("project", application.getKey())
      .execute().getInput();

    checkResponse(response, WARN);
  }

  @Test
  public void return_error_on_directory() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto directory = db.components().insertComponent(ComponentTesting.newDirectory(project, "path"));
    userSession.registerComponents(project);

    String response = ws.newRequest()
      .setParam("project", directory.getKey())
      .execute().getInput();

    checkError(response, "Project is invalid");
  }

  @Test
  public void return_error_on_short_living_branch() {
    ComponentDto project = db.components().insertMainBranch(p -> p.setPrivate(false));
    userSession.registerComponents(project);
    ComponentDto shortBranch = db.components().insertProjectBranch(project, b -> b.setBranchType(SHORT));

    String response = ws.newRequest()
      .setParam("project", shortBranch.getKey())
      .setParam("branch", shortBranch.getBranch())
      .execute().getInput();

    checkError(response, "Project is invalid");
  }

  @Test
  public void return_error_on_private_project() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(USER, project);

    String response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute().getInput();

    checkError(response, "Project is invalid");
  }

  @Test
  public void return_error_on_provisioned_project() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);

    String response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute().getInput();

    checkError(response, "Quality gate has not been found");
  }

  @Test
  public void return_error_on_not_existing_project() {
    String response = ws.newRequest()
      .setParam("project", "unknown")
      .execute().getInput();

    checkError(response, "Project has not been found");
  }

  @Test
  public void return_error_on_not_existing_branch() {
    ComponentDto project = db.components().insertMainBranch(p -> p.setPrivate(false));
    userSession.registerComponents(project);
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(LONG));

    String response = ws.newRequest()
      .setParam("project", branch.getKey())
      .setParam("branch", "unknown")
      .execute().getInput();

    checkError(response, "Project has not been found");
  }

  @Test
  public void return_error_if_measure_not_found() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);

    String response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute().getInput();

    checkError(response, "Quality gate has not been found");
  }

  @Test
  public void return_error_if_measure_value_is_null() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto metric = createQualityGateMetric();
    db.measures().insertLiveMeasure(project, metric, m -> m.setValue(null).setData((String) null));

    String response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute().getInput();

    checkError(response, "Quality gate has not been found");
  }

  @Test
  public void fail_on_invalid_quality_gate() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto metric = createQualityGateMetric();
    db.measures().insertLiveMeasure(project, metric, m -> m.setData("UNKNOWN"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("No enum constant org.sonar.api.measures.Metric.Level.UNKNOWN");

    ws.newRequest()
      .setParam("project", project.getKey())
      .execute();
  }

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("quality_gate");
    assertThat(def.isInternal()).isFalse();
    assertThat(def.isPost()).isFalse();
    assertThat(def.since()).isEqualTo("7.1");
    assertThat(def.responseExampleAsString()).isNotEmpty();

    assertThat(def.params())
      .extracting(Param::key, Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("project", true),
        tuple("branch", false));
  }

  private MetricDto createQualityGateMetric() {
    return db.measures().insertMetric(m -> m.setKey(ALERT_STATUS_KEY).setValueType(LEVEL.name()));
  }

  private void checkError(String svg, String expectedError) {
    assertThat(svg).contains("<text", ">" + expectedError + "</text>");
  }

  private void checkResponse(String response, Level status) {
    switch (status) {
      case OK:
        assertThat(response).contains("<!-- SONARQUBE QUALITY GATE PASS -->");
        break;
      case WARN:
        assertThat(response).contains("<!-- SONARQUBE QUALITY GATE WARN -->");
        break;
      case ERROR:
        assertThat(response).contains("<!-- SONARQUBE QUALITY GATE FAIL -->");
        break;
    }
  }

}
