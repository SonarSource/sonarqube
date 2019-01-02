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
import * as dates from '../dates';

const { parseDate } = dates;
const recentDate = parseDate('2017-08-16T12:00:00.000Z');
const recentDate2 = parseDate('2016-12-16T12:00:00.000Z');
const oldDate = parseDate('2014-01-12T12:00:00.000Z');

it('toShortNotSoISOString', () => {
  expect(dates.toShortNotSoISOString(recentDate)).toBe('2017-08-16');
});

it('toNotSoISOString', () => {
  expect(dates.toNotSoISOString(recentDate)).toBe('2017-08-16T12:00:00+0000');
});

it('startOfDay', () => {
  expect(dates.startOfDay(recentDate).toTimeString()).toContain('00:00:00');
  expect(dates.startOfDay(recentDate)).not.toBe(recentDate);
});

it('isValidDate', () => {
  expect(dates.isValidDate(recentDate)).toBeTruthy();
  expect(dates.isValidDate(new Date())).toBeTruthy();
  expect(dates.isValidDate(parseDate('foo'))).toBeFalsy();
});

it('isSameDay', () => {
  expect(dates.isSameDay(recentDate, parseDate(recentDate))).toBeTruthy();
  expect(dates.isSameDay(recentDate, recentDate2)).toBeFalsy();
  expect(dates.isSameDay(recentDate, oldDate)).toBeFalsy();
  expect(dates.isSameDay(recentDate, parseDate('2016-08-16T12:00:00.000Z'))).toBeFalsy();
});

it('differenceInYears', () => {
  expect(dates.differenceInYears(recentDate, recentDate2)).toBe(0);
  expect(dates.differenceInYears(recentDate, oldDate)).toBe(3);
  expect(dates.differenceInYears(oldDate, recentDate)).toBe(-3);
});

it('differenceInDays', () => {
  expect(dates.differenceInDays(recentDate, parseDate('2017-08-01T12:00:00.000Z'))).toBe(15);
  expect(dates.differenceInDays(recentDate, parseDate('2017-08-15T23:00:00.000Z'))).toBe(0);
  expect(dates.differenceInDays(recentDate, recentDate2)).toBe(243);
  expect(dates.differenceInDays(recentDate, oldDate)).toBe(1312);
});

it('differenceInSeconds', () => {
  expect(dates.differenceInSeconds(recentDate, parseDate('2017-08-16T10:00:00.000Z'))).toBe(7200);
  expect(dates.differenceInSeconds(recentDate, parseDate('2017-08-16T12:00:00.500Z'))).toBeCloseTo(
    0
  );
  expect(dates.differenceInSeconds(recentDate, oldDate)).toBe(113356800);
});
