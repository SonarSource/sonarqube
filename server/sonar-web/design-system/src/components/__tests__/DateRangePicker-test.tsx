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
import { formatISO, parseISO } from 'date-fns';
import { render } from '../../helpers/testUtils';
import { DateRangePicker } from '../DateRangePicker';

beforeEach(() => {
  jest.useFakeTimers().setSystemTime(parseISO('2022-06-12'));
});

afterEach(() => {
  jest.runOnlyPendingTimers();
  jest.useRealTimers();
});

it('behaves correctly', async () => {
  // Remove delay to play nice with fake timers
  const user = userEvent.setup({ delay: null });

  const onChange = jest.fn((_: { from?: Date; to?: Date }) => undefined);
  renderDateRangePicker({ onChange });

  await user.click(screen.getByRole('textbox', { name: 'from' }));

  const fromDateNav = screen.getByRole('navigation');
  expect(fromDateNav).toBeInTheDocument();

  await user.click(within(fromDateNav).getByRole('button', { name: 'previous' }));
  await user.click(screen.getByText('7'));

  expect(screen.queryByRole('navigation')).not.toBeInTheDocument();

  expect(onChange).toHaveBeenCalled();
  const { from } = onChange.mock.calls[0][0]; // first argument
  // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
  expect(formatISO(from!, { representation: 'date' })).toBe('2022-05-07');

  onChange.mockClear();

  jest.runAllTimers();

  const toDateNav = await screen.findByRole('navigation');
  const previousButton = within(toDateNav).getByRole('button', { name: 'previous' });
  const nextButton = within(toDateNav).getByRole('button', { name: 'next' });
  await user.click(previousButton);
  await user.click(nextButton);
  await user.click(previousButton);
  await user.click(screen.getByText('12'));

  expect(screen.queryByRole('navigation')).not.toBeInTheDocument();

  expect(onChange).toHaveBeenCalled();
  const { to } = onChange.mock.calls[0][0]; // first argument
  // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
  expect(formatISO(to!, { representation: 'date' })).toBe('2022-05-12');
});

function renderDateRangePicker(overrides: Partial<DateRangePicker['props']> = {}) {
  const defaultFormatter = (date?: Date) =>
    date ? formatISO(date, { representation: 'date' }) : '';

  render(
    <DateRangePicker
      ariaNextMonthLabel="next"
      ariaPreviousMonthLabel="previous"
      clearButtonLabel="clear"
      fromLabel="from"
      onChange={jest.fn()}
      toLabel="to"
      valueFormatter={defaultFormatter}
      {...overrides}
    />
  );
}
