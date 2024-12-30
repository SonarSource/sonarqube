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
package org.sonar.server.source.ws;

import java.net.URL;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
import static org.sonar.api.measures.CoreMetrics.TESTS_KEY;
import static org.sonar.api.measures.CoreMetrics.VIOLATIONS_KEY;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentTesting.newFileDto;

public class IssueSnippetsActionIT {

  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public final UserSessionRule userSession = UserSessionRule.standalone();

  private final DbClient dbClient = db.getDbClient();
  private final FileSourceTester fileSourceTester = new FileSourceTester(db);
  private ProjectData project;
  private WsActionTester actionTester;
  private ComponentDto mainBranchComponent;

  @Before
  public void setUp() {
    project = db.components().insertPrivateProject("projectUuid", c -> c.setKey("KEY_projectUuid").setName("NAME_projectUuid"));
    mainBranchComponent = project.getMainBranchComponent();

    HtmlSourceDecorator htmlSourceDecorator = mock(HtmlSourceDecorator.class);
    when(htmlSourceDecorator.getDecoratedSourceAsHtml(anyString(), anyString(), anyString()))
      .then((Answer<String>) invocationOnMock -> "<p>" + invocationOnMock.getArguments()[0] + "</p>");
    LinesJsonWriter linesJsonWriter = new LinesJsonWriter(htmlSourceDecorator);
    ComponentViewerJsonWriter componentViewerJsonWriter = new ComponentViewerJsonWriter(dbClient);
    SourceService sourceService = new SourceService(dbClient, htmlSourceDecorator);
    actionTester = new WsActionTester(new IssueSnippetsAction(dbClient, userSession, sourceService, linesJsonWriter, componentViewerJsonWriter));
  }

  @Test
  public void verify_definition() {
    var def = actionTester.getDef();
    assertThat(def.isInternal()).isTrue();
    assertThat(def.since()).isEqualTo("7.8");

    assertThat(def.param("issueKey")).extracting(Param::isRequired, Param::description)
      .containsExactly(true, "Issue or hotspot key");
  }

  @Test
  public void should_display_single_location_single_file() {
    ComponentDto file = insertFile(mainBranchComponent, "file");
    DbFileSources.Data fileSources = FileSourceTesting.newFakeData(10).build();
    fileSourceTester.insertFileSource(file, 10, dto -> dto.setSourceData(fileSources));
    userSession.logIn().addProjectPermission(CODEVIEWER, project.getProjectDto());

    String issueKey = insertIssue(file, newLocation(file.uuid(), 5, 5));

    TestResponse response = actionTester.newRequest().setParam("issueKey", issueKey).execute();
    response.assertJson(getClass(), "issue_snippets_single_location.json");
  }

  @Test
  public void should_add_measures_to_components() {
    ComponentDto file = insertFile(mainBranchComponent, "file");

    db.measures().insertMeasure(file, m -> m.addValue(LINES_KEY, 200d));
    db.measures().insertMeasure(file, m -> m.addValue(DUPLICATED_LINES_DENSITY_KEY, 7.4));
    db.measures().insertMeasure(file, m -> m.addValue(TESTS_KEY, 3d));
    db.measures().insertMeasure(file, m -> m.addValue(TECHNICAL_DEBT_KEY, 182d));
    db.measures().insertMeasure(file, m -> m.addValue(VIOLATIONS_KEY, 231d));
    db.measures().insertMeasure(file, m -> m.addValue(COVERAGE_KEY, 95.4d));

    DbFileSources.Data fileSources = FileSourceTesting.newFakeData(10).build();
    fileSourceTester.insertFileSource(file, 10, dto -> dto.setSourceData(fileSources));
    userSession.logIn().addProjectPermission(CODEVIEWER, project.getProjectDto());

    String issueKey = insertIssue(file, newLocation(file.uuid(), 5, 5));

    TestResponse response = actionTester.newRequest().setParam("issueKey", issueKey).execute();
    response.assertJson(getClass(), "issue_snippets_with_measures.json");
  }

  @Test
  public void issue_references_a_non_existing_component() {
    ComponentDto file = insertFile(mainBranchComponent, "file");
    ComponentDto file2 = newFileDto(mainBranchComponent, null, "nonexisting");

    DbFileSources.Data fileSources = FileSourceTesting.newFakeData(10).build();
    fileSourceTester.insertFileSource(file, 10, dto -> dto.setSourceData(fileSources));
    userSession.logIn().addProjectPermission(CODEVIEWER, project.getProjectDto());

    String issueKey = insertIssue(file, newLocation(file2.uuid(), 5, 5));

    TestResponse response = actionTester.newRequest().setParam("issueKey", issueKey).execute();
    response.assertJson("{}");
  }

