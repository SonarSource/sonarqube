/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.source;

import java.util.Iterator;
import javax.annotation.CheckForNull;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.scanner.protocol.output.ScannerReport;

public class CoverageLineReader implements LineReader {

  private final Iterator<ScannerReport.LineCoverage> coverageIterator;
  private ScannerReport.LineCoverage coverage;

  public CoverageLineReader(Iterator<ScannerReport.LineCoverage> coverageIterator) {
    this.coverageIterator = coverageIterator;
  }

  @Override
  public void read(DbFileSources.Line.Builder lineBuilder) {
    ScannerReport.LineCoverage reportCoverage = getNextLineCoverageIfMatchLine(lineBuilder.getLine());
    if (reportCoverage != null) {
      processUnitTest(lineBuilder, reportCoverage);
      processIntegrationTest(lineBuilder, reportCoverage);
      processOverallTest(lineBuilder, reportCoverage);
      coverage = null;
    }
  }

  private static void processUnitTest(DbFileSources.Line.Builder lineBuilder, ScannerReport.LineCoverage reportCoverage) {
    lineBuilder.setUtLineHits(reportCoverage.getUtHits() ? 1 : 0);
    if (reportCoverage.getConditions() > 0) {
      lineBuilder.setUtConditions(reportCoverage.getConditions());
      lineBuilder.setUtCoveredConditions(reportCoverage.getUtCoveredConditions());
    }
  }

  private static void processIntegrationTest(DbFileSources.Line.Builder lineBuilder, ScannerReport.LineCoverage reportCoverage) {
    lineBuilder.setItLineHits(reportCoverage.getItHits() ? 1 : 0);
    if (reportCoverage.getConditions() > 0) {
      lineBuilder.setItConditions(reportCoverage.getConditions());
      lineBuilder.setItCoveredConditions(reportCoverage.getItCoveredConditions());
    }
  }

  private static void processOverallTest(DbFileSources.Line.Builder lineBuilder, ScannerReport.LineCoverage reportCoverage) {
    lineBuilder.setOverallLineHits((reportCoverage.getUtHits() || reportCoverage.getItHits()) ? 1 : 0);
    if (reportCoverage.getConditions() > 0) {
      lineBuilder.setOverallConditions(reportCoverage.getConditions());
      lineBuilder.setOverallCoveredConditions(reportCoverage.getOverallCoveredConditions());
    }
  }

  @CheckForNull
  private ScannerReport.LineCoverage getNextLineCoverageIfMatchLine(int line) {
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
