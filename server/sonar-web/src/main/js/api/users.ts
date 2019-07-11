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
import { getJSON, post, postJSON } from 'sonar-ui-common/helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';

export function getCurrentUser(): Promise<T.CurrentUser> {
  return getJSON('/api/users/current');
}

export function changePassword(data: {
  login: string;
  password: string;
  previousPassword?: string;
}) {
  return post('/api/users/change_password', data).catch(throwGlobalError);
}

export interface UserGroup {
  default: boolean;
  description: string;
  id: number;
  name: string;
  selected: boolean;
}

export function getUserGroups(data: {
  login: string;
  organization?: string;
  p?: number;
  ps?: number;
  q?: string;
  selected?: string;
}): Promise<{ paging: T.Paging; groups: UserGroup[] }> {
  return getJSON('/api/users/groups', data);
}

export function getIdentityProviders(): Promise<{ identityProviders: T.IdentityProvider[] }> {
  return getJSON('/api/users/identity_providers').catch(throwGlobalError);
}

export function searchUsers(data: {
  p?: number;
  ps?: number;
  q?: string;
}): Promise<{ paging: T.Paging; users: T.User[] }> {
  data.q = data.q || undefined;
  return getJSON('/api/users/search', data).catch(throwGlobalError);
}

export function createUser(data: {
  email?: string;
  local?: boolean;
  login: string;
  name: string;
  password?: string;
  scmAccount: string[];
}): Promise<void | Response> {
  return post('/api/users/create', data);
}

export function updateUser(data: {
  email?: string;
  login: string;
  name?: string;
  scmAccount: string[];
}): Promise<T.User> {
  return postJSON('/api/users/update', {
    ...data,
    scmAccount: data.scmAccount.length > 0 ? data.scmAccount : ''
  });
}

export function deactivateUser(data: { login: string }): Promise<T.User> {
  return postJSON('/api/users/deactivate', data).catch(throwGlobalError);
}

export function skipOnboarding(): Promise<void | Response> {
  return post('/api/users/skip_onboarding_tutorial').catch(throwGlobalError);
}

export function setHomePage(homepage: T.HomePage): Promise<void | Response> {
  return post('/api/users/set_homepage', homepage).catch(throwGlobalError);
}

export function setUserSetting(setting: T.CurrentUserSetting): Promise<void | Response> {
  return post('/api/users/set_setting', setting).catch(throwGlobalError);
}
