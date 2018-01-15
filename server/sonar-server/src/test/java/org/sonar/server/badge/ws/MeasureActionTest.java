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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric.Level;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.badge.ws.SvgGenerator.Color;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating;
import org.sonar.server.tester.UserSessionRule;
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

  private WsActionTester ws = new WsActionTester(
    new MeasureAction(userSession, db.getDbClient(), new ComponentFinder(db.getDbClient(), null), new SvgGenerator()));

  @Test
  public void int_measure() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(BUGS_KEY).setValueType(INT.name()));
    db.measures().insertLiveMeasure(project, metric, m -> m.setValue(10_000d));

    String response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute().getInput();

    checkSvg(response, "bugs", "10k", DEFAULT);
  }

  @Test
  public void percent_measure() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(COVERAGE_KEY).setValueType(PERCENT.name()));
    db.measures().insertLiveMeasure(project, metric, m -> m.setValue(12.345d));

    String response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute().getInput();

    checkSvg(response, "coverage", "12.3%", DEFAULT);
  }

  @Test
  public void duration_measure() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(TECHNICAL_DEBT_KEY).setValueType(WORK_DUR.name()));
    db.measures().insertLiveMeasure(project, metric, m -> m.setValue(10_000d));

    String response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute().getInput();

    checkSvg(response, "technical debt", "21d", DEFAULT);
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

    String response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute().getInput();

    checkSvg(response, "maintainability", rating.name(), color);
  }

  @DataProvider
  public static Object[][] qualityGates() {
    return new Object[][] {
      {OK, "passed", QUALITY_GATE_OK},
      {WARN, "warning", QUALITY_GATE_WARN},
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

    String response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute().getInput();

    checkSvg(response, "quality gate", expectedValue, expectedColor);
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
    expectedException.expectMessage("Measure not found");

    ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute();
  }

  @Test
  public void project_does_not_exist() {
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(BUGS_KEY));

    String response = ws.newRequest()
      .setParam("project", "unknown")
      .setParam("metric", metric.getKey())
      .execute().getInput();

    checkError(response, "Component key 'unknown' not found");
  }

  @Test
  public void branch_does_not_exist() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.LONG));
    userSession.addProjectPermission(UserRole.USER, project);
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(BUGS_KEY));
    db.measures().insertLiveMeasure(project, metric, m -> m.setValue(10d));

    String response = ws.newRequest()
      .setParam("project", branch.getKey())
      .setParam("branch", "unknown")
      .setParam("metric", metric.getKey())
      .execute().getInput();

    checkError(response, String.format("Component '%s' on branch 'unknown' not found", branch.getKey()));
  }

  @Test
  public void measure_not_found() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(BUGS_KEY));

    String response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute().getInput();

    checkError(response, String.format("Measure '%s' has not been found for project '%s' and branch 'null'", metric.getKey(), project.getKey()));
  }

  @Test
  public void unauthorized() {
    ComponentDto project = db.components().insertPrivateProject();
    MetricDto metric = db.measures().insertMetric(m -> m.setKey(BUGS_KEY));

    String response = ws.newRequest()
      .setParam("project", project.getKey())
      .setParam("metric", metric.getKey())
      .execute().getInput();

    checkError(response, "Insufficient privileges");
  }

  @Test
  public void fail_when_metric_not_found() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.addProjectPermission(UserRole.USER, project);

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

  private void checkSvg(String svg, String expectedLabel, String expectedValue, Color expectedColorValue) {
    assertThat(svg).contains(
      "<text", expectedLabel + "</text>",
      "<text", expectedValue + "</text>",
      "rect fill=\"" + expectedColorValue.getValue() + "\"");
  }

  private void checkError(String svg, String expectedError) {
    assertThat(svg).contains("<text", ">" + expectedError + "</text>");
  }

  private MetricDto createQualityGateMetric() {
    return db.measures().insertMetric(m -> m.setKey(CoreMetrics.ALERT_STATUS_KEY).setValueType(LEVEL.name()));
  }
}
