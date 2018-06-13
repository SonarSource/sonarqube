/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueMapper;
import org.sonar.db.rule.RuleDefinitionDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;

public class IssueStorageTest {

  private static final System2 system2 = System2.INSTANCE;

  @org.junit.Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();

  @Test
  public void batch_insert_new_issues() {
    FakeBatchSaver saver = new FakeBatchSaver(dbClient, new FakeRuleFinder());

    DefaultIssueComment comment = DefaultIssueComment.create("ABCDE", "user_uuid", "the comment");
    // override generated key
    comment.setKey("FGHIJ");

    Date date = DateUtils.parseDateTime("2013-05-18T12:00:00+0000");
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setType(RuleType.BUG)
      .setNew(true)

      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
      .setLine(5000)
      .setEffort(Duration.create(10L))
      .setResolution("OPEN")
      .setStatus("OPEN")
      .setSeverity("BLOCKER")
      .setAttribute("foo", "bar")
      .addComment(comment)
      .setCreationDate(date)
      .setUpdateDate(date)
      .setCloseDate(date)

      .setComponentUuid("uuid-100")
      .setProjectUuid("uuid-10")
      .setComponentKey("struts:Action");

    assertThat(db.countRowsOfTable("issues")).isEqualTo(0);
    assertThat(db.countRowsOfTable("issue_changes")).isEqualTo(0);

    saver.save(issue);

    assertThat(db.countRowsOfTable("issues")).isEqualTo(1);
    assertThat(db.countRowsOfTable("issue_changes")).isEqualTo(1);
  }

  @Test
  public void batch_insert_new_issues_with_session() {
    FakeBatchSaver saver = new FakeBatchSaver(dbClient, new FakeRuleFinder());

    DefaultIssueComment comment = DefaultIssueComment.create("ABCDE", "user_uuid", "the comment");
    // override generated key
    comment.setKey("FGHIJ");

    Date date = DateUtils.parseDateTime("2013-05-18T12:00:00+0000");
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setType(RuleType.BUG)
      .setNew(true)

      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
      .setLine(5000)
      .setEffort(Duration.create(10L))
      .setResolution("OPEN")
      .setStatus("OPEN")
      .setSeverity("BLOCKER")
      .setAttribute("foo", "bar")
      .addComment(comment)
      .setCreationDate(date)
      .setUpdateDate(date)
      .setCloseDate(date)

      .setComponentUuid("uuid-100")
      .setProjectUuid("uuid-10")
      .setComponentKey("struts:Action");

    assertThat(db.countRowsOfTable("issues")).isEqualTo(0);
    assertThat(db.countRowsOfTable("issue_changes")).isEqualTo(0);

    saver.save(db.getSession(), issue);
    db.getSession().commit();

    assertThat(db.countRowsOfTable("issues")).isEqualTo(1);
    assertThat(db.countRowsOfTable("issue_changes")).isEqualTo(1);
  }

  @Test
  public void server_insert_new_issues_with_session() {
    ComponentDto project = new ComponentDto().setId(10L).setUuid("uuid-10");
    ComponentDto component = new ComponentDto().setId(100L).setUuid("uuid-100");
    FakeServerSaver saver = new FakeServerSaver(dbClient, new FakeRuleFinder(), component, project);

    DefaultIssueComment comment = DefaultIssueComment.create("ABCDE", "user_uuid", "the comment");
    // override generated key
    comment.setKey("FGHIJ");

    Date date = DateUtils.parseDateTime("2013-05-18T12:00:00+0000");
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setType(RuleType.BUG)
      .setNew(true)

      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
      .setLine(5000)
      .setEffort(Duration.create(10L))
      .setResolution("OPEN")
      .setStatus("OPEN")
      .setSeverity("BLOCKER")
      .setAttribute("foo", "bar")
      .addComment(comment)
      .setCreationDate(date)
      .setUpdateDate(date)
      .setCloseDate(date)

      .setComponentKey("struts:Action")
      .setComponentUuid("component-uuid")
      .setProjectUuid("project-uuid");

    assertThat(db.countRowsOfTable("issues")).isEqualTo(0);
    assertThat(db.countRowsOfTable("issue_changes")).isEqualTo(0);

    saver.save(db.getSession(), issue);
    db.getSession().commit();

    assertThat(db.countRowsOfTable("issues")).isEqualTo(1);
    assertThat(db.countRowsOfTable("issue_changes")).isEqualTo(1);
  }

