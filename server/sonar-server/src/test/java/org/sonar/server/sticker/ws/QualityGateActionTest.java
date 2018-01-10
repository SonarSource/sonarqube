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
package org.sonar.server.sticker.ws;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class QualityGateActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private WsActionTester ws = new WsActionTester(new QualityGateAction(userSession, db.getDbClient()));

  @Test
  public void passing_quality_gate() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto qualityGateMetric = db.measures().insertMetric(m -> m.setKey(CoreMetrics.ALERT_STATUS_KEY));
    db.measures().insertLiveMeasure(project, qualityGateMetric, m -> m.setData("OK"));

    String response = ws.newRequest()
      .setParam("component", project.getKey())
      .execute().getInput();

    assertResponse(response, "quality_gate-passing.svg");
  }

  @Test
  public void failing_quality_gate() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto qualityGateMetric = db.measures().insertMetric(m -> m.setKey(CoreMetrics.ALERT_STATUS_KEY));
    db.measures().insertLiveMeasure(project, qualityGateMetric, m -> m.setData("ERROR"));

    String response = ws.newRequest()
      .setParam("component", project.getKey())
      .execute().getInput();

    assertResponse(response, "quality_gate-failing.svg");
  }

  @Test
  public void warning_quality_gate() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto qualityGateMetric = db.measures().insertMetric(m -> m.setKey(CoreMetrics.ALERT_STATUS_KEY));
    db.measures().insertLiveMeasure(project, qualityGateMetric, m -> m.setData("WARN"));

    String response = ws.newRequest()
      .setParam("component", project.getKey())
      .execute().getInput();

    assertResponse(response, "quality_gate-warning.svg");
  }

  @Test
  public void branch() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);
    userSession.addProjectPermission(UserRole.USER, project);
    MetricDto qualityGateMetric = db.measures().insertMetric(m -> m.setKey(CoreMetrics.ALERT_STATUS_KEY));
    db.measures().insertLiveMeasure(branch, qualityGateMetric, m -> m.setData("WARN"));

    String response = ws.newRequest()
      .setParam("component", branch.getKey())
      .setParam("branch", branch.getBranch())
      .execute().getInput();

    assertResponse(response, "quality_gate-warning.svg");
  }

  @Test
  public void return_not_found_when_project_does_not_exist() {
    String response = ws.newRequest()
      .setParam("component", "unknown")
      .execute().getInput();

    assertResponse(response, "not_found.svg");
  }

  @Test
  public void return_not_found_when_branch_does_not_exist() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);
    userSession.addProjectPermission(UserRole.USER, project);
    MetricDto qualityGateMetric = db.measures().insertMetric(m -> m.setKey(CoreMetrics.ALERT_STATUS_KEY));
    db.measures().insertLiveMeasure(branch, qualityGateMetric, m -> m.setData("WARN"));

    String response = ws.newRequest()
      .setParam("component", branch.getKey())
      .setParam("branch", "unknown")
      .execute().getInput();

    assertResponse(response, "not_found.svg");
  }

  @Test
  public void return_not_found_when_no_quality_gate() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);

    String response = ws.newRequest()
      .setParam("component", project.getKey())
      .execute().getInput();

    assertResponse(response, "not_found.svg");
  }

  @Test
  public void unauthorized() {
    ComponentDto project = db.components().insertPrivateProject();
    MetricDto qualityGateMetric = db.measures().insertMetric(m -> m.setKey(CoreMetrics.ALERT_STATUS_KEY));
    db.measures().insertLiveMeasure(project, qualityGateMetric, m -> m.setData("WARN"));

    String response = ws.newRequest()
      .setParam("component", project.getKey())
      .execute().getInput();

    assertResponse(response, "unauthorized.svg");
  }

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("quality_gate");
    assertThat(def.isInternal()).isFalse();
    assertThat(def.isPost()).isFalse();
    assertThat(def.since()).isNull();
    assertThat(def.responseExampleAsString()).isNotEmpty();

    assertThat(def.params())
      .extracting(Param::key, Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("component", true),
        tuple("branch", false),
        tuple("type", false));
  }

  @Test
  public void test_example() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto qualityGateMetric = db.measures().insertMetric(m -> m.setKey(CoreMetrics.ALERT_STATUS_KEY));
    db.measures().insertLiveMeasure(project, qualityGateMetric, m -> m.setData("OK"));

    String response = ws.newRequest()
      .setParam("component", project.getKey())
      .execute().getInput();

    assertThat(response).isEqualTo(ws.getDef().responseExampleAsString());
  }

  private void assertResponse(String response, String expectedFile) {
    try {
      assertThat(response).isEqualTo(IOUtils.toString(getClass().getResource("templates/" + expectedFile).toURI().toURL(), UTF_8));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
