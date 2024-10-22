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
import { render } from '../../../helpers/testUtils';
import { FCProps } from '../../../types/misc';
import { getTabPanelId } from '../../helpers';
import { ToggleButton, ToggleButtonsOption } from '../ToggleButton';

it('should render all options', async () => {
  const user = userEvent.setup();
  const onChange = jest.fn();
  const options: Array<ToggleButtonsOption<number>> = [
    { value: 1, label: 'first' },
    { value: 2, label: 'disabled', disabled: true },
    { value: 3, label: 'has counter', counter: 7 },
  ];
  renderToggleButtons({ onChange, options, value: 1 });

  expect(screen.getAllByRole('radio')).toHaveLength(3);

  await user.click(screen.getByText('first'));

  expect(onChange).not.toHaveBeenCalled();

  await user.click(screen.getByText('has counter'));

  expect(onChange).toHaveBeenCalledWith(3);
});

it('should work in tablist mode', () => {
  const onChange = jest.fn();
  const options: Array<ToggleButtonsOption<number>> = [
    { value: 1, label: 'first' },
    { value: 2, label: 'second' },
    { value: 3, label: 'third' },
  ];
  renderToggleButtons({ onChange, options, value: 1, role: 'tablist' });

  expect(screen.getAllByRole('tab')).toHaveLength(3);
  expect(screen.getByRole('tab', { name: 'second' })).toHaveAttribute(
    'aria-controls',
    getTabPanelId(2),
  );
});

function renderToggleButtons(props: Partial<FCProps<typeof ToggleButton>> = {}) {
  return render(<ToggleButton onChange={jest.fn()} options={[]} {...props} />);
}
