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
import userEvent from '@testing-library/user-event';
import { getMonth, getYear, parseISO } from 'date-fns';
import { byRole } from '../../../../../src/main/js/sonar-aligned/helpers/testSelector';
import { renderWithContext } from '../../../helpers/testUtils';
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

  await user.click(byRole('navigation').byRole('button', { name: 'previous_month_x' }).get());
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
  await user.click(screen.getByRole('button', { name: 'next_month_x' }));
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

it('should disable next navigation when not in the accepted range', async () => {
  const user = userEvent.setup();

  const currentDate = parseISO('2022-11-13');

  renderDatePicker({
    currentMonth: currentDate,
    maxDate: parseISO('2022-12-30'),
    value: currentDate,
    // eslint-disable-next-line jest/no-conditional-in-test
    valueFormatter: (date?: Date) => (date ? 'formatted date' : 'no date'),
  });

  await user.click(screen.getByRole('textbox'));
  await user.click(screen.getByRole('button', { name: 'next_month_x' }));

  expect(screen.getByRole('button', { name: 'next_month_x' })).toBeDisabled();
});

it('should clear the value', async () => {
  const user = userEvent.setup();

  const onChange = jest.fn((_: Date) => undefined);

  const currentDate = parseISO('2022-06-13');

  renderDatePicker({
    currentMonth: currentDate,
    onChange,
    showClearButton: true,
    value: currentDate,
    // eslint-disable-next-line jest/no-conditional-in-test
    valueFormatter: (date?: Date) => (date ? 'formatted date' : 'no date'),
  });

  await user.click(screen.getByRole('textbox'));

  await user.click(screen.getByLabelText('clear'));

  expect(onChange).toHaveBeenCalledWith(undefined);
});

it.each([
  [{ highlightFrom: parseISO('2022-06-12'), value: parseISO('2022-06-14') }],
  [{ alignRight: true, highlightTo: parseISO('2022-06-14'), value: parseISO('2022-06-12') }],
])('highlights the appropriate days', async (props) => {
  const user = userEvent.setup();

  const hightlightClass = 'rdp-highlighted';

  renderDatePicker(props);

  await user.click(screen.getByRole('textbox'));

  expect(screen.getByText('11')).not.toHaveClass(hightlightClass);
  expect(screen.getByText('12')).toHaveClass(hightlightClass);
  expect(screen.getByText('13')).toHaveClass(hightlightClass);
  expect(screen.getByText('14')).toHaveClass(hightlightClass);
  expect(screen.getByText('15')).not.toHaveClass(hightlightClass);
});

function renderDatePicker(overrides: Partial<DatePicker['props']> = {}) {
  const defaultFormatter = (date?: Date) => (date ? date.toISOString() : '');

  renderWithContext(
    <DatePicker
      clearButtonLabel="clear"
      onChange={jest.fn()}
      placeholder="placeholder"
      valueFormatter={defaultFormatter}
      {...overrides}
    />,
  );
}
