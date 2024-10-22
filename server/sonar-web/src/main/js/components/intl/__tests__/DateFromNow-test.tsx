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

import { screen } from '@testing-library/react';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import DateFromNow, { DateFromNowProps } from '../DateFromNow';

jest.mock('../dateUtils', () => ({
  getRelativeTimeProps: jest.fn().mockReturnValue({ value: -1, unit: 'year' }),
}));

const date = '2020-02-20T20:20:20Z';

it('should render correctly', () => {
  renderDateFromNow({ date });

  expect(screen.getByText('1 year ago')).toBeInTheDocument();
});

it('should render correctly when there is no date or no children', () => {
  renderDateFromNow({ date: undefined });

  expect(screen.getByText('never')).toBeInTheDocument();
});

it('should render correctly when the date is less than one hour in the past', () => {
  const veryCloseDate = new Date(date);
  veryCloseDate.setMinutes(veryCloseDate.getMinutes() - 10);
  const mockDateNow = jest
    .spyOn(Date, 'now')
    .mockImplementation(() => new Date(date) as unknown as number);

  renderDateFromNow({ date: veryCloseDate, hourPrecision: true });

  expect(screen.getByText('less_than_1_hour_ago')).toBeInTheDocument();
  mockDateNow.mockRestore();
});

function renderDateFromNow(
  overrides: Partial<DateFromNowProps> = {},
  children: jest.Mock = jest.fn((d) => <>{d}</>),
) {
  return renderComponent(
    <DateFromNow date={date} {...overrides}>
      {children}
    </DateFromNow>,
  );
}
