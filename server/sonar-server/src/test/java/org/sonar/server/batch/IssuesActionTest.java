/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.batch;

import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.CloseableIterator;
import org.sonar.core.util.Protobuf;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.scanner.protocol.Constants.Severity;
import org.sonar.scanner.protocol.input.ScannerInput.ServerIssue;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;

public class IssuesActionTest {

  private System2 system2 = System2.INSTANCE;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private WsActionTester tester = new WsActionTester(new IssuesAction(db.getDbClient(), userSessionRule, TestComponentFinder.from(db)));

  @Test
  public void test_nullable_fields() throws Exception {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    ComponentDto file = db.components().insertComponent(newFileDto(module, null).setPath(null));
    IssueDto issue = db.issues().insert(rule, project, file, i ->
      i.setSeverity("BLOCKER")
        // non-null fields
        .setStatus("OPEN")
        .setType(BUG)
        .setManualSeverity(true)

        // all the nullable fields
        .setResolution(null)
        .setMessage(null)
        .setLine(null)
        .setChecksum(null)
        .setAssignee(null));
    addPermissionTo(project);

    ServerIssue serverIssue = call(project.getDbKey());

    assertThat(serverIssue.getKey()).isEqualTo(issue.getKey());
    assertThat(serverIssue.getModuleKey()).isEqualTo(module.getDbKey());
    assertThat(serverIssue.getRuleRepository()).isEqualTo(rule.getRepositoryKey());
    assertThat(serverIssue.getRuleKey()).isEqualTo(rule.getRuleKey());
    assertThat(serverIssue.getStatus()).isEqualTo("OPEN");
    assertThat(serverIssue.getSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(serverIssue.getType()).isEqualTo(BUG.name());
    assertThat(serverIssue.getManualSeverity()).isTrue();

    assertThat(serverIssue.hasPath()).isFalse();
    assertThat(serverIssue.hasLine()).isFalse();
    assertThat(serverIssue.hasMsg()).isFalse();
    assertThat(serverIssue.hasResolution()).isFalse();
    assertThat(serverIssue.hasChecksum()).isFalse();
    assertThat(serverIssue.hasAssigneeLogin()).isFalse();
  }

  @Test
  public void test_fields_with_non_null_values() throws Exception {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    ComponentDto file = db.components().insertComponent(newFileDto(module, null));
    IssueDto issue = db.issues().insert(rule, project, file, i ->
      i.setSeverity("BLOCKER")
        .setStatus("OPEN")
        .setType(BUG)
        .setManualSeverity(true)
        .setResolution("FIXED")
        .setMessage("the message")
        .setLine(10)
        .setChecksum("ABC")
        .setAssignee("foo"));
    addPermissionTo(project);

    ServerIssue serverIssue = call(project.getDbKey());

    assertThat(serverIssue.getKey()).isEqualTo(issue.getKey());
    assertThat(serverIssue.getModuleKey()).isEqualTo(module.getDbKey());
    assertThat(serverIssue.getRuleRepository()).isEqualTo(rule.getRepositoryKey());
    assertThat(serverIssue.getRuleKey()).isEqualTo(rule.getRuleKey());
    assertThat(serverIssue.getStatus()).isEqualTo("OPEN");
    assertThat(serverIssue.getSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(serverIssue.getType()).isEqualTo(BUG.name());
    assertThat(serverIssue.getManualSeverity()).isTrue();
    assertThat(serverIssue.getPath()).isEqualTo(file.path());
    assertThat(serverIssue.getLine()).isEqualTo(issue.getLine());
    assertThat(serverIssue.getMsg()).isEqualTo(issue.getMessage());
    assertThat(serverIssue.getResolution()).isEqualTo(issue.getResolution());
    assertThat(serverIssue.getChecksum()).isEqualTo(issue.getChecksum());
    assertThat(serverIssue.getAssigneeLogin()).isEqualTo(issue.getAssignee());
  }

  @Test
  public void return_issues_of_project() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    ComponentDto file = db.components().insertComponent(newFileDto(module, null));
    IssueDto issueOnFile = db.issues().insert(rule, project, file, i -> i.setKee("ON_FILE"));
    IssueDto issueOnModule = db.issues().insert(rule, project, module, i -> i.setKee("ON_MODULE"));
    IssueDto issueOnProject = db.issues().insert(rule, project, project, i -> i.setKee("ON_PROJECT"));

    addPermissionTo(project);
    try (CloseableIterator<ServerIssue> result = callStream(project.getDbKey())) {
      assertThat(result)
        .extracting(ServerIssue::getKey, ServerIssue::getModuleKey)
        .containsExactlyInAnyOrder(
          tuple(issueOnFile.getKey(), module.getDbKey()),
          tuple(issueOnModule.getKey(), module.getDbKey()),
          tuple(issueOnProject.getKey(), project.getDbKey()));
    }
  }

  @Test
  public void return_issues_of_module() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    ComponentDto file = db.components().insertComponent(newFileDto(module, null));
    IssueDto issueOnFile = db.issues().insert(rule, project, file, i -> i.setKee("ON_FILE"));
    IssueDto issueOnModule = db.issues().insert(rule, project, module, i -> i.setKee("ON_MODULE"));
    IssueDto issueOnProject = db.issues().insert(rule, project, project, i -> i.setKee("ON_PROJECT"));

    addPermissionTo(project);
    try (CloseableIterator<ServerIssue> result = callStream(module.getDbKey())) {
      assertThat(result)
        .extracting(ServerIssue::getKey, ServerIssue::getModuleKey)
        .containsExactlyInAnyOrder(
          tuple(issueOnFile.getKey(), module.getDbKey()),
          tuple(issueOnModule.getKey(), module.getDbKey()));
    }
  }

