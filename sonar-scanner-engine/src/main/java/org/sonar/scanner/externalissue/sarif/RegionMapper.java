/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
    Integer endLine = region.getEndLine();
    if (endLine != null) {
      int startColumn = Optional.ofNullable(region.getStartColumn()).orElse(1);
      int endColumn = Optional.ofNullable(region.getEndColumn())
        .orElseGet(() -> file.selectLine(endLine).end().lineOffset());
      return Optional.of(file.newRange(startLine, startColumn, endLine, endColumn));
    } else {
      return Optional.of(file.selectLine(startLine));
    }
  }
}
