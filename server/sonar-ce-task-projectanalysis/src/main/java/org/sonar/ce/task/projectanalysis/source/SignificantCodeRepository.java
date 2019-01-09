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
package org.sonar.ce.task.projectanalysis.source;

import java.util.Optional;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.hash.LineRange;
import org.sonar.core.util.CloseableIterator;
import org.sonar.scanner.protocol.output.ScannerReport.LineSgnificantCode;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;

public class SignificantCodeRepository {
  private final BatchReportReader reportReader;

  public SignificantCodeRepository(BatchReportReader reportReader) {
    this.reportReader = reportReader;
  }

  public Optional<LineRange[]> getRangesPerLine(Component component) {
    int numLines = component.getFileAttributes().getLines();

    Optional<CloseableIterator<LineSgnificantCode>> opt = reportReader.readComponentSignificantCode(component.getReportAttributes().getRef());
    if (!opt.isPresent()) {
      return Optional.empty();
    }
    try (CloseableIterator<LineSgnificantCode> significantCode = opt.get()) {
      return Optional.of(toArray(significantCode, numLines));
    }
  }

  private static LineRange[] toArray(CloseableIterator<LineSgnificantCode> lineRanges, int numLines) {
    LineRange[] ranges = new LineRange[numLines];
    LineSgnificantCode currentLine = null;

    for (int i = 0; i < numLines; i++) {
      if (currentLine == null) {
        if (!lineRanges.hasNext()) {
          break;
        }
        currentLine = lineRanges.next();
      }

      if (currentLine.getLine() == i + 1) {
        ranges[i] = new LineRange(currentLine.getStartOffset(), currentLine.getEndOffset());
        currentLine = null;
      }
    }

    return ranges;
  }
}
