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

import org.junit.rules.ExpectedException;
import org.junit.Rule;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.batch.scan.filesystem.InputPathCache;
import org.junit.Test;
import org.sonar.api.component.Component;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.resources.Project;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultIssuableTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  ModuleIssues moduleIssues = mock(ModuleIssues.class);
  IssueCache cache = mock(IssueCache.class);
  Project project = mock(Project.class);
  Component component = mock(Component.class);
  InputPathCache inputPathCache = mock(InputPathCache.class);

  @Test
  public void test_unresolved_issues() throws Exception {
    when(component.key()).thenReturn("struts:org.apache.Action");
    DefaultIssue resolved = new DefaultIssue().setResolution(Issue.RESOLUTION_FALSE_POSITIVE);
    DefaultIssue unresolved = new DefaultIssue();
    when(cache.byComponent("struts:org.apache.Action")).thenReturn(Arrays.asList(resolved, unresolved));

    DefaultIssuable perspective = new DefaultIssuable(component, project, moduleIssues, cache, inputPathCache);

    List<Issue> issues = perspective.issues();
    assertThat(issues).containsOnly(unresolved);
  }

  @Test
  public void validateIssueLine() {
    InputFile file = mock(InputFile.class);

    when(file.lines()).thenReturn(999);
    when(component.path()).thenReturn("file");
    when(inputPathCache.getFile("module", "file")).thenReturn(file);

    DefaultIssue issue = new DefaultIssue()
      .setLine(1000)
      .setProjectKey("module")
      .setRuleKey(RuleKey.of("repo", "rule"))
      .setComponentKey("componentKey");
    DefaultIssuable issuable = new DefaultIssuable(component, project, moduleIssues, cache, inputPathCache);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Invalid line 1000 (must be <= 999) in issue for 'componentKey' created by the rule 'repo:rule'");
    issuable.addIssue(issue);
  }

  @Test
  public void dontValidateNullLine() {
    DefaultIssuable issuable = new DefaultIssuable(component, project, moduleIssues, cache, inputPathCache);

    DefaultIssue issue = new DefaultIssue()
      .setLine(null);
    issuable.addIssue(issue);
    verifyNoMoreInteractions(inputPathCache);
  }

  @Test
  public void dontValidateUnknownFiles() {
    DefaultIssuable issuable = new DefaultIssuable(component, project, moduleIssues, cache, inputPathCache);
    when(component.path()).thenReturn("file");

    DefaultIssue issue = new DefaultIssue()
      .setLine(1505)
      .setProjectKey("module");

    issuable.addIssue(issue);
    verify(inputPathCache).getFile("module", "file");
  }

  @Test
  public void test_resolved_issues() throws Exception {
    when(component.key()).thenReturn("struts:org.apache.Action");
    DefaultIssue resolved = new DefaultIssue().setResolution(Issue.RESOLUTION_FALSE_POSITIVE);
    DefaultIssue unresolved = new DefaultIssue();
    when(cache.byComponent("struts:org.apache.Action")).thenReturn(Arrays.asList(resolved, unresolved));

    DefaultIssuable perspective = new DefaultIssuable(component, project, moduleIssues, cache, inputPathCache);

    List<Issue> issues = perspective.resolvedIssues();
    assertThat(issues).containsOnly(resolved);
  }
}
