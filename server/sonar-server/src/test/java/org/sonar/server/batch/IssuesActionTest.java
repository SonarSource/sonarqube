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

import com.google.common.base.Throwables;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.scanner.protocol.Constants.Severity;
import org.sonar.scanner.protocol.input.ScannerInput.ServerIssue;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.permission.index.AuthorizationTypeSupport;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.rule.RuleTesting.newRule;

public class IssuesActionTest {

  private static final String PROJECT_KEY = "struts";
  private static final String PROJECT_UUID = "ABCD";
  private static final String MODULE_KEY = "struts-core";
  private static final String MODULE_UUID = "BCDE";
  private final static String FILE_KEY = "Action.java";
  private static final String FILE_UUID = "CDEF";

  private System2 system2 = System2.INSTANCE;

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public EsTester es = new EsTester(new IssueIndexDefinition(new MapSettings()));
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private static RuleDefinitionDto RULE_DEFINITION = newRule(RuleKey.of("squid", "AvoidCycle"));

  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), new IssueIteratorFactory(db.getDbClient()));
  private PermissionIndexerTester authorizationIndexerTester = new PermissionIndexerTester(es, issueIndexer);
  private WsActionTester tester = new WsActionTester(new IssuesAction(db.getDbClient(),
    new IssueIndex(es.client(), system2, userSessionRule, new AuthorizationTypeSupport(userSessionRule)),
    userSessionRule, TestComponentFinder.from(db)));

  @Test
  public void return_minimal_fields() throws Exception {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(db.getDefaultOrganization(), PROJECT_UUID).setKey(PROJECT_KEY));
    ComponentDto module = db.components().insertComponent(newModuleDto(MODULE_UUID, project).setKey(MODULE_KEY));
    ComponentDto file = db.components().insertComponent(newFileDto(module, null, FILE_UUID).setKey(FILE_KEY).setPath(null));
    db.rules().insert(RULE_DEFINITION);
    db.issues().insert(RULE_DEFINITION, project, file, issue -> issue
      .setKee("EFGH")
      .setSeverity("BLOCKER")
      .setStatus("RESOLVED")
      .setType(BUG)
      .setResolution(null)
      .setManualSeverity(false)
      .setMessage(null)
      .setLine(null)
      .setChecksum(null)
      .setAssignee(null));
    indexIssues(project);
    addBrowsePermissionOnComponent(project);

    ServerIssue serverIssue = call(PROJECT_KEY);

    assertThat(serverIssue.getKey()).isEqualTo("EFGH");
    assertThat(serverIssue.getModuleKey()).isEqualTo(MODULE_KEY);
    assertThat(serverIssue.hasPath()).isFalse();
    assertThat(serverIssue.getRuleRepository()).isEqualTo("squid");
    assertThat(serverIssue.getRuleKey()).isEqualTo("AvoidCycle");
    assertThat(serverIssue.hasLine()).isFalse();
    assertThat(serverIssue.hasMsg()).isFalse();
    assertThat(serverIssue.hasResolution()).isFalse();
    assertThat(serverIssue.getStatus()).isEqualTo("RESOLVED");
    assertThat(serverIssue.getSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(serverIssue.getType()).isEqualTo(BUG.name());
    assertThat(serverIssue.getManualSeverity()).isFalse();
    assertThat(serverIssue.hasChecksum()).isFalse();
    assertThat(serverIssue.hasAssigneeLogin()).isFalse();
  }

  @Test
  public void issues_from_project() throws Exception {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(organizationDto, PROJECT_UUID).setKey(PROJECT_KEY));
    ComponentDto module = db.components().insertComponent(newModuleDto(MODULE_UUID, project).setKey(MODULE_KEY));
    ComponentDto file = db.components().insertComponent(newFileDto(module, null, FILE_UUID).setKey(FILE_KEY).setPath("src/org/struts/Action.java"));
    db.rules().insert(RULE_DEFINITION);
    db.issues().insert(RULE_DEFINITION, project, file, issue -> issue
      .setKee("EFGH")
      .setSeverity("BLOCKER")
      .setStatus("RESOLVED")
      .setResolution("FALSE-POSITIVE")
      .setManualSeverity(false)
      .setMessage("Do not use this method")
      .setLine(200)
      .setChecksum("123456")
      .setAssignee("john"));
    indexIssues(project);
    addBrowsePermissionOnComponent(project);

    ServerIssue serverIssue = call(PROJECT_KEY);

    assertThat(serverIssue.getKey()).isEqualTo("EFGH");
    assertThat(serverIssue.getModuleKey()).isEqualTo(MODULE_KEY);
    assertThat(serverIssue.getPath()).isEqualTo("src/org/struts/Action.java");
    assertThat(serverIssue.getRuleRepository()).isEqualTo("squid");
    assertThat(serverIssue.getRuleKey()).isEqualTo("AvoidCycle");
    assertThat(serverIssue.getLine()).isEqualTo(200);
    assertThat(serverIssue.getMsg()).isEqualTo("Do not use this method");
    assertThat(serverIssue.getResolution()).isEqualTo("FALSE-POSITIVE");
    assertThat(serverIssue.getStatus()).isEqualTo("RESOLVED");
    assertThat(serverIssue.getSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(serverIssue.getManualSeverity()).isFalse();
    assertThat(serverIssue.getChecksum()).isEqualTo("123456");
    assertThat(serverIssue.getAssigneeLogin()).isEqualTo("john");
  }

  @Test
  public void issues_from_module() throws Exception {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(db.getDefaultOrganization(), PROJECT_UUID).setKey(PROJECT_KEY));
    ComponentDto module = db.components().insertComponent(newModuleDto(MODULE_UUID, project).setKey(MODULE_KEY));
    ComponentDto file = db.components().insertComponent(newFileDto(module, null, FILE_UUID).setKey(FILE_KEY).setPath("src/org/struts/Action.java"));
    db.rules().insert(RULE_DEFINITION);
    db.issues().insert(RULE_DEFINITION, project, file, issue -> issue
      .setKee("EFGH")
      .setSeverity("BLOCKER")
      .setStatus("RESOLVED")
      .setResolution("FALSE-POSITIVE")
      .setManualSeverity(false)
      .setMessage("Do not use this method")
      .setLine(200)
      .setChecksum("123456")
      .setAssignee("john"));
    indexIssues(project);
    addBrowsePermissionOnComponent(project);

    ServerIssue serverIssue = call(PROJECT_KEY);

    assertThat(serverIssue.getKey()).isEqualTo("EFGH");
    assertThat(serverIssue.getModuleKey()).isEqualTo(MODULE_KEY);
    assertThat(serverIssue.getPath()).isEqualTo("src/org/struts/Action.java");
    assertThat(serverIssue.getRuleRepository()).isEqualTo("squid");
    assertThat(serverIssue.getRuleKey()).isEqualTo("AvoidCycle");
    assertThat(serverIssue.getLine()).isEqualTo(200);
    assertThat(serverIssue.getMsg()).isEqualTo("Do not use this method");
    assertThat(serverIssue.getResolution()).isEqualTo("FALSE-POSITIVE");
    assertThat(serverIssue.getStatus()).isEqualTo("RESOLVED");
    assertThat(serverIssue.getSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(serverIssue.getManualSeverity()).isFalse();
    assertThat(serverIssue.getChecksum()).isEqualTo("123456");
    assertThat(serverIssue.getAssigneeLogin()).isEqualTo("john");
  }

  @Test
  public void issues_from_file() throws Exception {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(db.getDefaultOrganization(), PROJECT_UUID).setKey(PROJECT_KEY));
    ComponentDto module = db.components().insertComponent(newModuleDto(MODULE_UUID, project).setKey(MODULE_KEY));
    ComponentDto file = db.components().insertComponent(newFileDto(module, null, FILE_UUID).setKey(FILE_KEY).setPath("src/org/struts/Action.java"));
    db.rules().insert(RULE_DEFINITION);
    db.issues().insert(RULE_DEFINITION, project, file, issue -> issue
      .setKee("EFGH")
      .setSeverity("BLOCKER")
      .setStatus("RESOLVED")
      .setResolution("FALSE-POSITIVE")
      .setManualSeverity(false)
      .setMessage("Do not use this method")
      .setLine(200)
      .setChecksum("123456")
      .setAssignee("john"));
    indexIssues(project);
    addBrowsePermissionOnComponent(project);

    ServerIssue serverIssue = call(FILE_KEY);

    assertThat(serverIssue.getKey()).isEqualTo("EFGH");
    assertThat(serverIssue.getModuleKey()).isEqualTo(MODULE_KEY);
    assertThat(serverIssue.getPath()).isEqualTo("src/org/struts/Action.java");
    assertThat(serverIssue.getRuleRepository()).isEqualTo("squid");
    assertThat(serverIssue.getRuleKey()).isEqualTo("AvoidCycle");
    assertThat(serverIssue.getLine()).isEqualTo(200);
    assertThat(serverIssue.getMsg()).isEqualTo("Do not use this method");
    assertThat(serverIssue.getResolution()).isEqualTo("FALSE-POSITIVE");
    assertThat(serverIssue.getStatus()).isEqualTo("RESOLVED");
    assertThat(serverIssue.getSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(serverIssue.getManualSeverity()).isFalse();
    assertThat(serverIssue.getChecksum()).isEqualTo("123456");
    assertThat(serverIssue.getAssigneeLogin()).isEqualTo("john");
  }

  @Test
  public void issues_attached_on_module() throws Exception {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(organizationDto, PROJECT_UUID).setKey(PROJECT_KEY));
    ComponentDto module = db.components().insertComponent(newModuleDto(MODULE_UUID, project).setKey(MODULE_KEY));
    db.rules().insert(RULE_DEFINITION);
    db.issues().insert(RULE_DEFINITION, project, module, issue -> issue
      .setKee("EFGH")
      .setSeverity("BLOCKER")
      .setStatus("RESOLVED")
      .setResolution("FALSE-POSITIVE")
      .setManualSeverity(false)
      .setMessage("Do not use this method")
      .setLine(200)
      .setChecksum("123456")
      .setAssignee("john"));
    indexIssues(project);
    addBrowsePermissionOnComponent(project);

    ServerIssue serverIssue = call(MODULE_KEY);

    assertThat(serverIssue.getKey()).isEqualTo("EFGH");
    assertThat(serverIssue.getModuleKey()).isEqualTo(MODULE_KEY);
    assertThat(serverIssue.hasPath()).isFalse();
    assertThat(serverIssue.getRuleRepository()).isEqualTo("squid");
    assertThat(serverIssue.getRuleKey()).isEqualTo("AvoidCycle");
    assertThat(serverIssue.getLine()).isEqualTo(200);
    assertThat(serverIssue.getMsg()).isEqualTo("Do not use this method");
    assertThat(serverIssue.getResolution()).isEqualTo("FALSE-POSITIVE");
    assertThat(serverIssue.getStatus()).isEqualTo("RESOLVED");
    assertThat(serverIssue.getSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(serverIssue.getManualSeverity()).isFalse();
    assertThat(serverIssue.getChecksum()).isEqualTo("123456");
    assertThat(serverIssue.getAssigneeLogin()).isEqualTo("john");
  }

  @Test
  public void project_issues_attached_file_on_removed_module() throws Exception {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(db.getDefaultOrganization(), PROJECT_UUID).setKey(PROJECT_KEY));
    // File and module are removed
    ComponentDto module = db.components().insertComponent(newModuleDto(MODULE_UUID, project).setKey(MODULE_KEY).setEnabled(false));
    ComponentDto file = db.components().insertComponent(newFileDto(module, null, FILE_UUID).setKey(FILE_KEY).setPath("src/org/struts/Action.java").setEnabled(false));
    db.rules().insert(RULE_DEFINITION);
    db.issues().insert(RULE_DEFINITION, project, file, issue -> issue
      .setKee("EFGH")
      .setSeverity("BLOCKER")
      .setStatus("RESOLVED")
      .setResolution("FALSE-POSITIVE")
      .setManualSeverity(false)
      .setMessage("Do not use this method")
      .setLine(200)
      .setChecksum("123456")
      .setAssignee("john"));
    indexIssues(project);
    addBrowsePermissionOnComponent(project);

    ServerIssue serverIssue = call(PROJECT_KEY);

    assertThat(serverIssue.getKey()).isEqualTo("EFGH");
    // Module key of removed file should be returned
    assertThat(serverIssue.getModuleKey()).isEqualTo(MODULE_KEY);
  }

  @Test
  public void fail_without_browse_permission_on_file() throws Exception {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));

    thrown.expect(ForbiddenException.class);

    tester.newRequest().setParam("key", file.key()).execute();
  }

  private void indexIssues(ComponentDto project) {
    issueIndexer.indexOnStartup(null);
    authorizationIndexerTester.allowOnlyAnyone(project);
  }

  private void addBrowsePermissionOnComponent(ComponentDto project) {
    userSessionRule.addProjectPermission(UserRole.USER, project);
  }

  private ServerIssue call(String componentKey) {
    try {
      TestResponse response = tester.newRequest().setParam("key", componentKey).execute();
      return ServerIssue.parseDelimitedFrom(response.getInputStream());
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
