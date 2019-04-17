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
import { keyBy, range } from 'lodash';
import { groupLocationsByComponent, createSnippets, expandSnippet } from '../utils';
import {
  mockFlowLocation,
  mockSnippetsByComponent,
  mockSourceLine
} from '../../../../helpers/testMocks';

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
      mockSnippetsByComponent('', [14, 15, 16, 17, 18, 19, 20, 21, 22]).sources,
      false
    );

    expect(results).toHaveLength(1);
    expect(results[0]).toHaveLength(8);
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
      mockSnippetsByComponent('', [12, 13, 14, 15, 16, 17, 18, 45, 46, 47, 48, 49]).sources,
      false
    );

    expect(results).toHaveLength(2);
    expect(results[0]).toHaveLength(7);
    expect(results[1]).toHaveLength(5);
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
      mockSnippetsByComponent('', [14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 45, 46, 47, 48, 49])
        .sources,
      false
    );

    expect(results).toHaveLength(2);
    expect(results[0]).toHaveLength(11);
    expect(results[1]).toHaveLength(5);
  });
});

describe('expandSnippet', () => {
  it('should add lines above', () => {
    const lines = keyBy(range(4, 19).map(line => mockSourceLine({ line })), 'line');
    const snippets = [[lines[14], lines[15], lines[16], lines[17], lines[18]]];

    const result = expandSnippet({ direction: 'up', lines, snippetIndex: 0, snippets });

    expect(result).toHaveLength(1);
    expect(result[0]).toHaveLength(15);
    expect(result[0].map(l => l.line)).toEqual(range(4, 19));
  });

  it('should add lines below', () => {
    const lines = keyBy(range(4, 19).map(line => mockSourceLine({ line })), 'line');
    const snippets = [[lines[4], lines[5], lines[6], lines[7], lines[8]]];

    const result = expandSnippet({ direction: 'down', lines, snippetIndex: 0, snippets });

    expect(result).toHaveLength(1);
    expect(result[0].map(l => l.line)).toEqual(range(4, 19));
  });

  it('should merge snippets if necessary', () => {
    const lines = keyBy(
      range(4, 23)
        .concat(range(38, 43))
        .map(line => mockSourceLine({ line })),
      'line'
    );
    const snippets = [
      [lines[4], lines[5], lines[6], lines[7], lines[8]],
      [lines[38], lines[39], lines[40], lines[41], lines[42]],
      [lines[17], lines[18], lines[19], lines[20], lines[21]]
    ];

    const result = expandSnippet({ direction: 'down', lines, snippetIndex: 0, snippets });

    expect(result).toHaveLength(2);
    expect(result[0].map(l => l.line)).toEqual(range(4, 22));
    expect(result[1].map(l => l.line)).toEqual(range(38, 43));
  });
});
