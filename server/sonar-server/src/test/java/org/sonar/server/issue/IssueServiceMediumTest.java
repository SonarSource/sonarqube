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
package org.sonar.server.issue;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.DefaultTransitions;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.ActionPlanDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.UserDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.permission.InternalPermissionService;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.source.db.FileSourceDb;
import org.sonar.server.source.index.FileSourcesUpdaterHelper;
import org.sonar.server.source.index.SourceLineResultSetIterator;
import org.sonar.server.source.index.SourceLineIndexer;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.NewUser;
import org.sonar.server.user.UserService;
import org.sonar.server.user.db.GroupDao;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.Assert.fail;

public class IssueServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db;
  IssueIndex IssueIndex;
  DbSession session;
  IssueService service;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    IssueIndex = tester.get(IssueIndex.class);
    session = db.openSession(false);
    service = tester.get(IssueService.class);
  }

  @After
  public void after() {
    session.close();
  }

  private void index() {
    tester.get(IssueIndexer.class).indexAll();
  }

  @Test
  public void get_by_key() throws Exception {
    RuleDto rule = newRule();
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    IssueDto issue = saveIssue(IssueTesting.newDto(rule, file, project));

    assertThat(service.getByKey(issue.getKey())).isNotNull();
  }

  @Test
  public void can_facet() throws Exception {
    RuleDto rule = newRule();
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);

    saveIssue(IssueTesting.newDto(rule, file, project).setActionPlanKey("P1"));
    saveIssue(IssueTesting.newDto(rule, file, project).setActionPlanKey("P2").setResolution("NONE"));

    SearchResult<IssueDoc> result = service.search(IssueQuery.builder().build(), new SearchOptions());
    assertThat(result.getDocs()).hasSize(2);
    assertThat(result.getFacets().getNames()).isEmpty();

    result = service.search(IssueQuery.builder().build(), new SearchOptions().addFacets("actionPlans", "assignees"));
    assertThat(result.getFacets().getNames()).hasSize(2);
    assertThat(result.getFacets().get("actionPlans")).hasSize(2);
    assertThat(result.getFacets().get("assignees")).hasSize(1);
  }

  @Test
  public void list_status() {
    assertThat(service.listStatus()).containsExactly("OPEN", "CONFIRMED", "REOPENED", "RESOLVED", "CLOSED");
  }

  @Test
  public void list_transitions() {
    RuleDto rule = newRule();
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    IssueDto issue = saveIssue(IssueTesting.newDto(rule, file, project).setStatus(Issue.STATUS_RESOLVED).setResolution(Issue.RESOLUTION_FALSE_POSITIVE));

    List<Transition> result = service.listTransitions(issue.getKey());
    assertThat(result).hasSize(1);
    assertThat(result.get(0).key()).isEqualTo("reopen");
  }

  @Test
  public void do_transition() {
    RuleDto rule = newRule();
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    MockUserSession.set().setLogin("john");

    IssueDto issue = saveIssue(IssueTesting.newDto(rule, file, project).setStatus(Issue.STATUS_OPEN));

    assertThat(IssueIndex.getByKey(issue.getKey()).status()).isEqualTo(Issue.STATUS_OPEN);

    service.doTransition(issue.getKey(), DefaultTransitions.CONFIRM);

    assertThat(IssueIndex.getByKey(issue.getKey()).status()).isEqualTo(Issue.STATUS_CONFIRMED);
  }

  @Test
  public void assign() {
    RuleDto rule = newRule();
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    MockUserSession.set().setLogin("john");

    IssueDto issue = saveIssue(IssueTesting.newDto(rule, file, project));

    UserDto user = new UserDto().setLogin("perceval").setName("Perceval");
    db.userDao().insert(session, user);
    session.commit();
    index();

    assertThat(IssueIndex.getByKey(issue.getKey()).assignee()).isNull();

    service.assign(issue.getKey(), user.getLogin());

    assertThat(IssueIndex.getByKey(issue.getKey()).assignee()).isEqualTo("perceval");
  }

  @Test
  public void unassign() {
    RuleDto rule = newRule();
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    MockUserSession.set().setLogin("john");

    IssueDto issue = saveIssue(IssueTesting.newDto(rule, file, project).setAssignee("perceval"));

    UserDto user = new UserDto().setLogin("perceval").setName("Perceval");
    db.userDao().insert(session, user);
    session.commit();
    index();

    assertThat(IssueIndex.getByKey(issue.getKey()).assignee()).isEqualTo("perceval");

    service.assign(issue.getKey(), "");

    assertThat(IssueIndex.getByKey(issue.getKey()).assignee()).isNull();
  }

  @Test
  public void fail_to_assign_on_unknown_user() {
    RuleDto rule = newRule();
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    MockUserSession.set().setLogin("john");

    IssueDto issue = saveIssue(IssueTesting.newDto(rule, file, project));

    try {
      service.assign(issue.getKey(), "unknown");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class).hasMessage("Unknown user: unknown");
    }
  }

  @Test
  public void plan() {
    RuleDto rule = newRule();
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    MockUserSession.set().setLogin("john").addProjectPermissions(UserRole.USER, project.key());

    IssueDto issue = saveIssue(IssueTesting.newDto(rule, file, project));

    String actionPlanKey = "EFGH";
    db.actionPlanDao().save(new ActionPlanDto().setKey(actionPlanKey).setProjectId(project.getId()));
    session.commit();
    index();

    assertThat(IssueIndex.getByKey(issue.getKey()).actionPlanKey()).isNull();

    service.plan(issue.getKey(), actionPlanKey);

    assertThat(IssueIndex.getByKey(issue.getKey()).actionPlanKey()).isEqualTo(actionPlanKey);
  }

  @Test
  public void un_plan() {
    RuleDto rule = newRule();
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    MockUserSession.set().setLogin("john");

    String actionPlanKey = "EFGH";
    db.actionPlanDao().save(new ActionPlanDto().setKey(actionPlanKey).setProjectId(project.getId()));
    IssueDto issue = saveIssue(IssueTesting.newDto(rule, file, project).setActionPlanKey(actionPlanKey));

    assertThat(IssueIndex.getByKey(issue.getKey()).actionPlanKey()).isEqualTo(actionPlanKey);

    service.plan(issue.getKey(), null);

    assertThat(IssueIndex.getByKey(issue.getKey()).actionPlanKey()).isNull();
  }

  @Test
  public void fail_plan_if_action_plan_not_found() {
    RuleDto rule = newRule();
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    MockUserSession.set().setLogin("john");

    IssueDto issue = saveIssue(IssueTesting.newDto(rule, file, project));
    try {
      service.plan(issue.getKey(), "unknown");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class).hasMessage("Unknown action plan: unknown");
    }
  }

  @Test
  public void set_severity() {
    RuleDto rule = newRule();
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    MockUserSession.set().setLogin("john").addProjectPermissions(UserRole.ISSUE_ADMIN, project.key());

    IssueDto issue = saveIssue(IssueTesting.newDto(rule, file, project).setSeverity(Severity.BLOCKER));

    assertThat(IssueIndex.getByKey(issue.getKey()).severity()).isEqualTo(Severity.BLOCKER);

    service.setSeverity(issue.getKey(), Severity.MINOR);

    assertThat(IssueIndex.getByKey(issue.getKey()).severity()).isEqualTo(Severity.MINOR);
  }

  @Test
  public void create_manual_issue() {
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    MockUserSession.set().setLogin("john").addProjectPermissions(UserRole.USER, project.key());

    RuleDto manualRule = RuleTesting.newManualRule("manualRuleKey");
    tester.get(RuleDao.class).insert(session, manualRule);
    session.commit();

    Issue result = service.createManualIssue(file.key(), manualRule.getKey(), null, "Fix it", Severity.MINOR, 2d);

    IssueDoc manualIssue = IssueIndex.getByKey(result.key());
    assertThat(manualIssue.componentUuid()).isEqualTo(file.uuid());
    assertThat(manualIssue.projectUuid()).isEqualTo(project.uuid());
    assertThat(manualIssue.ruleKey()).isEqualTo(manualRule.getKey());
    assertThat(manualIssue.message()).isEqualTo("Fix it");
    assertThat(manualIssue.line()).isNull();
    assertThat(manualIssue.severity()).isEqualTo(Severity.MINOR);
    assertThat(manualIssue.effortToFix()).isEqualTo(2d);
    assertThat(manualIssue.reporter()).isEqualTo("john");
  }

  @Test
  public void create_manual_issue_on_line() {
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    newSourceLine(file, 1, "arthur");
    createDefaultGroup();
    newUser("arthur");
    MockUserSession.set().setLogin("john").addProjectPermissions(UserRole.USER, project.key());

    RuleDto manualRule = RuleTesting.newManualRule("manualRuleKey");
    tester.get(RuleDao.class).insert(session, manualRule);
    session.commit();

    Issue result = service.createManualIssue(file.key(), manualRule.getKey(), 1, "Fix it", Severity.MINOR, 2d);

    IssueDoc manualIssue = IssueIndex.getByKey(result.key());
    assertThat(manualIssue.componentUuid()).isEqualTo(file.uuid());
    assertThat(manualIssue.projectUuid()).isEqualTo(project.uuid());
    assertThat(manualIssue.ruleKey()).isEqualTo(manualRule.getKey());
    assertThat(manualIssue.message()).isEqualTo("Fix it");
    assertThat(manualIssue.line()).isEqualTo(1);
    assertThat(manualIssue.severity()).isEqualTo(Severity.MINOR);
    assertThat(manualIssue.effortToFix()).isEqualTo(2d);
    assertThat(manualIssue.reporter()).isEqualTo("john");
    assertThat(manualIssue.assignee()).isEqualTo("arthur");
  }

  @Test
  public void create_manual_issue_with_major_severity_when_no_severity() {
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    MockUserSession.set().setLogin("john").addProjectPermissions(UserRole.USER, project.key());

    RuleDto manualRule = RuleTesting.newManualRule("manualRuleKey");
    tester.get(RuleDao.class).insert(session, manualRule);
    session.commit();

    Issue result = service.createManualIssue(file.key(), manualRule.getKey(), null, "Fix it", null, 2d);

    Issue manualIssue = IssueIndex.getByKey(result.key());
    assertThat(manualIssue.severity()).isEqualTo(Severity.MAJOR);
  }

  @Test
  public void create_manual_issue_with_rule_name_when_no_message() {
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    MockUserSession.set().setLogin("john").addProjectPermissions(UserRole.USER, project.key());

    RuleDto manualRule = RuleTesting.newManualRule("manualRuleKey").setName("Manual rule name");
    tester.get(RuleDao.class).insert(session, manualRule);
    session.commit();

    Issue result = service.createManualIssue(file.key(), manualRule.getKey(), null, null, null, 2d);

    Issue manualIssue = IssueIndex.getByKey(result.key());
    assertThat(manualIssue.message()).isEqualTo("Manual rule name");
  }

  @Test
  public void create_manual_issue_without_assignee_when_scm_author_do_not_match_user() {
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    // Unknown SCM account
    newSourceLine(file, 1, "unknown");
    createDefaultGroup();
    newUser("arthur");
    MockUserSession.set().setLogin("john").addProjectPermissions(UserRole.USER, project.key());

    RuleDto manualRule = RuleTesting.newManualRule("manualRuleKey");
    tester.get(RuleDao.class).insert(session, manualRule);
    session.commit();

    Issue result = service.createManualIssue(file.key(), manualRule.getKey(), 1, "Fix it", Severity.MINOR, 2d);

    IssueDoc manualIssue = IssueIndex.getByKey(result.key());
    assertThat(manualIssue.assignee()).isNull();
  }

  @Test
  public void create_manual_issue_without_assignee_when_no_scm_author_on_line() {
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    // No author on line 1
    newSourceLine(file, 1, "");
    MockUserSession.set().setLogin("john").addProjectPermissions(UserRole.USER, project.key());

    RuleDto manualRule = RuleTesting.newManualRule("manualRuleKey");
    tester.get(RuleDao.class).insert(session, manualRule);
    session.commit();

    Issue result = service.createManualIssue(file.key(), manualRule.getKey(), 1, "Fix it", Severity.MINOR, 2d);

    IssueDoc manualIssue = IssueIndex.getByKey(result.key());
    assertThat(manualIssue.assignee()).isNull();
  }

  @Test
  public void fail_create_manual_issue_on_not_manual_rule() {
    RuleDto rule = newRule();
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    MockUserSession.set().setLogin("john").addProjectPermissions(UserRole.USER, project.key());

    try {
      service.createManualIssue(file.key(), rule.getKey(), null, "Fix it", null, 2d);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Issues can be created only on rules marked as 'manual': xoo:x1");
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_create_manual_issue_if_rule_does_not_exists() {
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    MockUserSession.set().setLogin("john").addProjectPermissions(UserRole.USER, project.key());

    service.createManualIssue(file.key(), RuleKey.of("rule", "unknown"), 10, "Fix it", null, 2d);
  }

  @Test(expected = ForbiddenException.class)
  public void fail_create_manual_issue_if_not_having_required_role() {
    RuleDto rule = newRule();
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);

    // User has not the 'user' role on the project
    MockUserSession.set().setLogin("john").addProjectPermissions(UserRole.CODEVIEWER, project.key());

    RuleDto manualRule = RuleTesting.newManualRule("manualRuleKey");
    tester.get(RuleDao.class).insert(session, manualRule);
    session.commit();

    service.createManualIssue(file.key(), rule.getKey(), 10, "Fix it", null, 2d);
  }

  @Test
  public void search_issues() {
    RuleDto rule = newRule();
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    saveIssue(IssueTesting.newDto(rule, file, project));

    List<IssueDoc> result = service.search(IssueQuery.builder().build(), new SearchOptions()).getDocs();
    assertThat(result).hasSize(1);
  }

  @Test
  public void list_tags() {
    RuleDto rule = newRule();
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    saveIssue(IssueTesting.newDto(rule, file, project).setTags(ImmutableSet.of("convention", "java8", "bug")));
    saveIssue(IssueTesting.newDto(rule, file, project).setTags(ImmutableSet.of("convention", "bug")));
    saveIssue(IssueTesting.newDto(rule, file, project));
    saveIssue(IssueTesting.newDto(rule, file, project).setTags(ImmutableSet.of("convention")));

    assertThat(service.listTags(null, 5)).containsOnly("convention", "java8", "bug" /* from issues */, "systag1", "systag2" /* from rules */);
    assertThat(service.listTags(null, 2)).containsOnly("bug", "convention");
    assertThat(service.listTags("vent", 5)).containsOnly("convention");
    assertThat(service.listTags("sys", 5)).containsOnly("systag1", "systag2");
    assertThat(service.listTags(null, 1)).containsOnly("bug");
    assertThat(service.listTags(null, Integer.MAX_VALUE)).containsOnly("convention", "java8", "bug", "systag1", "systag2", "tag1", "tag2");
  }

  @Test
  public void set_tags() {
    RuleDto rule = newRule();
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    MockUserSession.set().setLogin("john");

    IssueDto issue = saveIssue(IssueTesting.newDto(rule, file, project));

    assertThat(IssueIndex.getByKey(issue.getKey()).tags()).isEmpty();

    // Tags are lowercased
    service.setTags(issue.getKey(), ImmutableSet.of("bug", "Convention"));
    assertThat(IssueIndex.getByKey(issue.getKey()).tags()).containsOnly("bug", "convention");

    // nulls and empty tags are ignored
    service.setTags(issue.getKey(), Sets.newHashSet("security", null, "", "convention"));
    assertThat(IssueIndex.getByKey(issue.getKey()).tags()).containsOnly("security", "convention");

    // tag validation
    try {
      service.setTags(issue.getKey(), ImmutableSet.of("pol op"));
    } catch (Exception exception) {
      assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }
    assertThat(IssueIndex.getByKey(issue.getKey()).tags()).containsOnly("security", "convention");

    // unchanged tags
    service.setTags(issue.getKey(), ImmutableSet.of("convention", "security"));
    assertThat(IssueIndex.getByKey(issue.getKey()).tags()).containsOnly("security", "convention");

    service.setTags(issue.getKey(), ImmutableSet.<String>of());
    assertThat(IssueIndex.getByKey(issue.getKey()).tags()).isEmpty();
  }

  @Test
  public void list_component_tags() {
    RuleDto rule = newRule();
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    saveIssue(IssueTesting.newDto(rule, file, project).setTags(ImmutableSet.of("convention", "java8", "bug")));
    saveIssue(IssueTesting.newDto(rule, file, project).setTags(ImmutableSet.of("convention", "bug")));
    saveIssue(IssueTesting.newDto(rule, file, project));
    saveIssue(IssueTesting.newDto(rule, file, project).setTags(ImmutableSet.of("convention", "java8", "bug")).setResolution(Issue.RESOLUTION_FIXED));
    saveIssue(IssueTesting.newDto(rule, file, project).setTags(ImmutableSet.of("convention")));

    assertThat(service.listTagsForComponent(projectQuery(project.uuid()), 5)).containsOnly(entry("convention", 3L), entry("bug", 2L), entry("java8", 1L));
    assertThat(service.listTagsForComponent(projectQuery(project.uuid()), 2)).contains(entry("convention", 3L), entry("bug", 2L)).doesNotContainEntry("java8", 1L);
    assertThat(service.listTagsForComponent(projectQuery("other"), 10)).isEmpty();
  }

  private IssueQuery projectQuery(String projectUuid) {
    return IssueQuery.builder().projectUuids(Arrays.asList(projectUuid)).resolved(false).build();
  }

  @Test
  public void list_authors() {
    RuleDto rule = newRule();
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    saveIssue(IssueTesting.newDto(rule, file, project).setAuthorLogin("luke.skywalker"));
    saveIssue(IssueTesting.newDto(rule, file, project).setAuthorLogin("luke@skywalker.name"));
    saveIssue(IssueTesting.newDto(rule, file, project));
    saveIssue(IssueTesting.newDto(rule, file, project).setAuthorLogin("anakin@skywalker.name"));

    assertThat(service.listAuthors(null, 5)).containsExactly("anakin@skywalker.name", "luke.skywalker", "luke@skywalker.name");
    assertThat(service.listAuthors(null, 2)).containsExactly("anakin@skywalker.name", "luke.skywalker");
    assertThat(service.listAuthors("uke", 5)).containsExactly("luke.skywalker", "luke@skywalker.name");
    assertThat(service.listAuthors(null, 1)).containsExactly("anakin@skywalker.name");
    assertThat(service.listAuthors(null, Integer.MAX_VALUE)).containsExactly("anakin@skywalker.name", "luke.skywalker", "luke@skywalker.name");
  }

  private RuleDto newRule() {
    RuleDto rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(session, rule);
    session.commit();
    return rule;
  }

  private ComponentDto newProject() {
    ComponentDto project = ComponentTesting.newProjectDto();
    tester.get(ComponentDao.class).insert(session, project);

    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.USER, project.key()).setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    session.commit();

    // project can be seen by group "anyone"
    tester.get(InternalPermissionService.class).addPermission(new PermissionChange().setComponentKey(project.getKey()).setGroup(DefaultGroups.ANYONE).setPermission(UserRole.USER));
    MockUserSession.set();

    return project;
  }

  private ComponentDto newFile(ComponentDto project) {
    ComponentDto file = ComponentTesting.newFileDto(project);
    tester.get(ComponentDao.class).insert(session, file);
    session.commit();
    return file;
  }

  private IssueDto saveIssue(IssueDto issue) {
    tester.get(IssueDao.class).insert(session, issue);
    session.commit();
    tester.get(IssueIndexer.class).indexAll();
    return issue;
  }

  private void newSourceLine(ComponentDto file, int line, String scmAuthor) {
    FileSourceDb.Data.Builder dataBuilder = FileSourceDb.Data.newBuilder();
    dataBuilder.addLinesBuilder()
      .setLine(line)
      .setScmAuthor(scmAuthor)
      .build();
    FileSourcesUpdaterHelper.Row row = SourceLineResultSetIterator.toRow(file.projectUuid(), file.uuid(), new Date(), dataBuilder.build());
    tester.get(SourceLineIndexer.class).index(Iterators.singletonIterator(row));
  }

  private void newUser(String login) {
    MockUserSession.set().setLogin("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    tester.get(UserService.class).create(NewUser.create().setLogin(login).setName(login).setPassword("test").setPasswordConfirmation("test"));
    MockUserSession.set();
  }

  private void createDefaultGroup() {
    tester.get(Settings.class).setProperty(CoreProperties.CORE_DEFAULT_GROUP, "sonar-users");
    tester.get(GroupDao.class).insert(session, new GroupDto().setName("sonar-users").setDescription("Sonar Users"));
    session.commit();
  }
}
