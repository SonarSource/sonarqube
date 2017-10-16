/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.issue;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.ShortBranchIssue;
import org.sonar.core.issue.tracking.SimpleTracker;
import org.sonar.server.computation.task.projectanalysis.component.Component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ShortBranchIssueMergerTest {
  @Mock
  private ShortBranchIssuesLoader resolvedShortBranchIssuesLoader;
  @Mock
  private IssueLifecycle issueLifecycle;
  @Mock
  private Component component;

  private SimpleTracker<DefaultIssue, ShortBranchIssue> tracker = new SimpleTracker<>();
  private ShortBranchIssueMerger copier;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    copier = new ShortBranchIssueMerger(resolvedShortBranchIssuesLoader, tracker, issueLifecycle);
  }

  @Test
  public void do_nothing_if_no_match() {
    when(resolvedShortBranchIssuesLoader.loadCandidateIssuesForMergingInTargetBranch(component)).thenReturn(Collections.emptyList());
    DefaultIssue i = createIssue("issue1", "rule1", Issue.STATUS_CONFIRMED, null);
    copier.tryMerge(component, Collections.singleton(i));

    verify(resolvedShortBranchIssuesLoader).loadCandidateIssuesForMergingInTargetBranch(component);
    verifyZeroInteractions(issueLifecycle);
  }

  @Test
  public void do_nothing_if_no_new_issue() {
    DefaultIssue i = createIssue("issue1", "rule1", Issue.STATUS_CONFIRMED, null);
    when(resolvedShortBranchIssuesLoader.loadCandidateIssuesForMergingInTargetBranch(component)).thenReturn(Collections.singleton(newShortBranchIssue(i, "myBranch")));
    copier.tryMerge(component, Collections.emptyList());

    verify(resolvedShortBranchIssuesLoader).loadCandidateIssuesForMergingInTargetBranch(component);
    verifyZeroInteractions(issueLifecycle);
  }

  @Test
  public void update_status_on_matches() {
    DefaultIssue issue1 = createIssue("issue1", "rule1", Issue.STATUS_CONFIRMED, null);
    ShortBranchIssue shortBranchIssue = newShortBranchIssue(issue1, "myBranch");
    DefaultIssue newIssue = createIssue("issue2", "rule1", Issue.STATUS_OPEN, null);

    when(resolvedShortBranchIssuesLoader.loadCandidateIssuesForMergingInTargetBranch(component)).thenReturn(Collections.singleton(shortBranchIssue));
    when(resolvedShortBranchIssuesLoader.loadDefaultIssuesWithChanges(anyListOf(ShortBranchIssue.class))).thenReturn(ImmutableMap.of(shortBranchIssue, issue1));
    copier.tryMerge(component, Collections.singleton(newIssue));
    ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
    verify(resolvedShortBranchIssuesLoader).loadDefaultIssuesWithChanges(captor.capture());
    assertThat(captor.getValue()).containsOnly(shortBranchIssue);
    verify(issueLifecycle).mergeConfirmedOrResolvedFromShortLivingBranch(newIssue, issue1, "myBranch");
  }

  @Test
  public void prefer_resolved_issues() {
    ShortBranchIssue shortBranchIssue1 = newShortBranchIssue(createIssue("issue1", "rule1", Issue.STATUS_CONFIRMED, null), "myBranch1");
    ShortBranchIssue shortBranchIssue2 = newShortBranchIssue(createIssue("issue2", "rule1", Issue.STATUS_CONFIRMED, null), "myBranch2");
    DefaultIssue issue3 = createIssue("issue3", "rule1", Issue.STATUS_RESOLVED, Issue.RESOLUTION_FALSE_POSITIVE);
    ShortBranchIssue shortBranchIssue3 = newShortBranchIssue(issue3, "myBranch3");
    DefaultIssue newIssue = createIssue("newIssue", "rule1", Issue.STATUS_OPEN, null);

    when(resolvedShortBranchIssuesLoader.loadCandidateIssuesForMergingInTargetBranch(component)).thenReturn(Arrays.asList(shortBranchIssue1, shortBranchIssue2, shortBranchIssue3));
    when(resolvedShortBranchIssuesLoader.loadDefaultIssuesWithChanges(anyListOf(ShortBranchIssue.class))).thenReturn(ImmutableMap.of(shortBranchIssue3, issue3));
    copier.tryMerge(component, Collections.singleton(newIssue));
    verify(issueLifecycle).mergeConfirmedOrResolvedFromShortLivingBranch(newIssue, issue3, "myBranch3");
  }

  private static DefaultIssue createIssue(String key, String ruleKey, String status, @Nullable String resolution) {
    DefaultIssue issue = new DefaultIssue();
    issue.setKey(key);
    issue.setRuleKey(RuleKey.of("repo", ruleKey));
    issue.setMessage("msg");
    issue.setLine(1);
    issue.setStatus(status);
    issue.setResolution(resolution);
    return issue;
  }

  private ShortBranchIssue newShortBranchIssue(DefaultIssue i, String originBranch) {
    return new ShortBranchIssue(i.key(), i.line(), i.message(), i.getLineHash(), i.ruleKey(), i.status(), originBranch);
  }
}
