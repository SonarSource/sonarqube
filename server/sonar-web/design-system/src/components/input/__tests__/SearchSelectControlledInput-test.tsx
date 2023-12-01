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
import { renderWithContext } from '../../../helpers/testUtils';
import { FCProps } from '../../../types/misc';
import { InputSearch } from '../InputSearch';
import { SearchSelectControlledInput } from '../SearchSelectControlledInput';

it('should work properly when input is passed as a children', async () => {
  const onChange = jest.fn();
  const { user } = setupWithProps({
    onChange,
    value: 'foo',
    children: <input onChange={onChange} />,
  });
  await user.click(screen.getByLabelText('clear'));
  expect(onChange).toHaveBeenCalledWith('');
});

it('should warn when input is too short', () => {
  setupWithProps({
    value: 'f',
    children: <input />,
  });
  expect(screen.getByRole('note')).toBeInTheDocument();
});

function setupWithProps(props: Partial<FCProps<typeof InputSearch>> = {}) {
  return renderWithContext(
    <SearchSelectControlledInput
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
