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
package org.sonar.server.computation.task.projectanalysis.issue.commonrule;

import com.google.common.base.Optional;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.computation.task.projectanalysis.qualityprofile.ActiveRule;
import org.sonar.server.computation.task.projectanalysis.qualityprofile.ActiveRulesHolder;
import org.sonar.server.rule.CommonRuleKeys;

import static java.lang.String.format;

public class CommentDensityRule extends CommonRule {

  private final MeasureRepository measureRepository;
  private final Metric commentDensityMetric;
  private final Metric commentLinesMetric;
  private final Metric nclocMetric;

  public CommentDensityRule(ActiveRulesHolder activeRulesHolder, MeasureRepository measureRepository, MetricRepository metricRepository) {
    super(activeRulesHolder, CommonRuleKeys.INSUFFICIENT_COMMENT_DENSITY);
    this.measureRepository = measureRepository;
    this.commentDensityMetric = metricRepository.getByKey(CoreMetrics.COMMENT_LINES_DENSITY_KEY);
    this.commentLinesMetric = metricRepository.getByKey(CoreMetrics.COMMENT_LINES_KEY);
    this.nclocMetric = metricRepository.getByKey(CoreMetrics.NCLOC_KEY);
  }

  @Override
  protected CommonRuleIssue doProcessFile(Component file, ActiveRule activeRule) {
    Optional<Measure> commentDensityMeasure = measureRepository.getRawMeasure(file, commentDensityMetric);
    Optional<Measure> commentLinesMeasure = measureRepository.getRawMeasure(file, commentLinesMetric);
    Optional<Measure> nclocMeasure = measureRepository.getRawMeasure(file, nclocMetric);

    if (commentDensityMeasure.isPresent() && nclocMeasure.isPresent() && nclocMeasure.get().getIntValue() > 0) {
      // this is a small optimization to not load the minimum value when the measures are not present
      double minCommentDensity = getMinDensity(activeRule);
      if (commentDensityMeasure.get().getDoubleValue() < minCommentDensity) {
        return generateIssue(commentDensityMeasure, commentLinesMeasure, nclocMeasure, minCommentDensity);
      }
    }
    return null;
  }

  private double getMinDensity(ActiveRule activeRule) {
    double min = getMinDensityParam(activeRule, CommonRuleKeys.INSUFFICIENT_COMMENT_DENSITY_PROPERTY);
    if (min >= 100.0) {
      throw new IllegalStateException("Minimum density of rule [" + activeRule.getRuleKey() + "] is incorrect. Got [100] but must be strictly less than 100.");
    }
    return min;
  }

  private static CommonRuleIssue generateIssue(Optional<Measure> commentDensityMeasure, Optional<Measure> commentLinesMeasure,
                                               Optional<Measure> nclocMeasure, double minCommentDensity) {
    int commentLines = commentLinesMeasure.isPresent() ? commentLinesMeasure.get().getIntValue() : 0;
    int ncloc = nclocMeasure.get().getIntValue();
    int minExpectedCommentLines = (int) Math.ceil(minCommentDensity * ncloc / (100 - minCommentDensity));
    int missingCommentLines = minExpectedCommentLines - commentLines;
    if (missingCommentLines <= 0) {
      throw new IllegalStateException(format("Bug in measures of comment lines - density=%s, comment_lines= %d, ncloc=%d, threshold=%s%%", commentDensityMeasure.get()
        .getDoubleValue(), commentLines, nclocMeasure.get().getIntValue(), minCommentDensity));
    }

    // TODO declare min threshold as int but not float ?
    String message = format("%d more comment lines need to be written to reach the minimum threshold of %s%% comment density.", missingCommentLines, minCommentDensity);
    return new CommonRuleIssue(missingCommentLines, message);
  }
}
