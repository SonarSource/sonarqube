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
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.resources.Scopes;
import org.sonar.batch.issue.ScanIssues;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueDto;
import org.sonar.core.issue.workflow.IssueWorkflow;

import java.util.Collection;
import java.util.Set;

@DependedUpon(DecoratorBarriers.END_OF_ISSUES_UPDATES)
public class IssueTrackingDecorator implements Decorator {

  private final ScanIssues scanIssues;
  private final InitialOpenIssuesStack initialOpenIssues;
  private final IssueTracking tracking;
  private final IssueFilters filters;
  private final IssueHandlers handlers;
  private final IssueWorkflow workflow;

  public IssueTrackingDecorator(ScanIssues scanIssues, InitialOpenIssuesStack initialOpenIssues, IssueTracking tracking,
                                IssueFilters filters, IssueHandlers handlers, IssueWorkflow workflow) {
    this.scanIssues = scanIssues;
    this.initialOpenIssues = initialOpenIssues;
    this.tracking = tracking;
    this.filters = filters;
    this.handlers = handlers;
    this.workflow = workflow;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return project.isLatestAnalysis();
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (canHaveIssues(resource)) {
      // all the issues created by rule engines during this module scan
      Collection<DefaultIssue> issues = Lists.newArrayList();
      for (Issue issue : scanIssues.issues(resource.getEffectiveKey())) {
        if (filters.accept(issue)) {
          issues.add((DefaultIssue) issue);
        } else {
          scanIssues.remove(issue);
        }
      }

      // all the issues that are open in db before starting this module scan
      Collection<IssueDto> dbOpenIssues = initialOpenIssues.selectAndRemove(resource.getId());
      Set<IssueDto> unmatchedDbIssues = tracking.track(resource, dbOpenIssues, issues);
      // TODO register manual issues (isAlive=true, isNew=false) ? Or are they included in unmatchedDbIssues ?
      addUnmatched(unmatchedDbIssues, issues);

      if (ResourceUtils.isProject(resource)) {
        // issues that relate to deleted components
        addDead(issues);
      }

      for (DefaultIssue issue : issues) {
        workflow.doAutomaticTransition(issue);
        handlers.execute(issue);
        scanIssues.addOrUpdate(issue);
      }
    }
  }

  private void addUnmatched(Set<IssueDto> unmatchedDbIssues, Collection<DefaultIssue> issues) {
    for (IssueDto unmatchedDto : unmatchedDbIssues) {
      DefaultIssue unmatched = unmatchedDto.toDefaultIssue();
      unmatched.setAlive(false);
      unmatched.setNew(false);
      issues.add(unmatched);
    }
  }

  private void addDead(Collection<DefaultIssue> issues) {
    for (IssueDto deadDto : initialOpenIssues.getAllIssues()) {
      DefaultIssue dead = deadDto.toDefaultIssue();
      dead.setAlive(false);
      dead.setNew(false);
      issues.add(dead);
    }
  }

  private boolean canHaveIssues(Resource resource) {
    // TODO check existence of perspective Issuable ?
    return Scopes.isHigherThanOrEquals(resource.getScope(), Scopes.FILE);
  }
}
