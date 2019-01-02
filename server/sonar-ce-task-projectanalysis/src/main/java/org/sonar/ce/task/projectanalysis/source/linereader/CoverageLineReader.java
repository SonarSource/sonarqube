/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.source.linereader;

import java.util.Iterator;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.LineCoverage.HasCoveredConditionsCase;
import org.sonar.scanner.protocol.output.ScannerReport.LineCoverage.HasHitsCase;

public class CoverageLineReader implements LineReader {

  private final Iterator<ScannerReport.LineCoverage> coverageIterator;
  private ScannerReport.LineCoverage coverage;

  public CoverageLineReader(Iterator<ScannerReport.LineCoverage> coverageIterator) {
    this.coverageIterator = coverageIterator;
  }

  @Override
  public Optional<ReadError> read(DbFileSources.Line.Builder lineBuilder) {
    ScannerReport.LineCoverage reportCoverage = getNextLineCoverageIfMatchLine(lineBuilder.getLine());
    if (reportCoverage != null) {
      processCoverage(lineBuilder, reportCoverage);
      coverage = null;
    }
    return Optional.empty();
  }

  private static void processCoverage(DbFileSources.Line.Builder lineBuilder, ScannerReport.LineCoverage reportCoverage) {
    if (reportCoverage.getHasHitsCase() == HasHitsCase.HITS) {
      lineBuilder.setLineHits(reportCoverage.getHits() ? 1 : 0);
    }
    if (reportCoverage.getHasCoveredConditionsCase() == HasCoveredConditionsCase.COVERED_CONDITIONS) {
      lineBuilder.setConditions(reportCoverage.getConditions());
      lineBuilder.setCoveredConditions(reportCoverage.getCoveredConditions());
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
