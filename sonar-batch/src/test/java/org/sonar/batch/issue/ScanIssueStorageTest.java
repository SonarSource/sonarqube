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
package org.sonar.batch.issue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.DefaultIssueComment;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.System2;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.index.SnapshotCache;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.resource.ResourceDao;

import java.util.Collection;
import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ScanIssueStorageTest extends AbstractDaoTestCase {

  @Mock
  SnapshotCache snapshotCache;

  @Mock
  ProjectTree projectTree;

  ScanIssueStorage storage;

  @Before
  public void setUp() throws Exception {
    storage = new ScanIssueStorage(getMyBatis(), new FakeRuleFinder(), snapshotCache, new ResourceDao(getMyBatis(), System2.INSTANCE), projectTree);
  }

  @Test
  public void should_load_component_id_from_cache() throws Exception {
    when(snapshotCache.get("struts:Action.java")).thenReturn(new Snapshot().setResourceId(123));

    long componentId = storage.componentId(new DefaultIssue().setComponentKey("struts:Action.java"));

    assertThat(componentId).isEqualTo(123);
  }

  @Test
  public void should_load_component_id_from_db() throws Exception {
    setupData("should_load_component_id_from_db");
    when(snapshotCache.get("struts:Action.java")).thenReturn(null);

    long componentId = storage.componentId(new DefaultIssue().setComponentKey("struts:Action.java"));

    assertThat(componentId).isEqualTo(123);
  }

  @Test
  public void should_fail_to_load_component_id_if_unknown_component() throws Exception {
    setupData("should_fail_to_load_component_id_if_unknown_component");
    when(snapshotCache.get("struts:Action.java")).thenReturn(null);

    try {
      storage.componentId(new DefaultIssue().setComponentKey("struts:Action.java"));
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessage("Unknown component: struts:Action.java");
    }
  }

  @Test
  public void should_load_project_id() throws Exception {
    when(projectTree.getRootProject()).thenReturn((Project) new Project("struts").setId(100));

    long projectId = storage.projectId();

    assertThat(projectId).isEqualTo(100);
  }

  @Test
  public void should_insert_new_issues() throws Exception {
    setupData("should_insert_new_issues");

    Project project = new Project("struts");
    project.setId(10);
    when(projectTree.getRootProject()).thenReturn(project);

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

    storage.save(issue);

    checkTables("should_insert_new_issues", new String[]{"id", "created_at", "updated_at", "issue_change_creation_date"}, "issues", "issue_changes");
  }

  @Test
  public void should_update_issues() throws Exception {
    setupData("should_update_issues");

    IssueChangeContext context = IssueChangeContext.createUser(new Date(), "emmerik");

    Project project = new Project("struts");
    project.setId(10);
    when(projectTree.getRootProject()).thenReturn(project);

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

    storage.save(issue);

    checkTables("should_update_issues", new String[]{"id", "created_at", "updated_at", "issue_change_creation_date"}, "issues", "issue_changes");
  }

  @Test
  public void should_resolve_conflicts_on_updates() throws Exception {
    setupData("should_resolve_conflicts_on_updates");

    Project project = new Project("struts");
    project.setId(10);
    when(projectTree.getRootProject()).thenReturn(project);

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

    storage.save(issue);

    checkTables("should_resolve_conflicts_on_updates", new String[]{"id", "created_at", "updated_at", "issue_change_creation_date"}, "issues");
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
