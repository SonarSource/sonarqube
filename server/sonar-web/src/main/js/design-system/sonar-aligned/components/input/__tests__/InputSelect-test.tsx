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
import { renderWithContext } from '../../../../helpers/testUtils';
import { FCProps } from '../../../../types/misc';
import { InputSelect } from '../InputSelect';

it('should render select input and be able to click and change', async () => {
  const setValue = jest.fn();
  const user = userEvent.setup();
  setupWithProps({ placeholder: 'placeholder-foo', onChange: setValue });
  expect(screen.getByText('placeholder-foo')).toBeInTheDocument();
  await user.click(screen.getByRole('combobox'));
  expect(screen.getByText(/option foo-bar focused/)).toBeInTheDocument();
  expect(screen.getByRole('note', { name: 'Icon' })).toBeInTheDocument();
  await user.click(screen.getByText('bar-foo'));
  expect(setValue).toHaveBeenCalled();
  expect(screen.queryByText('placeholder-foo')).not.toBeInTheDocument();
  expect(screen.getByRole('note', { name: 'Icon' })).toBeInTheDocument();
});

it('should render select input with clearable', async () => {
  const setValue = jest.fn();
  const user = userEvent.setup();
  setupWithProps({
    placeholder: 'placeholder-foo',
    onChange: setValue,
    isClearable: true,
    clearLabel: 'clear-label',
  });
  expect(screen.getByText('placeholder-foo')).toBeInTheDocument();
  await user.click(screen.getByRole('combobox'));
  expect(screen.getByText(/option foo-bar focused/)).toBeInTheDocument();
  expect(screen.getByRole('note', { name: 'Icon' })).toBeInTheDocument();
  await user.click(screen.getByText('bar-foo'));
  expect(setValue).toHaveBeenCalled();
  expect(screen.queryByText('placeholder-foo')).not.toBeInTheDocument();
  expect(screen.getByRole('note', { name: 'Icon' })).toBeInTheDocument();

  // Clear button container aria-hidden by default
  expect(screen.getByRole('button', { name: 'clear-label', hidden: true })).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: 'clear-label', hidden: true }));
  expect(screen.getByText('placeholder-foo')).toBeInTheDocument();
});

it('should render select input with disabled prop', () => {
  const setValue = jest.fn();
  setupWithProps({ placeholder: 'placeholder-foo', onChange: setValue, isDisabled: true });
  expect(screen.getByText('placeholder-foo')).toBeInTheDocument();
  expect(screen.getByRole('combobox')).toBeDisabled();
});

it('should render the select options with sorting when shouldSortOption is true and getOptionLabel passed', async () => {
  const { user } = setupWithProps({
    shouldSortOption: true,
    getOptionLabel: (o: { label: string }) => o.label,
  });
  await user.click(screen.getByRole('combobox'));
  const options = screen.getAllByRole('option');
  expect(options).toHaveLength(2);
  expect(options[0]).toHaveTextContent('bar-foo');
  expect(options[1]).toHaveTextContent('foo-bar');
});

function setupWithProps(props: Partial<FCProps<typeof InputSelect>>) {
  return renderWithContext(
    <InputSelect
      {...props}
      options={[
        { label: 'foo-bar', value: 'foo' },
        {
          label: 'bar-foo',
          value: 'bar',
          Icon: (
            <span role="note" title="Icon">
              Icon
            </span>
          ),
        },
      ]}
    />,
  );
}
