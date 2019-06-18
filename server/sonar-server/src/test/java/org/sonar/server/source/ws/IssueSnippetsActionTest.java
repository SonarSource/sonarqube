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
package org.sonar.server.source.ws;

import java.net.URL;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.stubbing.Answer;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.source.FileSourceTester;
import org.sonar.server.component.ws.ComponentViewerJsonWriter;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.source.HtmlSourceDecorator;
import org.sonar.server.source.SourceService;
import org.sonar.server.source.index.FileSourceTesting;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
import static org.sonar.api.measures.CoreMetrics.TESTS_KEY;
import static org.sonar.api.measures.CoreMetrics.VIOLATIONS_KEY;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentTesting.newFileDto;

public class IssueSnippetsActionTest {
  private static final String SCM_AUTHOR_JSON_FIELD = "scmAuthor";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DbClient dbClient = db.getDbClient();
  private FileSourceTester fileSourceTester = new FileSourceTester(db);
  private OrganizationDto organization;
  private ComponentDto project;
  private WsActionTester actionTester;

  @Before
  public void setUp() {
    organization = db.organizations().insert();
    project = db.components().insertPrivateProject(organization, "projectUuid");

    HtmlSourceDecorator htmlSourceDecorator = mock(HtmlSourceDecorator.class);
    when(htmlSourceDecorator.getDecoratedSourceAsHtml(anyString(), anyString(), anyString()))
      .then((Answer<String>) invocationOnMock -> "<p>" + invocationOnMock.getArguments()[0] + "</p>");
    LinesJsonWriter linesJsonWriter = new LinesJsonWriter(htmlSourceDecorator);
    ComponentViewerJsonWriter componentViewerJsonWriter = new ComponentViewerJsonWriter(dbClient);
    SourceService sourceService = new SourceService(dbClient, htmlSourceDecorator);
    actionTester = new WsActionTester(new IssueSnippetsAction(dbClient, userSession, sourceService, linesJsonWriter, componentViewerJsonWriter));
  }

  @Test
  public void should_display_single_location_single_file() {
    ComponentDto file = insertFile(project, "file");
    DbFileSources.Data fileSources = FileSourceTesting.newFakeData(10).build();
    fileSourceTester.insertFileSource(file, 10, dto -> dto.setSourceData(fileSources));
    userSession.logIn().addProjectPermission(USER, project, file);

    String issueKey = insertIssue(file, newLocation(file.uuid(), 5, 5));

    TestResponse response = actionTester.newRequest().setParam("issueKey", issueKey).execute();
    response.assertJson(getClass(), "issue_snippets_single_location.json");
  }

  @Test
  public void should_add_measures_to_components() {
    ComponentDto file = insertFile(project, "file");

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

    DbFileSources.Data fileSources = FileSourceTesting.newFakeData(10).build();
    fileSourceTester.insertFileSource(file, 10, dto -> dto.setSourceData(fileSources));
    userSession.logIn().addProjectPermission(USER, project, file);

    String issueKey = insertIssue(file, newLocation(file.uuid(), 5, 5));

    TestResponse response = actionTester.newRequest().setParam("issueKey", issueKey).execute();
    response.assertJson(getClass(), "issue_snippets_with_measures.json");
  }

  @Test
  public void issue_references_a_non_existing_component() {
    ComponentDto file = insertFile(project, "file");
    ComponentDto file2 = newFileDto(project, null, "nonexisting");

    DbFileSources.Data fileSources = FileSourceTesting.newFakeData(10).build();
    fileSourceTester.insertFileSource(file, 10, dto -> dto.setSourceData(fileSources));
    userSession.logIn().addProjectPermission(USER, project, file);

    String issueKey = insertIssue(file, newLocation(file2.uuid(), 5, 5));

    TestResponse response = actionTester.newRequest().setParam("issueKey", issueKey).execute();
    response.assertJson("{}");
  }

  @Test
  public void no_code_to_display() {
    ComponentDto file = insertFile(project, "file");
    userSession.logIn().addProjectPermission(USER, project, file);

    String issueKey = insertIssue(file, newLocation(file.uuid(), 5, 5));

    TestResponse response = actionTester.newRequest().setParam("issueKey", issueKey).execute();
    response.assertJson("{}");
  }

  @Test
  public void fail_if_no_project_permission() {
    ComponentDto file = insertFile(project, "file");
    String issueKey = insertIssue(file, newLocation(file.uuid(), 5, 5));

    expectedException.expect(ForbiddenException.class);
    actionTester.newRequest().setParam("issueKey", issueKey).execute();
  }

  @Test
  public void fail_if_issue_not_found() {
    ComponentDto file = insertFile(project, "file");
    insertIssue(file, newLocation(file.uuid(), 5, 5));
    userSession.logIn().addProjectPermission(USER, project, file);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Issue with key 'invalid' does not exist");
    actionTester.newRequest().setParam("issueKey", "invalid").execute();
  }

