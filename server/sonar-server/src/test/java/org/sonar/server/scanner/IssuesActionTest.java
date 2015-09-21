/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.scanner;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.Constants.Severity;
import org.sonar.batch.protocol.input.BatchInput.ServerIssue;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.issue.index.IssueAuthorizationDao;
import org.sonar.server.issue.index.IssueAuthorizationIndexer;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;
import org.sonar.test.DbTests;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Category(DbTests.class)
public class IssuesActionTest {

  private final static String PROJECT_KEY = "struts";
  private final static String MODULE_KEY = "struts-core";
  private final static String FILE_KEY = "Action.java";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @ClassRule
  public static EsTester es = new EsTester().addDefinitions(new IssueIndexDefinition(new Settings()));

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  IssueIndex issueIndex;
  IssueIndexer issueIndexer;
  IssueAuthorizationIndexer issueAuthorizationIndexer;

  WsTester tester;

  IssuesAction issuesAction;

  @Before
  public void before() {
    db.truncateTables();
    es.truncateIndices();

    issueIndex = new IssueIndex(es.client(), System2.INSTANCE, userSessionRule);
    issueIndexer = new IssueIndexer(null, es.client());
    issueAuthorizationIndexer = new IssueAuthorizationIndexer(null, es.client());
    issuesAction = new IssuesAction(db.getDbClient(), issueIndex, userSessionRule, new ComponentFinder(db.getDbClient()));

    tester = new WsTester(new ScannerWs(new ScannerIndex(mock(Server.class)), issuesAction));
  }

  @Test
  public void return_minimal_fields() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", project).setKey(MODULE_KEY);
    ComponentDto file = ComponentTesting.newFileDto(module, "CDEF").setKey(FILE_KEY).setPath(null);
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

    userSessionRule.login("henry").setGlobalPermissions(GlobalPermissions.PREVIEW_EXECUTION);

    WsTester.TestRequest request = tester.newGetRequest("scanner", "issues").setParam("key", PROJECT_KEY);

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
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", project).setKey(MODULE_KEY);
    ComponentDto file = ComponentTesting.newFileDto(module, "CDEF").setKey(FILE_KEY).setPath("src/org/struts/Action.java");
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

    userSessionRule.login("henry").setGlobalPermissions(GlobalPermissions.PREVIEW_EXECUTION);

    WsTester.TestRequest request = tester.newGetRequest("scanner", "issues").setParam("key", PROJECT_KEY);

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
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", project).setKey(MODULE_KEY);
    ComponentDto file = ComponentTesting.newFileDto(module, "CDEF").setKey(FILE_KEY).setPath("src/org/struts/Action.java");
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

    userSessionRule.login("henry").setGlobalPermissions(GlobalPermissions.PREVIEW_EXECUTION);

    WsTester.TestRequest request = tester.newGetRequest("scanner", "issues").setParam("key", MODULE_KEY);
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
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", project).setKey(MODULE_KEY);
    ComponentDto file = ComponentTesting.newFileDto(module, "CDEF").setKey(FILE_KEY).setPath("src/org/struts/Action.java");
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

    userSessionRule.login("henry").setGlobalPermissions(GlobalPermissions.PREVIEW_EXECUTION);

    WsTester.TestRequest request = tester.newGetRequest("scanner", "issues").setParam("key", FILE_KEY);
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
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", project).setKey(MODULE_KEY);
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

    userSessionRule.login("henry").setGlobalPermissions(GlobalPermissions.PREVIEW_EXECUTION);

    WsTester.TestRequest request = tester.newGetRequest("scanner", "issues").setParam("key", MODULE_KEY);
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
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY);
    // File and module are removed
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", project).setKey(MODULE_KEY).setEnabled(false);
    ComponentDto file = ComponentTesting.newFileDto(module, "CDEF").setKey(FILE_KEY).setPath("src/org/struts/Action.java").setEnabled(false);
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

    userSessionRule.login("henry").setGlobalPermissions(GlobalPermissions.PREVIEW_EXECUTION);

    WsTester.TestRequest request = tester.newGetRequest("scanner", "issues").setParam("key", PROJECT_KEY);
    ServerIssue serverIssue = ServerIssue.parseDelimitedFrom(new ByteArrayInputStream(request.execute().output()));
    assertThat(serverIssue.getKey()).isEqualTo("EFGH");
    // Module key of removed file should be returned
    assertThat(serverIssue.getModuleKey()).isEqualTo(MODULE_KEY);
  }

  @Test(expected = ForbiddenException.class)
  public void fail_without_preview_permission() throws Exception {
    userSessionRule.login("henry").setGlobalPermissions(GlobalPermissions.PROVISIONING);

    WsTester.TestRequest request = tester.newGetRequest("scanner", "issues").setParam("key", PROJECT_KEY);
    request.execute();
  }

  private void indexIssues(IssueDoc... issues) {
    issueIndexer.index(Arrays.asList(issues).iterator());
    for (IssueDoc issue : issues) {
      addIssueAuthorization(issue.projectUuid(), DefaultGroups.ANYONE, null);
    }
  }

  private void addIssueAuthorization(String projectUuid, @Nullable String group, @Nullable String user) {
    issueAuthorizationIndexer.index(newArrayList(new IssueAuthorizationDao.Dto(projectUuid, 1).addGroup(group).addUser(user)));
  }
}
