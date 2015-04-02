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

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.resources.Qualifiers;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.source.ReportIterator;
import org.sonar.server.source.db.FileSourceDb;

import java.io.File;

/**
 * Nothing is persist for the moment. Only Coverage are read and not persist for the moment
 */
public class PersistCoverageStep implements ComputationStep {

  // Temporary variable in order to be able to test that coverage are well computed. Will only contains data from last processed file
  private FileSourceDb.Data fileSourceData;

  @Override
  public String[] supportedProjectQualifiers() {
    return new String[] {Qualifiers.PROJECT};
  }

  @Override
  public void execute(ComputationContext context) {
    int rootComponentRef = context.getReportMetadata().getRootComponentRef();
    recursivelyProcessComponent(context, rootComponentRef);
  }

  private void recursivelyProcessComponent(ComputationContext context, int componentRef) {
    BatchReportReader reportReader = context.getReportReader();
    BatchReport.Component component = reportReader.readComponent(componentRef);
    if (component.getType().equals(Constants.ComponentType.FILE)) {
      File coverageFile = reportReader.readFileCoverage(componentRef);
      if (coverageFile != null) {
        ReportIterator<BatchReport.Coverage> coverageReport = new ReportIterator<>(coverageFile, BatchReport.Coverage.PARSER);
        processCoverage(component, coverageReport);
      }
    }

    for (Integer childRef : component.getChildRefList()) {
      recursivelyProcessComponent(context, childRef);
    }
  }

  private void processCoverage(BatchReport.Component component, ReportIterator<BatchReport.Coverage> coverageReport) {
    fileSourceData = null;
    FileSourceDb.Data.Builder dataBuilder = FileSourceDb.Data.newBuilder();
    while (coverageReport.hasNext()) {
      BatchReport.Coverage coverage = coverageReport.next();
      FileSourceDb.Line.Builder lineBuilder = dataBuilder.addLinesBuilder().setLine(coverage.getLine());
      processLineCoverage(coverage.getLine(), lineBuilder, coverage);
    }
    fileSourceData = dataBuilder.build();
  }

  private void processLineCoverage(int line, FileSourceDb.Line.Builder lineBuilder, BatchReport.Coverage coverage) {
    // Unit test
    if (coverage.getUtHits()) {
      lineBuilder.setUtLineHits(1);
    }
    lineBuilder.setUtConditions(coverage.getConditions());
    lineBuilder.setUtCoveredConditions(coverage.getUtCoveredConditions());

    // Integration test
    if (coverage.getItHits()) {
      lineBuilder.setItLineHits(1);
    }
    lineBuilder.setItConditions(coverage.getConditions());
    lineBuilder.setItCoveredConditions(coverage.getItCoveredConditions());

    // Overall test
    if (coverage.getUtHits() || coverage.getItHits()) {
      lineBuilder.setOverallLineHits(1);
    }
    lineBuilder.setOverallConditions(coverage.getConditions());
    lineBuilder.setOverallCoveredConditions(coverage.getOverallCoveredConditions());
  }

  @VisibleForTesting
  FileSourceDb.Data getFileSourceData() {
    return fileSourceData;
  }

  @Override
  public String getDescription() {
    return "Read Coverage";
  }
}
