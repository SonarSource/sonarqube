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
const LINES_ABOVE = 2;
const LINES_BELOW = 2;
export const MERGE_DISTANCE = 4; // Merge if snippets are four lines away (separated by 3 lines) or fewer
export const LINES_BELOW_LAST = 9;
export const EXPAND_BY_LINES = 10;

function unknownComponent(key: string): T.SnippetsByComponent {
  return {
    component: {
      key,
      measures: {},
      path: '',
      project: '',
      projectName: '',
      q: 'FIL',
      uuid: ''
    },
    sources: []
  };
}

function collision([startA, endA]: number[], [startB, endB]: number[]) {
  return !(startA > endB + MERGE_DISTANCE || endA < startB - MERGE_DISTANCE);
}

function getPrimaryLocation(issue: T.Issue): T.FlowLocation {
  return {
    component: issue.component,
    textRange: issue.textRange || {
      endLine: 0,
      endOffset: 0,
      startLine: 0,
      startOffset: 0
    }
  };
}

export function createSnippets(
  locations: T.FlowLocation[],
  last: boolean,
  issue?: T.Issue
): T.Snippet[] {
  // For each location's range (2 above and 2 below), and then compare with other ranges
  // to merge snippets that collide.
  return (issue ? [getPrimaryLocation(issue), ...locations] : locations).reduce(
    (snippets: T.Snippet[], loc, index) => {
      const startIndex = Math.max(1, loc.textRange.startLine - LINES_ABOVE);
      const endIndex =
        loc.textRange.endLine +
        ((issue && index === 0) || (last && index === locations.length - 1)
          ? LINES_BELOW_LAST
          : LINES_BELOW);

      let firstCollision: { start: number; end: number } | undefined;

      // Remove ranges that collide into the first collision
      snippets = snippets.filter(snippet => {
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
          index
        });
      }

      return snippets;
    },
    []
  );
}

export function linesForSnippets(snippets: T.Snippet[], componentLines: T.LineMap) {
  return snippets
    .map(snippet => {
      const lines = [];
      for (let i = snippet.start; i <= snippet.end; i++) {
        if (componentLines[i]) {
          lines.push(componentLines[i]);
        }
      }
      return lines;
    })
    .filter(snippet => snippet.length > 0);
}

export function groupLocationsByComponent(
  locations: T.FlowLocation[],
  components: { [key: string]: T.SnippetsByComponent }
) {
  let currentComponent = '';
  let currentGroup: T.SnippetGroup;
  const groups: T.SnippetGroup[] = [];

  locations.forEach((loc, index) => {
    if (loc.component !== currentComponent) {
      currentGroup = {
        ...(components[loc.component] || unknownComponent(loc.component)),
        locations: []
      };
      groups.push(currentGroup);
      currentComponent = loc.component;
    }
    loc.index = index;
    currentGroup.locations.push(loc);
  });

  return groups;
}

export function expandSnippet({
  direction,
  snippetIndex,
  snippets
}: {
  direction: T.ExpandDirection;
  snippetIndex: number;
  snippets: T.Snippet[];
}) {
  const snippetToExpand = snippets.find(s => s.index === snippetIndex);
  if (!snippetToExpand) {
    throw new Error(`Snippet ${snippetIndex} not found`);
  }

  snippetToExpand.start = Math.max(
    0,
    snippetToExpand.start - (direction === 'up' ? EXPAND_BY_LINES : 0)
  );
  snippetToExpand.end += direction === 'down' ? EXPAND_BY_LINES : 0;

  return snippets.map(snippet => {
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

export function inSnippet(line: number, snippet: T.SourceLine[]) {
  return line >= snippet[0].line && line <= snippet[snippet.length - 1].line;
}
