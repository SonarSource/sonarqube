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
export function getLinearLocations(textRange: T.TextRange | undefined): T.LinearIssueLocation[] {
  if (!textRange) {
    return [];
  }
  const locations = [];

  // go through all lines of the `textRange`
  for (let line = textRange.startLine; line <= textRange.endLine; line++) {
    // TODO fix 999999
    const from = line === textRange.startLine ? textRange.startOffset : 0;
    const to = line === textRange.endLine ? textRange.endOffset : 999999;
    locations.push({ line, from, to });
  }
  return locations;
}

export function getSecondaryIssueLocationsForLine(
  line: T.SourceLine,
  highlightedLocations: (T.FlowLocation | undefined)[] | undefined
): T.LinearIssueLocation[] {
  if (!highlightedLocations) {
    return [];
  }
  return highlightedLocations.reduce((locations, location) => {
    const linearLocations: T.LinearIssueLocation[] = location
      ? getLinearLocations(location.textRange)
          .filter(l => l.line === line.line)
          .map(l => ({
            ...l,
            startLine: location.textRange.startLine,
            index: location.index,
            text: location.msg
          }))
      : [];
    return [...locations, ...linearLocations];
  }, []);
}
