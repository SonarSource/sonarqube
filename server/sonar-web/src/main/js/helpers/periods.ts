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
import { parseDate } from '../helpers/dates';
import { translate, translateWithParameters } from '../helpers/l10n';
import { ApplicationPeriod } from '../types/application';
import { Period } from '../types/types';

export function getPeriodLabel(
  period: Period | undefined,
  dateFormatter: (date: string) => string
) {
  if (!period) {
    return undefined;
  }

  let parameter = period.modeParam || period.parameter || '';

  switch (period.mode) {
    case 'SPECIFIC_ANALYSIS':
      parameter = dateFormatter(period.date);
      break;
    case 'PREVIOUS_VERSION':
      parameter = parameter || dateFormatter(period.date);
      break;
    /*
     * Handle legacy period modes, that predate MMF-1579
     */
    case 'previous_version':
      if (!parameter) {
        return translate('overview.period.previous_version_only_date');
      }
      break;
    case 'date':
      parameter = parameter && dateFormatter(parameter);
      break;
    case 'manual_baseline':
      parameter = parameter || dateFormatter(period.date);
      break;
    default: // No change in the parameter
  }

  return translateWithParameters(`overview.period.${period.mode.toLowerCase()}`, parameter);
}

export function getPeriodDate(period?: { date?: string }): Date | undefined {
  return period && period.date ? parseDate(period.date) : undefined;
}

export function isApplicationPeriod(
  period: Period | ApplicationPeriod
): period is ApplicationPeriod {
  return (period as ApplicationPeriod).project !== undefined;
}
