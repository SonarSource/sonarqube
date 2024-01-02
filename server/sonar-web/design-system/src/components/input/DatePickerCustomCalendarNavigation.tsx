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
  format,
  getYear,
  isSameMonth,
  isSameYear,
  setMonth,
  setYear,
  startOfMonth,
} from 'date-fns';
import { range } from 'lodash';
import {
  CaptionProps,
  useNavigation as useCalendarNavigation,
  useDayPicker,
} from 'react-day-picker';
import { useIntl } from 'react-intl';
import { InteractiveIcon } from '../InteractiveIcon';
import { ChevronLeftIcon, ChevronRightIcon } from '../icons';
import { InputSelect } from './InputSelect';

const YEARS_TO_DISPLAY = 10;
const MONTHS_IN_A_YEAR = 12;

export function CustomCalendarNavigation(props: CaptionProps) {
  const { displayMonth } = props;
  const { fromYear, toYear } = useDayPicker();
  const { goToMonth, nextMonth, previousMonth } = useCalendarNavigation();

  const intl = useIntl();

  const formatChevronLabel = (date?: Date) => {
    if (date === undefined) {
      return intl.formatMessage({ id: 'disabled_' });
    }
    return `${intl.formatDate(date, { month: 'long', format: 'M' })} ${intl.formatDate(date, {
      year: 'numeric',
      format: 'y',
    })}`;
  };

  const baseDate = startOfMonth(displayMonth); // reference date

  const months = range(MONTHS_IN_A_YEAR).map((month) => {
    const monthValue = setMonth(baseDate, month);

    return {
      label: format(monthValue, 'MMM'),
      value: monthValue,
    };
  });

  const startYear = fromYear ?? getYear(Date.now()) - YEARS_TO_DISPLAY;

  const years = range(startYear, toYear ? toYear + 1 : undefined).map((year) => {
    const yearValue = setYear(baseDate, year);

    return {
      label: String(year),
      value: yearValue,
    };
  });

  return (
    <nav className="sw-flex sw-items-center sw-justify-between sw-py-1">
      <InteractiveIcon
        Icon={ChevronLeftIcon}
        aria-label={intl.formatMessage(
          { id: 'previous_month_x' },
          { month: formatChevronLabel(previousMonth) },
        )}
        className="sw-mr-2"
        disabled={previousMonth === undefined}
        onClick={() => {
          if (previousMonth) {
            goToMonth(previousMonth);
          }
        }}
        size="small"
      />

      <span data-testid="month-select">
        <InputSelect
          isClearable={false}
          onChange={(value) => {
            if (value) {
              goToMonth(value.value);
            }
          }}
          options={months}
          size="full"
          value={months.find((m) => isSameMonth(m.value, displayMonth))}
        />
      </span>

      <span data-testid="year-select">
        <InputSelect
          className="sw-ml-1"
          data-testid="year-select"
          isClearable={false}
          onChange={(value) => {
            if (value) {
              goToMonth(value.value);
            }
          }}
          options={years}
          size="full"
          value={years.find((y) => isSameYear(y.value, displayMonth))}
        />
      </span>

      <InteractiveIcon
        Icon={ChevronRightIcon}
        aria-label={intl.formatMessage(
          { id: 'next_month_x' },
          {
            month: formatChevronLabel(nextMonth),
          },
        )}
        className="sw-ml-2"
        disabled={nextMonth === undefined}
        onClick={() => {
          if (nextMonth) {
            goToMonth(nextMonth);
          }
        }}
        size="small"
      />
    </nav>
  );
}
