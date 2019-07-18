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
import { getOpenChainFromPath, getUrlsList, testPathAgainstUrl } from '../navTreeUtils';

const navTree = [
  'path/value',
  {
    title: 'My paths',
    children: [
      'child/path/1',
      {
        title: 'Child paths',
        children: [
          'sub/child/path/1',
          {
            title: 'External link 2',
            url: 'http://example.com/2'
          },
          {
            title: 'Last ones, promised',
            children: ['sub/sub/child/path/1']
          },
          'sub/child/path/3'
        ]
      },
      'child/path/2'
    ]
  },
  {
    title: 'External link',
    url: 'http://example.com'
  }
];

describe('getUrlsList', () => {
  it('should return the correct values for a list of paths', () => {
    expect(getUrlsList(navTree)).toEqual([
      'path/value',
      'child/path/1',
      'sub/child/path/1',
      'http://example.com/2',
      'sub/sub/child/path/1',
      'sub/child/path/3',
      'child/path/2',
      'http://example.com'
    ]);
  });
});

describe('getOpenChainFromPath', () => {
  it('should correctly fetch the chain of open elements for a given path', () => {
    expect(getOpenChainFromPath('path/value/', navTree)).toEqual([navTree[0]]);
    expect(getOpenChainFromPath('sub/child/path/3', navTree)).toEqual([
      navTree[1],
      (navTree as any)[1].children[1],
      (navTree as any)[1].children[1].children[3]
    ]);
  });
});

describe('testPathAgainstUrl', () => {
  it('should handle paths with trailing and/or leading slashes', () => {
    expect(testPathAgainstUrl('path/foo/', 'path/bar')).toBe(false);
    expect(testPathAgainstUrl('/path/foo/', '/path/bar/')).toBe(false);
    expect(testPathAgainstUrl('path/foo/', 'path/foo')).toBe(true);
    expect(testPathAgainstUrl('path/foo', 'path/foo/')).toBe(true);
    expect(testPathAgainstUrl('/path/foo/', 'path/foo')).toBe(true);
    expect(testPathAgainstUrl('path/foo', '/path/foo/')).toBe(true);
  });
});
