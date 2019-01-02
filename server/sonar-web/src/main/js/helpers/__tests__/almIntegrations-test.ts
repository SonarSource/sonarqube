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
import { isBitbucket, isGithub, isPersonal, isVSTS, sanitizeAlmId } from '../almIntegrations';

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

it('#isPersonal', () => {
  expect(
    isPersonal({ key: 'foo', name: 'Foo', personal: true, privateRepos: 0, publicRepos: 3 })
  ).toBeTruthy();
  expect(
    isPersonal({ key: 'foo', name: 'Foo', personal: false, privateRepos: 0, publicRepos: 3 })
  ).toBeFalsy();
});

it('#sanitizeAlmId', () => {
  expect(sanitizeAlmId('bitbucketcloud')).toBe('bitbucket');
  expect(sanitizeAlmId('bitbucket')).toBe('bitbucket');
  expect(sanitizeAlmId('github')).toBe('github');
});
