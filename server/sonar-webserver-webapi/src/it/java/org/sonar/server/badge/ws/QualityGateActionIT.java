/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.measures.Metric.Level;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectData;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.Metric.Level.ERROR;
import static org.sonar.api.measures.Metric.Level.OK;
import static org.sonar.api.measures.Metric.ValueType.LEVEL;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.BranchType.BRANCH;

@RunWith(DataProviderRunner.class)
public class QualityGateActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final MapSettings mapSettings = new MapSettings().setProperty(CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY, false);
  private final Configuration config = mapSettings.asConfig();

  private final WsActionTester ws = new WsActionTester(
    new QualityGateAction(db.getDbClient(),
      new ProjectBadgesSupport(new ComponentFinder(db.getDbClient(), null), db.getDbClient(), config),
      new SvgGenerator()));


  @Before
  public void before(){
    mapSettings.setProperty(CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY, false);
  }

  @Test
  public void quality_gate_passed() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());
    MetricDto metric = createQualityGateMetric();
    db.measures().insertMeasure(project, m -> m.addValue(metric.getKey(), OK.name()));

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute();

    checkResponse(response, OK);
  }

  @Test
  public void quality_gate_failed() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());
    MetricDto metric = createQualityGateMetric();
    db.measures().insertMeasure(project, m -> m.addValue(metric.getKey(), ERROR.name()));

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute();

    checkResponse(response, ERROR);
  }


  @DataProvider
  public static Object[][] publicProject_forceAuth_validToken_accessGranted(){
    return new Object[][] {
      // public project, force auth : access granted depending on token's validity
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
  @UseDataProvider("publicProject_forceAuth_validToken_accessGranted")
  public void badge_accessible_on_private_project_with_token(boolean publicProject, boolean forceAuth,
                                                               boolean validToken, boolean accessGranted) throws ParseException {
    ProjectData projectData = publicProject ? db.components().insertPublicProject() : db.components().insertPrivateProject();
    userSession.registerProjects(projectData.getProjectDto());
    MetricDto metric = createQualityGateMetric();

    db.measures().insertMeasure(projectData, m -> m.addValue(metric.getKey(), OK.name()));
    ProjectDto project = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), projectData.getProjectDto().getKey())
      .orElseThrow(() -> new IllegalStateException("project not found"));

    String token = db.getDbClient().projectBadgeTokenDao()
      .insert(db.getSession(), UuidFactoryFast.getInstance().create(), project, "user-uuid", "user-login")
      .getToken();
    db.commit();

    mapSettings.setProperty(CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY, forceAuth);

    TestResponse response = ws.newRequest()
      .setParam("project", projectData.getProjectDto().getKey())
      .setParam("token", validToken ? token : "invalid-token")
      .execute();

    if(accessGranted){
      checkResponse(response, OK);
    }else{
      checkError(response, "Project has not been found");
    }

  }

  @Test
  public void etag_should_be_different_if_quality_gate_is_different() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());
    MetricDto metric = createQualityGateMetric();
    MeasureDto measure = db.measures().insertMeasure(project, m -> m.addValue(metric.getKey(), OK.name()));

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute();
    String eTagOK = response.getHeader("ETag");

    measure.addValue(metric.getKey(), ERROR.name());
    db.getDbClient().measureDao().insertOrUpdate(db.getSession(), measure);
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
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());
    MetricDto metric = createQualityGateMetric();
    db.measures().insertMeasure(project, m -> m.addValue(metric.getKey(), OK.name()));

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
  public void quality_gate_on_branch() {
    ProjectData projectData = db.components().insertPublicProject(p -> p.setPrivate(false));
    ComponentDto project = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());
    MetricDto metric = createQualityGateMetric();
    db.measures().insertMeasure(project, m -> m.addValue(metric.getKey(), OK.name()));
    String branchName = secure().nextAlphanumeric(248);
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(BRANCH).setKey(branchName));
    db.measures().insertMeasure(branch, m -> m.addValue(metric.getKey(), ERROR.name()));

    TestResponse response = ws.newRequest()
      .setParam("project", branch.getKey())
      .setParam("branch", branchName)
      .execute();

    checkResponse(response, ERROR);
  }

  @Test
  public void quality_gate_on_application() {
    ProjectData projectData = db.components().insertPublicApplication();
    ComponentDto application = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());
    MetricDto metric = createQualityGateMetric();
    db.measures().insertMeasure(application, m -> m.addValue(metric.getKey(), ERROR.name()));

    TestResponse response = ws.newRequest()
      .setParam("project", application.getKey())
      .execute();

    checkResponse(response, ERROR);
  }

  @Test
  public void return_error_on_directory() throws ParseException {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto directory = db.components().insertComponent(ComponentTesting.newDirectory(project, "path"));
    userSession.registerProjects(projectData.getProjectDto());

    TestResponse response = ws.newRequest()
      .setParam("project", directory.getKey())
      .execute();

    checkError(response, "Project has not been found");
  }

  @Test
  public void return_error_on_private_project() throws ParseException {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(USER, project);

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute();

    checkError(response, "Project has not been found");
  }

  @Test
  public void return_error_on_provisioned_project() throws ParseException {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());

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
    ProjectData projectData = db.components().insertPublicProject(p -> p.setPrivate(false));
    ComponentDto project = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(BRANCH));

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("branch", "unknown")
      .execute();

    checkError(response, "Project has not been found");
  }

  @Test
  public void return_error_if_measure_not_found() throws ParseException {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute();

    checkError(response, "Quality gate has not been found");
  }

  @Test
  public void return_error_if_measure_value_is_null() throws ParseException {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());
    MetricDto metric = createQualityGateMetric();
    db.measures().insertMeasure(project);

    TestResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute();

    checkError(response, "Quality gate has not been found");
  }

  @Test
  public void fail_on_invalid_quality_gate() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());
    MetricDto metric = createQualityGateMetric();
    db.measures().insertMeasure(project, m -> m.addValue(metric.getKey(), "UNKNOWN"));

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("project", project.getKey())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("No enum constant org.sonar.api.measures.Metric.Level.UNKNOWN");
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
        tuple("branch", false),
        tuple("token", false));
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
