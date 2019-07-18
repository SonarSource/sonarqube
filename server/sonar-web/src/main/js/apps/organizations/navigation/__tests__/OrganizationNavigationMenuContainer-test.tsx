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
import { hasPrivateAccess, isCurrentUserMemberOf } from '../../../../helpers/organizations';
import { OrganizationNavigationMenu } from '../OrganizationNavigationMenuContainer';

jest.mock('../../../../helpers/organizations', () => ({
  isCurrentUserMemberOf: jest.fn().mockReturnValue(true),
  hasPrivateAccess: jest.fn().mockReturnValue(true)
}));

const organization: T.Organization = {
  key: 'foo',
  name: 'Foo',
  projectVisibility: 'public'
};

const loggedInUser = {
  isLoggedIn: true,
  login: 'luke',
  name: 'Skywalker',
  showOnboardingTutorial: false
};

beforeEach(() => {
  (isCurrentUserMemberOf as jest.Mock<any>).mockClear();
  (hasPrivateAccess as jest.Mock<any>).mockClear();
});

it('renders', () => {
  expect(
    shallow(
      <OrganizationNavigationMenu
        currentUser={loggedInUser}
        location={{ pathname: '' }}
        organization={organization}
        userOrganizations={[organization]}
      />
    )
  ).toMatchSnapshot();
});

it('renders for admin', () => {
  expect(
    shallow(
      <OrganizationNavigationMenu
        currentUser={loggedInUser}
        location={{ pathname: '' }}
        organization={{ ...organization, actions: { admin: true } }}
        userOrganizations={[organization]}
      />
    )
  ).toMatchSnapshot();
});
