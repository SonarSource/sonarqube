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
import { getRelativeTimeProps } from '../dateUtils';

const mockDateNow = jest.spyOn(Date, 'now');

describe('getRelativeTimeProps', () => {
  mockDateNow.mockImplementation(() => new Date('2021-02-20T20:20:20Z').getTime());

  it.each([
    ['year', '2020-02-19T20:20:20Z', -1],
    ['month', '2020-11-18T20:20:20Z', -3],
    ['day', '2021-02-18T18:20:20Z', -2],
  ])('should return the correct props for dates older than a %s', (unit, date, value) => {
    expect(getRelativeTimeProps(date)).toEqual({ value, unit });
  });

  it('should return the correct props for dates from less than a day ago', () => {
    expect(getRelativeTimeProps('2021-02-20T20:19:45Z')).toEqual({
      value: -35,
      unit: 'second',
      updateIntervalInSeconds: 10,
    });
  });
});
