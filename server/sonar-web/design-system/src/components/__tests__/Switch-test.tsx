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
import { render } from '../../helpers/testUtils';
import { Switch } from '../Switch';

const defaultProps = {
  value: false,
};

it('renders switch correctly if value is false and change to true on click', async () => {
  const user = userEvent.setup();
  const onChange = jest.fn();

  render(<Switch {...defaultProps} onChange={onChange} />);
  const switchContainer = screen.getByRole('switch');
  expect(switchContainer).not.toBeChecked();

  await user.click(switchContainer);

  expect(onChange).toHaveBeenCalledWith(true);
});

it('renders switch correctly if value is true and change to false on click', async () => {
  const user = userEvent.setup();
  const onChange = jest.fn();

  render(<Switch {...defaultProps} onChange={onChange} value />);
  const switchContainer = screen.getByRole('switch');
  expect(switchContainer).toBeChecked();

  await user.click(switchContainer);

  expect(onChange).toHaveBeenCalledWith(false);
});

it('renders switch correctly if value is true and disabled', () => {
  render(<Switch {...defaultProps} disabled value />);
  const switchContainer = screen.getByRole('switch');
  expect(switchContainer).toBeDisabled();
});
