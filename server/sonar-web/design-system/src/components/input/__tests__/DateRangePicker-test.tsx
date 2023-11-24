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
import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { formatISO, parseISO } from 'date-fns';
import { byRole } from '../../../../../src/main/js/helpers/testSelector';
import { IntlWrapper, render } from '../../../helpers/testUtils';
import { DateRangePicker } from '../DateRangePicker';

beforeEach(() => {
  jest.useFakeTimers().setSystemTime(parseISO('2022-06-12'));
});

afterEach(() => {
  jest.runOnlyPendingTimers();
  jest.useRealTimers();
});

it('behaves correctly', async () => {
  const nav = byRole('navigation');
  // Remove delay to play nice with fake timers
  const user = userEvent.setup({ delay: null });

  const onChange = jest.fn((_: { from?: Date; to?: Date }) => undefined);
  renderDateRangePicker({ onChange });

  await user.click(screen.getByRole('textbox', { name: 'from' }));

  const fromNav = nav.get();
  expect(fromNav).toBeInTheDocument();

  await user.click(nav.byRole('button', { name: 'previous_month_x' }).get());
  await user.click(screen.getByText('7'));

  expect(fromNav).not.toBeInTheDocument();

  expect(onChange).toHaveBeenCalled();
  const { from } = onChange.mock.calls[0][0]; // first argument
  // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
  expect(formatISO(from!, { representation: 'date' })).toBe('2022-05-07');

  onChange.mockClear();

  act(() => {
    jest.runAllTimers();
  });

  const previousButton = nav.byRole('button', { name: 'previous_month_x' });
  const nextButton = nav.byRole('button', { name: 'next_month_x' });
  await user.click(previousButton.get());
  await user.click(nextButton.get());
  await user.click(previousButton.get());
  await user.click(screen.getByText('12'));

  expect(nav.query()).not.toBeInTheDocument();

  expect(onChange).toHaveBeenCalled();
  const { to } = onChange.mock.calls[0][0]; // first argument
  // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
  expect(formatISO(to!, { representation: 'date' })).toBe('2022-05-12');
});

function renderDateRangePicker(overrides: Partial<DateRangePicker['props']> = {}) {
  const defaultFormatter = (date?: Date) =>
    date ? formatISO(date, { representation: 'date' }) : '';

  render(
    <IntlWrapper messages={{ next_: 'next', previous_: 'previous' }}>
      <DateRangePicker
        clearButtonLabel="clear"
        fromLabel="from"
        onChange={jest.fn()}
        toLabel="to"
        valueFormatter={defaultFormatter}
        {...overrides}
      />
    </IntlWrapper>,
  );
}
