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
package org.sonar.server.issue;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.issue.IssueDao;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueTesting;
import org.sonar.db.organization.OrganizationDao;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.permission.GroupPermissionChange;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.permission.PermissionUpdater;
import org.sonar.server.permission.ProjectId;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.ws.GroupIdOrAnyone;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.Assert.fail;

public class IssueServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withStartupTasks().withEsIndexes();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  private DbClient db;
  private IssueIndex issueIndex;
  private DbSession session;
  private IssueService service;
  private RuleIndexer ruleIndexer;

  @Before
  public void setUp() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    issueIndex = tester.get(IssueIndex.class);
    session = db.openSession(false);
    service = tester.get(IssueService.class);
    ruleIndexer = tester.get(RuleIndexer.class);
  }

  @After
  public void after() {
    session.close();
  }

  private void index() {
    tester.get(IssueIndexer.class).indexAll();
  }

  @Test
  public void assign() {
    RuleDto rule = newRule();
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    userSessionRule.login("john").addProjectUuidPermissions(UserRole.USER, project.uuid());

    IssueDto issue = saveIssue(IssueTesting.newDto(rule, file, project));

    UserDto user = new UserDto().setLogin("perceval").setName("Perceval");
    db.userDao().insert(session, user);
    session.commit();
    index();

    assertThat(issueIndex.getByKey(issue.getKey()).assignee()).isNull();

    service.assign(issue.getKey(), user.getLogin());

    assertThat(issueIndex.getByKey(issue.getKey()).assignee()).isEqualTo("perceval");
  }

  @Test
  public void unassign() {
    RuleDto rule = newRule();
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    userSessionRule.login("john").addProjectUuidPermissions(UserRole.USER, project.uuid());

    IssueDto issue = saveIssue(IssueTesting.newDto(rule, file, project).setAssignee("perceval"));

    UserDto user = new UserDto().setLogin("perceval").setName("Perceval");
    db.userDao().insert(session, user);
    session.commit();
    index();

    assertThat(issueIndex.getByKey(issue.getKey()).assignee()).isEqualTo("perceval");

    service.assign(issue.getKey(), "");

    assertThat(issueIndex.getByKey(issue.getKey()).assignee()).isNull();
  }

  @Test
  public void fail_to_assign_on_unknown_user() {
    RuleDto rule = newRule();
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    userSessionRule.login("john").addProjectUuidPermissions(UserRole.USER, project.uuid());

    IssueDto issue = saveIssue(IssueTesting.newDto(rule, file, project));

    try {
      service.assign(issue.getKey(), "unknown");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("Unknown user: unknown");
    }
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
    assertThat(service.listTags("invalidRegexp[", 5)).isEmpty();
  }

  @Test
  public void set_tags() {
    RuleDto rule = newRule();
    ComponentDto project = newProject();
    ComponentDto file = newFile(project);
    userSessionRule.login("john").addProjectUuidPermissions(UserRole.USER, project.uuid());

    IssueDto issue = saveIssue(IssueTesting.newDto(rule, file, project));

    assertThat(issueIndex.getByKey(issue.getKey()).tags()).isEmpty();

    // Tags are lowercased
    service.setTags(issue.getKey(), ImmutableSet.of("bug", "Convention"));
    assertThat(issueIndex.getByKey(issue.getKey()).tags()).containsOnly("bug", "convention");

    // nulls and empty tags are ignored
    service.setTags(issue.getKey(), Sets.newHashSet("security", null, "", "convention"));
    assertThat(issueIndex.getByKey(issue.getKey()).tags()).containsOnly("security", "convention");

    // tag validation
    try {
      service.setTags(issue.getKey(), ImmutableSet.of("pol op"));
    } catch (Exception exception) {
      assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }
    assertThat(issueIndex.getByKey(issue.getKey()).tags()).containsOnly("security", "convention");

    // unchanged tags
    service.setTags(issue.getKey(), ImmutableSet.of("convention", "security"));
    assertThat(issueIndex.getByKey(issue.getKey()).tags()).containsOnly("security", "convention");

    service.setTags(issue.getKey(), ImmutableSet.<String>of());
    assertThat(issueIndex.getByKey(issue.getKey()).tags()).isEmpty();
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
    return IssueQuery.builder(userSessionRule).projectUuids(asList(projectUuid)).resolved(false).build();
  }

  @Test
  public void test_listAuthors() {
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

  @Test
  public void listAuthors_escapes_regexp_special_characters() {
    saveIssue(IssueTesting.newDto(newRule(), newFile(newProject()), newProject()).setAuthorLogin("name++"));

    assertThat(service.listAuthors("invalidRegexp[", 5)).isEmpty();
    assertThat(service.listAuthors("nam+", 5)).isEmpty();
    assertThat(service.listAuthors("name+", 5)).containsExactly("name++");
    assertThat(service.listAuthors(".*", 5)).isEmpty();
  }

  private RuleDto newRule() {
    return newRule(RuleTesting.newXooX1());
  }

  private RuleDto newRule(RuleDto rule) {
    tester.get(RuleDao.class).insert(session, rule);
    session.commit();
    ruleIndexer.index();
    return rule;
  }

  private ComponentDto newProject() {
    OrganizationDto organization = OrganizationTesting.newOrganizationDto();
    tester.get(OrganizationDao.class).insert(session, organization);
    ComponentDto project = ComponentTesting.newProjectDto(organization);
    tester.get(ComponentDao.class).insert(session, project);

    userSessionRule.login("admin").addProjectPermissions(UserRole.USER, project.key()).setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    session.commit();

    // project can be seen by group "anyone"
    // TODO correctly feed default organization. Not a problem as long as issues search does not support "anyone"
    // for each organization
    GroupPermissionChange permissionChange = new GroupPermissionChange(PermissionChange.Operation.ADD, UserRole.USER, new ProjectId(project), GroupIdOrAnyone.forAnyone(organization.getUuid()));
    tester.get(PermissionUpdater.class).apply(session, asList(permissionChange));
    userSessionRule.login();

    return project;
  }

  private ComponentDto newFile(ComponentDto project) {
    ComponentDto file = ComponentTesting.newFileDto(project, null);
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

}
