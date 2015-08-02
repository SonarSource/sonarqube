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

package org.sonar.server.computation.source;

import java.util.Iterator;
import javax.annotation.CheckForNull;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.db.protobuf.DbFileSources;

public class CoverageLineReader implements LineReader {

  private final Iterator<BatchReport.Coverage> coverageIterator;
  private BatchReport.Coverage coverage;

  public CoverageLineReader(Iterator<BatchReport.Coverage> coverageIterator) {
    this.coverageIterator = coverageIterator;
  }

  @Override
  public void read(DbFileSources.Line.Builder lineBuilder) {
    BatchReport.Coverage reportCoverage = getNextLineCoverageIfMatchLine(lineBuilder.getLine());
    if (reportCoverage != null) {
      processUnitTest(lineBuilder, reportCoverage);
      processIntegrationTest(lineBuilder, reportCoverage);
      processOverallTest(lineBuilder, reportCoverage);
      coverage = null;
    }
  }

  private static void processUnitTest(DbFileSources.Line.Builder lineBuilder, BatchReport.Coverage reportCoverage) {
    if (reportCoverage.hasUtHits()) {
      lineBuilder.setUtLineHits(reportCoverage.getUtHits() ? 1 : 0);
    }
    if (reportCoverage.hasConditions() && reportCoverage.hasUtCoveredConditions()) {
      lineBuilder.setUtConditions(reportCoverage.getConditions());
      lineBuilder.setUtCoveredConditions(reportCoverage.getUtCoveredConditions());
    }
  }

  private static void processIntegrationTest(DbFileSources.Line.Builder lineBuilder, BatchReport.Coverage reportCoverage) {
    if (reportCoverage.hasItHits()) {
      lineBuilder.setItLineHits(reportCoverage.getItHits() ? 1 : 0);
    }
    if (reportCoverage.hasConditions() && reportCoverage.hasItCoveredConditions()) {
      lineBuilder.setItConditions(reportCoverage.getConditions());
      lineBuilder.setItCoveredConditions(reportCoverage.getItCoveredConditions());
    }
  }

  private static void processOverallTest(DbFileSources.Line.Builder lineBuilder, BatchReport.Coverage reportCoverage) {
    if (reportCoverage.hasUtHits() || reportCoverage.hasItHits()) {
      lineBuilder.setOverallLineHits((reportCoverage.getUtHits() || reportCoverage.getItHits()) ? 1 : 0);
    }
    if (reportCoverage.hasConditions() && reportCoverage.hasOverallCoveredConditions()) {
      lineBuilder.setOverallConditions(reportCoverage.getConditions());
      lineBuilder.setOverallCoveredConditions(reportCoverage.getOverallCoveredConditions());
    }
  }

  @CheckForNull
  private BatchReport.Coverage getNextLineCoverageIfMatchLine(int line) {
    // Get next element (if exists)
    if (coverage == null && coverageIterator.hasNext()) {
      coverage = coverageIterator.next();
    }
    // Return current element if lines match
    if (coverage != null && coverage.getLine() == line) {
      return coverage;
    }
    return null;
  }

}
