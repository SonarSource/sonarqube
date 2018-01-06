/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.issue.tracking;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.internal.DefaultInputComponent;
import org.sonar.core.util.CloseableIterator;
import org.sonar.scanner.ProjectAnalysisInfo;
import org.sonar.scanner.issue.IssueCache;
import org.sonar.scanner.issue.IssueTransformer;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.scanner.scan.filesystem.InputComponentStore;
import org.sonar.scanner.util.ProgressReport;

@ScannerSide
public class IssueTransition {
  private final IssueCache issueCache;
  private final InputComponentStore inputComponentStore;
  private final ReportPublisher reportPublisher;
  private final Date analysisDate;
  @Nullable
  private final LocalIssueTracking localIssueTracking;

  public IssueTransition(InputComponentStore inputComponentCache, ProjectAnalysisInfo projectAnalysisInfo, IssueCache issueCache, ReportPublisher reportPublisher,
    @Nullable LocalIssueTracking localIssueTracking) {
    this.inputComponentStore = inputComponentCache;
    this.issueCache = issueCache;
    this.reportPublisher = reportPublisher;
    this.localIssueTracking = localIssueTracking;
    this.analysisDate = projectAnalysisInfo.analysisDate();
  }

  public IssueTransition(InputComponentStore inputComponentCache, ProjectAnalysisInfo projectAnalysisInfo, IssueCache issueCache, ReportPublisher reportPublisher) {
    this(inputComponentCache, projectAnalysisInfo, issueCache, reportPublisher, null);
  }

  public void execute() {
    if (localIssueTracking != null) {
      localIssueTracking.init();
    }

    ScannerReportReader reader = new ScannerReportReader(reportPublisher.getReportDir().toFile());
    int nbComponents = inputComponentStore.all().size();

    if (nbComponents == 0) {
      return;
    }

    ProgressReport progressReport = new ProgressReport("issue-tracking-report", TimeUnit.SECONDS.toMillis(10));
    progressReport.start("Performing issue tracking");
    int count = 0;

    try {
      for (InputComponent component : inputComponentStore.all()) {
        trackIssues(reader, (DefaultInputComponent) component);
        count++;
        progressReport.message(count + "/" + nbComponents + " components tracked");
      }
    } finally {
      progressReport.stop(count + "/" + nbComponents + " components tracked");
    }
  }

  public void trackIssues(ScannerReportReader reader, DefaultInputComponent component) {
    // raw issues = all the issues created by rule engines during this module scan and not excluded by filters
    List<ScannerReport.Issue> rawIssues = new LinkedList<>();
    try (CloseableIterator<ScannerReport.Issue> it = reader.readComponentIssues(component.batchId())) {
      while (it.hasNext()) {
        rawIssues.add(it.next());
      }
    } catch (Exception e) {
      throw new IllegalStateException("Can't read issues for " + component.key(), e);
    }

    List<TrackedIssue> trackedIssues;
    if (localIssueTracking != null) {
      trackedIssues = localIssueTracking.trackIssues(component, rawIssues, analysisDate);
    } else {
      trackedIssues = doTransition(rawIssues, component);
    }

    for (TrackedIssue issue : trackedIssues) {
      issueCache.put(issue);
    }
  }

  private static List<TrackedIssue> doTransition(List<ScannerReport.Issue> rawIssues, InputComponent component) {
    List<TrackedIssue> issues = new ArrayList<>(rawIssues.size());

    for (ScannerReport.Issue issue : rawIssues) {
      issues.add(IssueTransformer.toTrackedIssue(component, issue, null));
    }

    return issues;
  }

}
