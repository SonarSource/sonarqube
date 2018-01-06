/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
// @flow
/*:: import type { TextRange, Issue } from '../../issue/types'; */

export function getLinearLocations(
  textRange /*: ?TextRange */
) /*: Array<{ line: number, from: number, to: number }> */ {
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

/*::
type Location = {
  msg: string,
  flowIndex: number,
  locationIndex: number,
  textRange?: TextRange,
  index?: number
}
*/

export function getIssueLocations(issue /*: Issue */) /*: Array<Location> */ {
  const allLocations = [];
  issue.flows.forEach((locations, flowIndex) => {
    if (locations) {
      const locationsCount = locations.length;
      locations.forEach((location, index) => {
        const flowLocation = {
          ...location,
          flowIndex,
          locationIndex: index,
          // set index only for real flows, do not set for just secondary locations
          index: locationsCount > 1 ? locationsCount - index : undefined
        };
        allLocations.push(flowLocation);
      });
    }
  });
  return allLocations;
}
