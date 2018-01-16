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

import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.Metric.Level;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.Metric.Level.ERROR;
import static org.sonar.api.measures.Metric.Level.OK;
import static org.sonar.api.measures.Metric.Level.WARN;
import static org.sonar.api.measures.Metric.ValueType.LEVEL;

public class QualityGateActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private WsActionTester ws = new WsActionTester(
    new QualityGateAction(userSession, db.getDbClient(), new ComponentFinder(db.getDbClient(), null), new SvgGenerator()));

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
  public void project_does_not_exist() {
    String response = ws.newRequest()
      .setParam("project", "unknown")
      .execute().getInput();

    checkError(response, "Component key 'unknown' not found");
  }

  @Test
  public void branch_does_not_exist() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.LONG));
    userSession.addProjectPermission(UserRole.USER, project);

    String response = ws.newRequest()
      .setParam("project", branch.getKey())
      .setParam("branch", "unknown")
      .execute().getInput();

    checkError(response, format("Component '%s' on branch 'unknown' not found", branch.getKey()));
  }

  @Test
  public void fail_on_invalid_quality_gate() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.addProjectPermission(UserRole.USER, project);
    MetricDto metric = createQualityGateMetric();
    db.measures().insertLiveMeasure(project, metric, m -> m.setData("UNKNOWN"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("No enum constant org.sonar.api.measures.Metric.Level.UNKNOWN");

    ws.newRequest()
      .setParam("project", project.getKey())
      .execute();
  }

  @Test
  public void measure_not_found() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);

    String response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute().getInput();

    checkError(response, format("Quality gate has not been found for project '%s' and branch 'null'", project.getKey()));
  }

  @Test
  public void measure_value_is_null() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto metric = createQualityGateMetric();
    db.measures().insertLiveMeasure(project, metric, m -> m.setValue(null).setData((String) null));

    String response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute().getInput();

    checkError(response, format("Quality gate has not been found for project '%s' and branch 'null'", project.getKey()));
  }

  @Test
  public void unauthorized() {
    ComponentDto project = db.components().insertPrivateProject();

    String response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute().getInput();

    checkError(response, "Insufficient privileges");
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
        assertThat(response).isEqualTo(readTemplate("quality_gate_passed.svg"));
        break;
      case WARN:
        assertThat(response).isEqualTo(readTemplate("quality_gate_warn.svg"));
        break;
      case ERROR:
        assertThat(response).isEqualTo(readTemplate("quality_gate_failed.svg"));
        break;
    }
  }

  private String readTemplate(String template) {
    try {
      return IOUtils.toString(getClass().getResource("templates/" + template), UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Can't read svg template '%s'", template), e);
    }
  }
}
