/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import { splitByTokens } from './highlight';
import { getLinearLocations, getIssueLocations } from './issueLocations';
import type { Issue } from '../../issue/types';
import type { SourceLine } from '../types';

export type LinearIssueLocation = {
  from: number,
  line: number,
  to: number
};

export type IndexedIssueLocation = {
  flowIndex: number,
  from: number,
  line: number,
  locationIndex: number,
  to: number
};

export type IndexedIssueLocationMessage = {
  flowIndex: number,
  locationIndex: number,
  msg?: string
};

export type IndexedIssueLocationsByIssueAndLine = {
  [issueKey: string]: {
    // $FlowFixMe
    [lineNumber: number]: Array<IndexedIssueLocation>
  }
};

export type IndexedIssueLocationMessagesByIssueAndLine = {
  [issueKey: string]: {
    [lineNumber: number]: Array<IndexedIssueLocationMessage>
  }
};

export const issuesByLine = (issues: Array<Issue>) => {
  const index = {};
  issues.forEach(issue => {
    const line = issue.line || 0;
    if (!(line in index)) {
      index[line] = [];
    }
    index[line].push(issue.key);
  });
  return index;
};

export const locationsByLine = (issues: Array<Issue>): { [number]: Array<LinearIssueLocation> } => {
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
};

export const locationsByIssueAndLine = (
  issues: Array<Issue>
): IndexedIssueLocationsByIssueAndLine => {
  const index = {};
  issues.forEach(issue => {
    const byLine = {};
    getIssueLocations(issue).forEach(location => {
      getLinearLocations(location.textRange).forEach(linearLocation => {
        if (!(linearLocation.line in byLine)) {
          byLine[linearLocation.line] = [];
        }
        byLine[linearLocation.line].push({
          ...linearLocation,
          flowIndex: location.flowIndex,
          locationIndex: location.locationIndex
        });
      });
    });
    index[issue.key] = byLine;
  });
  return index;
};

export const locationMessagesByIssueAndLine = (
  issues: Array<Issue>
): IndexedIssueLocationMessagesByIssueAndLine => {
  const index = {};
  issues.forEach(issue => {
    const byLine = {};
    getIssueLocations(issue).forEach(location => {
      const line = location.textRange ? location.textRange.startLine : 0;
      if (!(line in byLine)) {
        byLine[line] = [];
      }
      byLine[line].push(location);
    });
    index[issue.key] = byLine;
  });
  return index;
};

export const duplicationsByLine = (duplications: Array<*> | null) => {
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

export const symbolsByLine = (sources: Array<SourceLine>) => {
  const index = {};
  sources.forEach(line => {
    const tokens = splitByTokens(line.code);
    index[line.line] = tokens
      .map(token => {
        const key = token.className.match(/sym-\d+/);
        return key && key[0];
      })
      .filter(key => key);
  });
  return index;
};

export const findLocationByIndex = (
  locations: IndexedIssueLocationsByIssueAndLine,
  flowIndex: number,
  locationIndex: number
) => {
  const issueKeys = Object.keys(locations);
  for (const issueKey of issueKeys) {
    const lineNumbers = Object.keys(locations[issueKey]);
    for (let lineIndex = 0; lineIndex < lineNumbers.length; lineIndex++) {
      for (let i = 0; i < locations[issueKey][lineNumbers[lineIndex]].length; i++) {
        const location = locations[issueKey][lineNumbers[lineIndex]][i];
        if (location.flowIndex === flowIndex && location.locationIndex === locationIndex) {
          return location;
        }
      }
    }
  }

  return null;
};
