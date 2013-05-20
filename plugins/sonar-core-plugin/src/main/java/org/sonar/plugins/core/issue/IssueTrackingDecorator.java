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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.issue.IssueCache;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.issue.workflow.IssueWorkflow;

import java.util.Collection;

@DependedUpon(DecoratorBarriers.END_OF_ISSUES_UPDATES)
public class IssueTrackingDecorator implements Decorator {

  private final IssueCache issueCache;
  private final InitialOpenIssuesStack initialOpenIssues;
  private final IssueTracking tracking;
  private final IssueFilters filters;
  private final IssueHandlers handlers;
  private final IssueWorkflow workflow;
  private final IssueUpdater updater;
  private final IssueChangeContext changeContext;
  private final ResourcePerspectives perspectives;
  private final RuleFinder ruleFinder;

  public IssueTrackingDecorator(IssueCache issueCache, InitialOpenIssuesStack initialOpenIssues, IssueTracking tracking,
                                IssueFilters filters, IssueHandlers handlers, IssueWorkflow workflow,
                                IssueUpdater updater,
                                Project project, ResourcePerspectives perspectives,
                                RuleFinder ruleFinder) {
    this.issueCache = issueCache;
    this.initialOpenIssues = initialOpenIssues;
    this.tracking = tracking;
    this.filters = filters;
    this.handlers = handlers;
    this.workflow = workflow;
    this.updater = updater;
    this.changeContext = IssueChangeContext.createScan(project.getAnalysisDate());
    this.perspectives = perspectives;
    this.ruleFinder = ruleFinder;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return project.isLatestAnalysis();
  }

  public void decorate(Resource resource, DecoratorContext context) {
    Issuable issuable = perspectives.as(Issuable.class, resource);
    if (issuable != null) {
      doDecorate(resource);

    }
  }

  @VisibleForTesting
  void doDecorate(Resource resource) {
    Collection<DefaultIssue> issues = Lists.newArrayList();
    for (Issue issue : issueCache.byComponent(resource.getEffectiveKey())) {
      issueCache.remove(issue);
      if (filters.accept(issue)) {
        issues.add((DefaultIssue) issue);
      }
    }
    // issues = all the issues created by rule engines during this module scan and not excluded by filters

    // all the issues that are not closed in db before starting this module scan, including manual issues
    Collection<IssueDto> dbOpenIssues = initialOpenIssues.selectAndRemove(resource.getId());

    IssueTrackingResult trackingResult = tracking.track(resource, dbOpenIssues, issues);

    // unmatched = issues that have been resolved + issues on disabled/removed rules + manual issues
    addUnmatched(trackingResult.unmatched(), issues);

    mergeMatched(trackingResult);

    if (ResourceUtils.isProject(resource)) {
      // issues that relate to deleted components
      addDead(issues);
    }

    for (DefaultIssue issue : issues) {
      workflow.doAutomaticTransition(issue, changeContext);
      handlers.execute(issue, changeContext);
      issueCache.put(issue);
    }
  }

  private void mergeMatched(IssueTrackingResult result) {
    for (DefaultIssue issue : result.matched()) {
      IssueDto ref = result.matching(issue);

      issue.setKey(ref.getKee());
      issue.setResolution(ref.getResolution());
      issue.setStatus(ref.getStatus());
      issue.setNew(false);
      issue.setAlive(true);
      issue.setAssignee(ref.getAssignee());
      issue.setAuthorLogin(ref.getAuthorLogin());
      issue.setAssignee(ref.getAssignee());
      if (ref.getAttributes() != null) {
        issue.setAttributes(KeyValueFormat.parse(ref.getAttributes()));
      }
      issue.setCreationDate(ref.getIssueCreationDate());

      // must be done before the change of severity
      issue.setUpdateDate(ref.getIssueUpdateDate());

      // should be null
      issue.setCloseDate(ref.getIssueCloseDate());

      if (ref.isManualSeverity()) {
        issue.setManualSeverity(true);
        issue.setSeverity(ref.getSeverity());
      } else {
        // Emulate change of severity in the current scan.
        String severity = issue.severity();
        issue.setSeverity(ref.getSeverity());
        updater.setSeverity(issue, severity, changeContext);
      }
    }
  }

  private void addUnmatched(Collection<IssueDto> unmatchedIssues, Collection<DefaultIssue> issues) {
    for (IssueDto unmatchedDto : unmatchedIssues) {
      DefaultIssue unmatched = unmatchedDto.toDefaultIssue();
      unmatched.setNew(false);

      Rule rule = ruleFinder.findByKey(unmatched.ruleKey());
      boolean manualIssue = !Strings.isNullOrEmpty(unmatched.reporter());
      boolean onExistingRule = (rule != null && !Rule.STATUS_REMOVED.equals(rule.getStatus()));
      unmatched.setAlive(manualIssue && onExistingRule);

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
}
