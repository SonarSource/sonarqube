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
import BoxedTabs from '../../../../components/controls/BoxedTabs';
import {
  mockMainBranch,
  mockPullRequest,
  mockSetOfBranchAndPullRequest,
} from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { BranchLikeTable } from '../BranchLikeTable';
import BranchLikeTabs, { Tabs } from '../BranchLikeTabs';
import DeleteBranchModal from '../DeleteBranchModal';
import RenameBranchModal from '../RenameBranchModal';

it('should render all tabs correctly', () => {
  const wrapper = shallowRender();

  expect(wrapper.state().currentTab).toBe(Tabs.Branch);
  expect(wrapper).toMatchSnapshot();

  const onSelect = wrapper.find(BoxedTabs).prop('onSelect') as (currentTab: Tabs) => void;
  onSelect(Tabs.PullRequest);

  expect(wrapper.state().currentTab).toBe(Tabs.PullRequest);
  expect(wrapper).toMatchSnapshot();
});

it('should render deletion modal correctly', () => {
  const onBranchesChange = jest.fn();
  const wrapper = shallowRender({ onBranchesChange });

  wrapper.find(BranchLikeTable).props().onDelete(mockPullRequest());
  expect(wrapper.state().deleting).toBeDefined();
  expect(wrapper.find(DeleteBranchModal)).toMatchSnapshot();

  wrapper.find(DeleteBranchModal).props().onClose();
  expect(wrapper.state().deleting).toBeUndefined();
  expect(wrapper.find(DeleteBranchModal).exists()).toBe(false);

  wrapper.find(BranchLikeTable).props().onDelete(mockPullRequest());
  wrapper.find(DeleteBranchModal).props().onDelete();
  expect(wrapper.state().deleting).toBeUndefined();
  expect(wrapper.find(DeleteBranchModal).exists()).toBe(false);
  expect(onBranchesChange).toHaveBeenCalled();
});

it('should render renaming modal correctly', () => {
  const onBranchesChange = jest.fn();
  const wrapper = shallowRender({ onBranchesChange });

  wrapper.find(BranchLikeTable).props().onRename(mockMainBranch());
  expect(wrapper.state().renaming).toBeDefined();
  expect(wrapper.find(RenameBranchModal)).toMatchSnapshot();

  wrapper.find(RenameBranchModal).props().onClose();
  expect(wrapper.state().renaming).toBeUndefined();
  expect(wrapper.find(RenameBranchModal).exists()).toBe(false);

  wrapper.find(BranchLikeTable).props().onRename(mockMainBranch());
  wrapper.find(RenameBranchModal).props().onRename();
  expect(wrapper.state().renaming).toBeUndefined();
  expect(wrapper.find(RenameBranchModal).exists()).toBe(false);
  expect(onBranchesChange).toHaveBeenCalled();
});

it('should NOT render renaming modal for non-main branch', () => {
  const wrapper = shallowRender();

  wrapper.find(BranchLikeTable).props().onRename(mockPullRequest());
  expect(wrapper.state().renaming).toBeDefined();
  expect(wrapper.find(RenameBranchModal).exists()).toBe(false);
});

it('should correctly propagate an update of purge settings', () => {
  const onBranchesChange = jest.fn();
  const wrapper = shallowRender({ onBranchesChange });

  wrapper.find(BranchLikeTable).props().onUpdatePurgeSetting();

  expect(onBranchesChange).toHaveBeenCalled();
});

function shallowRender(props: Partial<BranchLikeTabs['props']> = {}) {
  return shallow<BranchLikeTabs>(
    <BranchLikeTabs
      branchLikes={mockSetOfBranchAndPullRequest()}
      component={mockComponent()}
      onBranchesChange={jest.fn()}
      {...props}
    />
  );
}
