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
package org.sonar.batch.issue.tracking;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.RequiresDB;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.issue.IssueCache;
import org.sonar.batch.scan.filesystem.InputPathCache;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.IssueChangeDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.issue.workflow.IssueWorkflow;

import java.util.Collection;

@DependsUpon(DecoratorBarriers.ISSUES_ADDED)
@DependedUpon(DecoratorBarriers.ISSUES_TRACKED)
@RequiresDB
public class IssueTrackingDecorator implements Decorator {

  private static final Logger LOG = LoggerFactory.getLogger(IssueTrackingDecorator.class);

  private final IssueCache issueCache;
  private final InitialOpenIssuesStack initialOpenIssues;
  private final IssueTracking tracking;
  private final ServerLineHashesLoader lastLineHashes;
  private final IssueHandlers handlers;
  private final IssueWorkflow workflow;
  private final IssueUpdater updater;
  private final IssueChangeContext changeContext;
  private final ResourcePerspectives perspectives;
  private final RulesProfile rulesProfile;
  private final RuleFinder ruleFinder;
  private final InputPathCache inputPathCache;
  private final Project project;

  public IssueTrackingDecorator(IssueCache issueCache, InitialOpenIssuesStack initialOpenIssues, IssueTracking tracking,
    ServerLineHashesLoader lastLineHashes,
    IssueHandlers handlers, IssueWorkflow workflow,
    IssueUpdater updater,
    Project project,
    ResourcePerspectives perspectives,
    RulesProfile rulesProfile,
    RuleFinder ruleFinder, InputPathCache inputPathCache) {
    this.issueCache = issueCache;
    this.initialOpenIssues = initialOpenIssues;
    this.tracking = tracking;
    this.lastLineHashes = lastLineHashes;
    this.handlers = handlers;
    this.workflow = workflow;
    this.updater = updater;
    this.project = project;
    this.inputPathCache = inputPathCache;
    this.changeContext = IssueChangeContext.createScan(project.getAnalysisDate());
    this.perspectives = perspectives;
    this.rulesProfile = rulesProfile;
    this.ruleFinder = ruleFinder;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @Override
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
      issues.add((DefaultIssue) issue);
    }
    issueCache.clear(resource.getEffectiveKey());
    // issues = all the issues created by rule engines during this module scan and not excluded by filters

    // all the issues that are not closed in db before starting this module scan, including manual issues
    Collection<ServerIssue> dbOpenIssues = initialOpenIssues.selectAndRemoveIssues(resource.getEffectiveKey());

    SourceHashHolder sourceHashHolder = null;
    if (ResourceUtils.isFile(resource)) {
      File sonarFile = (File) resource;
      InputFile file = inputPathCache.getFile(project.getEffectiveKey(), sonarFile.getPath());
      if (file == null) {
        throw new IllegalStateException("File " + resource + " was not found in InputPath cache");
      }
      sourceHashHolder = new SourceHashHolder((DefaultInputFile) file, lastLineHashes);
    }

    IssueTrackingResult trackingResult = tracking.track(sourceHashHolder, dbOpenIssues, issues);

    // unmatched = issues that have been resolved + issues on disabled/removed rules + manual issues
    addUnmatched(trackingResult.unmatched(), sourceHashHolder, issues);

    mergeMatched(trackingResult);

    if (ResourceUtils.isProject(resource)) {
      // issues that relate to deleted components
      addIssuesOnDeletedComponents(issues);
    }

