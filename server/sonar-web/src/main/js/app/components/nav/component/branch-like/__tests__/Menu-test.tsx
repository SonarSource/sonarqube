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
import Link from '../../../../../../components/common/Link';
import SearchBox from '../../../../../../components/controls/SearchBox';
import { KeyboardKeys } from '../../../../../../helpers/keycodes';
import {
  mockPullRequest,
  mockSetOfBranchAndPullRequest,
} from '../../../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../../../helpers/mocks/component';
import { mockRouter } from '../../../../../../helpers/testMocks';
import { click, mockEvent } from '../../../../../../helpers/testUtils';
import { queryToSearch } from '../../../../../../helpers/urls';
import { Menu } from '../Menu';
import { MenuItemList } from '../MenuItemList';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly with no current branch like', () => {
  const wrapper = shallowRender({ currentBranchLike: undefined });
  expect(wrapper).toMatchSnapshot();
});

it('should close the menu when "manage branches" link is clicked', () => {
  const onClose = jest.fn();
  const wrapper = shallowRender({ onClose });

  click(wrapper.find(Link));
  expect(onClose).toHaveBeenCalled();
});

it('should change url and close menu when an element is selected', () => {
  const onClose = jest.fn();
  const push = jest.fn();
  const router = mockRouter({ push });
  const component = mockComponent();
  const pr = mockPullRequest();

  const wrapper = shallowRender({ component, onClose, router });

  wrapper.find(MenuItemList).props().onSelect(pr);

  expect(onClose).toHaveBeenCalled();
  expect(push).toHaveBeenCalledWith(
    expect.objectContaining({
      search: queryToSearch({
        id: component.key,
        pullRequest: pr.key,
      }),
    })
  );
});

it('should filter branchlike list correctly', () => {
  const wrapper = shallowRender();

  wrapper.find(SearchBox).props().onChange('PR');

  expect(wrapper.state().branchLikesToDisplay.length).toBe(3);
});

it('should handle keyboard shortcut correctly', () => {
  const push = jest.fn();
  const router = mockRouter({ push });
  const wrapper = shallowRender({ currentBranchLike: branchLikes[1], router });

  const { onKeyDown } = wrapper.find(SearchBox).props();

  onKeyDown!(mockEvent({ nativeEvent: { key: KeyboardKeys.UpArrow } }));
  expect(wrapper.state().selectedBranchLike).toBe(branchLikes[3]);

  onKeyDown!(mockEvent({ nativeEvent: { key: KeyboardKeys.DownArrow } }));
  onKeyDown!(mockEvent({ nativeEvent: { key: KeyboardKeys.DownArrow } }));
  expect(wrapper.state().selectedBranchLike).toBe(branchLikes[0]);

  onKeyDown!(mockEvent({ nativeEvent: { key: KeyboardKeys.Enter } }));
  expect(push).toHaveBeenCalled();
});

const branchLikes = mockSetOfBranchAndPullRequest();

function shallowRender(props?: Partial<Menu['props']>) {
  return shallow<Menu>(
    <Menu
      branchLikes={branchLikes}
      canAdminComponent={true}
      component={mockComponent()}
      currentBranchLike={branchLikes[2]}
      onClose={jest.fn()}
      router={mockRouter()}
      {...props}
    />
  );
}
