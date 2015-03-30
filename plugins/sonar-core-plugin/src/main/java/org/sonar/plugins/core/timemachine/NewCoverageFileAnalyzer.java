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
package org.sonar.plugins.core.timemachine;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.report.ReportPublisher;

import java.util.List;

public class NewCoverageFileAnalyzer extends AbstractNewCoverageFileAnalyzer {

  public NewCoverageFileAnalyzer(TimeMachineConfiguration timeMachineConfiguration, ReportPublisher publishReportJob, ResourceCache resourceCache) {
    super(timeMachineConfiguration, publishReportJob, resourceCache);
  }

  NewCoverageFileAnalyzer(List<PeriodStruct> structs, ReportPublisher publishReportJob, ResourceCache resourceCache) {
    super(structs, publishReportJob, resourceCache);
  }

  @Override
  public Metric getCoverageLineHitsDataMetric() {
    return CoreMetrics.COVERAGE_LINE_HITS_DATA;
  }

  @Override
  public Metric getConditionsByLineMetric() {
    return CoreMetrics.CONDITIONS_BY_LINE;
  }

  @Override
  public Metric getCoveredConditionsByLineMetric() {
    return CoreMetrics.COVERED_CONDITIONS_BY_LINE;
  }

  @Override
  public Metric getNewLinesToCoverMetric() {
    return CoreMetrics.NEW_LINES_TO_COVER;
  }

  @Override
  public Metric getNewUncoveredLinesMetric() {
    return CoreMetrics.NEW_UNCOVERED_LINES;
  }

  @Override
  public Metric getNewConditionsToCoverMetric() {
    return CoreMetrics.NEW_CONDITIONS_TO_COVER;
  }

  @Override
  public Metric getNewUncoveredConditionsMetric() {
    return CoreMetrics.NEW_UNCOVERED_CONDITIONS;
  }
}
