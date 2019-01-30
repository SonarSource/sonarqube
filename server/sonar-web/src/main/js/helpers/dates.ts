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
import * as _differenceInDays from 'date-fns/difference_in_days';
import * as _differenceInHours from 'date-fns/difference_in_hours';
import * as _differenceInSeconds from 'date-fns/difference_in_seconds';
import * as _differenceInYears from 'date-fns/difference_in_years';
import * as _isSameDay from 'date-fns/is_same_day';
import * as _startOfDay from 'date-fns/start_of_day';
import * as parse from 'date-fns/parse';

function pad(number: number) {
  if (number < 10) {
    return '0' + number;
  }
  return number;
}

type ParsableDate = string | number | Date;

export function parseDate(rawDate: ParsableDate): Date {
  return parse(rawDate);
}

export function toShortNotSoISOString(rawDate: ParsableDate): string {
  const date = parseDate(rawDate);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function toNotSoISOString(rawDate: ParsableDate): string {
  const date = parseDate(rawDate);
  return date.toISOString().replace(/\..+Z$/, '+0000');
}

export function startOfDay(date: Date): Date {
  return _startOfDay(date);
}

export function isValidDate(date: Date): boolean {
  return !isNaN(date.getTime());
}

export function isSameDay(dateLeft: Date, dateRight: Date): boolean {
  return _isSameDay(dateLeft, dateRight);
}

export function differenceInYears(dateLeft: ParsableDate, dateRight: ParsableDate): number {
  return _differenceInYears(dateLeft, dateRight);
}

export function differenceInDays(dateLeft: ParsableDate, dateRight: ParsableDate): number {
  return _differenceInDays(dateLeft, dateRight);
}

export function differenceInHours(dateLeft: ParsableDate, dateRight: ParsableDate): number {
  return _differenceInHours(dateLeft, dateRight);
}

export function differenceInSeconds(dateLeft: ParsableDate, dateRight: ParsableDate): number {
  return _differenceInSeconds(dateLeft, dateRight);
}
