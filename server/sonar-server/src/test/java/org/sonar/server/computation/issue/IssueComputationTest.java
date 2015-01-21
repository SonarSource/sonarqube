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
package org.sonar.server.computation.issue;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.core.rule.RuleDto;

import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IssueComputationTest {

  public static final RuleKey RULE_KEY = RuleKey.of("squid", "R1");

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  // inputs
  RuleCache ruleCache = mock(RuleCache.class);
  SourceLinesCache lineCache = mock(SourceLinesCache.class);
  ScmAccountCache scmAccountCache = mock(ScmAccountCache.class);
  DefaultIssue issue = new DefaultIssue().setRuleKey(RULE_KEY).setKey("ISSUE_A");
  RuleDto rule = new RuleDto().setRepositoryKey(RULE_KEY.repository()).setRuleKey(RULE_KEY.rule());

  // output
  IssueCache issueCache;

  @Before
  public void setUp() throws IOException {
    when(ruleCache.get(RULE_KEY)).thenReturn(rule);
    issueCache = new IssueCache(temp.newFile(), System2.INSTANCE);
  }

  @Test
  public void store_issues_on_disk() throws Exception {
    process();

    assertThat(Iterators.getOnlyElement(issueCache.traverse()).key()).isEqualTo("ISSUE_A");
  }

  @Test
  public void copy_rule_tags_on_new_issues() throws Exception {
    issue.setNew(true);
    rule.setTags(ImmutableSet.of("bug", "performance"));
    rule.setSystemTags(ImmutableSet.of("blocker"));

    process();

    assertThat(Iterators.getOnlyElement(issueCache.traverse()).tags()).containsOnly("blocker", "bug", "performance");
  }

  @Test
  public void do_not_copy_rule_tags_on_existing_issues() throws Exception {
    issue.setNew(false);
    rule.setTags(ImmutableSet.of("bug", "performance"));
    rule.setSystemTags(ImmutableSet.of("blocker"));

    process();

    assertThat(Iterators.getOnlyElement(issueCache.traverse()).tags()).isEmpty();
  }

  @Test
  public void guess_author_of_new_issues() throws Exception {
    issue.setNew(true);
    issue.setLine(3);
    when(lineCache.lineAuthor(3)).thenReturn("charlie");

    process();

    assertThat(Iterators.getOnlyElement(issueCache.traverse()).authorLogin()).isEqualTo("charlie");
  }

  @Test
  public void do_not_fail_if_missing_author_for_new_issues() throws Exception {
    issue.setNew(true);
    issue.setLine(3);
    when(lineCache.lineAuthor(3)).thenReturn(null);

    process();

    assertThat(Iterators.getOnlyElement(issueCache.traverse()).authorLogin()).isNull();
  }

  @Test
  public void do_not_guess_author_of_existing_issues() throws Exception {
    issue.setNew(false);
    issue.setLine(3);
    when(lineCache.lineAuthor(3)).thenReturn("charlie");

    process();

    assertThat(Iterators.getOnlyElement(issueCache.traverse()).authorLogin()).isNull();
  }

  @Test
  public void auto_assign_new_issues() throws Exception {
    issue.setNew(true);
    issue.setAuthorLogin("charlie");
    when(scmAccountCache.getNullable("charlie")).thenReturn("char.lie");

    process();

    assertThat(Iterators.getOnlyElement(issueCache.traverse()).assignee()).isEqualTo("char.lie");
  }

  @Test
  public void do_not_auto_assign_existing_issues() throws Exception {
    issue.setNew(false);
    issue.setAuthorLogin("charlie");
    when(scmAccountCache.getNullable("charlie")).thenReturn("char.lie");

    process();

    assertThat(Iterators.getOnlyElement(issueCache.traverse()).assignee()).isNull();
  }

  private void process() {
    IssueComputation computation = new IssueComputation(ruleCache, lineCache, scmAccountCache, issueCache);
    computation.processComponentIssues("FILE_A", Arrays.asList(issue));
    computation.afterReportProcessing();
  }
}
