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
import { mockSetOfBranchAndPullRequest } from '../../../../helpers/mocks/branch-pull-request';
import { mockComponent } from '../../../../helpers/testMocks';
import { BranchLikeRowRenderer } from '../BranchLikeRowRenderer';
import { BranchLikeTableRenderer, BranchLikeTableRendererProps } from '../BranchLikeTableRenderer';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should properly propagate delete event', () => {
  const onDelete = jest.fn();
  const wrapper = shallowRender({ onDelete });

  wrapper
    .find(BranchLikeRowRenderer)
    .first()
    .props()
    .onDelete();

  expect(onDelete).toHaveBeenCalled();
});

it('should properly propagate rename event', () => {
  const onDelete = jest.fn();
  const onRename = jest.fn();
  const wrapper = shallowRender({ onDelete, onRename });

  wrapper
    .find(BranchLikeRowRenderer)
    .first()
    .props()
    .onRename();

  expect(onRename).toHaveBeenCalled();
});

function shallowRender(props?: Partial<BranchLikeTableRendererProps>) {
  return shallow(
    <BranchLikeTableRenderer
      branchLikes={mockSetOfBranchAndPullRequest()}
      component={mockComponent()}
      onDelete={jest.fn()}
      onRename={jest.fn()}
      tableTitle="tableTitle"
      {...props}
    />
  );
}
