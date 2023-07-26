/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { throwGlobalError } from '../helpers/error';
import { getJSON, HttpStatus, parseJSON, post, postJSON } from '../helpers/request';
import { IdentityProvider, Paging } from '../types/types';
import { ChangePasswordResults, CurrentUser, HomePage, NoticeType, User } from '../types/users';

export function getCurrentUser(): Promise<CurrentUser> {
  return getJSON('/api/users/current', undefined, true);
}

export function dismissNotice(notice: NoticeType) {
  return post('/api/users/dismiss_notice', { notice }).catch(throwGlobalError);
}

export function changePassword(data: {
  login: string;
  password: string;
  previousPassword?: string;
}) {
  return post('/api/users/change_password', data).catch(async (response) => {
    if (response.status === HttpStatus.BadRequest) {
      const { result } = await parseJSON(response);
      return Promise.reject<ChangePasswordResults>(result);
    }

    return throwGlobalError(response);
  });
}

export interface UserGroup {
  default: boolean;
  description: string;
  id: number;
  name: string;
  selected: boolean;
}

export interface UserGroupsParams {
  login: string;
  p?: number;
  ps?: number;
  q?: string;
  selected?: string;
}

export function getUserGroups(
  data: UserGroupsParams
): Promise<{ paging: Paging; groups: UserGroup[] }> {
  return getJSON('/api/users/groups', data);
}

export function getIdentityProviders(): Promise<{ identityProviders: IdentityProvider[] }> {
  return getJSON('/api/users/identity_providers').catch(throwGlobalError);
}

export interface SearchUsersParams {
  p?: number;
  ps?: number;
  q?: string;
  managed?: boolean;
  lastConnectedAfter?: string;
  lastConnectedBefore?: string;
  slLastConnectedAfter?: string;
  slLastConnectedBefore?: string;
}

export function searchUsers(data: SearchUsersParams): Promise<{ paging: Paging; users: User[] }> {
  data.q = data.q || undefined;
  return getJSON('/api/users/search', data).catch(throwGlobalError);
}

export interface GetUsersParams {
  q: string;
  active?: boolean;
  managed?: boolean;
  sonarQubeLastConnectionDateFrom?: string;
  sonarQubeLastConnectionDateTo?: string;
  sonarLintLastConnectionDateFrom?: string;
  sonarLintLastConnectionDateTo?: string;
  pageSize?: number;
  pageIndex?: number;
}

export type Permission = 'admin' | 'anonymous' | 'user';

export type RestUser<T extends Permission> = T extends 'admin'
  ? {
      id: string;
      login: string;
      name: string;
      email: string;
      active: boolean;
      local: boolean;
      externalProvider: string;
      avatar: string;
      managed: boolean;
      externalLogin: string;
      sonarQubeLastConnectionDate: string | null;
      sonarLintLastConnectionDate: string | null;
      scmAccounts: string[];
      groupsCount: number;
      tokensCount: number;
    }
  : T extends 'anonymous'
  ? { id: string; login: string; name: string }
  : {
      id: string;
      login: string;
      name: string;
      email: string;
      active: boolean;
      local: boolean;
      externalProvider: string;
      avatar: string;
    };

export function getUsers<T extends Permission>(
  data: GetUsersParams
): Promise<{ pageRestResponse: Paging; users: RestUser<T>[] }> {
  return getJSON('/api/v2/users', data).catch(throwGlobalError);
}

export interface CreateUserParams {
  email?: string;
  local?: boolean;
  login: string;
  name: string;
  password?: string;
  scmAccount: string[];
}

export function createUser(data: CreateUserParams): Promise<void | Response> {
  return post('/api/users/create', data);
}

export interface UpdateUserParams {
  email?: string;
  login: string;
  name?: string;
  scmAccount: string[];
}

export function updateUser(data: UpdateUserParams): Promise<{ user: User }> {
  return postJSON('/api/users/update', {
    ...data,
    scmAccount: data.scmAccount.length > 0 ? data.scmAccount : '',
  });
}

export interface DeactivateUserParams {
  login: string;
  anonymize?: boolean;
}

export function deactivateUser(data: DeactivateUserParams): Promise<{ user: RestUser<'admin'> }> {
  return postJSON('/api/users/deactivate', data).catch(throwGlobalError);
}

export function setHomePage(homepage: HomePage): Promise<void | Response> {
  return post('/api/users/set_homepage', homepage).catch(throwGlobalError);
}
