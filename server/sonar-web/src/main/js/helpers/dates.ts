/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

const MILLISECONDS_IN_MINUTE = 60 * 1000;
const MILLISECONDS_IN_DAY = MILLISECONDS_IN_MINUTE * 60 * 24;

function pad(number: number) {
  if (number < 10) {
    return '0' + number;
  }
  return number;
}

function compareDateAsc(dateLeft: Date, dateRight: Date): number {
  var timeLeft = dateLeft.getTime();
  var timeRight = dateRight.getTime();

  if (timeLeft < timeRight) {
    return -1;
  } else if (timeLeft > timeRight) {
    return 1;
  } else {
    return 0;
  }
}

export function toShortNotSoISOString(date: Date): string {
  return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate());
}

export function toNotSoISOString(date: Date): string {
  return date.toISOString().replace(/\..+Z$/, '+0000');
}

export function startOfDay(date: Date): Date {
  const startDay = new Date(date);
  startDay.setHours(0, 0, 0, 0);
  return startDay;
}

export function isValidDate(date: Date): boolean {
  return !isNaN(date.getTime());
}

export function isSameDay(dateLeft: Date, dateRight: Date): boolean {
  const startDateLeft = startOfDay(dateLeft);
  const startDateRight = startOfDay(dateRight);
  return startDateLeft.getTime() === startDateRight.getTime();
}

export function differenceInYears(dateLeft: Date, dateRight: Date): number {
  const sign = compareDateAsc(dateLeft, dateRight);
  const diff = Math.abs(dateLeft.getFullYear() - dateRight.getFullYear());
  const tmpLeftDate = new Date(dateLeft);
  tmpLeftDate.setFullYear(dateLeft.getFullYear() - sign * diff);
  const isLastYearNotFull = compareDateAsc(tmpLeftDate, dateRight) === -sign;
  return sign * (diff - (isLastYearNotFull ? 1 : 0));
}

export function differenceInDays(dateLeft: Date, dateRight: Date): number {
  const startDateLeft = startOfDay(dateLeft);
  const startDateRight = startOfDay(dateRight);
  const timestampLeft =
    startDateLeft.getTime() - startDateLeft.getTimezoneOffset() * MILLISECONDS_IN_MINUTE;
  const timestampRight =
    startDateRight.getTime() - startDateRight.getTimezoneOffset() * MILLISECONDS_IN_MINUTE;
  return Math.round((timestampLeft - timestampRight) / MILLISECONDS_IN_DAY);
}

export function differenceInSeconds(dateLeft: Date, dateRight: Date): number {
  const diff = (dateLeft.getTime() - dateRight.getTime()) / 1000;
  return diff > 0 ? Math.floor(diff) : Math.ceil(diff);
}
