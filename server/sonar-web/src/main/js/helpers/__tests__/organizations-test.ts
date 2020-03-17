/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

const org: T.Organization = { key: 'foo', name: 'Foo', subscription: 'PAID' };
const adminOrg = { actions: { admin: true }, key: 'bar', name: 'Bar' };
const randomOrg = { key: 'bar', name: 'Bar' };

const loggedIn = {
  isLoggedIn: true,
  login: 'luke',
  name: 'Skywalker'
};
const loggedOut = { isLoggedIn: false };

describe('isCurrentUserMemberOf', () => {
  it('should be a member', () => {
    expect(isCurrentUserMemberOf(loggedIn, adminOrg, [])).toBe(true);
    expect(isCurrentUserMemberOf(loggedIn, org, [org])).toBe(true);
  });

  it('should not be a member', () => {
    expect(isCurrentUserMemberOf(loggedIn, undefined, [])).toBe(false);
    expect(isCurrentUserMemberOf(loggedIn, org, [])).toBe(false);
    expect(isCurrentUserMemberOf(loggedIn, org, [randomOrg])).toBe(false);
    expect(isCurrentUserMemberOf(loggedOut, org, [org])).toBe(false);
  });
});

describe('hasPrivateAccess', () => {
  it('should have access', () => {
    expect(hasPrivateAccess(loggedIn, randomOrg, [])).toBe(true);
    expect(hasPrivateAccess(loggedIn, org, [org])).toBe(true);
  });

  it('should not have access', () => {
    expect(hasPrivateAccess(loggedIn, org, [])).toBe(false);
  });
});
