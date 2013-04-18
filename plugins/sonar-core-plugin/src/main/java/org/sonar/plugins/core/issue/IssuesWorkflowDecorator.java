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
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.KeyValueFormat;
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
        addManualIssuesAndCloseResolvedOnes(openIssue, resource);
        closeResolvedStandardIssues(openIssue, issueKeys, resource);
        keepFalsePositiveIssues(openIssue, resource);
        reopenUnresolvedIssues(openIssue, resource);
      }

      if (ResourceUtils.isRootProject(resource)) {
        closeIssuesOnDeletedResources(initialOpenIssuesStack.getAllIssues(), resource);
      }
    }
  }

  private void updateIssues(Collection<DefaultIssue> newIssues) {
    for (DefaultIssue issue : newIssues) {
      moduleIssues.addOrUpdate(issue);
    }
  }

  private void addManualIssuesAndCloseResolvedOnes(IssueDto openIssue, Resource resource) {
    if (openIssue.isManualIssue()) {
      DefaultIssue issue = toIssue(openIssue, resource);
      if (Issue.STATUS_RESOLVED.equals(issue.status())) {
        close(issue);
      }
      moduleIssues.addOrUpdate(issue);
    }
  }

  private void closeResolvedStandardIssues(IssueDto openIssue, Set<String> issueKeys, Resource resource) {
    if (!openIssue.isManualIssue() && !issueKeys.contains(openIssue.getUuid())) {
      closeAndSave(openIssue, resource);
    }
  }

  private void keepFalsePositiveIssues(IssueDto openIssue, Resource resource) {
    if (!openIssue.isManualIssue() && Issue.RESOLUTION_FALSE_POSITIVE.equals(openIssue.getResolution())) {
      DefaultIssue issue = toIssue(openIssue, resource);
      issue.setResolution(openIssue.getResolution());
      issue.setStatus(openIssue.getStatus());
      issue.setUpdatedAt(new Date());
      moduleIssues.addOrUpdate(issue);
    }

  }

  private void reopenUnresolvedIssues(IssueDto openIssue, Resource resource) {
    if (Issue.STATUS_RESOLVED.equals(openIssue.getStatus()) && !Issue.RESOLUTION_FALSE_POSITIVE.equals(openIssue.getResolution())
        && !openIssue.isManualIssue()) {
      reopenAndSave(openIssue, resource);
    }
  }

  /**
   * Close issues that relate to resources that have been deleted or renamed.
   */
  private void closeIssuesOnDeletedResources(Collection<IssueDto> openIssues, Resource resource) {
    for (IssueDto openIssue : openIssues) {
      closeAndSave(openIssue, resource);
    }
  }

  private void close(DefaultIssue issue) {
    issue.setStatus(Issue.STATUS_CLOSED);
    issue.setUpdatedAt(new Date());
  }

  private void closeAndSave(IssueDto openIssue, Resource resource) {
    DefaultIssue issue = toIssue(openIssue, resource);
    close(issue);
    moduleIssues.addOrUpdate(issue);
  }

  private void reopenAndSave(IssueDto openIssue, Resource resource) {
    DefaultIssue issue = toIssue(openIssue, resource);
    issue.setStatus(Issue.STATUS_REOPENED);
    issue.setResolution(null);
    issue.setUpdatedAt(new Date());
    moduleIssues.addOrUpdate(issue);
  }

  private DefaultIssue toIssue(IssueDto dto, Resource resource) {
    DefaultIssue issue = new DefaultIssue();
    issue.setKey(dto.getUuid());
    issue.setStatus(dto.getStatus());
    issue.setResolution(dto.getResolution());
    issue.setMessage(dto.getMessage());
    issue.setTitle(dto.getTitle());
    issue.setCost(dto.getCost());
    issue.setLine(dto.getLine());
    issue.setSeverity(dto.getSeverity());
    issue.setUserLogin(dto.getUserLogin());
    issue.setAssigneeLogin(dto.getAssigneeLogin());
    issue.setCreatedAt(dto.getCreatedAt());
    issue.setUpdatedAt(dto.getUpdatedAt());
    issue.setClosedAt(dto.getClosedAt());
    issue.setAttributes(KeyValueFormat.parse(dto.getData()));
    issue.setComponentKey(resource.getKey());
    issue.setManual(dto.isManualIssue());
    issue.setManualSeverity(dto.isManualSeverity());

    // TODO add person

    Rule rule = ruleFinder.findById(dto.getRuleId());
    issue.setRuleKey(rule.ruleKey());
    return issue;
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
}
