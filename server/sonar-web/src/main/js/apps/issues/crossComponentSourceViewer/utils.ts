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
import { ComponentQualifier } from '../../../types/component';
import {
  ExpandDirection,
  FlowLocation,
  Issue,
  LineMap,
  Snippet,
  SnippetGroup,
  SnippetsByComponent,
  SourceLine,
} from '../../../types/types';

const LINES_ABOVE = 5;
const LINES_BELOW = 5;
export const MERGE_DISTANCE = 11; // Merge if snippets are eleven lines away (separated by 10 lines) or fewer
export const LINES_BELOW_ISSUE = 9;
export const EXPAND_BY_LINES = 50;

function unknownComponent(key: string): SnippetsByComponent {
  return {
    component: {
      key,
      measures: {},
      path: '',
      project: '',
      projectName: '',
      q: ComponentQualifier.File,
      uuid: '',
    },
    sources: [],
  };
}

function collision([startA, endA]: number[], [startB, endB]: number[]) {
  return !(startA > endB + MERGE_DISTANCE || endA < startB - MERGE_DISTANCE);
}

export function getPrimaryLocation(issue: Issue): FlowLocation {
  return {
    component: issue.component,
    textRange: issue.textRange || {
      endLine: 0,
      endOffset: 0,
      startLine: 0,
      startOffset: 0,
    },
  };
}

function addLinesBellow(params: { issue: Issue; locationEnd: number }) {
  const { issue, locationEnd } = params;
  const issueEndLine = (issue.textRange && issue.textRange.endLine) || 0;

  if (!issueEndLine || issueEndLine === locationEnd) {
    return locationEnd + LINES_BELOW_ISSUE;
  }

  return locationEnd + LINES_BELOW;
}

export function createSnippets(params: {
  component: string;
  issue: Issue;
  locations: FlowLocation[];
}): Snippet[] {
  const { component, issue, locations } = params;

  const hasSecondaryLocations = issue.secondaryLocations.length > 0;
  const addIssueLocation =
    hasSecondaryLocations && issue.component === component && issue.textRange !== undefined;

  // For each location: compute its range, and then compare with other ranges
  // to merge snippets that collide.
  const ranges = (addIssueLocation ? [getPrimaryLocation(issue), ...locations] : locations).reduce(
    (snippets: Snippet[], loc, index) => {
      const startIndex = Math.max(1, loc.textRange.startLine - LINES_ABOVE);
      const endIndex = addLinesBellow({ issue, locationEnd: loc.textRange.endLine });

      let firstCollision: { start: number; end: number } | undefined;

      // Remove ranges that collide into the first collision
      snippets = snippets.filter((snippet) => {
        if (collision([snippet.start, snippet.end], [startIndex, endIndex])) {
          let keep = false;
          // Check if we've already collided
          if (!firstCollision) {
            firstCollision = snippet;
            keep = true;
          }
          // Merge with first collision:
          firstCollision.start = Math.min(startIndex, snippet.start, firstCollision.start);
          firstCollision.end = Math.max(endIndex, snippet.end, firstCollision.end);

          // remove the range if it was not the first collision
          return keep;
        }
        return true;
      });

      if (firstCollision === undefined) {
        snippets.push({
          start: startIndex,
          end: endIndex,
          index,
        });
      }

      return snippets;
    },
    []
  );

  // Sort snippets by line number in the case of secondary locations
  // Preserve location order for flows!
  return hasSecondaryLocations ? ranges.sort((a, b) => a.start - b.start) : ranges;
}

export function linesForSnippets(snippets: Snippet[], componentLines: LineMap) {
  return snippets
    .map((snippet) => {
      const lines = [];
      for (let i = snippet.start; i <= snippet.end; i++) {
        if (componentLines[i]) {
          lines.push(componentLines[i]);
        }
      }
      return lines;
    })
    .filter((snippet) => snippet.length > 0);
}

export function groupLocationsByComponent(
  issue: Issue,
  locations: FlowLocation[],
  components: { [key: string]: SnippetsByComponent }
) {
  let currentComponent = '';
  let currentGroup: SnippetGroup;
  const groups: SnippetGroup[] = [];

  const addGroup = (componentKey: string) => {
    currentGroup = {
      ...(components[componentKey] || unknownComponent(componentKey)),
      locations: [],
    };
    groups.push(currentGroup);
    currentComponent = componentKey;
  };

  if (
    issue.secondaryLocations.length > 0 &&
    locations.every((loc) => loc.component !== issue.component)
  ) {
    addGroup(issue.component);
  }

  locations.forEach((loc, index) => {
    if (loc.component !== currentComponent) {
      addGroup(loc.component);
    }
    loc.index = index;
    currentGroup.locations.push(loc);
  });

  if (groups.length === 0) {
    groups.push({ locations: [], ...components[issue.component] });
  }

  return groups;
}

export function expandSnippet({
  direction,
  snippetIndex,
  snippets,
}: {
  direction: ExpandDirection;
  snippetIndex: number;
  snippets: Snippet[];
}) {
  const snippetToExpand = snippets.find((s) => s.index === snippetIndex);
  if (!snippetToExpand) {
    throw new Error(`Snippet ${snippetIndex} not found`);
  }

  snippetToExpand.start = Math.max(
    0,
    snippetToExpand.start - (direction === 'up' ? EXPAND_BY_LINES : 0)
  );
  snippetToExpand.end += direction === 'down' ? EXPAND_BY_LINES : 0;

  return snippets.map((snippet) => {
    if (snippet.index === snippetIndex) {
      return snippetToExpand;
    }
    if (collision([snippet.start, snippet.end], [snippetToExpand.start, snippetToExpand.end])) {
      // Merge with expanded snippet
      snippetToExpand.start = Math.min(snippet.start, snippetToExpand.start);
      snippetToExpand.end = Math.max(snippet.end, snippetToExpand.end);
      snippet.toDelete = true;
    }
    return snippet;
  });
}

export function inSnippet(line: number, snippet: SourceLine[]) {
  return line >= snippet[0].line && line <= snippet[snippet.length - 1].line;
}
