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

describe('formatDuration', () => {
  const ONE_MINUTE = 60000;
  const ONE_HOUR = 60 * ONE_MINUTE;
  const ONE_DAY = 24 * ONE_HOUR;
  const ONE_MONTH = 30 * ONE_DAY;
  const ONE_YEAR = 12 * ONE_MONTH;
  it('render years and months only', () => {
    expect(utils.formatDuration(ONE_YEAR * 4 + ONE_MONTH * 2 + ONE_DAY * 10)).toEqual(
      'duration.years.4 duration.months.2 '
    );
  });

  it('render years only', () => {
    expect(utils.formatDuration(ONE_YEAR * 4 + ONE_DAY * 10)).toEqual('duration.years.4 ');
  });

  it('render hours and minutes', () => {
    expect(utils.formatDuration(ONE_HOUR * 4 + ONE_MINUTE * 10)).toEqual(
      'duration.hours.4 duration.minutes.10 '
    );
  });

  it('render days only', () => {
    expect(utils.formatDuration(ONE_DAY * 4 + ONE_MINUTE * 10)).toEqual('duration.days.4 ');
  });

  it('render less than a minute', () => {
    expect(utils.formatDuration(1000)).toEqual('duration.seconds');
  });
});
