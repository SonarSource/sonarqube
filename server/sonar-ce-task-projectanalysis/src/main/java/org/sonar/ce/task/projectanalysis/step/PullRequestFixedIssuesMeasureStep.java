/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.step;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.issue.fixedissues.PullRequestFixedIssueRepository;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.step.ComputationStep;

/**
 * Compute the measure for the metric {@link CoreMetrics#PULL_REQUEST_FIXED_ISSUES_KEY} that contains the number of issues that would be
 * fixed by the pull request on the target branch.
 */
public class PullRequestFixedIssuesMeasureStep implements ComputationStep {

  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;
  private final PullRequestFixedIssueRepository pullRequestFixedIssueRepository;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public PullRequestFixedIssuesMeasureStep(TreeRootHolder treeRootHolder, MetricRepository metricRepository,
                                           MeasureRepository measureRepository,
                                           PullRequestFixedIssueRepository pullRequestFixedIssueRepository,
                                           AnalysisMetadataHolder analysisMetadataHolder) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.pullRequestFixedIssueRepository = pullRequestFixedIssueRepository;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @Override
  public void execute(Context context) {
    if (analysisMetadataHolder.isPullRequest()) {
      int fixedIssuesCount = pullRequestFixedIssueRepository.getFixedIssues().size();
      measureRepository.add(treeRootHolder.getRoot(), metricRepository.getByKey(CoreMetrics.PULL_REQUEST_FIXED_ISSUES_KEY),
        Measure.newMeasureBuilder().create(fixedIssuesCount));
    }
  }

  @Override
  public String getDescription() {
    return "Compute pull request fixed issues measure";
  }
}
