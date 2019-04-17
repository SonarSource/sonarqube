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
import { flatten } from 'lodash';
import { splitByTokens } from './highlight';
import { getLinearLocations } from './issueLocations';

export function issuesByLine(issues: T.Issue[]) {
  const index: { [line: number]: T.Issue[] } = {};
  issues.forEach(issue => {
    const line = issue.textRange ? issue.textRange.endLine : 0;
    if (!(line in index)) {
      index[line] = [];
    }
    index[line].push(issue);
  });
  return index;
}

export function issuesByComponentAndLine(
  issues: T.Issue[] = []
): { [component: string]: { [line: number]: T.Issue[] } } {
  return issues.reduce((mapping: { [component: string]: { [line: number]: T.Issue[] } }, issue) => {
    mapping[issue.component] = mapping[issue.component] || {};
    const line = issue.textRange ? issue.textRange.endLine : 0;
    mapping[issue.component][line] = mapping[issue.component][line] || [];
    mapping[issue.component][line].push(issue);
    return mapping;
  }, {});
}

export function locationsByLine(issues: T.Issue[]) {
  const index: { [line: number]: T.LinearIssueLocation[] } = {};
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

export function duplicationsByLine(duplications: T.Duplication[] | undefined) {
  if (duplications == null) {
    return {};
  }

  const duplicationsByLine: { [line: number]: number[] } = {};

  duplications.forEach(({ blocks }, duplicationIndex) => {
    blocks.forEach(block => {
      // eslint-disable-next-line no-underscore-dangle
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
}

export function symbolsByLine(sources: T.SourceLine[]) {
  const index: { [line: number]: string[] } = {};
  sources.forEach(line => {
    const tokens = splitByTokens(line.code || '');
    const symbols = flatten(
      tokens.map(token => {
        const keys = token.className.match(/sym-\d+/g);
        return keys != null ? keys : [];
      })
    );
    index[line.line] = symbols.filter(key => key);
  });
  return index;
}
