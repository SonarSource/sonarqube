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

package org.sonar.server.computation.step;

import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.computation.analysis.MutableAnalysisMetadataHolder;
import org.sonar.server.computation.batch.BatchReportReader;

/**
 * Feed analysis metadata holder with metadata from the analysis report.
 */
public class LoadReportAnalysisMetadataHolderStep implements ComputationStep {

  private final BatchReportReader reportReader;
  private final MutableAnalysisMetadataHolder mutableAnalysisMetadataHolder;

  public LoadReportAnalysisMetadataHolderStep(BatchReportReader reportReader, MutableAnalysisMetadataHolder mutableAnalysisMetadataHolder) {
    this.reportReader = reportReader;
    this.mutableAnalysisMetadataHolder = mutableAnalysisMetadataHolder;
  }

  @Override
  public void execute() {
    BatchReport.Metadata reportMetadata = reportReader.readMetadata();
    mutableAnalysisMetadataHolder.setRootComponentRef(reportMetadata.getRootComponentRef());
    mutableAnalysisMetadataHolder.setBranch(reportMetadata.hasBranch() ? reportMetadata.getBranch() : null);
    mutableAnalysisMetadataHolder.setAnalysisDate(reportMetadata.getAnalysisDate());
    mutableAnalysisMetadataHolder.setCrossProjectDuplicationEnabled(reportMetadata.hasCrossProjectDuplicationActivated() && reportMetadata.getCrossProjectDuplicationActivated());
  }

  @Override
  public String getDescription() {
    return "Load analysis metadata";
  }
}
