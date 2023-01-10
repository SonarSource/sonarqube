/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.externalissue.sarif;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.core.sarif.Region;

@ScannerSide
public class RegionMapper {

  Optional<TextRange> mapRegion(@Nullable Region region, InputFile file) {
    if (region == null) {
      return Optional.empty();
    }
    int startLine = Objects.requireNonNull(region.getStartLine(), "No start line defined for the region.");
    int endLine = Optional.ofNullable(region.getEndLine()).orElse(startLine);
    int startColumn = Optional.ofNullable(region.getStartColumn()).map(RegionMapper::adjustSarifColumnIndexToSqIndex).orElse(0);
    int endColumn = Optional.ofNullable(region.getEndColumn()).map(RegionMapper::adjustSarifColumnIndexToSqIndex)
      .orElseGet(() -> file.selectLine(endLine).end().lineOffset());
    if (rangeIsEmpty(startLine, endLine, startColumn, endColumn)) {
      return Optional.of(file.selectLine(startLine));
    } else {
      return Optional.of(file.newRange(startLine, startColumn, endLine, endColumn));
    }
  }

  private static int adjustSarifColumnIndexToSqIndex(int index) {
    return index - 1;
  }

  private static boolean rangeIsEmpty(int startLine, int endLine, int startColumn, int endColumn) {
    return startLine == endLine && startColumn == endColumn;
  }
}
