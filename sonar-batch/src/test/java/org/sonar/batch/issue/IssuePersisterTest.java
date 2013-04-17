/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.batch.issue;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.DateUtils;
import org.sonar.batch.index.SnapshotCache;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueDao;
import org.sonar.core.persistence.AbstractDaoTestCase;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IssuePersisterTest extends AbstractDaoTestCase {

  IssueDao dao;
  IssueCache issues = mock(IssueCache.class);
  SnapshotCache snapshots = new SnapshotCache();
  RuleFinder ruleFinder = mock(RuleFinder.class);
  IssuePersister persister;

  @Before
  public void before() throws Exception {
    dao = new IssueDao(getMyBatis());
    persister = new IssuePersister(dao, issues, snapshots, ruleFinder);
  }

  @Test
  public void should_insert_new_issue() throws Exception {
    setupData("should_insert_new_issue");
    Snapshot snapshot = new Snapshot();
    snapshot.setId(100);
    snapshot.setResourceId(200);
    snapshots.put("org/struts/Action.java", snapshot);

    Issue issue = new DefaultIssue()
      .setKey("ABCD")
      .setComponentKey("org/struts/Action.java")
      .setRuleKey(RuleKey.of("squid", "NullDef"))
      .setNew(true)
      .setStatus(Issue.STATUS_OPEN)
      .setSeverity(Severity.BLOCKER)
      .setCreatedAt(DateUtils.parseDate("2013-05-18"))
      .setUpdatedAt(DateUtils.parseDate("2013-05-23"));
    when(issues.componentIssues("org/struts/Action.java")).thenReturn(newArrayList(issue));

    Rule rule = Rule.create("squid", "NullDef");
    rule.setId(300);
    when(ruleFinder.findByKey("squid", "NullDef")).thenReturn(rule);

    persister.persist();

    checkTables("should_insert_new_issue", "issues");
  }

  @Test
  public void should_update_existing_issue() throws Exception {
    setupData("should_update_existing_issue");
    Snapshot snapshot = new Snapshot();
    snapshot.setId(100);
    snapshot.setResourceId(200);
    snapshots.put("org/struts/Action.java", snapshot);

    Issue issue = new DefaultIssue()
      .setKey("ABCD")
      .setComponentKey("org/struts/Action.java")
      .setRuleKey(RuleKey.of("squid", "NullDef"))
      .setNew(false)
      .setStatus(Issue.STATUS_CLOSED)
      .setSeverity(Severity.BLOCKER)
      .setCreatedAt(DateUtils.parseDate("2013-05-18"))
      .setUpdatedAt(DateUtils.parseDate("2013-05-23"));
    when(issues.componentIssues("org/struts/Action.java")).thenReturn(newArrayList(issue));

    Rule rule = Rule.create("squid", "NullDef");
    rule.setId(300);
    when(ruleFinder.findByKey("squid", "NullDef")).thenReturn(rule);

    persister.persist();

    checkTables("should_update_existing_issue", "issues");
  }


  @Test
  public void should_fail_if_rule_not_found() throws Exception {
    Snapshot snapshot = new Snapshot();
    snapshot.setId(100);
    snapshot.setResourceId(200);
    snapshots.put("org/struts/Action.java", snapshot);

    Issue issue = new DefaultIssue()
      .setKey("ABCD")
      .setComponentKey("org/struts/Action.java")
      .setRuleKey(RuleKey.of("squid", "NullDef"))
      .setNew(false)
      .setStatus(Issue.STATUS_CLOSED)
      .setSeverity(Severity.BLOCKER)
      .setCreatedAt(DateUtils.parseDate("2013-05-18"))
      .setUpdatedAt(DateUtils.parseDate("2013-05-23"));
    when(issues.componentIssues("org/struts/Action.java")).thenReturn(newArrayList(issue));

    when(ruleFinder.findByKey("squid", "NullDef")).thenReturn(null);

    try {
      persister.persist();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Rule not found: squid:NullDef");
    }
  }
}
