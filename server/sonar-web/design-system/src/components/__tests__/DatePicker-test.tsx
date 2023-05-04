/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { getMonth, getYear, parseISO } from 'date-fns';
import { render } from '../../helpers/testUtils';
import { DatePicker } from '../DatePicker';

it('behaves correctly', async () => {
  const user = userEvent.setup();

  const onChange = jest.fn((_: Date) => undefined);
  const currentMonth = parseISO('2022-06-13');
  renderDatePicker({ currentMonth, onChange });

  /*
   * Open the DatePicker, navigate to the previous month and choose an arbitrary day (7)
   * Then check that onChange was correctly called with a date in the previous month
   */
  await user.click(screen.getByRole('textbox'));

  const nav = screen.getByRole('navigation');
  expect(nav).toBeInTheDocument();

  await user.click(within(nav).getByRole('button', { name: 'previous' }));
  await user.click(screen.getByText('7'));

  expect(onChange).toHaveBeenCalled();
  const newDate = onChange.mock.calls[0][0]; // first argument of the first and only call
  expect(getMonth(newDate)).toBe(getMonth(currentMonth) - 1);

  onChange.mockClear();

  /*
   * Open the DatePicker, navigate to the next month twice and choose an arbitrary day (12)
   * Then check that onChange was correctly called with a date in the following month
   */
  await user.click(screen.getByRole('textbox'));
  const nextButton = screen.getByRole('button', { name: 'next' });
  await user.click(nextButton);
  await user.click(nextButton);
  await user.click(screen.getByText('12'));

  expect(onChange).toHaveBeenCalled();
  const newDate2 = onChange.mock.calls[0][0]; // first argument
  expect(getMonth(newDate2)).toBe(getMonth(currentMonth) + 1);

  onChange.mockClear();

  /*
   * Open the DatePicker, select the month, select the year and choose an arbitrary day (10)
   * Then check that onChange was correctly called with a date in the selected month & year
   */
  await user.click(screen.getByRole('textbox'));
  // Select month
  await user.click(screen.getByText('Jun'));
  await user.click(screen.getByText('Feb'));

  // Select year
  await user.click(screen.getByText('2022'));
  await user.click(screen.getByText('2019'));

  await user.click(screen.getByText('10'));

  const newDate3 = onChange.mock.calls[0][0]; // first argument

  expect(getMonth(newDate3)).toBe(1);
  expect(getYear(newDate3)).toBe(2019);
});

it('highlights the appropriate days', async () => {
  const user = userEvent.setup();

  const value = parseISO('2022-06-14');
  renderDatePicker({ highlightFrom: parseISO('2022-06-12'), showClearButton: true, value });

  await user.click(screen.getByRole('textbox'));

  expect(screen.getByText('11')).not.toHaveClass('rdp-highlighted');
  expect(screen.getByText('12')).toHaveClass('rdp-highlighted');
  expect(screen.getByText('13')).toHaveClass('rdp-highlighted');
  expect(screen.getByText('14')).toHaveClass('rdp-highlighted');
  expect(screen.getByText('15')).not.toHaveClass('rdp-highlighted');
});

function renderDatePicker(overrides: Partial<DatePicker['props']> = {}) {
  const defaultFormatter = (date?: Date) => (date ? date.toISOString() : '');

  render(
    <DatePicker
      ariaNextMonthLabel="next"
      ariaPreviousMonthLabel="previous"
      clearButtonLabel="clear"
      onChange={jest.fn()}
      placeholder="placeholder"
      valueFormatter={defaultFormatter}
      {...overrides}
    />
  );
}
