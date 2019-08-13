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
package org.sonar.server.issue;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.System2;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.rule.DefaultRuleFinder;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.api.rule.RuleStatus.REMOVED;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.issue.IssueTesting.newIssue;

public class WebIssueStorageTest {

  private System2 system2 = new TestSystem2().setNow(2_000_000_000L);

  @org.junit.Rule
  public DbTester db = DbTester.create(system2);

  private DbClient dbClient = db.getDbClient();

  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);

  private IssueIndexer issueIndexer = mock(IssueIndexer.class);
  private WebIssueStorage underTest = new WebIssueStorage(system2, dbClient, new DefaultRuleFinder(db.getDbClient(), defaultOrganizationProvider), issueIndexer);

  @Test
  public void load_component_id_from_db() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organizationDto);
    ComponentDto file = db.components().insertComponent(newFileDto(project));

    long componentId = underTest.component(db.getSession(), new DefaultIssue().setComponentUuid(file.uuid())).getId();

    assertThat(componentId).isEqualTo(file.getId());
  }

  @Test
  public void load_project_id_from_db() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organizationDto);
    ComponentDto file = db.components().insertComponent(newFileDto(project));

    long projectId = underTest.project(db.getSession(), new DefaultIssue().setProjectUuid(project.uuid())).getId();

    assertThat(projectId).isEqualTo(project.getId());
  }

  @Test
  public void insert_new_issues() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    ComponentDto file = db.components().insertComponent(newFileDto(module));

    String issueKey = "ABCDE";
    DefaultIssueComment comment = DefaultIssueComment.create(issueKey, "user_uuid", "the comment");
    // override generated key
    comment.setKey("FGHIJ");

    Date date = DateUtils.parseDateTime("2013-05-18T12:00:00+0000");
    DefaultIssue issue = new DefaultIssue()
      .setKey(issueKey)
      .setType(RuleType.BUG)
      .setNew(true)
      .setRuleKey(rule.getKey())
      .setProjectUuid(project.uuid())
      .setComponentUuid(file.uuid())
      .setLine(5000)
      .setEffort(Duration.create(10L))
      .setResolution("OPEN")
      .setStatus("OPEN")
      .setSeverity("BLOCKER")
      .setAttribute("foo", "bar")
      .addComment(comment)
      .setCreationDate(date)
      .setUpdateDate(date)
      .setCloseDate(date);

    underTest.save(db.getSession(), singletonList(issue));

    assertThat(db.countRowsOfTable("issues")).isEqualTo(1);
    assertThat(db.selectFirst("select * from issues"))
      .containsEntry("PROJECT_UUID", project.uuid())
      .containsEntry("COMPONENT_UUID", file.uuid())
      .containsEntry("KEE", issue.key())
      .containsEntry("RESOLUTION", issue.resolution())
      .containsEntry("STATUS", issue.status())
      .containsEntry("SEVERITY", issue.severity());

    assertThat(db.countRowsOfTable("issue_changes")).isEqualTo(1);
    assertThat(db.selectFirst("select * from issue_changes"))
      .containsEntry("KEE", comment.key())
      .containsEntry("ISSUE_KEY", issue.key())
      .containsEntry("CHANGE_DATA", comment.markdownText())
      .containsEntry("USER_LOGIN", comment.userUuid());
  }

  @Test
  public void update_issues() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    ComponentDto file = db.components().insertComponent(newFileDto(module));

    Date date = DateUtils.parseDateTime("2013-05-18T12:00:00+0000");
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setType(RuleType.BUG)
      .setNew(true)
      .setRuleKey(rule.getKey())
      .setProjectUuid(project.uuid())
      .setComponentUuid(file.uuid())
      .setLine(5000)
      .setEffort(Duration.create(10L))
      .setResolution("OPEN")
      .setStatus("OPEN")
      .setSeverity("BLOCKER")
      .setAttribute("foo", "bar")
      .setCreationDate(date)
      .setUpdateDate(date)
      .setCloseDate(date);

    underTest.save(db.getSession(), singletonList(issue));

    assertThat(db.countRowsOfTable("issues")).isEqualTo(1);
    assertThat(db.countRowsOfTable("issue_changes")).isEqualTo(0);

    DefaultIssue updated = new DefaultIssue()
      .setKey(issue.key())
      .setType(RuleType.VULNERABILITY)
      .setNew(false)
      .setChanged(true)

      // updated fields
      .setLine(issue.getLine() + 10)
      .setProjectUuid("foo")
      .setEffort(Duration.create(issue.effortInMinutes() + 10L))
      .setChecksum("FFFFF")
      .setAuthorLogin("simon")
      .setAssigneeUuid("loic")
      .setFieldChange(IssueChangeContext.createUser(new Date(), "user_uuid"), "severity", "INFO", "BLOCKER")
      .addComment(DefaultIssueComment.create("ABCDE", "user_uuid", "the comment"))
      .setResolution("FIXED")
      .setStatus("RESOLVED")
      .setSeverity("MAJOR")
      .setAttribute("fox", "bax")
      .setCreationDate(DateUtils.addDays(date, 1))
      .setUpdateDate(DateUtils.addDays(date, 1))
      .setCloseDate(DateUtils.addDays(date, 1))

      // unmodifiable fields
      .setRuleKey(rule.getKey())
      .setComponentKey("struts:Action")
      .setProjectKey("struts");

    underTest.save(db.getSession(), singletonList(updated));

    assertThat(db.countRowsOfTable("issues")).isEqualTo(1);
    assertThat(db.selectFirst("select * from issues"))
      .containsEntry("ASSIGNEE", updated.assignee())
      .containsEntry("AUTHOR_LOGIN", updated.authorLogin())
      .containsEntry("CHECKSUM", updated.checksum())
      .containsEntry("COMPONENT_UUID", issue.componentUuid())
      .containsEntry("EFFORT", updated.effortInMinutes())
      .containsEntry("ISSUE_ATTRIBUTES", "fox=bax")
      .containsEntry("ISSUE_TYPE", 3)
      .containsEntry("KEE", issue.key())
      .containsEntry("LINE", (long) updated.line())
      .containsEntry("PROJECT_UUID", updated.projectUuid())
      .containsEntry("RESOLUTION", updated.resolution())
      .containsEntry("STATUS", updated.status())
      .containsEntry("SEVERITY", updated.severity());

    List<Map<String, Object>> rows = db.select("select * from issue_changes order by id");
    assertThat(rows).hasSize(2);
    assertThat(rows.get(0))
      .extracting("CHANGE_DATA", "CHANGE_TYPE", "USER_LOGIN")
      .containsExactlyInAnyOrder("the comment", "comment", "user_uuid");
    assertThat(rows.get(1))
      .extracting("CHANGE_DATA", "CHANGE_TYPE", "USER_LOGIN")
      .containsExactlyInAnyOrder("severity=INFO|BLOCKER", "diff", "user_uuid");
  }

  @Test
  public void rule_id_is_set_on_updated_issue() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    ComponentDto file = db.components().insertComponent(newFileDto(module));
    DefaultIssue issue = newIssue(rule, project, file).toDefaultIssue();

    Collection<IssueDto> results = underTest.save(db.getSession(), singletonList(issue));

    assertThat(results).hasSize(1);
    assertThat(results.iterator().next().getRuleId()).isEqualTo(rule.getId());
  }

  @Test
  public void rule_id_is_not_set_on_updated_issue_when_rule_is_removed() {
    RuleDefinitionDto rule = db.rules().insert(r -> r.setStatus(REMOVED));
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    ComponentDto file = db.components().insertComponent(newFileDto(module));
    DefaultIssue issue = newIssue(rule, project, file).toDefaultIssue();

    Collection<IssueDto> results = underTest.save(db.getSession(), singletonList(issue));

    assertThat(results).hasSize(1);
    assertThat(results.iterator().next().getRuleId()).isNull();
  }

}
