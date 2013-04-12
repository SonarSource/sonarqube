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
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.batch.index.SnapshotCache;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueDao;
import org.sonar.core.issue.IssueDto;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Mockito.*;

public class IssuePersisterTest {

private IssuePersister issuePersister;

  private  IssueDao dao;
  private  IssueCache issueCache;
  private  SnapshotCache snapshotCache;
  private  RuleFinder ruleFinder;

  @Before
  public void before() throws Exception {
    dao = mock(IssueDao.class);
    issueCache = mock(IssueCache.class);
    snapshotCache = mock(SnapshotCache.class);
    ruleFinder = mock(RuleFinder.class);
    issuePersister = new IssuePersister(dao, issueCache, snapshotCache, ruleFinder);
  }

  @Test
  public void should_persist_new_rule() throws Exception {
    Issue issue = new DefaultIssue().setComponentKey("componentKey").setRuleRepositoryKey("repoKey").setRuleKey("ruleKey").setNew(true);
    when(issueCache.issues()).thenReturn(newArrayList(issue));

    Snapshot snapshot = mock(Snapshot.class);
    when(snapshotCache.get("componentKey")).thenReturn(snapshot);

    Rule rule = Rule.create("repoKey", "ruleKey");
    when(ruleFinder.findByKey("repoKey", "ruleKey")).thenReturn(rule);

    issuePersister.persist();

    verify(dao).insert(any(IssueDto.class));
  }

  @Test
  public void should_persist_updated_rule() throws Exception {
    Issue issue = new DefaultIssue().setComponentKey("componentKey").setRuleRepositoryKey("repoKey").setRuleKey("ruleKey").setNew(false);
    when(issueCache.issues()).thenReturn(newArrayList(issue));

    Snapshot snapshot = mock(Snapshot.class);
    when(snapshotCache.get("componentKey")).thenReturn(snapshot);

    Rule rule = Rule.create("repoKey", "ruleKey");
    when(ruleFinder.findByKey("repoKey", "ruleKey")).thenReturn(rule);

    issuePersister.persist();

    verify(dao).update(anyCollectionOf(IssueDto.class));
  }

  @Test(expected = IllegalStateException.class)
  public void should_throw_exception_if_snapshot_is_null() throws Exception {
    Issue issue = new DefaultIssue().setComponentKey("componentKey");
    when(issueCache.issues()).thenReturn(newArrayList(issue));
    when(snapshotCache.get("componentKey")).thenReturn(null);

    issuePersister.persist();
  }

  @Test(expected = IllegalStateException.class)
  public void should_throw_exception_if_rule_not_found() throws Exception {
    Issue issue = new DefaultIssue().setComponentKey("componentKey").setRuleRepositoryKey("repoKey").setRuleKey("ruleKey");
    when(issueCache.issues()).thenReturn(newArrayList(issue));

    Snapshot snapshot = mock(Snapshot.class);
    when(snapshotCache.get("componentKey")).thenReturn(snapshot);

    when(ruleFinder.findByKey("repoKey", "ruleKey")).thenReturn(null);

    issuePersister.persist();
  }
}
