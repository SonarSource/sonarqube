/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

it('renders correctly with no value for login message', () => {
  renderInputForFormattedText();
  expect(screen.getByRole('textbox')).toBeInTheDocument();
});

it('renders correctly with a value for login message', async () => {
  const user = userEvent.setup();
  renderInputForFormattedText({
    setting: mockSetting({ values: ['*text*', 'text'], hasValue: true })
  });
  expect(screen.getByRole('button', { name: 'edit' })).toBeInTheDocument();
  expect(screen.getByText('text')).toBeInTheDocument();

  await user.click(screen.getByRole('button', { name: 'edit' }));
  expect(screen.getByRole('textbox')).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'edit' })).not.toBeInTheDocument();
});

function renderInputForFormattedText(props: Partial<DefaultSpecializedInputProps> = {}) {
  renderComponent(
    <InputForFormattedText
      isDefault={true}
      name="name"
      onChange={jest.fn()}
      setting={mockSetting({ value: undefined, hasValue: false })}
      value="*text*"
      {...props}
    />
  );
}
