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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.component.ProjectSettingsRepository;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;

import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class IssueComputationTest {

  private static final RuleKey RULE_KEY = RuleKey.of("squid", "R1");
  private static final String PROJECT_KEY = "PROJECT_KEY";

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  IssueComputation sut;

  // inputs
  RuleCache ruleCache = mock(RuleCache.class);
  SourceLinesCache lineCache = mock(SourceLinesCache.class);
  ScmAccountCache scmAccountCache = mock(ScmAccountCache.class);
  RuleDto rule = new RuleDto().setRepositoryKey(RULE_KEY.repository()).setRuleKey(RULE_KEY.rule());
  BatchReport.Issue.Builder inputIssue = BatchReport.Issue.newBuilder()
    .setUuid("ISSUE_A")
    .setRuleRepository(RULE_KEY.repository())
    .setRuleKey(RULE_KEY.rule())
    .setStatus(Issue.STATUS_OPEN);
  Settings projectSettings;
  ProjectSettingsRepository projectSettingsRepository = mock(ProjectSettingsRepository.class);
  ComputationContext context = mock(ComputationContext.class, Mockito.RETURNS_DEEP_STUBS);
  UserIndex userIndex = mock(UserIndex.class);

  // output
  IssueCache outputIssues;

  @Before
  public void setUp() throws IOException {
    when(ruleCache.get(RULE_KEY)).thenReturn(rule);
    outputIssues = new IssueCache(temp.newFile(), System2.INSTANCE);
    projectSettings = new Settings();
    when(projectSettingsRepository.getProjectSettings(PROJECT_KEY)).thenReturn(projectSettings);
    sut = new IssueComputation(ruleCache, lineCache, scmAccountCache, outputIssues, userIndex, projectSettingsRepository);
  }

  @After
  public void after() {
    sut.afterReportProcessing();
  }

  @Test
  public void store_issues_on_disk() {
    process();

    assertThat(Iterators.getOnlyElement(outputIssues.traverse()).key()).isEqualTo("ISSUE_A");
  }

  @Test
  public void copy_rule_tags_on_new_issues() {
    inputIssue.setIsNew(true);
    rule.setTags(ImmutableSet.of("bug", "performance"));
    rule.setSystemTags(ImmutableSet.of("blocker"));

    process();

    assertThat(Iterators.getOnlyElement(outputIssues.traverse()).tags()).containsOnly("blocker", "bug", "performance");
  }

  @Test
  public void do_not_copy_rule_tags_on_existing_issues() {
    inputIssue.setIsNew(false);
    rule.setTags(ImmutableSet.of("bug", "performance"));
    rule.setSystemTags(ImmutableSet.of("blocker"));

    process();

    assertThat(Iterators.getOnlyElement(outputIssues.traverse()).tags()).isEmpty();
  }

  @Test
  public void guess_author_of_new_issues() {
    inputIssue.setIsNew(true);
    inputIssue.setLine(3);
    when(lineCache.lineAuthor(3)).thenReturn("charlie");

    process();

    assertThat(Iterators.getOnlyElement(outputIssues.traverse()).authorLogin()).isEqualTo("charlie");
  }

  @Test
  public void do_not_fail_if_missing_author_for_new_issues() {
    inputIssue.setIsNew(true);
    inputIssue.setLine(3);
    when(lineCache.lineAuthor(3)).thenReturn(null);

    process();

    assertThat(Iterators.getOnlyElement(outputIssues.traverse()).authorLogin()).isNull();
  }

  @Test
  public void do_not_guess_author_of_existing_issues() {
    inputIssue.setIsNew(false);
    inputIssue.setLine(3);
    when(lineCache.lineAuthor(3)).thenReturn("charlie");

    process();

    assertThat(Iterators.getOnlyElement(outputIssues.traverse()).authorLogin()).isNull();
  }

  @Test
  public void auto_assign_new_issues() {
    inputIssue.setIsNew(true);
    inputIssue.setAuthorLogin("charlie");
    when(scmAccountCache.getNullable("charlie")).thenReturn("char.lie");

    process();

    assertThat(Iterators.getOnlyElement(outputIssues.traverse()).assignee()).isEqualTo("char.lie");
  }

  @Test
  public void do_not_auto_assign_existing_issues() {
    inputIssue.setIsNew(false);
    inputIssue.setAuthorLogin("charlie");
    when(scmAccountCache.getNullable("charlie")).thenReturn("char.lie");

    process();

    assertThat(Iterators.getOnlyElement(outputIssues.traverse()).assignee()).isNull();
  }

  @Test
  public void do_not_override_author_and_assignee_set_by_old_batch_plugins() {
    inputIssue.setIsNew(true);

    // these fields were provided during project analysis, for instance
    // by developer cockpit or issue-assign plugins
    inputIssue.setAuthorLogin("charlie");
    inputIssue.setAssignee("cabu");

    process();

    // keep the values, without trying to update them
    DefaultIssue cachedIssue = Iterators.getOnlyElement(outputIssues.traverse());
    assertThat(cachedIssue.assignee()).isEqualTo("cabu");
    assertThat(cachedIssue.authorLogin()).isEqualTo("charlie");
    verifyZeroInteractions(scmAccountCache);
  }

  @Test
  public void assign_default_assignee_when_available() {
    inputIssue.setIsNew(true);
    String wolinski = "wolinski";
    projectSettings.setProperty(CoreProperties.DEFAULT_ISSUE_ASSIGNEE, wolinski);
    when(userIndex.getNullableByLogin(wolinski)).thenReturn(new UserDoc());

    process();

    assertThat(Iterators.getOnlyElement(outputIssues.traverse()).assignee()).isEqualTo(wolinski);
    assertThat(logTester.logs()).doesNotContain(String.format("the %s property was set with an unknown login: %s", CoreProperties.DEFAULT_ISSUE_ASSIGNEE, wolinski));
  }

  @Test
  public void do_not_assign_default_assignee_when_not_found_in_index() {
    inputIssue.setIsNew(true);
    String wolinski = "wolinski";
    projectSettings.setProperty(CoreProperties.DEFAULT_ISSUE_ASSIGNEE, wolinski);
    when(userIndex.getNullableByLogin(wolinski)).thenReturn(null);

    process();

    assertThat(Iterators.getOnlyElement(outputIssues.traverse()).assignee()).isNull();
    assertThat(logTester.logs()).contains(String.format("the %s property was set with an unknown login: %s", CoreProperties.DEFAULT_ISSUE_ASSIGNEE, wolinski));
  }

  private void process() {
    sut.processComponentIssues(context, Arrays.asList(inputIssue.build()), "FILE_A", 1, PROJECT_KEY, "PROJECT_UUID");
  }
}
