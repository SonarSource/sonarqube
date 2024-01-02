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
import { mockBranch } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockIssue } from '../../../../helpers/testMocks';
import ListItem from '../ListItem';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<ListItem['props']> = {}) {
  return shallow<ListItem>(
    <ListItem
      branchLike={mockBranch()}
      checked={false}
      component={mockComponent()}
      issue={mockIssue()}
      onChange={jest.fn()}
      onCheck={jest.fn()}
      onClick={jest.fn()}
      onFilterChange={jest.fn()}
      onPopupToggle={jest.fn()}
      openPopup={undefined}
      previousIssue={mockIssue(false, { branch: 'branch-8.7' })}
      selected={false}
      {...props}
    />
  );
}
