/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.plugins.core.issue;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.batch.issue.ScanIssues;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueDto;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class IssueTrackingDecoratorTest extends AbstractDaoTestCase {

  IssueTrackingDecorator decorator;
  ScanIssues scanIssues = mock(ScanIssues.class);
  InitialOpenIssuesStack initialOpenIssuesStack = mock(InitialOpenIssuesStack.class);
  IssueTracking tracking = mock(IssueTracking.class);
  Date loadedDate = new Date();

  @Before
  public void init() {
    when(initialOpenIssuesStack.getLoadedDate()).thenReturn(loadedDate);
    decorator = new IssueTrackingDecorator(scanIssues, initialOpenIssuesStack, tracking);
  }

  @Test
  public void should_execute_on_project() {
    Project project = mock(Project.class);
    when(project.isLatestAnalysis()).thenReturn(true);
    assertTrue(decorator.shouldExecuteOnProject(project));
  }

  @Test
  public void should_execute_on_project_not_if_past_scan() {
    Project project = mock(Project.class);
    when(project.isLatestAnalysis()).thenReturn(false);
    assertFalse(decorator.shouldExecuteOnProject(project));
  }

  @Test
  public void should_close_resolved_issue() {
    when(scanIssues.issues(anyString())).thenReturn(Collections.<Issue>emptyList());
    when(initialOpenIssuesStack.selectAndRemove(anyInt())).thenReturn(newArrayList(
        new IssueDto().setKey("100").setRuleId(10).setRuleKey_unit_test_only("squid", "AvoidCycle")));

    decorator.decorate(mock(Resource.class), null);

    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(scanIssues).addOrUpdate(argument.capture());
    assertThat(argument.getValue().status()).isEqualTo(Issue.STATUS_CLOSED);
    assertThat(argument.getValue().updatedAt()).isEqualTo(loadedDate);
    assertThat(argument.getValue().closedAt()).isEqualTo(loadedDate);
  }

  @Test
  public void should_close_resolved_manual_issue() {
    when(scanIssues.issues(anyString())).thenReturn(Collections.<Issue>emptyList());
    when(initialOpenIssuesStack.selectAndRemove(anyInt())).thenReturn(newArrayList(
        new IssueDto().setKey("100").setRuleId(1).setManualIssue(true).setStatus(Issue.STATUS_RESOLVED).setRuleKey_unit_test_only("squid", "AvoidCycle")));

    decorator.decorate(mock(Resource.class), null);

    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(scanIssues).addOrUpdate(argument.capture());
    assertThat(argument.getValue().status()).isEqualTo(Issue.STATUS_CLOSED);
    assertThat(argument.getValue().updatedAt()).isEqualTo(loadedDate);
    assertThat(argument.getValue().closedAt()).isEqualTo(loadedDate);
  }

  @Test
  public void should_reopen_unresolved_issue() {
    when(scanIssues.issues(anyString())).thenReturn(Lists.<Issue>newArrayList(
      new DefaultIssue().setKey("100")));
    when(initialOpenIssuesStack.selectAndRemove(anyInt())).thenReturn(newArrayList(
        new IssueDto().setKey("100").setRuleId(1).setStatus(Issue.STATUS_RESOLVED).setResolution(Issue.RESOLUTION_FIXED)
          .setRuleKey_unit_test_only("squid", "AvoidCycle")));

    decorator.decorate(mock(Resource.class), null);

    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(scanIssues, times(2)).addOrUpdate(argument.capture());

    List<DefaultIssue> capturedDefaultIssues = argument.getAllValues();
    // First call is done when updating issues after calling issue tracking and we don't care
    DefaultIssue defaultIssue = capturedDefaultIssues.get(1);
    assertThat(defaultIssue.status()).isEqualTo(Issue.STATUS_REOPENED);
    assertThat(defaultIssue.resolution()).isEqualTo(Issue.RESOLUTION_OPEN);
    assertThat(defaultIssue.updatedAt()).isEqualTo(loadedDate);
  }

  @Test
  public void should_keep_false_positive_issue() {
    when(scanIssues.issues(anyString())).thenReturn(Lists.<Issue>newArrayList(
      new DefaultIssue().setKey("100")));
    when(initialOpenIssuesStack.selectAndRemove(anyInt())).thenReturn(newArrayList(
      new IssueDto().setKey("100").setRuleId(1).setStatus(Issue.STATUS_RESOLVED).setResolution(Issue.RESOLUTION_FALSE_POSITIVE)
        .setRuleKey_unit_test_only("squid", "AvoidCycle")));

    decorator.decorate(mock(Resource.class), null);

    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(scanIssues, times(2)).addOrUpdate(argument.capture());

    List<DefaultIssue> capturedDefaultIssues = argument.getAllValues();
    // First call is done when updating issues after calling issue tracking and we don't care
    DefaultIssue defaultIssue = capturedDefaultIssues.get(1);
    assertThat(defaultIssue.status()).isEqualTo(Issue.STATUS_RESOLVED);
    assertThat(defaultIssue.resolution()).isEqualTo(Issue.RESOLUTION_FALSE_POSITIVE);
    assertThat(defaultIssue.updatedAt()).isEqualTo(loadedDate);
  }

  @Test
  public void should_close_remaining_open_issue_on_root_project() {
    when(scanIssues.issues(anyString())).thenReturn(Collections.<Issue>emptyList());
    when(initialOpenIssuesStack.selectAndRemove(anyInt())).thenReturn(Collections.<IssueDto>emptyList());

    when(initialOpenIssuesStack.getAllIssues()).thenReturn(newArrayList(new IssueDto().setKey("100").setRuleId(1).setRuleKey_unit_test_only("squid", "AvoidCycle")));

    Resource resource = mock(Resource.class);
    when(resource.getQualifier()).thenReturn(Qualifiers.PROJECT);
    decorator.decorate(resource, null);

    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(scanIssues).addOrUpdate(argument.capture());
    assertThat(argument.getValue().status()).isEqualTo(Issue.STATUS_CLOSED);
    assertThat(argument.getValue().updatedAt()).isEqualTo(loadedDate);
    assertThat(argument.getValue().closedAt()).isEqualTo(loadedDate);
  }

}
