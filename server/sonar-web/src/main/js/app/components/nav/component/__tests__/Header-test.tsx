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
import Favorite from '../../../../../components/controls/Favorite';
import { mockSetOfBranchAndPullRequest } from '../../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../../helpers/mocks/component';
import { mockCurrentUser } from '../../../../../helpers/testMocks';
import { Header, HeaderProps } from '../Header';

it('should render correctly', () => {
  const wrapper = shallowRender({ currentUser: mockCurrentUser({ isLoggedIn: true }) });
  expect(wrapper).toMatchSnapshot();
});

it('should not render favorite button if the user is not logged in', () => {
  const wrapper = shallowRender();
  expect(wrapper.find(Favorite).exists()).toBe(false);
});

function shallowRender(props?: Partial<HeaderProps>) {
  const branchLikes = mockSetOfBranchAndPullRequest();

  return shallow(
    <Header
      branchLikes={branchLikes}
      component={mockComponent()}
      currentBranchLike={branchLikes[0]}
      currentUser={mockCurrentUser()}
      {...props}
    />
  );
}
