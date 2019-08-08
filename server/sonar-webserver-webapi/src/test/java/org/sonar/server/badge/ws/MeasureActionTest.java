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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric.Level;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.badge.ws.SvgGenerator.Color;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.measure.Rating;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.measures.CoreMetrics.BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
import static org.sonar.api.measures.Metric.Level.ERROR;
import static org.sonar.api.measures.Metric.Level.OK;
import static org.sonar.api.measures.Metric.Level.WARN;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.LEVEL;
import static org.sonar.api.measures.Metric.ValueType.PERCENT;
import static org.sonar.api.measures.Metric.ValueType.RATING;
import static org.sonar.api.measures.Metric.ValueType.WORK_DUR;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.BranchType.LONG;
import static org.sonar.db.component.BranchType.SHORT;
import static org.sonar.server.badge.ws.SvgGenerator.Color.DEFAULT;
import static org.sonar.server.badge.ws.SvgGenerator.Color.QUALITY_GATE_ERROR;
import static org.sonar.server.badge.ws.SvgGenerator.Color.QUALITY_GATE_OK;
import static org.sonar.server.badge.ws.SvgGenerator.Color.QUALITY_GATE_WARN;

@RunWith(DataProviderRunner.class)
public class MeasureActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private MapSettings mapSettings = new MapSettings().setProperty("sonar.sonarcloud.enabled", false);

  private WsActionTester ws = new WsActionTester(
    new MeasureAction(
      db.getDbClient(),
      new ProjectBadgesSupport(userSession, db.getDbClient(), new ComponentFinder(db.getDbClient(), null)),
      new SvgGenerator(mapSettings.asConfig())));

  @Test
  public void int_measure() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(BUGS_KEY).setValueType(INT.name()));
    db.measures().insertLiveMeasure(project, metric, m -> m.setValue(10_000d));

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute();

    checkSvg(response, "bugs", "10k", DEFAULT);

    // Second call with If-None-Match must return 304
    checkWithIfNoneMatchHeader(project, metric, response);
  }

  @Test
  public void percent_measure() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(COVERAGE_KEY).setValueType(PERCENT.name()));
    db.measures().insertLiveMeasure(project, metric, m -> m.setValue(12.345d));

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute();

    checkSvg(response, "coverage", "12.3%", DEFAULT);

    // Second call with If-None-Match must return 304
    checkWithIfNoneMatchHeader(project, metric, response);
  }

  @Test
  public void duration_measure() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(TECHNICAL_DEBT_KEY).setValueType(WORK_DUR.name()));
    db.measures().insertLiveMeasure(project, metric, m -> m.setValue(10_000d));

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute();

    checkSvg(response, "technical debt", "21d", DEFAULT);

    // Second call with If-None-Match must return 304
    checkWithIfNoneMatchHeader(project, metric, response);
  }

  @DataProvider
  public static Object[][] ratings() {
    return new Object[][] {
      {Rating.A, Color.RATING_A},
      {Rating.B, Color.RATING_B},
      {Rating.C, Color.RATING_C},
      {Rating.D, Color.RATING_D},
      {Rating.E, Color.RATING_E}
    };
  }

  @Test
  @UseDataProvider("ratings")
  public void rating_measure(Rating rating, Color color) {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(SQALE_RATING_KEY).setValueType(RATING.name()));
    db.measures().insertLiveMeasure(project, metric, m -> m.setValue((double) rating.getIndex()).setData(rating.name()));

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute();

    checkSvg(response, "maintainability", rating.name(), color);

    // Second call with If-None-Match must return 304
    checkWithIfNoneMatchHeader(project, metric, response);
  }

  @DataProvider
  public static Object[][] qualityGates() {
    return new Object[][] {
      {OK, "passed", QUALITY_GATE_OK},
      {ERROR, "failed", QUALITY_GATE_ERROR}
    };
  }

  @Test
  @UseDataProvider("qualityGates")
  public void quality_gate(Level status, String expectedValue, Color expectedColor) {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto metric = createQualityGateMetric();
    db.measures().insertLiveMeasure(project, metric, m -> m.setData(status.name()));

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute();

    checkSvg(response, "quality gate", expectedValue, expectedColor);

    // Second call with If-None-Match must return 304
    checkWithIfNoneMatchHeader(project, metric, response);
  }

  @Test
  public void display_deprecated_warning_quality_gate() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto metric = createQualityGateMetric();
    db.measures().insertLiveMeasure(project, metric, m -> m.setData(WARN.name()));

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute();

    checkSvg(response, "quality gate", "warning", QUALITY_GATE_WARN);
  }

  @Test
  public void measure_on_long_living_branch() {
    ComponentDto project = db.components().insertMainBranch(p -> p.setPrivate(false));
    userSession.registerComponents(project);
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(BUGS_KEY).setValueType(INT.name()));
    db.measures().insertLiveMeasure(project, metric, m -> m.setValue(5_000d));
    ComponentDto longBranch = db.components().insertProjectBranch(project, b -> b.setBranchType(LONG));
    db.measures().insertLiveMeasure(longBranch, metric, m -> m.setValue(10_000d));

    TestResponse response = ws.newRequest()
      .setParam("project", longBranch.getKey())
      .setParam("branch", longBranch.getBranch())
      .setParam("metric", metric.getKey())
      .execute();

    checkSvg(response, "bugs", "10k", DEFAULT);

    // Second call with If-None-Match must return 304
    response = ws.newRequest()
      .setHeader("If-None-Match", response.getHeader("ETag"))
      .setParam("project", longBranch.getKey())
      .setParam("branch", longBranch.getBranch())
      .setParam("metric", metric.getKey())
      .execute();

    assertThat(response.getInput()).isEmpty();
    assertThat(response.getStatus()).isEqualTo(304);
  }

  @Test
  public void measure_on_application() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto application = db.components().insertPublicApplication(organization);
    userSession.registerComponents(application);
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(BUGS_KEY).setValueType(INT.name()));
    db.measures().insertLiveMeasure(application, metric, m -> m.setValue(10_000d));

    TestResponse response = ws.newRequest()
      .setParam("project", application.getKey())
      .setParam("metric", metric.getKey())
      .execute();

    checkSvg(response, "bugs", "10k", DEFAULT);

    // Second call with If-None-Match must return 304
    checkWithIfNoneMatchHeader(application, metric, response);
  }

  @Test
  public void return_error_if_project_does_not_exist() throws ParseException {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(BUGS_KEY));

    TestResponse response = ws.newRequest()
      .setParam("project", "unknown")
      .setParam("metric", metric.getKey())
      .execute();

    checkError(response, "Project has not been found");
  }

  @Test
  public void return_error_if_branch_does_not_exist() throws ParseException {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.LONG));
    userSession.addProjectPermission(USER, project);
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(BUGS_KEY));

    TestResponse response = ws.newRequest()
      .setParam("project", branch.getKey())
      .setParam("branch", "unknown")
      .setParam("metric", metric.getKey())
      .execute();

    checkError(response, "Project has not been found");
  }

  @Test
  public void return_error_if_measure_not_found() throws ParseException {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(BUGS_KEY));

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute();

    checkError(response, "Measure has not been found");
  }

  @Test
  public void return_error_on_directory() throws ParseException {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto directory = db.components().insertComponent(ComponentTesting.newDirectory(project, "path"));
    userSession.registerComponents(project);
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(BUGS_KEY).setValueType(INT.name()));

    TestResponse response = ws.newRequest()
      .setParam("project", directory.getKey())
      .setParam("metric", metric.getKey())
      .execute();

    checkError(response, "Project is invalid");
  }

  @Test
  public void return_error_on_short_living_branch() throws ParseException {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto shortBranch = db.components().insertProjectBranch(project, b -> b.setBranchType(SHORT));
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(USER, project);
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(BUGS_KEY).setValueType(INT.name()));

    TestResponse response = ws.newRequest()
      .setParam("project", shortBranch.getKey())
      .setParam("branch", shortBranch.getBranch())
      .setParam("metric", metric.getKey())
      .execute();

    checkError(response, "Project is invalid");
  }

  @Test
  public void return_error_on_private_project() throws ParseException {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(USER, project);
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(BUGS_KEY).setValueType(INT.name()));

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute();

    checkError(response, "Project is invalid");
  }

  @Test
  public void return_error_on_provisioned_project() throws ParseException {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(BUGS_KEY).setValueType(INT.name()));

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute();

    checkError(response, "Measure has not been found");
  }

  @Test
  public void return_error_if_unauthorized() throws ParseException {
    ComponentDto project = db.components().insertPublicProject();
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(BUGS_KEY));

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute();

    checkError(response, "Insufficient privileges");
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
      .setParam("metric", metric.getKey())
      .execute();
  }

  @Test
  public void fail_when_measure_value_is_null() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(BUGS_KEY).setValueType(INT.name()));
    db.measures().insertLiveMeasure(project, metric, m -> m.setValue(null));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Measure has not been found");

    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute();
  }

  @Test
  public void fail_when_metric_not_found() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Metric 'bugs' hasn't been found");

    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", BUGS_KEY)
      .execute();
  }

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("measure");
    assertThat(def.isInternal()).isFalse();
    assertThat(def.isPost()).isFalse();
    assertThat(def.since()).isEqualTo("7.1");
    assertThat(def.responseExampleAsString()).isNotEmpty();

    assertThat(def.params())
      .extracting(Param::key, Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("project", true),
        tuple("branch", false),
        tuple("metric", true));
  }

  private void checkSvg(TestResponse response, String expectedLabel, String expectedValue, Color expectedColorValue) {
    assertThat(response.getHeader("ETag")).startsWith("W/");
    assertThat(response.getHeader("Cache-Control")).contains("no-cache");
    assertThat(response.getHeader("Expires")).isNull();

    assertThat(response.getInput()).contains(
      "<text", expectedLabel + "</text>",
      "<text", expectedValue + "</text>",
      "rect fill=\"" + expectedColorValue.getValue() + "\"");
  }

  private void checkError(TestResponse response, String expectedError) throws ParseException {
    SimpleDateFormat expiresDateFormat = new SimpleDateFormat(ETagUtils.RFC1123_DATE, Locale.US);
    assertThat(response.getHeader("Cache-Control")).contains("no-cache");
    assertThat(response.getHeader("Expires")).isNotNull();
    assertThat(response.getHeader("ETag")).isNull();
    assertThat(expiresDateFormat.parse(response.getHeader("Expires"))).isBeforeOrEqualsTo(new Date());
    assertThat(response.getInput()).contains("<text", ">" + expectedError + "</text>");
  }

  private void checkWithIfNoneMatchHeader(ComponentDto application, MetricDto metric, TestResponse response) {
    TestResponse newResponse = ws.newRequest()
      .setHeader("If-None-Match", response.getHeader("ETag"))
      .setParam("project", application.getKey())
      .setParam("metric", metric.getKey())
      .execute();

    assertThat(newResponse.getInput()).isEmpty();
    assertThat(newResponse.getStatus()).isEqualTo(304);
  }
  private MetricDto createQualityGateMetric() {
    return db.measures().insertMetric(m -> m.setKey(CoreMetrics.ALERT_STATUS_KEY).setValueType(LEVEL.name()));
  }
}
