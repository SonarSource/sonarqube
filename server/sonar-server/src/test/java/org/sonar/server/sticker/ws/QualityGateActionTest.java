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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.server.sticker.ws.SvgGenerator.Color.GREEN;
import static org.sonar.server.sticker.ws.SvgGenerator.Color.ORANGE;
import static org.sonar.server.sticker.ws.SvgGenerator.Color.RED;

public class QualityGateActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private WsActionTester ws = new WsActionTester(new QualityGateAction(userSession, db.getDbClient(), new SvgGenerator()));

  @Test
  public void quality_gate_in_success() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto qualityGateMetric = db.measures().insertMetric(m -> m.setKey(CoreMetrics.ALERT_STATUS_KEY));
    db.measures().insertLiveMeasure(project, qualityGateMetric, m -> m.setData("OK"));

    String response = ws.newRequest()
      .setParam("component", project.getKey())
      .execute().getInput();

    checkSvg(response, "Quality Gate", "Success", GREEN.getValue());
  }

  @Test
  public void quality_gate_in_error() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto qualityGateMetric = db.measures().insertMetric(m -> m.setKey(CoreMetrics.ALERT_STATUS_KEY));
    db.measures().insertLiveMeasure(project, qualityGateMetric, m -> m.setData("ERROR"));

    String response = ws.newRequest()
      .setParam("component", project.getKey())
      .execute().getInput();

    checkSvg(response, "Quality Gate", "Failed", RED.getValue());
  }

  @Test
  public void quality_gate_in_warning() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto qualityGateMetric = db.measures().insertMetric(m -> m.setKey(CoreMetrics.ALERT_STATUS_KEY));
    db.measures().insertLiveMeasure(project, qualityGateMetric, m -> m.setData("WARN"));

    String response = ws.newRequest()
      .setParam("component", project.getKey())
      .execute().getInput();

    checkSvg(response, "Quality Gate", "Warning", ORANGE.getValue());
  }

  @Test
  public void long_living_branch_branch() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.LONG));
    userSession.addProjectPermission(UserRole.USER, project);
    MetricDto qualityGateMetric = db.measures().insertMetric(m -> m.setKey(CoreMetrics.ALERT_STATUS_KEY));
    db.measures().insertLiveMeasure(branch, qualityGateMetric, m -> m.setData("WARN"));

    String response = ws.newRequest()
      .setParam("component", branch.getKey())
      .setParam("branch", branch.getBranch())
      .execute().getInput();

    checkSvg(response, "Quality Gate", "Warning", ORANGE.getValue());
  }

  @Test
  public void short_living_branch_is_not_supported() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.SHORT));
    userSession.addProjectPermission(UserRole.USER, project);
    MetricDto qualityGateMetric = db.measures().insertMetric(m -> m.setKey(CoreMetrics.ALERT_STATUS_KEY));
    db.measures().insertLiveMeasure(branch, qualityGateMetric, m -> m.setData("WARN"));

    String response = ws.newRequest()
      .setParam("component", branch.getKey())
      .setParam("branch", branch.getBranch())
      .execute().getInput();

    checkError(response, "Short branch has no quality gate");
  }

  @Test
  public void project_does_not_exist() {
    String response = ws.newRequest()
      .setParam("component", "unknown")
      .execute().getInput();

    checkError(response, "Project 'unknown' does not exist");
  }

  @Test
  public void branch_does_not_exist() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.LONG));
    userSession.addProjectPermission(UserRole.USER, project);
    MetricDto qualityGateMetric = db.measures().insertMetric(m -> m.setKey(CoreMetrics.ALERT_STATUS_KEY));
    db.measures().insertLiveMeasure(branch, qualityGateMetric, m -> m.setData("WARN"));

    String response = ws.newRequest()
      .setParam("component", branch.getKey())
      .setParam("branch", "unknown")
      .execute().getInput();

    checkError(response, "Branch 'unknown' does not exist");
  }

  @Test
  public void unauthorized() {
    ComponentDto project = db.components().insertPrivateProject();
    MetricDto qualityGateMetric = db.measures().insertMetric(m -> m.setKey(CoreMetrics.ALERT_STATUS_KEY));
    db.measures().insertLiveMeasure(project, qualityGateMetric, m -> m.setData("WARN"));

    String response = ws.newRequest()
      .setParam("component", project.getKey())
      .execute().getInput();

    checkError(response, "Insufficient privileges");
  }

  @Test
  public void fail_when_no_quality_gate_found() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.addProjectPermission(UserRole.USER, project);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(format("No quality gate found for project '%s' and branch 'null'", project.getKey()));

    ws.newRequest()
      .setParam("component", project.getKey())
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
        tuple("component", true),
        tuple("branch", false),
        tuple("type", false));
  }

  private void checkSvg(String svg, String expectedLabel, String expectedValue, String expectedColorValue) {
    assertThat(svg).contains(
      "<text", expectedLabel, "</text>",
      "<text", expectedValue, "</text>",
      "rect fill=\"" + expectedColorValue + "\"");
  }

  private void checkError(String svg, String expectedError) {
    assertThat(svg).contains("<text", expectedError, "</text>");
  }

}
