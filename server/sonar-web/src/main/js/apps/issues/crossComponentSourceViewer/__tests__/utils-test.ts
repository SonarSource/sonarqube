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
import {
  mockFlowLocation,
  mockIssue,
  mockSnippetsByComponent
} from '../../../../helpers/testMocks';
import { createSnippets, expandSnippet, groupLocationsByComponent } from '../utils';

describe('groupLocationsByComponent', () => {
  it('should handle empty args', () => {
    expect(groupLocationsByComponent([], {})).toEqual([]);
  });

  it('should group correctly', () => {
    const results = groupLocationsByComponent(
      [
        mockFlowLocation({
          textRange: { startLine: 16, startOffset: 10, endLine: 16, endOffset: 14 }
        }),
        mockFlowLocation({
          textRange: { startLine: 16, startOffset: 2, endLine: 16, endOffset: 3 }
        }),
        mockFlowLocation({
          textRange: { startLine: 24, startOffset: 1, endLine: 24, endOffset: 2 }
        })
      ],
      { 'main.js': mockSnippetsByComponent('main.js', [14, 15, 16, 17, 18, 22, 23, 24, 25, 26]) }
    );

    expect(results).toHaveLength(1);
  });

  it('should preserve step order when jumping between files', () => {
    const results = groupLocationsByComponent(
      [
        mockFlowLocation({
          component: 'A.js',
          textRange: { startLine: 16, startOffset: 10, endLine: 16, endOffset: 14 }
        }),
        mockFlowLocation({
          component: 'B.js',
          textRange: { startLine: 16, startOffset: 10, endLine: 16, endOffset: 14 }
        }),
        mockFlowLocation({
          component: 'A.js',
          textRange: { startLine: 15, startOffset: 2, endLine: 15, endOffset: 3 }
        })
      ],
      {
        'A.js': mockSnippetsByComponent('A.js', [13, 14, 15, 16, 17, 18]),
        'B.js': mockSnippetsByComponent('B.js', [14, 15, 16, 17, 18])
      }
    );

    expect(results).toHaveLength(3);
    expect(results[0].component.key).toBe('A.js');
    expect(results[1].component.key).toBe('B.js');
    expect(results[2].component.key).toBe('A.js');
    expect(results[0].locations).toHaveLength(1);
    expect(results[1].locations).toHaveLength(1);
    expect(results[2].locations).toHaveLength(1);
  });
});

describe('createSnippets', () => {
  it('should merge snippets correctly', () => {
    const results = createSnippets(
      [
        mockFlowLocation({
          textRange: { startLine: 16, startOffset: 10, endLine: 16, endOffset: 14 }
        }),
        mockFlowLocation({
          textRange: { startLine: 19, startOffset: 2, endLine: 19, endOffset: 3 }
        })
      ],
      false
    );

    expect(results).toHaveLength(1);
    expect(results[0]).toEqual({ index: 0, start: 14, end: 21 });
  });

  it('should merge snippets correctly, even when not in sequence', () => {
    const results = createSnippets(
      [
        mockFlowLocation({
          textRange: { startLine: 16, startOffset: 10, endLine: 16, endOffset: 14 }
        }),
        mockFlowLocation({
          textRange: { startLine: 47, startOffset: 2, endLine: 47, endOffset: 3 }
        }),
        mockFlowLocation({
          textRange: { startLine: 14, startOffset: 2, endLine: 14, endOffset: 3 }
        })
      ],
      false
    );

    expect(results).toHaveLength(2);
    expect(results[0]).toEqual({ index: 0, start: 12, end: 18 });
    expect(results[1]).toEqual({ index: 1, start: 45, end: 49 });
  });

  it('should merge three snippets together', () => {
    const results = createSnippets(
      [
        mockFlowLocation({
          textRange: { startLine: 16, startOffset: 10, endLine: 16, endOffset: 14 }
        }),
        mockFlowLocation({
          textRange: { startLine: 47, startOffset: 2, endLine: 47, endOffset: 3 }
        }),
        mockFlowLocation({
          textRange: { startLine: 22, startOffset: 2, endLine: 22, endOffset: 3 }
        }),
        mockFlowLocation({
          textRange: { startLine: 18, startOffset: 2, endLine: 18, endOffset: 3 }
        })
      ],
      false
    );

    expect(results).toHaveLength(2);
    expect(results[0]).toEqual({ index: 0, start: 14, end: 24 });
    expect(results[1]).toEqual({ index: 1, start: 45, end: 49 });
  });

  it("should prepend the issue's main location if necessary", () => {
    const results = createSnippets(
      [
        mockFlowLocation({
          textRange: { startLine: 47, startOffset: 2, endLine: 47, endOffset: 3 }
        }),
        mockFlowLocation({
          textRange: { startLine: 22, startOffset: 2, endLine: 22, endOffset: 3 }
        })
      ],
      false,
      mockIssue(false, { textRange: { startLine: 5, endLine: 5, startOffset: 0, endOffset: 0 } })
    );

    expect(results).toHaveLength(3);
    expect(results[0]).toEqual({ index: 0, start: 3, end: 14 });
  });

  it('should handle last component', () => {
    const results = createSnippets(
      [
        mockFlowLocation({
          textRange: { startLine: 16, startOffset: 10, endLine: 16, endOffset: 14 }
        }),
        mockFlowLocation({
          textRange: { startLine: 19, startOffset: 2, endLine: 19, endOffset: 3 }
        })
      ],
      true
    );

    expect(results).toHaveLength(1);
    expect(results[0]).toEqual({ index: 0, start: 14, end: 28 });
  });
});

describe('expandSnippet', () => {
  it('should add lines above', () => {
    const snippets = [{ start: 14, end: 18, index: 0 }];

    const result = expandSnippet({ direction: 'up', snippetIndex: 0, snippets });

    expect(result).toHaveLength(1);
    expect(result[0]).toEqual({ start: 4, end: 18, index: 0 });
  });

  it('should add lines below', () => {
    const snippets = [{ start: 4, end: 8, index: 0 }];

    const result = expandSnippet({ direction: 'down', snippetIndex: 0, snippets });

    expect(result).toHaveLength(1);
    expect(result[0]).toEqual({ start: 4, end: 18, index: 0 });
  });

  it('should merge snippets if necessary', () => {
    const snippets = [
      { index: 1, start: 4, end: 8 },
      { index: 2, start: 38, end: 42 },
      { index: 3, start: 17, end: 21 }
    ];

    const result = expandSnippet({ direction: 'down', snippetIndex: 1, snippets });

    expect(result).toHaveLength(3);
    expect(result[0]).toEqual({ index: 1, start: 4, end: 21 });
    expect(result[1]).toEqual({ index: 2, start: 38, end: 42 });
    expect(result[2]).toEqual({ index: 3, start: 17, end: 21, toDelete: true });
  });
});
