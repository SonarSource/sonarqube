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
import { ParsableDate } from '../types/dates';

function pad(number: number) {
  if (number < 10) {
    return '0' + number.toString();
  }
  return number;
}

export function parseDate(rawDate: ParsableDate): Date {
  return new Date(rawDate);
}

export function toShortNotSoISOString(rawDate: ParsableDate): string {
  const date = parseDate(rawDate);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function toNotSoISOString(rawDate: ParsableDate): string {
  const date = parseDate(rawDate);
  const dateWithoutZone = date.toISOString().split('.')[0];
  return dateWithoutZone + '+0000';
}

export function isValidDate(date: Date): boolean {
  return !isNaN(date.getTime());
}

export function now() {
  return new Date();
}
