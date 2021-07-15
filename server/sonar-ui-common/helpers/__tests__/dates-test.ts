/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import * as dates from '../dates';

const { parseDate } = dates;
const recentDate = parseDate('2017-08-16T12:00:00.000Z');

it('toShortNotSoISOString', () => {
  expect(dates.toShortNotSoISOString(recentDate)).toBe('2017-08-16');
});

it('toNotSoISOString', () => {
  expect(dates.toNotSoISOString(recentDate)).toBe('2017-08-16T12:00:00+0000');
});

it('isValidDate', () => {
  expect(dates.isValidDate(recentDate)).toBe(true);
  expect(dates.isValidDate(new Date())).toBe(true);
  expect(dates.isValidDate(parseDate('foo'))).toBe(false);
});
