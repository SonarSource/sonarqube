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
import { mockIssue } from '../../../../helpers/testMocks';
import { issuesByComponentAndLine, symbolsByLine } from '../indexing';

describe('issuesByComponentAndLine', () => {
  it('should map issues by lines and components', () => {
    const issues = [
      mockIssue(true, { component: 'foo.js' }),
      mockIssue(false, {
        component: 'foo.js',
        textRange: { startLine: 5, endLine: 5, startOffset: 0, endOffset: 0 }
      }),
      mockIssue(false, { component: 'bar.js' }),
      mockIssue(),
      mockIssue()
    ];
    const result = issuesByComponentAndLine(issues);
    expect(Object.keys(result['foo.js'])).toHaveLength(2);
    expect(Object.keys(result['foo.js'])).toEqual(['5', '26']);
    expect(result['foo.js'][5]).toHaveLength(1);

    expect(Object.keys(result['bar.js'])).toHaveLength(1);

    expect(Object.keys(result['main.js'])).toHaveLength(1);
    expect(result['main.js'][26]).toHaveLength(2);
  });
});

describe('symbolsByLine', () => {
  it('should highlight symbols marked twice', () => {
    const lines = [
      { line: 1, code: '<span class="sym-54 sym"><span class="sym-56 sym">foo</span></span>' },
      { line: 2, code: '<span class="sym-56 sym">bar</span>' },
      { line: 3, code: '<span class="k">qux</span>' }
    ];
    expect(symbolsByLine(lines)).toEqual({
      1: ['sym-54', 'sym-56'],
      2: ['sym-56'],
      3: []
    });
  });
});
