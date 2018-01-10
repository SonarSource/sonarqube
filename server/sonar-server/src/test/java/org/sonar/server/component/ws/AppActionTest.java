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
package org.sonar.server.component.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
import static org.sonar.api.measures.CoreMetrics.TESTS_KEY;
import static org.sonar.api.measures.CoreMetrics.VIOLATIONS_KEY;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.test.JsonAssert.assertJson;

public class AppActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private WsActionTester ws = new WsActionTester(new AppAction(db.getDbClient(), userSession, TestComponentFinder.from(db)));

  @Test
  public void file_info() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "src"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, directory));
    userSession.logIn("john").addProjectPermission(USER, project);

    String result = ws.newRequest()
      .setParam("component", file.getKey())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"key\": \"" + file.getKey() + "\",\n" +
      "  \"uuid\": \"" + file.uuid() + "\",\n" +
      "  \"path\": \"" + file.path() + "\",\n" +
      "  \"name\": \"" + file.name() + "\",\n" +
      "  \"longName\": \"" + file.longName() + "\",\n" +
      "  \"q\": \"" + file.qualifier() + "\",\n" +
      "  \"project\": \"" + project.getKey() + "\",\n" +
      "  \"projectName\": \"" + project.longName() + "\",\n" +
      "  \"fav\": false,\n" +
      "  \"canMarkAsFavorite\": true,\n" +
      "  \"measures\": {}\n" +
      "}\n");
  }

  @Test
  public void file_on_module() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    ComponentDto directory = db.components().insertComponent(newDirectory(module, "src"));
    ComponentDto file = db.components().insertComponent(newFileDto(module, directory));
    userSession.logIn("john").addProjectPermission(USER, project);

    String result = ws.newRequest()
      .setParam("component", file.getKey())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"key\": \"" + file.getKey() + "\",\n" +
      "  \"uuid\": \"" + file.uuid() + "\",\n" +
      "  \"path\": \"" + file.path() + "\",\n" +
      "  \"name\": \"" + file.name() + "\",\n" +
      "  \"longName\": \"" + file.longName() + "\",\n" +
      "  \"q\": \"" + file.qualifier() + "\",\n" +
      "  \"subProject\": \"" + module.getKey() + "\",\n" +
      "  \"subProjectName\": \"" + module.longName() + "\",\n" +
      "  \"project\": \"" + project.getKey() + "\",\n" +
      "  \"projectName\": \"" + project.longName() + "\",\n" +
      "  \"fav\": false,\n" +
      "  \"canMarkAsFavorite\": true,\n" +
      "  \"measures\": {}\n" +
      "}\n");
  }

  @Test
  public void file_without_measures() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    userSession.logIn("john").addProjectPermission(USER, project);

    String result = ws.newRequest()
      .setParam("component", file.getKey())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"measures\": {}\n" +
      "}\n");
  }

  @Test
  public void file_with_measures() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "src"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, directory));
    MetricDto lines = db.measures().insertMetric(m -> m.setKey(LINES_KEY));
    db.measures().insertLiveMeasure(file, lines, m -> m.setValue(200d));
    MetricDto duplicatedLines = db.measures().insertMetric(m -> m.setKey(DUPLICATED_LINES_DENSITY_KEY));
    db.measures().insertLiveMeasure(file, duplicatedLines, m -> m.setValue(7.4));
    MetricDto tests = db.measures().insertMetric(m -> m.setKey(TESTS_KEY));
    db.measures().insertLiveMeasure(file, tests, m -> m.setValue(3d));
    MetricDto technicalDebt = db.measures().insertMetric(m -> m.setKey(TECHNICAL_DEBT_KEY));
    db.measures().insertLiveMeasure(file, technicalDebt, m -> m.setValue(182d));
    MetricDto issues = db.measures().insertMetric(m -> m.setKey(VIOLATIONS_KEY));
    db.measures().insertLiveMeasure(file, issues, m -> m.setValue(231d));
    MetricDto coverage = db.measures().insertMetric(m -> m.setKey(COVERAGE_KEY));
    db.measures().insertLiveMeasure(file, coverage, m -> m.setValue(95.4d));
    userSession.logIn("john").addProjectPermission(USER, project);

    String result = ws.newRequest()
      .setParam("component", file.getKey())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"measures\": {\n" +
      "    \"lines\": \"200.0\",\n" +
      "    \"coverage\": \"95.4\",\n" +
      "    \"duplicationDensity\": \"7.4\",\n" +
      "    \"issues\": \"231.0\",\n" +
      "    \"tests\": \"3.0\"\n" +
      "  }" +
      "}\n");
  }

  @Test
  public void get_by_uuid() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project, project));
    MetricDto coverage = db.measures().insertMetric(m -> m.setKey(COVERAGE_KEY));
    db.measures().insertLiveMeasure(file, coverage, m -> m.setValue(95.4d));
    userSession.logIn("john").addProjectPermission(USER, project);

    String result = ws.newRequest()
      .setParam("uuid", file.uuid())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"key\": \"" + file.getKey() + "\",\n" +
      "  \"uuid\": \"" + file.uuid() + "\",\n" +
      "  \"measures\": {\n" +
      "    \"coverage\": \"95.4\"\n" +
      "  }\n" +
      "}\n");
  }

  @Test
  public void canMarkAsFavorite_is_true_when_logged() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn("john").addProjectPermission(USER, project);

    String result = ws.newRequest()
      .setParam("component", project.getKey())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"canMarkAsFavorite\": true,\n" +
      "}\n");
  }

  @Test
  public void canMarkAsFavorite_is_false_when_not_logged() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(USER, project);

    String result = ws.newRequest()
      .setParam("component", project.getKey())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"canMarkAsFavorite\": false,\n" +
      "}\n");
  }

  @Test
  public void component_is_favorite() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn("john").addProjectPermission(USER, project);
    db.favorites().add(project, userSession.getUserId());

    String result = ws.newRequest()
      .setParam("component", project.getKey())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"fav\": true,\n" +
      "}\n");
  }

  @Test
  public void component_is_not_favorite() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn("john").addProjectPermission(USER, project);

    String result = ws.newRequest()
      .setParam("component", project.getKey())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"fav\": false,\n" +
      "}\n");
  }

  @Test
  public void branch() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn("john").addProjectPermission(USER, project);
    ComponentDto branch = db.components().insertProjectBranch(project);
    ComponentDto module = db.components().insertComponent(newModuleDto(branch));
    ComponentDto directory = db.components().insertComponent(newDirectory(module, "src"));
    ComponentDto file = db.components().insertComponent(newFileDto(module, directory));
    MetricDto coverage = db.measures().insertMetric(m -> m.setKey(COVERAGE_KEY));
    db.measures().insertLiveMeasure(file, coverage, m -> m.setValue(95.4d));

    String result = ws.newRequest()
      .setParam("component", file.getKey())
      .setParam("branch", file.getBranch())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"key\": \"" + file.getKey() + "\",\n" +
      "  \"branch\": \"" + file.getBranch() + "\",\n" +
      "  \"uuid\": \"" + file.uuid() + "\",\n" +
      "  \"path\": \"" + file.path() + "\",\n" +
      "  \"name\": \"" + file.name() + "\",\n" +
      "  \"longName\": \"" + file.longName() + "\",\n" +
      "  \"q\": \"" + file.qualifier() + "\",\n" +
      "  \"subProject\": \"" + module.getKey() + "\",\n" +
      "  \"subProjectName\": \"" + module.longName() + "\",\n" +
      "  \"project\": \"" + project.getKey() + "\",\n" +
      "  \"projectName\": \"" + project.longName() + "\",\n" +
      "  \"fav\": false,\n" +
      "  \"canMarkAsFavorite\": true,\n" +
      "  \"measures\": {\n" +
      "    \"coverage\": \"95.4\"\n" +
      "  }\n" +
      "}\n");
  }

  @Test
  public void fail_if_no_parameter_provided() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either 'componentId' or 'component' must be provided");

    ws.newRequest().execute();
  }

  @Test
  public void fail_if_both_componentId_and_branch_parameters_provided() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);
    ComponentDto file = db.components().insertComponent(newFileDto(branch));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'componentId' and 'branch' parameters cannot be used at the same time");

    ws.newRequest()
      .setParam("uuid", file.uuid())
      .setParam("branch", file.getBranch())
      .execute();
  }

  @Test
  public void fail_when_component_not_found() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));

    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setParam("component", "unknown")
      .execute();
  }

  @Test
  public void fail_when_branch_not_found() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(project);
    ComponentDto file = db.components().insertComponent(newFileDto(branch));

    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setParam("component", file.getKey())
      .setParam("branch", "unknown")
      .execute();
  }

  @Test
  public void fail_when_missing_permission() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam("component", file.getKey())
      .execute();
  }

  @Test
  public void define_app_action() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isFalse();
    assertThat(action.handler()).isNotNull();
    assertThat(action.params()).hasSize(3);
  }

}