    for (DefaultIssue issue : issues) {
      workflow.doAutomaticTransition(issue, changeContext);
      handlers.execute(issue, changeContext);
      issueCache.put(issue);
    }
  }

  @VisibleForTesting
  protected void mergeMatched(IssueTrackingResult result) {
    for (DefaultIssue issue : result.matched()) {
      IssueDto ref = ((ServerIssueFromDb) result.matching(issue)).getDto();

      // invariant fields
      issue.setKey(ref.getKee());
      issue.setCreationDate(ref.getIssueCreationDate());
      issue.setUpdateDate(ref.getIssueUpdateDate());
      issue.setCloseDate(ref.getIssueCloseDate());

      // non-persisted fields
      issue.setNew(false);
      issue.setEndOfLife(false);
      issue.setOnDisabledRule(false);
      issue.setSelectedAt(ref.getSelectedAt());

      // fields to update with old values
      issue.setActionPlanKey(ref.getActionPlanKey());
      issue.setResolution(ref.getResolution());
      issue.setStatus(ref.getStatus());
      issue.setAssignee(ref.getAssignee());
      issue.setAuthorLogin(ref.getAuthorLogin());
      issue.setTags(ref.getTags());

      if (ref.getIssueAttributes() != null) {
        issue.setAttributes(KeyValueFormat.parse(ref.getIssueAttributes()));
      }

      // populate existing changelog
      Collection<IssueChangeDto> issueChangeDtos = initialOpenIssues.selectChangelog(issue.key());
      for (IssueChangeDto issueChangeDto : issueChangeDtos) {
        issue.addChange(issueChangeDto.toFieldDiffs());
      }

      // fields to update with current values
      if (ref.isManualSeverity()) {
        issue.setManualSeverity(true);
        issue.setSeverity(ref.getSeverity());
      } else {
        updater.setPastSeverity(issue, ref.getSeverity(), changeContext);
      }
      updater.setPastLine(issue, ref.getLine());
      updater.setPastMessage(issue, ref.getMessage(), changeContext);
      updater.setPastEffortToFix(issue, ref.getEffortToFix(), changeContext);
      Long debtInMinutes = ref.getDebt();
      Duration previousTechnicalDebt = debtInMinutes != null ? Duration.create(debtInMinutes) : null;
      updater.setPastTechnicalDebt(issue, previousTechnicalDebt, changeContext);
      updater.setPastProject(issue, ref.getProjectKey(), changeContext);
    }
  }

  private void addUnmatched(Collection<ServerIssue> unmatchedIssues, SourceHashHolder sourceHashHolder, Collection<DefaultIssue> issues) {
    for (ServerIssue unmatchedIssue : unmatchedIssues) {
      IssueDto unmatchedDto = ((ServerIssueFromDb) unmatchedIssue).getDto();
      DefaultIssue unmatched = unmatchedDto.toDefaultIssue();
      if (StringUtils.isNotBlank(unmatchedDto.getReporter()) && !Issue.STATUS_CLOSED.equals(unmatchedDto.getStatus())) {
        relocateManualIssue(unmatched, unmatchedDto, sourceHashHolder);
      }
      updateUnmatchedIssue(unmatched, false /* manual issues can be kept open */);
      issues.add(unmatched);
    }
  }

  private void addIssuesOnDeletedComponents(Collection<DefaultIssue> issues) {
    for (IssueDto deadDto : initialOpenIssues.selectAllIssues()) {
      DefaultIssue dead = deadDto.toDefaultIssue();
      updateUnmatchedIssue(dead, true);
      issues.add(dead);
    }
    initialOpenIssues.clear();
  }

  private void updateUnmatchedIssue(DefaultIssue issue, boolean forceEndOfLife) {
    issue.setNew(false);

    boolean manualIssue = !Strings.isNullOrEmpty(issue.reporter());
    Rule rule = ruleFinder.findByKey(issue.ruleKey());
    if (manualIssue) {
      // Manual rules are not declared in Quality profiles, so no need to check ActiveRule
      boolean isRemovedRule = rule == null || Rule.STATUS_REMOVED.equals(rule.getStatus());
      issue.setEndOfLife(forceEndOfLife || isRemovedRule);
      issue.setOnDisabledRule(isRemovedRule);
    } else {
      ActiveRule activeRule = rulesProfile.getActiveRule(issue.ruleKey().repository(), issue.ruleKey().rule());
      issue.setEndOfLife(true);
      issue.setOnDisabledRule(activeRule == null || rule == null || Rule.STATUS_REMOVED.equals(rule.getStatus()));
    }
  }

  private void relocateManualIssue(DefaultIssue newIssue, IssueDto oldIssue, SourceHashHolder sourceHashHolder) {
    LOG.debug("Trying to relocate manual issue {}", oldIssue.getKee());

    Integer previousLine = oldIssue.getLine();
    if (previousLine == null) {
      LOG.debug("Cannot relocate issue at resource level");
      return;
    }

    Collection<Integer> newLinesWithSameHash = sourceHashHolder.getNewLinesMatching(previousLine);
    LOG.debug("Found the following lines with same hash: {}", newLinesWithSameHash);
    if (newLinesWithSameHash.isEmpty()) {
      if (previousLine > sourceHashHolder.getHashedSource().length()) {
        LOG.debug("Old issue line {} is out of new source, closing and removing line number", previousLine);
        newIssue.setLine(null);
        updater.setStatus(newIssue, Issue.STATUS_CLOSED, changeContext);
        updater.setResolution(newIssue, Issue.RESOLUTION_REMOVED, changeContext);
        updater.setPastLine(newIssue, previousLine);
        updater.setPastMessage(newIssue, oldIssue.getMessage(), changeContext);
        updater.setPastEffortToFix(newIssue, oldIssue.getEffortToFix(), changeContext);
      }
    } else if (newLinesWithSameHash.size() == 1) {
      Integer newLine = newLinesWithSameHash.iterator().next();
      LOG.debug("Relocating issue to line {}", newLine);

      newIssue.setLine(newLine);
      updater.setPastLine(newIssue, previousLine);
      updater.setPastMessage(newIssue, oldIssue.getMessage(), changeContext);
      updater.setPastEffortToFix(newIssue, oldIssue.getEffortToFix(), changeContext);
    }
  }
}
