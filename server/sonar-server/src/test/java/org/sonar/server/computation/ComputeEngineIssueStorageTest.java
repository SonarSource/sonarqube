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

package org.sonar.server.computation;

import org.sonar.batch.protocol.output.component.ReportComponent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.DefaultIssueComment;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.System2;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.db.IssueDao;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComputeEngineIssueStorageTest extends AbstractDaoTestCase {

  DbClient dbClient;
  DbSession dbSession;
  Map<Long, ReportComponent> components;
  ComponentDto project;

  ComputeEngineIssueStorage sut;

  @Before
  public void setUp() throws Exception {
    System2 system = mock(System2.class);
    when(system.now()).thenReturn(2000000000L);
    dbClient = new DbClient(getDatabase(), getMyBatis(),
      new ComponentDao(system),
      new IssueDao(getMyBatis()),
      new ComponentDao(system));
    dbSession = dbClient.openSession(false);
    components = new HashMap<>();
    project = new ComponentDto();

    sut = new ComputeEngineIssueStorage(getMyBatis(), dbClient, new FakeRuleFinder(), project);
  }

  @After
  public void tearDown() throws Exception {
    MyBatis.closeQuietly(dbSession);
  }

  @Test
  public void should_get_component_id_set_in_issue() throws Exception {
    DefaultIssue issue = new DefaultIssue().setComponentId(123L);

    long componentId = sut.componentId(dbSession, issue);

    assertThat(componentId).isEqualTo(123L);
  }

  @Test
  public void should_load_component_id_from_db() throws Exception {
    setupData("should_load_component_id_from_db");

    long componentId = sut.componentId(dbSession, new DefaultIssue().setComponentKey("struts:Action.java"));

    assertThat(componentId).isEqualTo(123);
  }

  @Test(expected = IllegalStateException.class)
  public void should_fail_to_load_component_id_if_unknown_component() throws Exception {
    setupData("should_fail_to_load_component_id_if_unknown_component");

    sut.componentId(dbSession, new DefaultIssue().setComponentKey("struts:Action.java"));
  }

  @Test
  public void should_load_project_id() throws Exception {
    project.setId(100L);

    long projectId = sut.projectId();

    assertThat(projectId).isEqualTo(100);
  }

  @Test
  public void should_insert_new_issues() throws Exception {
    setupData("should_insert_new_issues");
    project.setId(10L).setKey("struts");

    DefaultIssueComment comment = DefaultIssueComment.create("ABCDE", "emmerik", "the comment");
    // override generated key
    comment.setKey("FGHIJ");

    Date date = DateUtils.parseDate("2013-05-18");
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setNew(true)
      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
      .setLine(5000)
      .setDebt(Duration.create(10L))
      .setReporter("emmerik")
      .setResolution("OPEN")
      .setStatus("OPEN")
      .setSeverity("BLOCKER")
      .setAttribute("foo", "bar")
      .addComment(comment)
      .setCreationDate(date)
      .setUpdateDate(date)
      .setCloseDate(date)

      .setComponentKey("struts:Action");

    sut.save(issue);

    checkTables("should_insert_new_issues", new String[] {"id", "created_at", "updated_at", "issue_change_creation_date"}, "issues", "issue_changes");
  }

  @Test
  public void should_update_issues() throws Exception {
    setupData("should_update_issues");

    IssueChangeContext context = IssueChangeContext.createUser(new Date(), "emmerik");

    project.setId(10L).setKey("struts");

    DefaultIssueComment comment = DefaultIssueComment.create("ABCDE", "emmerik", "the comment");
    // override generated key
    comment.setKey("FGHIJ");

    Date date = DateUtils.parseDate("2013-05-18");
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setNew(false)
      .setChanged(true)

      // updated fields
      .setLine(5000)
      .setDebt(Duration.create(10L))
      .setChecksum("FFFFF")
      .setAuthorLogin("simon")
      .setAssignee("loic")
      .setFieldChange(context, "severity", "INFO", "BLOCKER")
      .setReporter("emmerik")
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
      .setComponentKey("not:a:component");

    sut.save(issue);

    checkTables("should_update_issues", new String[] {"id", "created_at", "updated_at", "issue_change_creation_date"}, "issues", "issue_changes");
  }

  @Test
  public void should_resolve_conflicts_on_updates() throws Exception {
    setupData("should_resolve_conflicts_on_updates");

    project.setId(10L).setKey("struts");

    Date date = DateUtils.parseDate("2013-05-18");
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setNew(false)
      .setChanged(true)
      .setCreationDate(DateUtils.parseDate("2005-05-12"))
      .setUpdateDate(date)
      .setRuleKey(RuleKey.of("squid", "AvoidCycles"))
      .setComponentKey("struts:Action")

      // issue in database has been updated in 2015, after the loading by scan
      .setSelectedAt(1400000000000L)

      // fields to be updated
      .setLine(444)
      .setSeverity("BLOCKER")
      .setChecksum("FFFFF")
      .setAttribute("JIRA", "http://jira.com")

      // fields overridden by end-user -> do not save
      .setAssignee("looser")
      .setResolution(null)
      .setStatus("REOPEN");

    sut.save(issue);

    checkTables("should_resolve_conflicts_on_updates", new String[] {"id", "created_at", "updated_at", "issue_change_creation_date"}, "issues");
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