  @Test
  public void return_issues_of_file() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    ComponentDto file = db.components().insertComponent(newFileDto(module, null));
    IssueDto issueOnFile = db.issues().insert(rule, project, file);
    IssueDto issueOnModule = db.issues().insert(rule, project, module);
    IssueDto issueOnProject = db.issues().insert(rule, project, project);

    addPermissionTo(project);
    try (CloseableIterator<ServerIssue> result = callStream(file.getDbKey())) {
      assertThat(result)
        .extracting(ServerIssue::getKey, ServerIssue::getModuleKey)
        .containsExactlyInAnyOrder(
          tuple(issueOnFile.getKey(), module.getDbKey()));
    }
  }

  @Test
  public void fail_if_requested_component_is_a_directory() throws IOException {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "src/main/java"));
    addPermissionTo(project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Component of scope 'DIR' is not allowed");

    call(directory.getDbKey());
  }

  @Test
  public void issues_on_disabled_modules_are_returned() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project).setEnabled(false));
    ComponentDto file = db.components().insertComponent(newFileDto(module, null).setEnabled(false));
    IssueDto issue = db.issues().insert(rule, project, file);

    addPermissionTo(project);
    try (CloseableIterator<ServerIssue> result = callStream(project.getDbKey())) {
      // Module key of removed file should be returned
      assertThat(result)
        .extracting(ServerIssue::getKey, ServerIssue::getModuleKey)
        .containsExactly(tuple(issue.getKey(), module.getDbKey()));
    }
  }

  @Test
  public void fail_if_user_does_not_have_permission_on_project() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));

    expectedException.expect(ForbiddenException.class);

    tester.newRequest().setParam("key", file.getDbKey()).execute();
  }

  @Test
  public void fail_if_component_does_not_exist() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component key 'does_not_exist' not found");

    tester.newRequest().setParam("key", "does_not_exist").execute();
  }

  private void addPermissionTo(ComponentDto project) {
    userSessionRule.addProjectPermission(UserRole.USER, project);
  }

  private ServerIssue call(String componentKey) throws IOException {
    TestResponse response = tester.newRequest().setParam("key", componentKey).execute();
    return ServerIssue.parseDelimitedFrom(response.getInputStream());
  }

  private CloseableIterator<ServerIssue> callStream(String componentKey) {
    TestResponse response = tester.newRequest().setParam("key", componentKey).execute();
    return Protobuf.readStream(response.getInputStream(), ServerIssue.parser());
  }
}
