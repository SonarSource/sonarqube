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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from '../../../helpers/testUtils';
import { FCProps } from '../../../types/misc';
import { RadioButton } from '../RadioButton';

const value = 'value';

it('should render properly', () => {
  setupWithProps();
  expect(screen.getByRole('radio')).not.toBeChecked();
});

it('should render properly when checked', () => {
  setupWithProps({ checked: true });
  expect(screen.getByRole('radio')).toBeChecked();
});

it('should invoke callback on click', async () => {
  const user = userEvent.setup();
  const onCheck = jest.fn();
  setupWithProps({ onCheck, value });

  await user.click(screen.getByRole('radio'));
  expect(onCheck).toHaveBeenCalled();
});

it('should not invoke callback on click when disabled', async () => {
  const user = userEvent.setup();
  const onCheck = jest.fn();
  setupWithProps({ disabled: true, onCheck });

  await user.click(screen.getByRole('radio'));
  expect(onCheck).not.toHaveBeenCalled();
});

function setupWithProps(props?: Partial<FCProps<typeof RadioButton>>) {
  return render(
    <RadioButton checked={false} onCheck={jest.fn()} value="value" {...props}>
      foo
    </RadioButton>,
  );
}
