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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rules.RuleFinder;
import org.sonar.batch.issue.InitialOpenIssuesStack;
import org.sonar.batch.issue.ModuleIssues;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueDto;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;

@DependedUpon(DecoratorBarriers.END_OF_ISSUES_UPDATES)
public class IssuesWorkflowDecorator implements Decorator {

  private final ModuleIssues moduleIssues;
  private final InitialOpenIssuesStack initialOpenIssuesStack;
  private final IssueTracking issueTracking;
  private final RuleFinder ruleFinder;

  public IssuesWorkflowDecorator(ModuleIssues moduleIssues, InitialOpenIssuesStack initialOpenIssuesStack, IssueTracking issueTracking, RuleFinder ruleFinder) {
    this.moduleIssues = moduleIssues;
    this.initialOpenIssuesStack = initialOpenIssuesStack;
    this.issueTracking = issueTracking;
    this.ruleFinder = ruleFinder;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return project.isLatestAnalysis();
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (isComponentSupported(resource)) {
      Collection<Issue> newIssues = moduleIssues.issues(resource.getEffectiveKey());
      Collection<IssueDto> openIssues = initialOpenIssuesStack.selectAndRemove(resource.getId());

      Collection<DefaultIssue> newDefaultIssues = toDefaultIssues(newIssues);
      issueTracking.track(resource, openIssues, newDefaultIssues);
      updateIssues(newDefaultIssues);

      Set<String> issueKeys = Sets.newHashSet(Collections2.transform(newIssues, new IssueToKeyfunction()));
      for (IssueDto openIssue : openIssues) {
        addManualIssuesAndCloseResolvedOnes(openIssue);
        closeResolvedStandardIssues(openIssue, issueKeys);
        keepFalsePositiveIssues(openIssue);
        reopenUnresolvedIssues(openIssue);
      }

      if (ResourceUtils.isRootProject(resource)) {
        closeIssuesOnDeletedResources(initialOpenIssuesStack.getAllIssues());
      }
    }
  }

  private void updateIssues(Collection<DefaultIssue> newIssues) {
    for (DefaultIssue issue : newIssues) {
      moduleIssues.addOrUpdate(issue);
    }
  }

  private void addManualIssuesAndCloseResolvedOnes(IssueDto openIssue) {
    if (openIssue.isManualIssue()) {
      DefaultIssue issue = openIssue.toDefaultIssue();
      if (Issue.STATUS_RESOLVED.equals(issue.status())) {
        close(issue);
      }
      moduleIssues.addOrUpdate(issue);
    }
  }

  private void closeResolvedStandardIssues(IssueDto openIssue, Set<String> issueKeys) {
    if (!openIssue.isManualIssue() && !issueKeys.contains(openIssue.getUuid())) {
      closeAndSave(openIssue);
    }
  }

  private void keepFalsePositiveIssues(IssueDto openIssue) {
    if (!openIssue.isManualIssue() && Issue.RESOLUTION_FALSE_POSITIVE.equals(openIssue.getResolution())) {
      DefaultIssue issue = openIssue.toDefaultIssue();
      issue.setResolution(openIssue.getResolution());
      issue.setStatus(openIssue.getStatus());
      issue.setUpdatedAt(getLoadedDate());
      moduleIssues.addOrUpdate(issue);
    }

  }

  private void reopenUnresolvedIssues(IssueDto openIssue) {
    if (Issue.STATUS_RESOLVED.equals(openIssue.getStatus()) && !Issue.RESOLUTION_FALSE_POSITIVE.equals(openIssue.getResolution())
      && !openIssue.isManualIssue()) {
      reopenAndSave(openIssue);
    }
  }

  /**
   * Close issues that relate to resources that have been deleted or renamed.
   */
  private void closeIssuesOnDeletedResources(Collection<IssueDto> openIssues) {
    for (IssueDto openIssue : openIssues) {
      closeAndSave(openIssue);
    }
  }

  private void close(DefaultIssue issue) {
    issue.setStatus(Issue.STATUS_CLOSED);
    issue.setUpdatedAt(getLoadedDate());
    issue.setClosedAt(getLoadedDate());
  }

  private void closeAndSave(IssueDto openIssue) {
    DefaultIssue issue = openIssue.toDefaultIssue();
    close(issue);
    moduleIssues.addOrUpdate(issue);
  }

  private void reopenAndSave(IssueDto openIssue) {
    DefaultIssue issue = openIssue.toDefaultIssue();
    issue.setStatus(Issue.STATUS_REOPENED);
    issue.setResolution(null);
    issue.setUpdatedAt(getLoadedDate());
    moduleIssues.addOrUpdate(issue);
  }


  private Collection<DefaultIssue> toDefaultIssues(Collection<Issue> issues) {
    return newArrayList(Iterables.transform(issues, new Function<Issue, DefaultIssue>() {
      @Override
      public DefaultIssue apply(Issue issue) {
        return (DefaultIssue) issue;
      }
    }));
  }

  private boolean isComponentSupported(Resource resource) {
    return Scopes.isHigherThanOrEquals(resource.getScope(), Scopes.FILE);
  }

  private static final class IssueToKeyfunction implements Function<Issue, String> {
    public String apply(@Nullable Issue issue) {
      return (issue != null ? issue.key() : null);
    }
  }

  private Date getLoadedDate(){
    return initialOpenIssuesStack.getLoadedDate();
  }
}