  @Test
  public void fail_if_parameter_missing() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'issueKey' parameter is missing");
    actionTester.newRequest().execute();
  }

  @Test
  public void should_display_multiple_locations_multiple_files() {
    ComponentDto file1 = insertFile(project, "file1");
    ComponentDto file2 = insertFile(project, "file2");

    DbFileSources.Data fileSources = FileSourceTesting.newFakeData(10).build();
    fileSourceTester.insertFileSource(file1, 10, dto -> dto.setSourceData(fileSources));
    fileSourceTester.insertFileSource(file2, 10, dto -> dto.setSourceData(fileSources));

    userSession.logIn().addProjectPermission(USER, project, file1, file2);

    String issueKey1 = insertIssue(file1, newLocation(file1.uuid(), 5, 5),
      newLocation(file1.uuid(), 9, 9), newLocation(file2.uuid(), 1, 5));

    TestResponse response = actionTester.newRequest().setParam("issueKey", issueKey1).execute();
    JsonAssert.assertJson(response.getInput())
      .ignoreFields(SCM_AUTHOR_JSON_FIELD)
      .isSimilarTo(toUrl("issue_snippets_multiple_locations.json"));
    assertThat(response.getInput()).doesNotContain(SCM_AUTHOR_JSON_FIELD);
  }

  @Test
  public void should_connect_snippets_close_to_each_other() {
    ComponentDto file1 = insertFile(project, "file1");

    DbFileSources.Data fileSources = FileSourceTesting.newFakeData(20).build();
    fileSourceTester.insertFileSource(file1, 20, dto -> dto.setSourceData(fileSources));

    userSession.logIn().addProjectPermission(USER, project, file1);

    // these two locations should get connected, making a single range 3-14
    String issueKey1 = insertIssue(file1, newLocation(file1.uuid(), 5, 5),
      newLocation(file1.uuid(), 12, 12));

    TestResponse response = actionTester.newRequest().setParam("issueKey", issueKey1).execute();
    JsonAssert.assertJson(response.getInput())
      .ignoreFields(SCM_AUTHOR_JSON_FIELD)
      .isSimilarTo(toUrl("issue_snippets_close_to_each_other.json"));
    assertThat(response.getInput()).doesNotContain(SCM_AUTHOR_JSON_FIELD);
  }

  @Test
  public void returns_scmAuthors_if_user_belongs_to_organization_of_project_of_issue() {
    ComponentDto file1 = insertFile(project, "file1");
    ComponentDto file2 = insertFile(project, "file2");

    DbFileSources.Data fileSources = FileSourceTesting.newFakeData(10).build();
    fileSourceTester.insertFileSource(file1, 10, dto -> dto.setSourceData(fileSources));
    fileSourceTester.insertFileSource(file2, 10, dto -> dto.setSourceData(fileSources));

    userSession.logIn()
      .addProjectPermission(USER, project, file1, file2)
      .addMembership(organization);

    String issueKey1 = insertIssue(file1, newLocation(file1.uuid(), 5, 5),
      newLocation(file1.uuid(), 9, 9), newLocation(file2.uuid(), 1, 5));

    TestResponse response = actionTester.newRequest().setParam("issueKey", issueKey1).execute();
    JsonAssert.assertJson(response.getInput())
      .isSimilarTo(toUrl("issue_snippets_multiple_locations.json"));
  }

  private DbIssues.Location newLocation(String fileUuid, int startLine, int endLine) {
    return DbIssues.Location.newBuilder()
      .setTextRange(DbCommons.TextRange.newBuilder().setStartLine(startLine).setEndLine(endLine).build())
      .setComponentId(fileUuid)
      .build();
  }

  private ComponentDto insertFile(ComponentDto project, String name) {
    ComponentDto file = newFileDto(project, null, name);
    db.components().insertComponents(file);
    return file;
  }

  private String insertIssue(ComponentDto file, DbIssues.Location... locations) {
    RuleDefinitionDto rule = db.rules().insert();
    DbIssues.Flow flow = DbIssues.Flow.newBuilder().addAllLocation(Arrays.asList(locations)).build();

    IssueDto issue = db.issues().insert(rule, project, file, i -> {
      i.setLocations(DbIssues.Locations.newBuilder().addFlow(flow).build());
      i.setProjectUuid(project.uuid());
      i.setLine(locations[0].getTextRange().getStartLine());
    });

    db.commit();
    return issue.getKey();
  }

  private URL toUrl(String fileName) {
    Class clazz = getClass();
    String path = clazz.getSimpleName() + "/" + fileName;
    URL url = clazz.getResource(path);
    if (url == null) {
      throw new IllegalStateException("Cannot find " + path);
    }
    return url;
  }

}
