/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import {
  mockComponent,
  mockLongLivingBranch,
  mockMainBranch,
  mockPullRequest,
  mockShortLivingBranch
} from '../../../../helpers/testMocks';
import { BranchLikeRow, BranchLikeRowProps } from '../BranchLikeRow';

it('should render correctly for pull request', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly for short lived branch', () => {
  const wrapper = shallowRender({ branchLike: mockShortLivingBranch(), displayPurgeSetting: true });
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly for long lived branch', () => {
  const wrapper = shallowRender({ branchLike: mockLongLivingBranch(), displayPurgeSetting: true });
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly for main branch', () => {
  const wrapper = shallowRender({ branchLike: mockMainBranch(), displayPurgeSetting: true });
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props?: Partial<BranchLikeRowProps>) {
  return shallow(
    <BranchLikeRow
      branchLike={mockPullRequest()}
      component={mockComponent()}
      onDelete={jest.fn()}
      onRename={jest.fn()}
      {...props}
    />
  );
}
