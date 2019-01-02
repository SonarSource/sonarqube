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
import * as React from 'react';
import { shallow } from 'enzyme';
import { Location } from 'history';
import { hasAdminAccess, OrganizationAccess } from '../OrganizationAccessContainer';

jest.mock('../../../../app/utils/handleRequiredAuthorization', () => ({ default: jest.fn() }));

const locationMock = {} as Location;

const currentUser = {
  isLoggedIn: false
};

const loggedInUser = {
  isLoggedIn: true,
  login: 'luke',
  name: 'Skywalker',
  showOnboardingTutorial: false
};

const organization: T.Organization = {
  actions: { admin: false },
  key: 'foo',
  name: 'Foo',
  projectVisibility: 'public'
};

const adminOrganization: T.Organization = { ...organization, actions: { admin: true } };

describe('component', () => {
  it('should render children', () => {
    expect(
      shallow(
        <OrganizationAccess
          currentUser={loggedInUser}
          hasAccess={() => true}
          location={locationMock}
          organization={adminOrganization}>
          <div>hello</div>
        </OrganizationAccess>
      )
    ).toMatchSnapshot();
  });

  it('should not render anything', () => {
    expect(
      shallow(
        <OrganizationAccess
          currentUser={loggedInUser}
          hasAccess={() => false}
          location={locationMock}
          organization={adminOrganization}>
          <div>hello</div>
        </OrganizationAccess>
      ).type()
    ).toBeNull();
  });
});

describe('access functions', () => {
  it('should correctly handle access to admin only space', () => {
    expect(
      hasAdminAccess({ currentUser: loggedInUser, organization: adminOrganization })
    ).toBeTruthy();
    expect(hasAdminAccess({ currentUser, organization: adminOrganization })).toBeFalsy();
    expect(hasAdminAccess({ currentUser: loggedInUser, organization })).toBeFalsy();
  });
});