  @Test
  public void no_code_to_display() {
    ComponentDto file = insertFile(mainBranchComponent, "file");
    userSession.logIn().addProjectPermission(CODEVIEWER, project.getProjectDto());

    String issueKey = insertIssue(file, newLocation(file.uuid(), 5, 5));

    TestResponse response = actionTester.newRequest().setParam("issueKey", issueKey).execute();
    response.assertJson("{}");
  }

  @Test
  public void fail_if_no_project_permission() {
    ComponentDto file = insertFile(mainBranchComponent, "file");
    userSession.logIn().addProjectPermission(USER, project.getProjectDto());
    String issueKey = insertIssue(file, newLocation(file.uuid(), 5, 5));

    var request = actionTester.newRequest().setParam("issueKey", issueKey);
    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_issue_not_found() {
    ComponentDto file = insertFile(mainBranchComponent, "file");
    insertIssue(file, newLocation(file.uuid(), 5, 5));
    userSession.logIn().addProjectPermission(CODEVIEWER, project.getProjectDto());

    var request = actionTester.newRequest().setParam("issueKey", "invalid");
    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Issue with key 'invalid' does not exist");
  }

  @Test
  public void fail_if_parameter_missing() {
    ComponentDto file = insertFile(mainBranchComponent, "file");
    userSession.logIn().addProjectPermission(CODEVIEWER, project.getProjectDto());

    var request = actionTester.newRequest();
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("The 'issueKey' parameter is missing");
  }

  @Test
  public void should_display_multiple_locations_multiple_files() {
    ComponentDto file1 = insertFile(mainBranchComponent, "file1");
    ComponentDto file2 = insertFile(mainBranchComponent, "file2");

    DbFileSources.Data fileSources = FileSourceTesting.newFakeData(10).build();
    fileSourceTester.insertFileSource(file1, 10, dto -> dto.setSourceData(fileSources));
    fileSourceTester.insertFileSource(file2, 10, dto -> dto.setSourceData(fileSources));

    userSession.logIn().addProjectPermission(CODEVIEWER, project.getProjectDto());

    String issueKey1 = insertIssue(file1, newLocation(file1.uuid(), 5, 5),
      newLocation(file1.uuid(), 9, 9), newLocation(file2.uuid(), 1, 5));

    TestResponse response = actionTester.newRequest().setParam("issueKey", issueKey1).execute();
    JsonAssert.assertJson(response.getInput())
      .isSimilarTo(toUrl("issue_snippets_multiple_locations.json"));
  }

  @Test
  public void should_connect_snippets_close_to_each_other() {
    ComponentDto file1 = insertFile(mainBranchComponent, "file1");

    DbFileSources.Data fileSources = FileSourceTesting.newFakeData(20).build();
    fileSourceTester.insertFileSource(file1, 20, dto -> dto.setSourceData(fileSources));

    userSession.logIn().addProjectPermission(CODEVIEWER, project.getProjectDto());

    // these two locations should get connected, making a single range 3-14
    String issueKey1 = insertIssue(file1, newLocation(file1.uuid(), 5, 5),
      newLocation(file1.uuid(), 12, 12));

    TestResponse response = actionTester.newRequest().setParam("issueKey", issueKey1).execute();
    JsonAssert.assertJson(response.getInput())
      .isSimilarTo(toUrl("issue_snippets_close_to_each_other.json"));
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
    RuleDto rule = db.rules().insert();
    DbIssues.Flow flow = DbIssues.Flow.newBuilder().addAllLocation(Arrays.asList(locations)).build();

    IssueDto issue = db.issues().insert(rule, project.getMainBranchComponent(), file, i -> {
      i.setLocations(DbIssues.Locations.newBuilder().addFlow(flow).build());
      i.setProjectUuid(mainBranchComponent.uuid());
      i.setLine(locations[0].getTextRange().getStartLine());
    });

    db.commit();
    return issue.getKey();
  }

  private URL toUrl(String fileName) {
    Class<?> clazz = getClass();
    String path = clazz.getSimpleName() + "/" + fileName;
    URL url = clazz.getResource(path);
    if (url == null) {
      throw new IllegalStateException("Cannot find " + path);
    }
    return url;
  }

}
