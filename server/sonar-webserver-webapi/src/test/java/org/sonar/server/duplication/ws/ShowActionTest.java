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
package org.sonar.server.duplication.ws;

import java.util.function.Function;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.metric.MetricToDto;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.test.JsonAssert.assertJson;

public class ShowActionTest {

  private static MetricDto dataMetric = MetricToDto.INSTANCE.apply(CoreMetrics.DUPLICATIONS_DATA);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create();
  private DuplicationsParser parser = new DuplicationsParser(db.getDbClient().componentDao());
  private ShowResponseBuilder showResponseBuilder = new ShowResponseBuilder(db.getDbClient());

  private WsActionTester ws = new WsActionTester(new ShowAction(db.getDbClient(), parser, showResponseBuilder, userSessionRule, TestComponentFinder.from(db)));

  @Before
  public void setUp() {
    db.getDbClient().metricDao().insert(db.getSession(), dataMetric);
    db.commit();
  }

  @Test
  public void define_ws() {
    WebService.Action show = ws.getDef();
    assertThat(show).isNotNull();
    assertThat(show.handler()).isNotNull();
    assertThat(show.since()).isEqualTo("4.4");
    assertThat(show.isInternal()).isFalse();
    assertThat(show.responseExampleAsString()).isNotEmpty();
    assertThat(show.params()).hasSize(4);
  }

  @Test
  public void get_duplications_by_file_key() throws Exception {
    TestRequest request = newBaseRequest();
    verifyCallToFileWithDuplications(file -> request.setParam("key", file.getDbKey()));
  }

  @Test
  public void get_duplications_by_file_id() throws Exception {
    TestRequest request = newBaseRequest();
    verifyCallToFileWithDuplications(file -> request.setParam("uuid", file.uuid()));
  }

