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
import { translate, translateWithParameters } from './l10n';
import { parseDate } from './dates';

export interface Period {
  date: string;
  index: number;
  mode: string;
  modeParam?: string;
  parameter: string;
}

export function getPeriod(periods: Period[] | undefined, index: number): Period | undefined {
  if (!Array.isArray(periods)) {
    return undefined;
  }

  return periods.find(period => period.index === index);
}

export function getLeakPeriod(periods: Period[] | undefined): Period | undefined {
  return getPeriod(periods, 1);
}

export function getPeriodLabel(period: Period | undefined): string | undefined {
  if (!period) {
    return undefined;
  }

  const parameter = period.modeParam || period.parameter;

  if (period.mode === 'previous_version' && !parameter) {
    return translate('overview.period.previous_version_only_date');
  }

  return translateWithParameters(`overview.period.${period.mode}`, parameter);
}

export function getPeriodDate(period?: { date?: string }): Date | undefined {
  return period && period.date ? parseDate(period.date) : undefined;
}

export function getLeakPeriodLabel(periods: Period[]): string | undefined {
  return getPeriodLabel(getLeakPeriod(periods));
}
