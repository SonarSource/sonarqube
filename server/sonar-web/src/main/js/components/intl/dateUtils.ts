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
import {
  differenceInDays,
  differenceInMonths,
  differenceInSeconds,
  differenceInYears,
} from 'date-fns';
import { Props as FormattedRelativeTimeProps } from 'react-intl/src/components/relative';
import { parseDate } from '../../helpers/dates';
import { ParsableDate } from '../../types/dates';

const UPDATE_INTERVAL_IN_SECONDS = 10;

export function getRelativeTimeProps(
  parsableDate: ParsableDate,
): Pick<FormattedRelativeTimeProps, 'unit' | 'value' | 'updateIntervalInSeconds'> {
  const date = parseDate(parsableDate);
  const y = differenceInYears(date, Date.now());

  if (Math.abs(y) > 0) {
    return { value: y, unit: 'year' };
  }

  const m = differenceInMonths(date, Date.now());
  if (Math.abs(m) > 0) {
    return { value: m, unit: 'month' };
  }

  const d = differenceInDays(date, Date.now());
  if (Math.abs(d) > 0) {
    return { value: d, unit: 'day' };
  }

  return {
    value: differenceInSeconds(date, Date.now()),
    unit: 'second',
    updateIntervalInSeconds: UPDATE_INTERVAL_IN_SECONDS,
  };
}
