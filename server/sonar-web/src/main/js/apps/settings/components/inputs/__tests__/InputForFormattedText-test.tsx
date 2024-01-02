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
import { mockSetting } from '../../../../../helpers/mocks/settings';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { DefaultSpecializedInputProps } from '../../../utils';
import InputForFormattedText from '../InputForFormattedText';

it('should render correctly with no value for login message', () => {
  renderInputForFormattedText();
  expect(screen.getByRole('textbox')).toBeInTheDocument();
});

it('should render correctly with a value for login message', () => {
  renderInputForFormattedText({
    setting: mockSetting({ values: ['*text*', 'text'], hasValue: true }),
  });
  expect(screen.getByRole('button', { name: 'edit' })).toBeInTheDocument();
  expect(screen.getByText('text')).toBeInTheDocument();
});

it('should render correctly with a value for login message if hasValue is set', () => {
  renderInputForFormattedText({
    setting: mockSetting({ hasValue: true }),
  });
  expect(screen.getByRole('button', { name: 'edit' })).toBeInTheDocument();
});

it('should render editMode when value is empty', () => {
  renderInputForFormattedText({
    value: '',
  });
  expect(screen.getByRole('textbox')).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'edit' })).not.toBeInTheDocument();
});

it('should render correctly if in editMode', async () => {
  const user = userEvent.setup();
  const onChange = jest.fn();

  renderInputForFormattedText({
    setting: mockSetting({ values: ['*text*', 'text'], hasValue: true }),
    isEditing: true,
    onChange,
  });
  expect(screen.getByRole('textbox')).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'edit' })).not.toBeInTheDocument();

  await user.click(screen.getByRole('textbox'));
  await user.keyboard('N');
  expect(onChange).toHaveBeenCalledTimes(1);
});

function renderInputForFormattedText(props: Partial<DefaultSpecializedInputProps> = {}) {
  renderComponent(
    <InputForFormattedText
      onEditing={jest.fn()}
      isEditing={false}
      isDefault={true}
      name="name"
      onChange={jest.fn()}
      setting={mockSetting({ value: undefined, hasValue: false })}
      value="*text*"
      {...props}
    />
  );
}
