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
import { getJSON, post, postJSON, RequestData } from '../helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';
import { Paging, HomePage, CurrentUser, IdentityProvider, User } from '../app/types';

export function getCurrentUser(): Promise<CurrentUser> {
  return getJSON('/api/users/current');
}

export function changePassword(data: {
  login: string;
  password: string;
  previousPassword?: string;
}): Promise<void> {
  return post('/api/users/change_password', data);
}

export function getUserGroups(login: string, organization?: string): Promise<any> {
  const data: RequestData = { login };
  if (organization) {
    data.organization = organization;
  }
  return getJSON('/api/users/groups', data);
}

export function getIdentityProviders(): Promise<{ identityProviders: IdentityProvider[] }> {
  return getJSON('/api/users/identity_providers').catch(throwGlobalError);
}

export function searchUsers(data: {
  p?: number;
  ps?: number;
  q?: string;
}): Promise<{ paging: Paging; users: User[] }> {
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
  scmAccount?: string;
}): Promise<User> {
  return postJSON('/api/users/update', data);
}

export function deactivateUser(data: { login: string }): Promise<User> {
  return postJSON('/api/users/deactivate', data).catch(throwGlobalError);
}

export function skipOnboarding(): Promise<void | Response> {
  return post('/api/users/skip_onboarding_tutorial').catch(throwGlobalError);
}

export function setHomePage(homepage: HomePage): Promise<void | Response> {
  return post('/api/users/set_homepage', homepage).catch(throwGlobalError);
}
