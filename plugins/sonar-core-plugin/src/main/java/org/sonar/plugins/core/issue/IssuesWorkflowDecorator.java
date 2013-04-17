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
import com.google.common.collect.Sets;
import org.sonar.api.batch.*;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.issue.InitialOpenIssuesStack;
import org.sonar.batch.issue.ModuleIssues;
import org.sonar.core.DryRunIncompatible;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueDto;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

@DryRunIncompatible
@DependsUpon(DecoratorBarriers.END_OF_VIOLATION_TRACKING)
@DependedUpon(IssuesWorkflowDecorator.END_OF_ISSUES_UPDATES)
public class IssuesWorkflowDecorator implements Decorator {

  public static final String END_OF_ISSUES_UPDATES = "END_OF_ISSUES_UPDATES";

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
    Collection<Issue> newIssues = moduleIssues.issues(resource.getEffectiveKey());
    Collection<IssueDto> openIssues = initialOpenIssuesStack.selectAndRemove(resource.getId());
    if (!openIssues.isEmpty()) {
      issueTracking.track(resource, openIssues, (List) newIssues);
      updateIssues(newIssues);

      addManualIssues(openIssues);
      closeResolvedStandardIssues(openIssues, newIssues, resource);
      closeResolvedManualIssues(openIssues, resource);
      reopenUnresolvedIssues(openIssues, resource);
    }
    if (ResourceUtils.isRootProject(resource)) {
      closeIssuesOnDeletedResources(openIssues, resource);
    }
  }

  private void updateIssues(Collection<Issue> newIssues){
    for (Issue issue : newIssues){
      moduleIssues.addOrUpdate((DefaultIssue)issue);
    }
  }

  private void addManualIssues(Collection<IssueDto> openIssues) {
    for (IssueDto openIssue : openIssues) {
      if (openIssue.isManualIssue()) {
        DefaultIssue newIssue = new DefaultIssue();
        moduleIssues.addOrUpdate(newIssue);
      }
    }
  }

  /**
   * Close issues that relate to resources that have been deleted or renamed.
   */
  private void closeIssuesOnDeletedResources(Collection<IssueDto> openIssues, Resource resource) {
    for (IssueDto openIssue : openIssues) {
      close(openIssue, resource);
    }
  }

  private void closeResolvedManualIssues(Collection<IssueDto> openIssues, Resource resource) {
    for (IssueDto openIssue : openIssues) {
      if (openIssue.isManualIssue() && Issue.STATUS_RESOLVED.equals(openIssue.getStatus())) {
        close(openIssue, resource);
      }
    }
  }

  private void closeResolvedStandardIssues(Collection<IssueDto> openIssues, Collection<Issue> issues, Resource resource) {
    Set<String> issueKeys = Sets.newHashSet(Collections2.transform(issues, new IssueToKeyfunction()));

    for (IssueDto openIssue : openIssues) {
      if (!openIssue.isManualIssue() && !issueKeys.contains(openIssue.getUuid())) {
        close(openIssue, resource);
      }
    }
  }

  private void reopenUnresolvedIssues(Collection<IssueDto> openIssues, Resource resource) {
    for (IssueDto openIssue : openIssues) {
      if (Issue.STATUS_RESOLVED.equals(openIssue.getStatus()) && !Issue.RESOLUTION_FALSE_POSITIVE.equals(openIssue.getResolution())
          && !openIssue.isManualIssue()) {
        reopen(openIssue, resource);
      }
    }
  }

  private void close(IssueDto openIssue, Resource resource) {
    DefaultIssue issue = toIssue(openIssue, resource);
    issue.setStatus(Issue.STATUS_CLOSED);
    issue.setUpdatedAt(new Date());
    moduleIssues.addOrUpdate(issue);
  }

  private void reopen(IssueDto openIssue, Resource resource) {
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

    Rule rule = ruleFinder.findById(dto.getRuleId());
    issue.setRuleKey(rule.getKey());
    issue.setRuleRepositoryKey(rule.getRepositoryKey());
    return issue;
  }

  private static final class IssueToKeyfunction implements Function<Issue, String> {
    public String apply(@Nullable Issue issue) {
      return (issue != null ? issue.key() : null);
    }
  }
}
