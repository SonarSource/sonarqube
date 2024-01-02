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
import { mockSetOfBranchAndPullRequest } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { BranchLikeRow } from '../BranchLikeRow';
import { BranchLikeTable, BranchLikeTableProps } from '../BranchLikeTable';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should render purge setting correctly', () => {
  const wrapper = shallowRender({ displayPurgeSetting: true });
  expect(wrapper).toMatchSnapshot();
});

it('should properly propagate delete event', () => {
  const onDelete = jest.fn();
  const wrapper = shallowRender({ onDelete });

  wrapper.find(BranchLikeRow).first().props().onDelete();

  expect(onDelete).toHaveBeenCalled();
});

it('should properly propagate rename event', () => {
  const onDelete = jest.fn();
  const onRename = jest.fn();
  const wrapper = shallowRender({ onDelete, onRename });

  wrapper.find(BranchLikeRow).first().props().onRename();

  expect(onRename).toHaveBeenCalled();
});

function shallowRender(props?: Partial<BranchLikeTableProps>) {
  return shallow(
    <BranchLikeTable
      branchLikes={mockSetOfBranchAndPullRequest()}
      component={mockComponent()}
      onDelete={jest.fn()}
      onRename={jest.fn()}
      onUpdatePurgeSetting={jest.fn()}
      title="title"
      {...props}
    />
  );
}
