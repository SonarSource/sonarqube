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
package org.sonar.server.badge.ws;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
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
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.Metric.Level.ERROR;
import static org.sonar.api.measures.Metric.Level.OK;
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

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute();

    checkResponse(response, OK);
  }

  @Test
  public void quality_gate_failed() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto metric = createQualityGateMetric();
    db.measures().insertLiveMeasure(project, metric, m -> m.setData(ERROR.name()));

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute();

    checkResponse(response, ERROR);
  }

  @Test
  public void etag_should_be_different_if_quality_gate_is_different() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto metric = createQualityGateMetric();
    LiveMeasureDto liveMeasure = db.measures().insertLiveMeasure(project, metric, m -> m.setData(OK.name()));

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute();
    String eTagOK = response.getHeader("ETag");

    liveMeasure.setData(ERROR.name());
    db.getDbClient().liveMeasureDao().insertOrUpdate(db.getSession(), liveMeasure);
    db.commit();

    response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute();

    String eTagERROR = response.getHeader("ETag");

    assertThat(Arrays.asList(eTagOK, eTagERROR))
      .doesNotContainNull()
      .doesNotHaveDuplicates();
  }

  @Test
  public void when_IfNoneMatch_match_etag_http_304_must_be_send() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto metric = createQualityGateMetric();
    db.measures().insertLiveMeasure(project, metric, m -> m.setData(OK.name()));

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute();
    String eTag = response.getHeader("ETag");

    response = ws.newRequest()
      .setParam("project", project.getKey())
      .setHeader("If-None-Match", eTag)
      .execute();

    assertThat(response.getInput()).isEmpty();
    assertThat(response.getStatus()).isEqualTo(304);
  }

  @Test
  public void quality_gate_on_long_living_branch() {
    ComponentDto project = db.components().insertMainBranch(p -> p.setPrivate(false));
    userSession.registerComponents(project);
    MetricDto metric = createQualityGateMetric();
    db.measures().insertLiveMeasure(project, metric, m -> m.setData(OK.name()));
    ComponentDto longBranch = db.components().insertProjectBranch(project, b -> b.setBranchType(LONG));
    db.measures().insertLiveMeasure(longBranch, metric, m -> m.setData(ERROR.name()));

    TestResponse response = ws.newRequest()
      .setParam("project", longBranch.getKey())
      .setParam("branch", longBranch.getBranch())
      .execute();

    checkResponse(response, ERROR);
  }

  @Test
  public void quality_gate_on_application() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto application = db.components().insertPublicApplication(organization);
    userSession.registerComponents(application);
    MetricDto metric = createQualityGateMetric();
    db.measures().insertLiveMeasure(application, metric, m -> m.setData(ERROR.name()));

    TestResponse response = ws.newRequest()
      .setParam("project", application.getKey())
      .execute();

    checkResponse(response, ERROR);
  }

  @Test
  public void return_error_on_directory() throws ParseException {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto directory = db.components().insertComponent(ComponentTesting.newDirectory(project, "path"));
    userSession.registerComponents(project);

    TestResponse response = ws.newRequest()
      .setParam("project", directory.getKey())
      .execute();

    checkError(response, "Project is invalid");
  }

  @Test
  public void return_error_on_short_living_branch() throws ParseException {
    ComponentDto project = db.components().insertMainBranch(p -> p.setPrivate(false));
    userSession.registerComponents(project);
    ComponentDto shortBranch = db.components().insertProjectBranch(project, b -> b.setBranchType(SHORT));

    TestResponse response = ws.newRequest()
      .setParam("project", shortBranch.getKey())
      .setParam("branch", shortBranch.getBranch())
      .execute();

    checkError(response, "Project is invalid");
  }

  @Test
  public void return_error_on_private_project() throws ParseException {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(USER, project);

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute();

    checkError(response, "Project is invalid");
  }

  @Test
  public void return_error_on_provisioned_project() throws ParseException {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute();

    checkError(response, "Quality gate has not been found");
  }

  @Test
  public void return_error_on_not_existing_project() throws ParseException {
    TestResponse response = ws.newRequest()
      .setParam("project", "unknown")
      .execute();

    checkError(response, "Project has not been found");
  }

  @Test
  public void return_error_on_not_existing_branch() throws ParseException {
    ComponentDto project = db.components().insertMainBranch(p -> p.setPrivate(false));
    userSession.registerComponents(project);
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(LONG));

    TestResponse response = ws.newRequest()
      .setParam("project", branch.getKey())
      .setParam("branch", "unknown")
      .execute();

    checkError(response, "Project has not been found");
  }

  @Test
  public void return_error_if_measure_not_found() throws ParseException {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute();

    checkError(response, "Quality gate has not been found");
  }

  @Test
  public void return_error_if_measure_value_is_null() throws ParseException {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto metric = createQualityGateMetric();
    db.measures().insertLiveMeasure(project, metric, m -> m.setValue(null).setData((String) null));

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute();

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

  private void checkError(TestResponse response, String expectedError) throws ParseException {
    SimpleDateFormat expiresDateFormat = new SimpleDateFormat(ETagUtils.RFC1123_DATE, Locale.US);
    assertThat(response.getHeader("Cache-Control")).contains("no-cache");
    assertThat(response.getHeader("Expires")).isNotNull();
    assertThat(response.getHeader("ETag")).isNull();
    assertThat(expiresDateFormat.parse(response.getHeader("Expires"))).isBeforeOrEqualsTo(new Date());
    assertThat(response.getInput()).contains("<text", ">" + expectedError + "</text>");
  }

  private void checkResponse(TestResponse response, Level status) {
    assertThat(response.getHeader("ETag")).startsWith("W/");
    assertThat(response.getHeader("Cache-Control")).contains("no-cache");
    assertThat(response.getHeader("Expires")).isNull();
    switch (status) {
      case OK:
        assertThat(response.getInput()).contains("<!-- SONARQUBE QUALITY GATE PASS -->");
        break;
      case ERROR:
        assertThat(response.getInput()).contains("<!-- SONARQUBE QUALITY GATE FAIL -->");
        break;
    }
  }

}
