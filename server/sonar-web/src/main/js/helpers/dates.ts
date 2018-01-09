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
import {
  differenceInDays as _differenceInDays,
  differenceInSeconds as _differenceInSeconds,
  differenceInYears as _differenceInYears,
  isSameDay as _isSameDay,
  parse,
  startOfDay as _startOfDay
} from 'date-fns';

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

export function differenceInYears(dateLeft: Date, dateRight: Date): number {
  return _differenceInYears(dateLeft, dateRight);
}

export function differenceInDays(dateLeft: Date, dateRight: Date): number {
  return _differenceInDays(dateLeft, dateRight);
}

export function differenceInSeconds(dateLeft: Date, dateRight: Date): number {
  return _differenceInSeconds(dateLeft, dateRight);
}
