/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric.Level;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectData;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.badge.ws.SvgGenerator.Color;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.measure.Rating;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.measures.CoreMetrics.BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
import static org.sonar.api.measures.Metric.Level.ERROR;
import static org.sonar.api.measures.Metric.Level.OK;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.LEVEL;
import static org.sonar.api.measures.Metric.ValueType.PERCENT;
import static org.sonar.api.measures.Metric.ValueType.RATING;
import static org.sonar.api.measures.Metric.ValueType.WORK_DUR;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.server.badge.ws.SvgGenerator.Color.DEFAULT;
import static org.sonar.server.badge.ws.SvgGenerator.Color.QUALITY_GATE_ERROR;
import static org.sonar.server.badge.ws.SvgGenerator.Color.QUALITY_GATE_OK;

@RunWith(DataProviderRunner.class)
public class MeasureActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final MapSettings mapSettings = new MapSettings();
  private final Configuration config = mapSettings.asConfig();

  private final WsActionTester ws = new WsActionTester(
    new MeasureAction(
      db.getDbClient(),
      new ProjectBadgesSupport(new ComponentFinder(db.getDbClient(), null), db.getDbClient(), config),
      new SvgGenerator()));

  @Before
  public void before() {
    mapSettings.setProperty(CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY, false);
  }

  @Test
  public void int_measure() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());
    MetricDto metric = createIntMetricAndMeasure(project, BUGS_KEY, 10_000);

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
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());
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
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());
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
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());
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
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());
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
  public void security_hotspots() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());
    MetricDto metric = createIntMetricAndMeasure(project, SECURITY_HOTSPOTS_KEY, 42);

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute();

    checkSvg(response, "security hotspots", "42", DEFAULT);

    // Second call with If-None-Match must return 304
    checkWithIfNoneMatchHeader(project, metric, response);

  }

  @Test
  public void measure_on_non_main_branch() {
    ProjectData projectData = db.components().insertPublicProject(p -> p.setPrivate(false));
    ComponentDto project = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());
    MetricDto metric = createIntMetricAndMeasure(project, BUGS_KEY, 5_000);
    String branchName = randomAlphanumeric(248);
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(BRANCH).setKey(branchName));
    db.measures().insertLiveMeasure(branch, metric, m -> m.setValue(10_000d));

    TestResponse response = ws.newRequest()
      .setParam("project", branch.getKey())
      .setParam("branch", branchName)
      .setParam("metric", metric.getKey())
      .execute();

    checkSvg(response, "bugs", "10k", DEFAULT);

    // Second call with If-None-Match must return 304
    response = ws.newRequest()
      .setHeader("If-None-Match", response.getHeader("ETag"))
      .setParam("project", branch.getKey())
      .setParam("branch", branchName)
      .setParam("metric", metric.getKey())
      .execute();

    assertThat(response.getInput()).isEmpty();
    assertThat(response.getStatus()).isEqualTo(304);
  }

  @Test
  public void measure_on_application() {
    ProjectData projectData = db.components().insertPublicApplication();
    ComponentDto application = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());
    MetricDto metric = createIntMetricAndMeasure(application, BUGS_KEY, 10_000);

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
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(BRANCH));
    userSession.registerProjects(projectData.getProjectDto());
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(BUGS_KEY));

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "unknown")
      .setParam("metric", metric.getKey())
      .execute();

    checkError(response, "Project has not been found");
  }

  @Test
  public void return_error_if_measure_not_found() throws ParseException {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(BUGS_KEY));

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute();

    checkError(response, "Measure has not been found");
  }

  @Test
  public void return_error_on_directory() throws ParseException {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto directory = db.components().insertComponent(ComponentTesting.newDirectory(project, "path"));
    userSession.registerProjects(projectData.getProjectDto());
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(BUGS_KEY).setValueType(INT.name()));

    TestResponse response = ws.newRequest()
      .setParam("project", directory.getKey())
      .setParam("metric", metric.getKey())
      .execute();

    checkError(response, "Project has not been found");
  }

  @Test
  public void return_error_on_private_project_without_token() throws ParseException {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(USER, project);
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(BUGS_KEY).setValueType(INT.name()));

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute();

    checkError(response, "Project has not been found");
  }

  @DataProvider
  public static Object[][] publicProject_forceAuth_accessGranted() {
    return new Object[][] {
      // public project, force auth : works depending on token's validity
      {true, true, true, true},
      {true, true, false, false},

      // public project, no force auth : access always granted
      {true, false, true, true},
      {true, false, false, true},

      // private project, regardless of force auth, access granted depending on token's validity:
      {false, true, true, true},
      {false, true, false, false},
      {false, false, true, true},
      {false, false, false, false},
    };
  }

  @Test
  @UseDataProvider("publicProject_forceAuth_accessGranted")
  public void badge_accessible_on_private_project_with_token(boolean publicProject, boolean forceAuth,
    boolean validToken, boolean accessGranted) throws ParseException {
    ProjectData project = publicProject ? db.components().insertPublicProject() : db.components().insertPrivateProject();
    userSession.registerProjects(project.getProjectDto());
    MetricDto metric = createIntMetricAndMeasure(project.getMainBranchComponent(), BUGS_KEY, 10_000);

    String token = db.getDbClient().projectBadgeTokenDao()
      .insert(db.getSession(), UuidFactoryFast.getInstance().create(), project.getProjectDto(), "user-uuid", "user-login")
      .getToken();
    db.commit();

    mapSettings.setProperty(CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY, forceAuth);

    TestResponse response = ws.newRequest()
      .setParam("project", project.getProjectDto().getKey())
      .setParam("metric", metric.getKey())
      .setParam("token", validToken ? token : "invalid-token")
      .execute();

    if (accessGranted) {
      checkSvg(response, "bugs", "10k", DEFAULT);
    } else {
      checkError(response, "Project has not been found");
    }
  }

  @Test
  public void return_error_on_provisioned_project() throws ParseException {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();

    userSession.registerProjects(project);
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(BUGS_KEY).setValueType(INT.name()));

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute();

    checkError(response, "Measure has not been found");
  }

  @Test
  public void fail_on_invalid_quality_gate() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());
    MetricDto metric = createQualityGateMetric();
    db.measures().insertLiveMeasure(project, metric, m -> m.setData("UNKNOWN"));

    TestRequest request = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey());
    assertThatThrownBy(() -> {
      request
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("No enum constant org.sonar.api.measures.Metric.Level.UNKNOWN");
  }

  @Test
  public void fail_when_measure_value_is_null() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(BUGS_KEY).setValueType(INT.name()));
    db.measures().insertLiveMeasure(project, metric, m -> m.setValue(null));

    TestRequest request = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey());

    assertThatThrownBy(() -> {
      request
        .execute();
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Measure has not been found");
  }

  @Test
  public void fail_when_metric_not_found() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());

    TestRequest request = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", BUGS_KEY);

    assertThatThrownBy(() -> {
      request
        .execute();
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Metric 'bugs' hasn't been found");
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
        tuple("metric", true),
        tuple("token", false));
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

  private MetricDto createIntMetricAndMeasure(ComponentDto project, String key, Integer value) {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(key).setValueType(INT.name()));
    db.measures().insertLiveMeasure(project, metric, m -> m.setValue(value.doubleValue()));
    return metric;
  }
}
