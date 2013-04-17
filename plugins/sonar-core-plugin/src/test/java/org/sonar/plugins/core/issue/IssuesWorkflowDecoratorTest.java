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
package org.sonar.plugins.core.issue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;
import org.sonar.api.violations.ViolationQuery;
import org.sonar.batch.issue.InitialOpenIssuesStack;
import org.sonar.batch.issue.ModuleIssues;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueDto;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Collections;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IssuesWorkflowDecoratorTest extends AbstractDaoTestCase {

  private IssuesWorkflowDecorator decorator;

  private ModuleIssues moduleIssues;
  private InitialOpenIssuesStack initialOpenIssuesStack;
  private IssueTracking issueTracking;
  private RuleFinder ruleFinder;

  @Before
  public void init() {
    moduleIssues = mock(ModuleIssues.class);
    initialOpenIssuesStack = mock(InitialOpenIssuesStack.class);
    issueTracking = mock(IssueTracking.class);
    ruleFinder = mock(RuleFinder.class);

    decorator = new IssuesWorkflowDecorator(moduleIssues, initialOpenIssuesStack, issueTracking, ruleFinder);
  }

  @Test
  public void should_execute_on_project() {
    Project project = mock(Project.class);
    when(project.isLatestAnalysis()).thenReturn(true);
    assertTrue(decorator.shouldExecuteOnProject(project));
  }

  @Test
  public void should_execute_on_project_not_if_past_inspection() {
    Project project = mock(Project.class);
    when(project.isLatestAnalysis()).thenReturn(false);
    assertFalse(decorator.shouldExecuteOnProject(project));
  }

  @Test
  @Ignore
  public void shouldCloseIssuesOnResolvedViolations() {
    //setupData("shouldCloseReviewsOnResolvedViolations");

    when(moduleIssues.issues(anyString())).thenReturn(newArrayList((Issue) new DefaultIssue().setKey("111").setComponentKey("key")));
    when(initialOpenIssuesStack.selectAndRemove(anyInt())).thenReturn(newArrayList(new IssueDto().setUuid("111").setResourceId(1)));

    Resource resource = new JavaFile("key");
    decorator.decorate(resource, null);

    //checkTables("shouldCloseReviewsOnResolvedViolations", new String[] {"updated_at"}, "reviews");
  }

  @Test
  @Ignore
  public void shouldCloseResolvedManualViolations() {
    setupData("shouldCloseResolvedManualViolations");
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getViolations(any(ViolationQuery.class))).thenReturn(Collections.<Violation>emptyList());

    Resource resource = new JavaFile("org.foo.Bar");
    decorator.decorate(resource, context);

    checkTables("shouldCloseResolvedManualViolations", new String[] {"updated_at"}, "reviews");
  }

  @Test
  @Ignore
  public void shouldReopenViolations() {
    setupData("shouldReopenViolations");
    DecoratorContext context = mock(DecoratorContext.class);
    Violation violation = new Violation(new Rule());
    violation.setPermanentId(1000);
    when(context.getViolations(any(ViolationQuery.class))).thenReturn(newArrayList(violation));

    Resource resource = new JavaFile("org.foo.Bar");
    decorator.decorate(resource, context);

    checkTables("shouldReopenViolations", new String[] {"updated_at"}, "reviews");
  }

}
