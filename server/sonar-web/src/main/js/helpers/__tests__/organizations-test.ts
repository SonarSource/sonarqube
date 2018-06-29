/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { hasPrivateAccess, isCurrentUserMemberOf } from '../organizations';
import { getCurrentUser, getMyOrganizations } from '../../store/rootReducer';
import { OrganizationSubscription } from '../../app/types';

jest.mock('../../app/utils/getStore', () => ({
  default: () => ({
    getState: jest.fn()
  })
}));

jest.mock('../../store/rootReducer', () => ({
  getCurrentUser: jest.fn().mockReturnValue({
    isLoggedIn: true,
    login: 'luke',
    name: 'Skywalker',
    showOnboardingTutorial: false
  }),
  getMyOrganizations: jest.fn().mockReturnValue([])
}));

const organization = {
  key: 'foo',
  name: 'Foo',
  subscription: OrganizationSubscription.Paid
};

const loggedOut = { isLoggedIn: false };

beforeEach(() => {
  (getCurrentUser as jest.Mock<any>).mockClear();
  (getMyOrganizations as jest.Mock<any>).mockClear();
});

describe('isCurrentUserMemberOf', () => {
  it('should be a member', () => {
    expect(isCurrentUserMemberOf({ key: 'bar', name: 'Bar', canAdmin: true })).toBeTruthy();

    (getMyOrganizations as jest.Mock<any>).mockReturnValueOnce([organization]);
    expect(isCurrentUserMemberOf(organization)).toBeTruthy();
  });

  it('should not be a member', () => {
    expect(isCurrentUserMemberOf(undefined)).toBeFalsy();
    expect(isCurrentUserMemberOf(organization)).toBeFalsy();

    (getMyOrganizations as jest.Mock<any>).mockReturnValueOnce([{ key: 'bar', name: 'Bar' }]);
    expect(isCurrentUserMemberOf(organization)).toBeFalsy();

    (getCurrentUser as jest.Mock<any>).mockReturnValueOnce(loggedOut);
    expect(isCurrentUserMemberOf(organization)).toBeFalsy();
  });
});

describe('hasPrivateAccess', () => {
  it('should have access', () => {
    expect(hasPrivateAccess({ key: 'bar', name: 'Bar' })).toBeTruthy();

    (getMyOrganizations as jest.Mock<any>).mockReturnValueOnce([organization]);
    expect(hasPrivateAccess(organization)).toBeTruthy();
  });

  it('should not have access', () => {
    expect(hasPrivateAccess(organization)).toBeFalsy();
  });
});
