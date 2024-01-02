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
import * as React from 'react';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import ButtonToggle, { ButtonToggleProps } from '../ButtonToggle';

it('should behave properly', async () => {
  const onCheck = jest.fn();
  const user = userEvent.setup();

  render({ onCheck });
  expect(screen.getAllByRole('button')).toHaveLength(3);

  await user.click(screen.getByRole('button', { name: 'first' }));
  expect(onCheck).toHaveBeenCalledWith('one');

  await user.click(screen.getByRole('button', { name: 'second' }));
  expect(onCheck).not.toHaveBeenLastCalledWith('two');
});

it('should behave properly when disabled', async () => {
  const onCheck = jest.fn();
  const user = userEvent.setup();

  render({ disabled: true, onCheck });

  await user.click(screen.getByRole('button', { name: 'first' }));
  expect(onCheck).not.toHaveBeenCalled();
});

function render(props?: Partial<ButtonToggleProps>) {
  renderComponent(
    <ButtonToggle
      label="test-label"
      onCheck={jest.fn()}
      disabled={false}
      options={[
        { value: 'one', label: 'first' },
        { value: 'two', label: 'second' },
        { value: 'tree', label: 'third' },
      ]}
      value="two"
      {...props}
    />
  );
}
