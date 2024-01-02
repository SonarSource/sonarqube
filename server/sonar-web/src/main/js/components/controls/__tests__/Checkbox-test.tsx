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
import Checkbox from '../Checkbox';

describe.each([
  { children: null, describtion: 'with no children' },
  { children: <a>child</a>, describtion: 'with children' },
])('Checkbox $describtion', ({ children }) => {
  it('should call check function', async () => {
    const user = userEvent.setup();
    const onCheck = jest.fn();
    const rerender = renderCheckbox({
      label: 'me',
      children,
      onCheck,
      checked: false,
      title: 'title',
    });
    await user.click(screen.getByRole('checkbox', { name: 'me' }));
    expect(onCheck).toHaveBeenCalledWith(true, undefined);
    expect(screen.getByTitle('title')).toBeInTheDocument();
    rerender({ checked: true });
    await user.click(screen.getByRole('checkbox', { name: 'me' }));
    expect(onCheck).toHaveBeenCalledWith(false, undefined);
  });

  it('should accept partial state', () => {
    renderCheckbox({ label: 'me', thirdState: true, children, checked: false });
    expect(screen.getByRole('checkbox', { name: 'me' })).not.toBeChecked();
  });

  it('should render loading state', () => {
    renderCheckbox({ label: 'me', children, loading: true });
    expect(screen.getByTestId('spinner')).toMatchSnapshot();
  });
});

it('should render the checkbox on the right', () => {
  renderCheckbox({ label: 'me', children: <a>child</a>, right: true });
  expect(screen.getByRole('checkbox', { name: 'me' })).toMatchSnapshot();
});

function renderCheckbox(override?: Partial<Checkbox['props']>) {
  const { rerender } = renderComponent(<Checkbox checked onCheck={jest.fn()} {...override} />);
  return function (reoverride?: Partial<Checkbox['props']>) {
    rerender(<Checkbox checked onCheck={jest.fn()} {...override} {...reoverride} />);
  };
}