  @Test
  public void batch_update_issues() {
    FakeBatchSaver saver = new FakeBatchSaver(dbClient, new FakeRuleFinder());

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
      .addComment(DefaultIssueComment.create("ABCDE", "user_uuid", "first comment"))
      .setResolution("OPEN")
      .setStatus("OPEN")
      .setSeverity("BLOCKER")
      .setAttribute("foo", "bar")
      .setCreationDate(date)
      .setUpdateDate(date)
      .setCloseDate(date);

    saver.save(issue);

    assertThat(db.countRowsOfTable("issues")).isEqualTo(1);
    assertThat(db.countRowsOfTable("issue_changes")).isEqualTo(1);

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
      .setRuleKey(RuleKey.of("xxx", "unknown"))
      .setComponentKey("struts:Action")
      .setProjectKey("struts");

    saver.save(updated);

    assertThat(db.countRowsOfTable("issues")).isEqualTo(1);
    assertThat(db.selectFirst("select * from issues"))
      .containsEntry("ASSIGNEE", updated.assignee())
      .containsEntry("AUTHOR_LOGIN", updated.authorLogin())
      .containsEntry("CHECKSUM", updated.checksum())
      .containsEntry("COMPONENT_UUID", issue.componentUuid())
      .containsEntry("EFFORT", updated.effortInMinutes())
      .containsEntry("ISSUE_ATTRIBUTES", "fox=bax")
      .containsEntry("ISSUE_TYPE", (byte) 3)
      .containsEntry("KEE", issue.key())
      .containsEntry("LINE", (long) updated.line())
      .containsEntry("PROJECT_UUID", updated.projectUuid())
      .containsEntry("RESOLUTION", updated.resolution())
      .containsEntry("STATUS", updated.status())
      .containsEntry("SEVERITY", updated.severity());

    List<Map<String, Object>> rows = db.select("select * from issue_changes order by id");
    assertThat(rows).hasSize(3);
    assertThat(rows.get(0))
      .extracting("CHANGE_DATA", "CHANGE_TYPE", "USER_LOGIN")
      .containsExactlyInAnyOrder("first comment", "comment", "user_uuid");
    assertThat(rows.get(1))
      .extracting("CHANGE_DATA", "CHANGE_TYPE", "USER_LOGIN")
      .containsExactlyInAnyOrder("the comment", "comment", "user_uuid");
    assertThat(rows.get(2))
      .extracting("CHANGE_DATA", "CHANGE_TYPE", "USER_LOGIN")
      .containsExactlyInAnyOrder("severity=INFO|BLOCKER", "diff", "user_uuid");
  }

