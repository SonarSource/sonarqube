/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.task.projectanalysis.issue;

import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;

import java.util.List;
import java.util.Map;

import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Tracking;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.Component.Status;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.task.projectanalysis.filemove.MovedFilesRepository;
import org.sonar.server.util.cache.DiskCache;

import com.google.common.base.Optional;

public class IntegrateIssuesVisitor extends TypeAwareVisitorAdapter {

  private final TrackerExecution tracker;
  private final IssueCache issueCache;
  private final IssueLifecycle issueLifecycle;
  private final IssueVisitors issueVisitors;
  private final ComponentsWithUnprocessedIssues componentsWithUnprocessedIssues;
  private final MovedFilesRepository movedFilesRepository;
  private final BaseIssuesLoader baseIssuesLoader;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public IntegrateIssuesVisitor(TrackerExecution tracker, IssueCache issueCache, IssueLifecycle issueLifecycle, IssueVisitors issueVisitors,
    ComponentsWithUnprocessedIssues componentsWithUnprocessedIssues, MovedFilesRepository movedFilesRepository, BaseIssuesLoader baseIssuesLoader, 
    AnalysisMetadataHolder analysisMetadataHolder) {
    super(CrawlerDepthLimit.FILE, POST_ORDER);
    this.tracker = tracker;
    this.issueCache = issueCache;
    this.issueLifecycle = issueLifecycle;
    this.issueVisitors = issueVisitors;
    this.componentsWithUnprocessedIssues = componentsWithUnprocessedIssues;
    this.movedFilesRepository = movedFilesRepository;
    this.baseIssuesLoader = baseIssuesLoader;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @Override
  public void visitAny(Component component) {
    processIssues(component);

    componentsWithUnprocessedIssues.remove(component.getUuid());
    Optional<MovedFilesRepository.OriginalFile> originalFile = movedFilesRepository.getOriginalFile(component);
    if (originalFile.isPresent()) {
      componentsWithUnprocessedIssues.remove(originalFile.get().getUuid());
    }
  }

  private void processIssues(Component component) {
    DiskCache<DefaultIssue>.DiskAppender cacheAppender = issueCache.newAppender();
    try {
      issueVisitors.beforeComponent(component);
      if (isIncremental(component)) {
        fillIncrementalOpenIssues(component, cacheAppender);
      } else {
        Tracking<DefaultIssue, DefaultIssue> tracking = tracker.track(component);
        fillNewOpenIssues(component, tracking, cacheAppender);
        fillExistingOpenIssues(component, tracking, cacheAppender);
        closeUnmatchedBaseIssues(component, tracking, cacheAppender);
      }
      issueVisitors.afterComponent(component);
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Fail to process issues of component '%s'", component.getKey()), e);
    } finally {
      cacheAppender.close();
    }
  }

  private boolean isIncremental(Component component) {
    return analysisMetadataHolder.isIncrementalAnalysis() && component.getStatus() == Status.SAME;
  }

  private void fillNewOpenIssues(Component component, Tracking<DefaultIssue, DefaultIssue> tracking, DiskCache<DefaultIssue>.DiskAppender cacheAppender) {
    for (DefaultIssue issue : tracking.getUnmatchedRaws()) {
      issueLifecycle.initNewOpenIssue(issue);
      process(component, issue, cacheAppender);
    }
  }

  private void fillIncrementalOpenIssues(Component component, DiskCache<DefaultIssue>.DiskAppender cacheAppender) {
    List<DefaultIssue> issues = baseIssuesLoader.loadForComponentUuid(component.getUuid());

    for (DefaultIssue issue : issues) {
      issueLifecycle.updateExistingOpenissue(issue);
      process(component, issue, cacheAppender);
    }
  }

  private void fillExistingOpenIssues(Component component, Tracking<DefaultIssue, DefaultIssue> tracking, DiskCache<DefaultIssue>.DiskAppender cacheAppender) {
    for (Map.Entry<DefaultIssue, DefaultIssue> entry : tracking.getMatchedRaws().entrySet()) {
      DefaultIssue raw = entry.getKey();
      DefaultIssue base = entry.getValue();
      issueLifecycle.mergeExistingOpenIssue(raw, base);
      process(component, raw, cacheAppender);
    }
  }

  private void closeUnmatchedBaseIssues(Component component, Tracking<DefaultIssue, DefaultIssue> tracking, DiskCache<DefaultIssue>.DiskAppender cacheAppender) {
    for (DefaultIssue issue : tracking.getUnmatchedBases()) {
      // TODO should replace flag "beingClosed" by express call to transition "automaticClose"
      issue.setBeingClosed(true);
      // TODO manual issues -> was updater.setResolution(newIssue, Issue.RESOLUTION_REMOVED, changeContext);. Is it a problem ?
      process(component, issue, cacheAppender);
    }
  }

  private void process(Component component, DefaultIssue issue, DiskCache<DefaultIssue>.DiskAppender cacheAppender) {
    issueLifecycle.doAutomaticTransition(issue);
    issueVisitors.onIssue(component, issue);
    cacheAppender.append(issue);
  }

}