  @Test
  public void return_file_with_missing_duplication_data() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project).setDbKey("foo.js"));
    db.components().insertSnapshot(newAnalysis(project));

    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project);

    TestResponse result = newBaseRequest().setParam("key", file.getDbKey()).execute();

    assertJson(result.getInput()).isSimilarTo("{\n" +
      "  \"duplications\": [],\n" +
      "  \"files\": {}\n" +
      "}");
  }

  @Test
  public void duplications_by_file_key_and_branch() {
    ComponentDto project = db.components().insertMainBranch();
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project);
    ComponentDto branch = db.components().insertProjectBranch(project);
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    db.measures().insertLiveMeasure(file, dataMetric, m -> m.setData(format("<duplications>\n" +
      "  <g>\n" +
      "    <b s=\"31\" l=\"5\" r=\"%s\"/>\n" +
      "    <b s=\"20\" l=\"5\" r=\"%s\"/>\n" +
      "  </g>\n" +
      "</duplications>\n", file.getDbKey(), file.getDbKey())));

    String result = ws.newRequest()
      .setParam("key", file.getKey())
      .setParam("branch", branch.getBranch())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo(
      format("{\n" +
        "  \"duplications\": [\n" +
        "    {\n" +
        "      \"blocks\": [\n" +
        "        {\n" +
        "          \"from\": 20,\n" +
        "          \"size\": 5,\n" +
        "          \"_ref\": \"1\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"from\": 31,\n" +
        "          \"size\": 5,\n" +
        "          \"_ref\": \"1\"\n" +
        "        }\n" +
        "      ]\n" +
        "    }\n" +
        "  ],\n" +
        "  \"files\": {\n" +
        "    \"1\": {\n" +
        "      \"key\": \"%s\",\n" +
        "      \"name\": \"%s\",\n" +
        "      \"uuid\": \"%s\",\n" +
        "      \"project\": \"%s\",\n" +
        "      \"projectUuid\": \"%s\",\n" +
        "      \"projectName\": \"%s\"\n" +
        "      \"branch\": \"%s\"\n" +
        "    }\n" +
        "  }\n" +
        "}",
        file.getKey(), file.longName(), file.uuid(), branch.getKey(), branch.uuid(), project.longName(), file.getBranch()));
  }

  @Test
  public void duplications_by_file_key_and_pull_request() {
    ComponentDto project = db.components().insertMainBranch();
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project);
    ComponentDto pullRequest = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST));
    ComponentDto file = db.components().insertComponent(newFileDto(pullRequest));
    db.measures().insertLiveMeasure(file, dataMetric, m -> m.setData(format("<duplications>\n" +
      "  <g>\n" +
      "    <b s=\"31\" l=\"5\" r=\"%s\"/>\n" +
      "    <b s=\"20\" l=\"5\" r=\"%s\"/>\n" +
      "  </g>\n" +
      "</duplications>\n", file.getDbKey(), file.getDbKey())));

    String result = ws.newRequest()
      .setParam("key", file.getKey())
      .setParam("pullRequest", pullRequest.getPullRequest())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo(
      format("{\n" +
        "  \"duplications\": [\n" +
        "    {\n" +
        "      \"blocks\": [\n" +
        "        {\n" +
        "          \"from\": 20,\n" +
        "          \"size\": 5,\n" +
        "          \"_ref\": \"1\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"from\": 31,\n" +
        "          \"size\": 5,\n" +
        "          \"_ref\": \"1\"\n" +
        "        }\n" +
        "      ]\n" +
        "    }\n" +
        "  ],\n" +
        "  \"files\": {\n" +
        "    \"1\": {\n" +
        "      \"key\": \"%s\",\n" +
        "      \"name\": \"%s\",\n" +
        "      \"uuid\": \"%s\",\n" +
        "      \"project\": \"%s\",\n" +
        "      \"projectUuid\": \"%s\",\n" +
        "      \"projectName\": \"%s\"\n" +
        "      \"pullRequest\": \"%s\"\n" +
        "    }\n" +
        "  }\n" +
        "}",
        file.getKey(), file.longName(), file.uuid(), pullRequest.getKey(), pullRequest.uuid(), project.longName(), file.getPullRequest()));
  }

  @Test
  public void fail_if_file_does_not_exist() {
    expectedException.expect(NotFoundException.class);

    newBaseRequest().setParam("key", "missing").execute();
  }

  @Test
  public void fail_if_user_is_not_allowed_to_access_project() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));

    expectedException.expect(ForbiddenException.class);

    newBaseRequest().setParam("key", file.getDbKey()).execute();
  }

  @Test
  public void fail_if_no_parameter_provided() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either 'uuid' or 'key' must be provided");

    newBaseRequest().execute();
  }

  @Test
  public void fail_when_using_branch_db_key() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organization);
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component key '%s' not found", branch.getDbKey()));

    ws.newRequest()
      .setParam("key", branch.getDbKey())
      .execute();
  }

  @Test
  public void fail_when_using_branch_uuid() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organization);
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component id '%s' not found", branch.uuid()));

    ws.newRequest()
      .setParam("uuid", branch.uuid())
      .execute();
  }

  private TestRequest newBaseRequest() {
    return ws.newRequest();
  }

  private void verifyCallToFileWithDuplications(Function<ComponentDto, TestRequest> requestFactory) {
    ComponentDto project = db.components().insertPrivateProject();
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project);
    ComponentDto file = db.components().insertComponent(newFileDto(project).setDbKey("foo.js"));
    String xml = "<duplications>\n" +
      "  <g>\n" +
      "    <b s=\"31\" l=\"5\" r=\"foo.js\"/>\n" +
      "    <b s=\"20\" l=\"5\" r=\"foo.js\"/>\n" +
      "  </g>\n" +
      "</duplications>\n";
    db.measures().insertLiveMeasure(file, dataMetric, m -> m.setData(xml));

    TestRequest request = requestFactory.apply(file);
    TestResponse result = request.execute();

    assertJson(result.getInput()).isSimilarTo("{\"duplications\":[" +
      "{\"blocks\":[{\"from\":20,\"size\":5,\"_ref\":\"1\"},{\"from\":31,\"size\":5,\"_ref\":\"1\"}]}]," +
      "\"files\":{\"1\":{\"key\":\"foo.js\",\"uuid\":\"" + file.uuid() + "\"}}}");
  }
}
