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
import * as utils from '../utils';

describe('localizeSorting', () => {
  it('localizes default sorting', () => {
    expect(utils.localizeSorting()).toBe('projects.sort.name');
  });

  it('localizes custom sorting', () => {
    expect(utils.localizeSorting('size')).toBe('projects.sort.size');
  });
});

describe('parseSorting', () => {
  it('parses ascending', () => {
    expect(utils.parseSorting('size')).toEqual({ sortDesc: false, sortValue: 'size' });
  });

  it('parses descending', () => {
    expect(utils.parseSorting('-size')).toEqual({ sortDesc: true, sortValue: 'size' });
  });
});

describe('cumulativeMapFacetValues', () => {
  it('should correctly cumulate facets', () => {
    expect(
      utils.cumulativeMapFacetValues([
        { val: '1', count: 50 },
        { val: '2', count: 1 },
        { val: '3', count: 6 },
        { val: '4', count: 2 },
        { val: '5', count: 0 }
      ])
      // eslint-disable-next-line
    ).toEqual({ '1': 50, '2': 9, '3': 8, '4': 2, '5': 0 });
  });
  it('should correctly cumulate facets with NO_DATA items', () => {
    const expectedResult = {
      '80.0-*': 59,
      '70.0-80.0': 26,
      '50.0-70.0': 15,
      '30.0-50.0': 11,
      '*-30.0': 7,
      NO_DATA: 5
    };
    expect(
      utils.cumulativeMapFacetValues([
        { val: '80.0-*', count: 59 },
        { val: '70.0-80.0', count: 11 },
        { val: '50.0-70.0', count: 4 },
        { val: '30.0-50.0', count: 4 },
        { val: '*-30.0', count: 7 },
        { val: 'NO_DATA', count: 5 }
      ])
    ).toEqual(expectedResult);
    expect(
      utils.cumulativeMapFacetValues([
        { val: 'NO_DATA', count: 5 },
        { val: '80.0-*', count: 59 },
        { val: '70.0-80.0', count: 11 },
        { val: '50.0-70.0', count: 4 },
        { val: '30.0-50.0', count: 4 },
        { val: '*-30.0', count: 7 }
      ])
    ).toEqual(expectedResult);
    expect(
      utils.cumulativeMapFacetValues([
        { val: '80.0-*', count: 59 },
        { val: '70.0-80.0', count: 11 },
        { val: '50.0-70.0', count: 4 },
        { val: 'NO_DATA', count: 5 },
        { val: '30.0-50.0', count: 4 },
        { val: '*-30.0', count: 7 }
      ])
    ).toEqual(expectedResult);
  });
});
