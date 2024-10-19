/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import type { LineMap, SourceLine } from '../types/types';

export function decorateWithUnderlineFlags(line: SourceLine, sourcesMap: LineMap) {
  const previousLine: SourceLine | undefined = sourcesMap[line.line - 1];

  const decoratedLine = { ...line };

  if (line.coverageStatus) {
    decoratedLine.coverageBlock =
      line.coverageStatus === previousLine?.coverageStatus
        ? (previousLine.coverageBlock ?? line.line)
        : line.line;
  }

  if (line.isNew) {
    decoratedLine.newCodeBlock = previousLine?.isNew
      ? (previousLine.newCodeBlock ?? line.line)
      : line.line;
  }

  return decoratedLine;
}
