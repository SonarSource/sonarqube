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

import { screen, waitFor } from '@testing-library/react';
import { renderWithContext } from '../../../helpers/testUtils';
import { FCProps } from '../../../types/misc';
import { InputSearch } from '../InputSearch';

it('should warn when input is too short', async () => {
  const { user } = setupWithProps({ value: 'f' });
  expect(screen.getByRole('note')).toBeInTheDocument();
  await user.type(screen.getByRole('searchbox'), 'oo');
  expect(screen.queryByRole('note')).not.toBeInTheDocument();
});

it('should show clear button only when there is a value', async () => {
  const { user } = setupWithProps({ value: 'f' });
  expect(screen.getByRole('button')).toBeInTheDocument();
  await user.clear(screen.getByRole('searchbox'));
  expect(screen.queryByRole('button')).not.toBeInTheDocument();
});

it('should attach ref', () => {
  const ref = jest.fn() as jest.Mock<unknown, unknown[]>;
  setupWithProps({ innerRef: ref });
  expect(ref).toHaveBeenCalled();
  expect(ref.mock.calls[0][0]).toBeInstanceOf(HTMLInputElement);
});

it('should trigger reset correctly with clear button', async () => {
  const onChange = jest.fn();
  const { user } = setupWithProps({ onChange });
  await user.click(screen.getByRole('button'));
  expect(onChange).toHaveBeenCalledWith('');
});

it('should trigger change correctly', async () => {
  const onChange = jest.fn();
  const { user } = setupWithProps({ onChange, value: 'f' });
  await user.type(screen.getByRole('searchbox'), 'oo');
  await waitFor(() => {
    expect(onChange).toHaveBeenCalledWith('foo');
  });
});

it('should not change when value is too short', async () => {
  const onChange = jest.fn();
  const { user } = setupWithProps({ onChange, value: '', minLength: 3 });
  await user.type(screen.getByRole('searchbox'), 'fo');
  expect(onChange).not.toHaveBeenCalled();
});

it('should clear input using escape', async () => {
  const onChange = jest.fn();
  const { user } = setupWithProps({ onChange, value: 'foo' });
  await user.type(screen.getByRole('searchbox'), '{Escape}');
  expect(onChange).toHaveBeenCalledWith('');
});

function setupWithProps(props: Partial<FCProps<typeof InputSearch>> = {}) {
  return renderWithContext(
    <InputSearch
      maxLength={150}
      minLength={2}
      onChange={jest.fn()}
      placeholder="placeholder"
      searchInputAriaLabel=""
      value="foo"
      {...props}
    />,
  );
}