  @Test
  public void server_update_issues() {
    ComponentDto project = new ComponentDto().setId(10L).setUuid("whatever-uuid");
    ComponentDto component = new ComponentDto().setId(100L).setUuid("whatever-uuid-2");
    FakeServerSaver saver = new FakeServerSaver(dbClient, new FakeRuleFinder(), component, project);

    RuleDefinitionDto rule = db.rules().insert();

    Date date = DateUtils.parseDateTime("2013-05-18T12:00:00+0000");
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setType(RuleType.BUG)
      .setNew(true)
      .setRuleKey(rule.getKey())
      .setProjectUuid(project.uuid())
      .setComponentUuid(component.uuid())
      .setLine(5000)
      .setEffort(Duration.create(10L))
      .addComment(DefaultIssueComment.create("ABCDE", "user_uuid", "first comment"))
      .setResolution("OPEN")
      .setStatus("OPEN")
      .setSeverity("BLOCKER")
      .setAttribute("foo", "bar")
      .setCreationDate(date)
      .setUpdateDate(date)
      .setCloseDate(date);

    saver.save(issue);

    assertThat(db.countRowsOfTable("issues")).isEqualTo(1);
    assertThat(db.countRowsOfTable("issue_changes")).isEqualTo(1);

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
      .setRuleKey(RuleKey.of("xxx", "unknown"))
      .setComponentKey("struts:Action")
      .setProjectKey("struts");

    saver.save(updated);

    assertThat(db.countRowsOfTable("issues")).isEqualTo(1);
    assertThat(db.selectFirst("select * from issues"))
      .containsEntry("ASSIGNEE", updated.assignee())
      .containsEntry("AUTHOR_LOGIN", updated.authorLogin())
      .containsEntry("CHECKSUM", updated.checksum())
      .containsEntry("COMPONENT_UUID", issue.componentUuid())
      .containsEntry("EFFORT", updated.effortInMinutes())
      .containsEntry("ISSUE_ATTRIBUTES", "fox=bax")
      .containsEntry("ISSUE_TYPE", (byte) 3)
      .containsEntry("KEE", issue.key())
      .containsEntry("LINE", (long) updated.line())
      .containsEntry("PROJECT_UUID", updated.projectUuid())
      .containsEntry("RESOLUTION", updated.resolution())
      .containsEntry("STATUS", updated.status())
      .containsEntry("SEVERITY", updated.severity());

    List<Map<String, Object>> rows = db.select("select * from issue_changes order by id");
    assertThat(rows).hasSize(3);
    assertThat(rows.get(0))
      .extracting("CHANGE_DATA", "CHANGE_TYPE", "USER_LOGIN")
      .containsExactlyInAnyOrder("first comment", "comment", "user_uuid");
    assertThat(rows.get(1))
      .extracting("CHANGE_DATA", "CHANGE_TYPE", "USER_LOGIN")
      .containsExactlyInAnyOrder("the comment", "comment", "user_uuid");
    assertThat(rows.get(2))
      .extracting("CHANGE_DATA", "CHANGE_TYPE", "USER_LOGIN")
      .containsExactlyInAnyOrder("severity=INFO|BLOCKER", "diff", "user_uuid");
  }

  static class FakeBatchSaver extends IssueStorage {

    protected FakeBatchSaver(DbClient dbClient, RuleFinder ruleFinder) {
      super(system2, dbClient, ruleFinder);
    }

    @Override
    protected IssueDto doInsert(DbSession session, long now, DefaultIssue issue) {
      int ruleId = rule(issue).getId();
      IssueDto dto = IssueDto.toDtoForComputationInsert(issue, ruleId, now);

      session.getMapper(IssueMapper.class).insert(dto);
      return dto;
    }

    @Override
    protected IssueDto doUpdate(DbSession session, long now, DefaultIssue issue) {
      IssueDto dto = IssueDto.toDtoForUpdate(issue, now);
      session.getMapper(IssueMapper.class).update(dto);
      return dto;
    }
  }

  static class FakeServerSaver extends IssueStorage {

    private final ComponentDto component;
    private final ComponentDto project;

    protected FakeServerSaver(DbClient dbClient, RuleFinder ruleFinder, ComponentDto component, ComponentDto project) {
      super(system2, dbClient, ruleFinder);
      this.component = component;
      this.project = project;
    }

    @Override
    protected IssueDto doInsert(DbSession session, long now, DefaultIssue issue) {
      int ruleId = rule(issue).getId();
      IssueDto dto = IssueDto.toDtoForServerInsert(issue, component, project, ruleId, now);

      session.getMapper(IssueMapper.class).insert(dto);
      return dto;
    }

    @Override
    protected IssueDto doUpdate(DbSession session, long now, DefaultIssue issue) {
      IssueDto dto = IssueDto.toDtoForUpdate(issue, now);
      session.getMapper(IssueMapper.class).update(dto);
      return dto;
    }
  }

  static class FakeRuleFinder implements RuleFinder {

    @Override
    public Rule findById(int ruleId) {
      return null;
    }

    @Override
    public Rule findByKey(String repositoryKey, String key) {
      return null;
    }

    @Override
    public Rule findByKey(RuleKey key) {
      Rule rule = new Rule().setRepositoryKey(key.repository()).setKey(key.rule());
      rule.setId(200);
      return rule;
    }

    @Override
    public Rule find(RuleQuery query) {
      return null;
    }

    @Override
    public Collection<Rule> findAll(RuleQuery query) {
      return null;
    }
  }
}
