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
import { mockProjectGithubBindingResponse } from '../../../../helpers/mocks/alm-settings';
import { mockBranch, mockMainBranch, mockPullRequest } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockCurrentUser, mockLoggedInUser } from '../../../../helpers/testMocks';
import { ComponentQualifier } from '../../../../types/component';
import { EmptyOverview, EmptyOverviewProps } from '../EmptyOverview';

it('renders correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ hasAnalyses: true })).toMatchSnapshot();
  expect(shallowRender({ currentUser: mockCurrentUser() })).toMatchSnapshot();
  expect(shallowRender({ projectBinding: mockProjectGithubBindingResponse() })).toMatchSnapshot();
});

it('should render another message when there are branches', () => {
  expect(shallowRender({ branchLikes: [mockMainBranch(), mockBranch()] })).toMatchSnapshot();
  expect(
    shallowRender({
      branchLikes: [mockMainBranch(), mockBranch(), mockBranch({ name: 'branch-7.8' })],
    })
  ).toMatchSnapshot();
});

it('should not render warning message for pull requests', () => {
  expect(shallowRender({ branchLike: mockPullRequest() }).type()).toBeNull();
});

it('should not render the tutorial for applications', () => {
  expect(
    shallowRender({ component: mockComponent({ qualifier: ComponentQualifier.Application }) })
  ).toMatchSnapshot();
});

function shallowRender(props: Partial<EmptyOverviewProps> = {}) {
  return shallow<EmptyOverviewProps>(
    <EmptyOverview
      branchLike={mockMainBranch()}
      branchLikes={[mockMainBranch()]}
      component={mockComponent({ version: '0.0.1' })}
      currentUser={mockLoggedInUser()}
      {...props}
    />
  );
}
