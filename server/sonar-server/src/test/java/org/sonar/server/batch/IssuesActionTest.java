/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.scanner.protocol.Constants.Severity;
import org.sonar.scanner.protocol.input.ScannerInput.ServerIssue;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.permission.index.AuthorizationDao;
import org.sonar.server.permission.index.AuthorizationIndexer;
import org.sonar.server.platform.ServerFileSystem;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class IssuesActionTest {

  final static String PROJECT_KEY = "struts";
  static final String PROJECT_UUID = "ABCD";

  final static String MODULE_KEY = "struts-core";
  static final String MODULE_UUID = "BCDE";

  final static String FILE_KEY = "Action.java";
  static final String FILE_UUID = "CDEF";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public EsTester es = new EsTester(new IssueIndexDefinition(new MapSettings()));

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private IssueIndex issueIndex;
  private IssueIndexer issueIndexer;
  private AuthorizationIndexer issueAuthorizationIndexer;
  private ServerFileSystem fs = mock(ServerFileSystem.class);

  WsTester tester;

  IssuesAction issuesAction;

  @Before
  public void before() {
    issueIndex = new IssueIndex(es.client(), System2.INSTANCE, userSessionRule);
    issueIndexer = new IssueIndexer(null, es.client());
    issueAuthorizationIndexer = new AuthorizationIndexer(null, es.client());
    issuesAction = new IssuesAction(db.getDbClient(), issueIndex, userSessionRule, new ComponentFinder(db.getDbClient()));

    tester = new WsTester(new BatchWs(new BatchIndex(fs), issuesAction));
  }

  @Test
  public void return_minimal_fields() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto(PROJECT_UUID).setKey(PROJECT_KEY);
    ComponentDto module = ComponentTesting.newModuleDto(MODULE_UUID, project).setKey(MODULE_KEY);
    ComponentDto file = ComponentTesting.newFileDto(module, null, FILE_UUID).setKey(FILE_KEY).setPath(null);
    db.getDbClient().componentDao().insert(db.getSession(), project, module, file);
    db.getSession().commit();

    indexIssues(IssueTesting.newDoc("EFGH", file)
      .setRuleKey("squid:AvoidCycle")
      .setSeverity("BLOCKER")
      .setStatus("RESOLVED")
      .setResolution(null)
      .setManualSeverity(false)
      .setMessage(null)
      .setLine(null)
      .setChecksum(null)
      .setAssignee(null));

    addBrowsePermissionOnComponent(PROJECT_KEY);
    WsTester.TestRequest request = tester.newGetRequest("batch", "issues").setParam("key", PROJECT_KEY);

    ServerIssue serverIssue = ServerIssue.parseDelimitedFrom(new ByteArrayInputStream(request.execute().output()));
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
    assertThat(serverIssue.getManualSeverity()).isFalse();
    assertThat(serverIssue.hasChecksum()).isFalse();
    assertThat(serverIssue.hasAssigneeLogin()).isFalse();
  }

  @Test
  public void issues_from_project() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto(PROJECT_UUID).setKey(PROJECT_KEY);
    ComponentDto module = ComponentTesting.newModuleDto(MODULE_UUID, project).setKey(MODULE_KEY);
    ComponentDto file = ComponentTesting.newFileDto(module, null, FILE_UUID).setKey(FILE_KEY).setPath("src/org/struts/Action.java");
    db.getDbClient().componentDao().insert(db.getSession(), project, module, file);
    db.getSession().commit();

    indexIssues(IssueTesting.newDoc("EFGH", file)
      .setRuleKey("squid:AvoidCycle")
      .setSeverity("BLOCKER")
      .setStatus("RESOLVED")
      .setResolution("FALSE-POSITIVE")
      .setManualSeverity(false)
      .setMessage("Do not use this method")
      .setLine(200)
      .setChecksum("123456")
      .setAssignee("john"));

    addBrowsePermissionOnComponent(PROJECT_KEY);
    WsTester.TestRequest request = tester.newGetRequest("batch", "issues").setParam("key", PROJECT_KEY);

    ServerIssue serverIssue = ServerIssue.parseDelimitedFrom(new ByteArrayInputStream(request.execute().output()));
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
    ComponentDto project = ComponentTesting.newProjectDto(PROJECT_UUID).setKey(PROJECT_KEY);
    ComponentDto module = ComponentTesting.newModuleDto(MODULE_UUID, project).setKey(MODULE_KEY);
    ComponentDto file = ComponentTesting.newFileDto(module, null, FILE_UUID).setKey(FILE_KEY).setPath("src/org/struts/Action.java");
    db.getDbClient().componentDao().insert(db.getSession(), project, module, file);
    db.getSession().commit();

    indexIssues(IssueTesting.newDoc("EFGH", file)
      .setRuleKey("squid:AvoidCycle")
      .setSeverity("BLOCKER")
      .setStatus("RESOLVED")
      .setResolution("FALSE-POSITIVE")
      .setManualSeverity(false)
      .setMessage("Do not use this method")
      .setLine(200)
      .setChecksum("123456")
      .setAssignee("john"));

    addBrowsePermissionOnComponent(PROJECT_KEY);
    WsTester.TestRequest request = tester.newGetRequest("batch", "issues").setParam("key", PROJECT_KEY);

    ServerIssue serverIssue = ServerIssue.parseDelimitedFrom(new ByteArrayInputStream(request.execute().output()));
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
    ComponentDto project = ComponentTesting.newProjectDto(PROJECT_UUID).setKey(PROJECT_KEY);
    ComponentDto module = ComponentTesting.newModuleDto(MODULE_UUID, project).setKey(MODULE_KEY);
    ComponentDto file = ComponentTesting.newFileDto(module, null, FILE_UUID).setKey(FILE_KEY).setPath("src/org/struts/Action.java");
    db.getDbClient().componentDao().insert(db.getSession(), project, module, file);
    db.getSession().commit();

    indexIssues(IssueTesting.newDoc("EFGH", file)
      .setRuleKey("squid:AvoidCycle")
      .setSeverity("BLOCKER")
      .setStatus("RESOLVED")
      .setResolution("FALSE-POSITIVE")
      .setManualSeverity(false)
      .setMessage("Do not use this method")
      .setLine(200)
      .setChecksum("123456")
      .setAssignee("john"));

    addBrowsePermissionOnComponent(FILE_KEY);
    WsTester.TestRequest request = tester.newGetRequest("batch", "issues").setParam("key", FILE_KEY);

    ServerIssue serverIssue = ServerIssue.parseDelimitedFrom(new ByteArrayInputStream(request.execute().output()));
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
    ComponentDto project = ComponentTesting.newProjectDto(PROJECT_UUID).setKey(PROJECT_KEY);
    ComponentDto module = ComponentTesting.newModuleDto(MODULE_UUID, project).setKey(MODULE_KEY);
    db.getDbClient().componentDao().insert(db.getSession(), project, module);
    db.getSession().commit();

    indexIssues(IssueTesting.newDoc("EFGH", module)
      .setRuleKey("squid:AvoidCycle")
      .setSeverity("BLOCKER")
      .setStatus("RESOLVED")
      .setResolution("FALSE-POSITIVE")
      .setManualSeverity(false)
      .setMessage("Do not use this method")
      .setLine(200)
      .setChecksum("123456")
      .setAssignee("john"));

    addBrowsePermissionOnComponent(MODULE_KEY);
    WsTester.TestRequest request = tester.newGetRequest("batch", "issues").setParam("key", MODULE_KEY);

    ServerIssue previousIssue = ServerIssue.parseDelimitedFrom(new ByteArrayInputStream(request.execute().output()));
    assertThat(previousIssue.getKey()).isEqualTo("EFGH");
    assertThat(previousIssue.getModuleKey()).isEqualTo(MODULE_KEY);
    assertThat(previousIssue.hasPath()).isFalse();
    assertThat(previousIssue.getRuleRepository()).isEqualTo("squid");
    assertThat(previousIssue.getRuleKey()).isEqualTo("AvoidCycle");
    assertThat(previousIssue.getLine()).isEqualTo(200);
    assertThat(previousIssue.getMsg()).isEqualTo("Do not use this method");
    assertThat(previousIssue.getResolution()).isEqualTo("FALSE-POSITIVE");
    assertThat(previousIssue.getStatus()).isEqualTo("RESOLVED");
    assertThat(previousIssue.getSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(previousIssue.getManualSeverity()).isFalse();
    assertThat(previousIssue.getChecksum()).isEqualTo("123456");
    assertThat(previousIssue.getAssigneeLogin()).isEqualTo("john");
  }

  @Test
  public void project_issues_attached_file_on_removed_module() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto(PROJECT_UUID).setKey(PROJECT_KEY);
    // File and module are removed
    ComponentDto module = ComponentTesting.newModuleDto(MODULE_UUID, project).setKey(MODULE_KEY).setEnabled(false);
    ComponentDto file = ComponentTesting.newFileDto(module, null, FILE_UUID).setKey(FILE_KEY).setPath("src/org/struts/Action.java").setEnabled(false);
    db.getDbClient().componentDao().insert(db.getSession(), project, module, file);
    db.getSession().commit();

    indexIssues(IssueTesting.newDoc("EFGH", file)
      .setRuleKey("squid:AvoidCycle")
      .setSeverity("BLOCKER")
      .setStatus("RESOLVED")
      .setResolution("FALSE-POSITIVE")
      .setManualSeverity(false)
      .setMessage("Do not use this method")
      .setLine(200)
      .setChecksum("123456")
      .setAssignee("john"));

    addBrowsePermissionOnComponent(PROJECT_KEY);
    WsTester.TestRequest request = tester.newGetRequest("batch", "issues").setParam("key", PROJECT_KEY);

    ServerIssue serverIssue = ServerIssue.parseDelimitedFrom(new ByteArrayInputStream(request.execute().output()));
    assertThat(serverIssue.getKey()).isEqualTo("EFGH");
    // Module key of removed file should be returned
    assertThat(serverIssue.getModuleKey()).isEqualTo(MODULE_KEY);
  }

  @Test
  public void fail_without_browse_permission_on_file() throws Exception {
    addBrowsePermissionOnComponent(PROJECT_KEY);

    thrown.expect(ForbiddenException.class);
    tester.newGetRequest("batch", "issues").setParam("key", "Other component key").execute();
  }

  private void indexIssues(IssueDoc... issues) {
    issueIndexer.index(Arrays.asList(issues).iterator());
    for (IssueDoc issue : issues) {
      addIssueAuthorization(issue.projectUuid(), DefaultGroups.ANYONE, null);
    }
  }

  private void addIssueAuthorization(String projectUuid, @Nullable String group, @Nullable String user) {
    issueAuthorizationIndexer.index(newArrayList(new AuthorizationDao.Dto(projectUuid, 1).addGroup(group).addUser(user)));
  }

  private void addBrowsePermissionOnComponent(String componentKey) {
    userSessionRule.addComponentPermission(UserRole.USER, PROJECT_KEY, componentKey);
  }
}
