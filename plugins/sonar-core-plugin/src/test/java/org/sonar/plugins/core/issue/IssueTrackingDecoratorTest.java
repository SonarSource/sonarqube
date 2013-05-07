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

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.batch.issue.ScanIssues;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.issue.workflow.IssueWorkflow;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.java.api.JavaClass;

import java.util.*;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class IssueTrackingDecoratorTest extends AbstractDaoTestCase {

  IssueTrackingDecorator decorator;
  ScanIssues scanIssues = mock(ScanIssues.class);
  InitialOpenIssuesStack initialOpenIssues = mock(InitialOpenIssuesStack.class);
  IssueTracking tracking = mock(IssueTracking.class);
  IssueFilters filters = mock(IssueFilters.class);
  IssueHandlers handlers = mock(IssueHandlers.class);
  IssueWorkflow workflow = mock(IssueWorkflow.class);
  Date loadedDate = new Date();

  @Before
  public void init() {
    when(initialOpenIssues.getLoadedDate()).thenReturn(loadedDate);
    decorator = new IssueTrackingDecorator(scanIssues, initialOpenIssues, tracking,
      filters, handlers, workflow, mock(Project.class));
  }

  @Test
  public void should_execute_on_project() {
    Project project = mock(Project.class);
    when(project.isLatestAnalysis()).thenReturn(true);
    assertThat(decorator.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void should_not_execute_on_project_if_past_scan() {
    Project project = mock(Project.class);
    when(project.isLatestAnalysis()).thenReturn(false);
    assertThat(decorator.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void should_not_be_executed_on_classes_not_methods() throws Exception {
    DecoratorContext context = mock(DecoratorContext.class);
    decorator.decorate(JavaClass.create("org.foo.Bar"), context);
    verifyZeroInteractions(context, scanIssues, tracking, filters, handlers, workflow);
  }

  @Test
  public void should_process_open_issues() throws Exception {
    Resource file = new File("Action.java").setEffectiveKey("struts:Action.java").setId(123);
    final DefaultIssue issue = new DefaultIssue();

    // INPUT : one issue, no open issues during previous scan, no filtering
    when(scanIssues.issues("struts:Action.java")).thenReturn(Arrays.asList(issue));
    when(filters.accept(issue)).thenReturn(true);
    List<IssueDto> dbIssues = Collections.emptyList();
    when(initialOpenIssues.selectAndRemove(123)).thenReturn(dbIssues);

    decorator.decorate(file, mock(DecoratorContext.class, Mockito.RETURNS_MOCKS));

    // Apply filters, track, apply transitions, notify extensions then update cache
    verify(filters).accept(issue);
    verify(tracking).track(eq(file), eq(dbIssues), argThat(new ArgumentMatcher<Collection<DefaultIssue>>() {
      @Override
      public boolean matches(Object o) {
        List<DefaultIssue> issues = (List<DefaultIssue>) o;
        return issues.size() == 1 && issues.get(0) == issue;
      }
    }));
    verify(workflow).doAutomaticTransition(eq(issue), any(IssueChangeContext.class));
    verify(handlers).execute(eq(issue), any(IssueChangeContext.class));
    verify(scanIssues).addOrUpdate(issue);
  }

  @Test
  public void should_register_unmatched_issues() throws Exception {
    // "Unmatched" issues existed in previous scan but not in current one -> they have to be closed
    Resource file = new File("Action.java").setEffectiveKey("struts:Action.java").setId(123);
    DefaultIssue openIssue = new DefaultIssue();

    // INPUT : one issue, one open issue during previous scan, no filtering
    when(scanIssues.issues("struts:Action.java")).thenReturn(Arrays.asList(openIssue));
    when(filters.accept(openIssue)).thenReturn(true);
    IssueDto unmatchedIssue = new IssueDto().setKee("ABCDE").setResolution("OPEN").setStatus("OPEN").setRuleKey_unit_test_only("squid", "AvoidCycle");
    List<IssueDto> unmatchedIssues = Arrays.asList(unmatchedIssue);
    when(tracking.track(eq(file), anyCollection(), anyCollection())).thenReturn(Sets.newHashSet(unmatchedIssues));

    decorator.decorate(file, mock(DecoratorContext.class, Mockito.RETURNS_MOCKS));

    verify(workflow, times(2)).doAutomaticTransition(any(DefaultIssue.class), any(IssueChangeContext.class));
    verify(handlers, times(2)).execute(any(DefaultIssue.class), any(IssueChangeContext.class));
    verify(scanIssues, times(2)).addOrUpdate(any(DefaultIssue.class));

    verify(scanIssues).addOrUpdate(argThat(new ArgumentMatcher<DefaultIssue>() {
      @Override
      public boolean matches(Object o) {
        DefaultIssue issue = (DefaultIssue) o;
        return "ABCDE".equals(issue.key());
      }
    }));
  }

  @Test
  public void should_register_issues_on_deleted_components() throws Exception {
    Project project = new Project("struts");
    DefaultIssue openIssue = new DefaultIssue();
    when(scanIssues.issues("struts")).thenReturn(Arrays.asList(openIssue));
    when(filters.accept(openIssue)).thenReturn(true);
    IssueDto deadIssue = new IssueDto().setKee("ABCDE").setResolution("OPEN").setStatus("OPEN").setRuleKey_unit_test_only("squid", "AvoidCycle");
    when(initialOpenIssues.getAllIssues()).thenReturn(Arrays.asList(deadIssue));

    decorator.decorate(project, mock(DecoratorContext.class, Mockito.RETURNS_MOCKS));

    // the dead issue must be closed -> apply automatic transition, notify handlers and add to cache
    verify(workflow, times(2)).doAutomaticTransition(any(DefaultIssue.class), any(IssueChangeContext.class));
    verify(handlers, times(2)).execute(any(DefaultIssue.class), any(IssueChangeContext.class));
    verify(scanIssues, times(2)).addOrUpdate(any(DefaultIssue.class));

    verify(scanIssues).addOrUpdate(argThat(new ArgumentMatcher<DefaultIssue>() {
      @Override
      public boolean matches(Object o) {
        DefaultIssue dead = (DefaultIssue) o;
        return "ABCDE".equals(dead.key()) && !dead.isNew() && !dead.isAlive();
      }
    }));
  }
}
