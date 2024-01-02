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
import { mockSnippetsByComponent } from '../../../../helpers/mocks/sources';
import { mockFlowLocation, mockIssue } from '../../../../helpers/testMocks';
import { createSnippets, expandSnippet, groupLocationsByComponent } from '../utils';

describe('groupLocationsByComponent', () => {
  it('should handle empty args', () => {
    expect(groupLocationsByComponent(mockIssue(), [], {})).toEqual([{ locations: [] }]);
  });

  it('should group correctly', () => {
    const results = groupLocationsByComponent(
      mockIssue(),
      [
        mockFlowLocation({
          textRange: { startLine: 16, startOffset: 10, endLine: 16, endOffset: 14 },
        }),
        mockFlowLocation({
          textRange: { startLine: 16, startOffset: 2, endLine: 16, endOffset: 3 },
        }),
        mockFlowLocation({
          textRange: { startLine: 24, startOffset: 1, endLine: 24, endOffset: 2 },
        }),
      ],
      {
        'main.js': mockSnippetsByComponent(
          'main.js',
          'project',
          [14, 15, 16, 17, 18, 22, 23, 24, 25, 26],
        ),
      },
    );

    expect(results).toHaveLength(1);
  });

  it('should preserve step order when jumping between files', () => {
    const results = groupLocationsByComponent(
      mockIssue(),
      [
        mockFlowLocation({
          component: 'A.js',
          textRange: { startLine: 16, startOffset: 10, endLine: 16, endOffset: 14 },
        }),
        mockFlowLocation({
          component: 'B.js',
          textRange: { startLine: 16, startOffset: 10, endLine: 16, endOffset: 14 },
        }),
        mockFlowLocation({
          component: 'A.js',
          textRange: { startLine: 15, startOffset: 2, endLine: 15, endOffset: 3 },
        }),
      ],
      {
        'A.js': mockSnippetsByComponent('A.js', 'project', [13, 14, 15, 16, 17, 18]),
        'B.js': mockSnippetsByComponent('B.js', 'project', [14, 15, 16, 17, 18]),
      },
    );

    expect(results).toHaveLength(3);
    expect(results[0].component.key).toBe('project:A.js');
    expect(results[1].component.key).toBe('project:B.js');
    expect(results[2].component.key).toBe('project:A.js');
    expect(results[0].locations).toHaveLength(1);
    expect(results[1].locations).toHaveLength(1);
    expect(results[2].locations).toHaveLength(1);
  });
});

describe('createSnippets', () => {
  it('should merge snippets correctly', () => {
    const locations = [
      mockFlowLocation({
        textRange: { startLine: 16, startOffset: 10, endLine: 16, endOffset: 14 },
      }),
      mockFlowLocation({
        textRange: { startLine: 19, startOffset: 2, endLine: 19, endOffset: 3 },
      }),
    ];
    const results = createSnippets({
      component: '',
      locations,
      issue: mockIssue(false, locations[1]),
    });

    expect(results).toHaveLength(1);
    expect(results[0]).toEqual({ index: 0, start: 11, end: 28 });
  });

  it('should merge snippets correctly, even when not in sequence', () => {
    const locations = [
      mockFlowLocation({
        textRange: { startLine: 16, startOffset: 10, endLine: 16, endOffset: 14 },
      }),
      mockFlowLocation({
        textRange: { startLine: 47, startOffset: 2, endLine: 47, endOffset: 3 },
      }),
      mockFlowLocation({
        textRange: { startLine: 14, startOffset: 2, endLine: 14, endOffset: 3 },
      }),
    ];
    const results = createSnippets({
      component: '',
      locations,
      issue: mockIssue(false, locations[2]),
    });

    expect(results).toHaveLength(2);
    expect(results[0]).toEqual({ index: 0, start: 9, end: 23 });
    expect(results[1]).toEqual({ index: 1, start: 42, end: 52 });
  });

  it('should merge three snippets together', () => {
    const locations = [
      mockFlowLocation({
        textRange: { startLine: 16, startOffset: 10, endLine: 16, endOffset: 14 },
      }),
      mockFlowLocation({
        textRange: { startLine: 47, startOffset: 2, endLine: 47, endOffset: 3 },
      }),
      mockFlowLocation({
        textRange: { startLine: 23, startOffset: 2, endLine: 23, endOffset: 3 },
      }),
      mockFlowLocation({
        textRange: { startLine: 18, startOffset: 2, endLine: 18, endOffset: 3 },
      }),
    ];
    const results = createSnippets({
      component: '',
      locations,
      issue: mockIssue(false, locations[0]),
    });

    expect(results).toHaveLength(2);
    expect(results[0]).toEqual({ index: 0, start: 11, end: 28 });
    expect(results[1]).toEqual({ index: 1, start: 42, end: 52 });
  });

  it("should prepend the issue's main location if necessary", () => {
    const locations = [
      mockFlowLocation({
        textRange: { startLine: 85, startOffset: 2, endLine: 85, endOffset: 3 },
      }),
      mockFlowLocation({
        textRange: { startLine: 42, startOffset: 2, endLine: 42, endOffset: 3 },
      }),
    ];
    const issue = mockIssue(false, {
      secondaryLocations: [mockFlowLocation()],
      textRange: { startLine: 12, endLine: 12, startOffset: 0, endOffset: 0 },
    });
    const results = createSnippets({
      component: issue.component,
      locations,
      issue,
    });

    expect(results).toHaveLength(3);
    expect(results[0]).toEqual({ index: 0, start: 7, end: 21 });
    // the third snippet (index 2) should be second, because it comes before in the code!
    expect(results[1]).toEqual({ index: 2, start: 37, end: 47 });
  });

  it('should ignore location with no textrange', () => {
    const locations = [
      mockFlowLocation({
        textRange: { startLine: 85, startOffset: 2, endLine: 85, endOffset: 3 },
      }),
    ];
    const issue = mockIssue(false, {
      secondaryLocations: [mockFlowLocation()],
      textRange: undefined,
    });
    const results = createSnippets({
      component: issue.component,
      locations,
      issue,
    });

    expect(results).toHaveLength(1);
    expect(results[0]).toEqual({ index: 0, start: 80, end: 94 });
  });
});

describe('expandSnippet', () => {
  it('should add lines above', () => {
    const snippets = [{ start: 14, end: 18, index: 0 }];

    const result = expandSnippet({ direction: 'up', snippetIndex: 0, snippets });

    expect(result).toHaveLength(1);
    expect(result[0]).toEqual({ start: 0, end: 18, index: 0 });
  });

  it('should add lines below', () => {
    const snippets = [{ start: 4, end: 8, index: 0 }];

    const result = expandSnippet({ direction: 'down', snippetIndex: 0, snippets });

    expect(result).toHaveLength(1);
    expect(result[0]).toEqual({ start: 4, end: 58, index: 0 });
  });

  it('should merge snippets if necessary', () => {
    const snippets = [
      { index: 1, start: 4, end: 14 },
      { index: 2, start: 82, end: 92 },
      { index: 3, start: 37, end: 47 },
    ];

    const result = expandSnippet({ direction: 'down', snippetIndex: 1, snippets });

    expect(result).toHaveLength(3);
    expect(result[0]).toEqual({ index: 1, start: 4, end: 64 });
    expect(result[1]).toEqual({ index: 2, start: 82, end: 92 });
    expect(result[2]).toEqual({ index: 3, start: 37, end: 47, toDelete: true });
  });
});
