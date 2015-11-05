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

import org.sonar.batch.issue.IssueTransformer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonar.api.batch.BatchSide;
import org.sonar.api.resources.Project;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.issue.IssueCache;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.report.ReportPublisher;
import org.sonar.core.util.CloseableIterator;

@BatchSide
public class IssueTransition {
  private final IssueCache issueCache;
  private final BatchComponentCache componentCache;
  private final ReportPublisher reportPublisher;
  private final Date analysisDate;
  @Nullable
  private final LocalIssueTracking localIssueTracking;

  public IssueTransition(BatchComponentCache componentCache, IssueCache issueCache, ReportPublisher reportPublisher,
    @Nullable LocalIssueTracking localIssueTracking) {
    this.componentCache = componentCache;
    this.issueCache = issueCache;
    this.reportPublisher = reportPublisher;
    this.localIssueTracking = localIssueTracking;
    this.analysisDate = ((Project) componentCache.getRoot().resource()).getAnalysisDate();
  }

  public IssueTransition(BatchComponentCache componentCache, IssueCache issueCache, ReportPublisher reportPublisher) {
    this(componentCache, issueCache, reportPublisher, null);
  }

  public void execute() {
    if (localIssueTracking != null) {
      localIssueTracking.init();
    }

    BatchReportReader reader = new BatchReportReader(reportPublisher.getReportDir());

    for (BatchComponent component : componentCache.all()) {
      trackIssues(reader, component);
    }
  }

  public void trackIssues(BatchReportReader reader, BatchComponent component) {
    // raw issues = all the issues created by rule engines during this module scan and not excluded by filters
    Set<BatchReport.Issue> rawIssues = Sets.newIdentityHashSet();
    try (CloseableIterator<BatchReport.Issue> it = reader.readComponentIssues(component.batchId())) {
      while (it.hasNext()) {
        rawIssues.add(it.next());
      }
    } catch (Exception e) {
      throw new IllegalStateException("Can't read issues for " + component.key(), e);
    }

    List<TrackedIssue> trackedIssues;
    if (localIssueTracking != null) {
      trackedIssues = localIssueTracking.trackIssues(component, rawIssues);
    } else {
      trackedIssues = Lists.newArrayList();
    }

    // Unmatched raw issues = new issues
    addUnmatchedRawIssues(component, rawIssues, trackedIssues);

    for (TrackedIssue issue : trackedIssues) {
      issueCache.put(issue);
    }
  }

  private void addUnmatchedRawIssues(BatchComponent component, Set<org.sonar.batch.protocol.output.BatchReport.Issue> rawIssues, List<TrackedIssue> trackedIssues) {
    for (BatchReport.Issue rawIssue : rawIssues) {

      TrackedIssue tracked = IssueTransformer.toTrackedIssue(component, rawIssue);
      tracked.setCreationDate(analysisDate);

      trackedIssues.add(tracked);
    }
  }

}
