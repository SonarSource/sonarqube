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
import org.junit.Before;
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
import org.sonar.db.DbTester;
import org.sonar.server.issue.index.IssueIndexer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerIssueStorageTest {

  private System2 system2 = mock(System2.class);

  @org.junit.Rule
  public DbTester dbTester = DbTester.create(system2);

  private DbClient dbClient = dbTester.getDbClient();

  private ServerIssueStorage underTest = new ServerIssueStorage(system2, new FakeRuleFinder(), dbClient, mock(IssueIndexer.class));

  @Before
  public void setupDbClient() {
    when(system2.now()).thenReturn(2000000000L);
  }

  @Test
  public void load_component_id_from_db() {
    dbTester.prepareDbUnit(getClass(), "load_component_id_from_db.xml");

    long componentId = underTest.component(dbTester.getSession(), new DefaultIssue().setComponentUuid("BCDE")).getId();

    assertThat(componentId).isEqualTo(100);
  }

  @Test
  public void load_project_id_from_db() {
    dbTester.prepareDbUnit(getClass(), "load_project_id_from_db.xml");

    long projectId = underTest.project(dbTester.getSession(), new DefaultIssue().setProjectUuid("ABCD")).getId();

    assertThat(projectId).isEqualTo(1);
  }

  @Test
  public void should_insert_new_issues() {
    dbTester.prepareDbUnit(getClass(), "should_insert_new_issues.xml");

    DefaultIssueComment comment = DefaultIssueComment.create("ABCDE", "emmerik", "the comment");
    // override generated key
    comment.setKey("FGHIJ");

    Date date = DateUtils.parseDateTime("2013-05-18T12:00:00+0000");
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setType(RuleType.BUG)
      .setNew(true)

      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
      .setProjectUuid("ABCD")
      .setComponentUuid("BCDE")
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

    underTest.save(issue);

    dbTester.assertDbUnit(getClass(), "should_insert_new_issues-result.xml",
      new String[] {"id", "created_at", "updated_at", "issue_change_creation_date"}, "issues", "issue_changes");
  }

  @Test
  public void should_update_issues() {
    dbTester.prepareDbUnit(getClass(), "should_update_issues.xml");

    IssueChangeContext context = IssueChangeContext.createUser(new Date(), "emmerik");

    DefaultIssueComment comment = DefaultIssueComment.create("ABCDE", "emmerik", "the comment");
    // override generated key
    comment.setKey("FGHIJ");

    Date date = DateUtils.parseDateTime("2013-05-18T12:00:00+0000");
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setType(RuleType.BUG)
      .setNew(false)
      .setChanged(true)

      // updated fields
      .setLine(5000)
      .setProjectUuid("CDEF")
      .setEffort(Duration.create(10L))
      .setChecksum("FFFFF")
      .setAuthorLogin("simon")
      .setAssignee("loic")
      .setFieldChange(context, "severity", "INFO", "BLOCKER")
      .setResolution("FIXED")
      .setStatus("RESOLVED")
      .setSeverity("BLOCKER")
      .setAttribute("foo", "bar")
      .addComment(comment)
      .setCreationDate(date)
      .setUpdateDate(date)
      .setCloseDate(date)

      // unmodifiable fields
      .setRuleKey(RuleKey.of("xxx", "unknown"))
      .setComponentKey("struts:Action")
      .setProjectKey("struts");

    underTest.save(issue);

    dbTester.assertDbUnit(getClass(), "should_update_issues-result.xml",
      new String[] {"id", "created_at", "updated_at", "issue_change_creation_date"}, "issues", "issue_changes");
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
      Rule rule = Rule.create().setRepositoryKey(key.repository()).setKey(key.rule());
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
