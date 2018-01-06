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
import { flatten } from 'lodash';
import { splitByTokens } from './highlight';
import { getLinearLocations } from './issueLocations';
/*:: import type { Issue } from '../../issue/types'; */
/*:: import type { SourceLine } from '../types'; */

/*::
export type LinearIssueLocation = {
  from: number,
  line: number,
  to: number,
  index?: number
};
*/

/*::
export type IndexedIssueLocation = {
  from: number,
  line: number,
  to: number
};
*/

/*::
export type IndexedIssueLocationMessage = {
  flowIndex: number,
  locationIndex: number,
  msg?: string
};
*/

export const issuesByLine = (issues /*: Array<Issue> */) => {
  const index = {};
  issues.forEach(issue => {
    const line = issue.textRange ? issue.textRange.endLine : 0;
    if (!(line in index)) {
      index[line] = [];
    }
    index[line].push(issue);
  });
  return index;
};

export function locationsByLine(
  issues /*: Array<Issue> */
) /*: { [number]: Array<LinearIssueLocation> } */ {
  const index = {};
  issues.forEach(issue => {
    getLinearLocations(issue.textRange).forEach(location => {
      if (!(location.line in index)) {
        index[location.line] = [];
      }
      index[location.line].push(location);
    });
  });
  return index;
}

export const duplicationsByLine = (duplications /*: Array<*> | null */) => {
  if (duplications == null) {
    return {};
  }

  const duplicationsByLine = {};

  duplications.forEach(({ blocks }, duplicationIndex) => {
    blocks.forEach(block => {
      if (block._ref === '1') {
        for (let line = block.from; line < block.from + block.size; line++) {
          if (!(line in duplicationsByLine)) {
            duplicationsByLine[line] = [];
          }
          duplicationsByLine[line].push(duplicationIndex);
        }
      }
    });
  });

  return duplicationsByLine;
};

export const symbolsByLine = (sources /*: Array<SourceLine> */) => {
  const index = {};
  sources.forEach(line => {
    const tokens = splitByTokens(line.code);
    const symbols = flatten(
      tokens.map(token => {
        const keys = token.className.match(/sym-\d+/g);
        return keys != null ? keys : [];
      })
    );
    index[line.line] = symbols.filter(key => key);
  });
  return index;
};
