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
import { shallow } from 'enzyme';
import * as React from 'react';
import { mockMainBranch, mockPullRequest } from '../../../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../../../helpers/mocks/component';
import { click } from '../../../../../../helpers/testUtils';
import { MenuItem, MenuItemProps } from '../MenuItem';

it('should render a main branch correctly', () => {
  const wrapper = shallowRender({ branchLike: mockMainBranch() });
  expect(wrapper).toMatchSnapshot();
});

it('should render a non-main branch, indented and selected item correctly', () => {
  const wrapper = shallowRender({ branchLike: mockPullRequest(), indent: true, selected: true });
  expect(wrapper).toMatchSnapshot();
});

it('should propagate click event correctly', () => {
  const onSelect = jest.fn();
  const wrapper = shallowRender({ onSelect });

  click(wrapper.find('li'));
  expect(onSelect).toHaveBeenCalled();
});

function shallowRender(props?: Partial<MenuItemProps>) {
  return shallow(
    <MenuItem
      branchLike={mockMainBranch()}
      component={mockComponent()}
      onSelect={jest.fn()}
      selected={false}
      setSelectedNode={jest.fn()}
      {...props}
    />
  );
}
