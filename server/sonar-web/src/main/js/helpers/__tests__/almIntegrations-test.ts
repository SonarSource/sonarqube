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
import {
  getAlmMembersUrl,
  getUserAlmKey,
  isBitbucket,
  isGithub,
  isVSTS,
  sanitizeAlmId
} from '../almIntegrations';
import { mockCurrentUser, mockLoggedInUser } from '../testMocks';

it('#getAlmMembersUrl', () => {
  expect(getAlmMembersUrl('github', 'https://github.com/Foo')).toBe(
    'https://github.com/orgs/Foo/people'
  );
  expect(getAlmMembersUrl('bitbucket', 'https://bitbucket.com/Foo/')).toBe(
    'https://bitbucket.com/Foo/profile/members'
  );
});

it('#isBitbucket', () => {
  expect(isBitbucket('bitbucket')).toBeTruthy();
  expect(isBitbucket('bitbucketcloud')).toBeTruthy();
  expect(isBitbucket('github')).toBeFalsy();
});

it('#isGithub', () => {
  expect(isGithub('github')).toBeTruthy();
  expect(isGithub('bitbucket')).toBeFalsy();
});

it('#isVSTS', () => {
  expect(isVSTS('microsoft')).toBeTruthy();
  expect(isVSTS('github')).toBeFalsy();
});

it('#sanitizeAlmId', () => {
  expect(sanitizeAlmId('bitbucketcloud')).toBe('bitbucket');
  expect(sanitizeAlmId('bitbucket')).toBe('bitbucket');
  expect(sanitizeAlmId('github')).toBe('github');
});

describe('getUserAlmKey', () => {
  it('should return sanitized almKey', () => {
    expect(getUserAlmKey(mockLoggedInUser({ externalProvider: 'bitbucketcloud' }))).toBe(
      'bitbucket'
    );
  });

  it('should return undefined', () => {
    expect(getUserAlmKey(mockCurrentUser())).toBeUndefined();
    expect(getUserAlmKey(mockCurrentUser({ isLoggedIn: undefined }))).toBeUndefined();
  });
});
