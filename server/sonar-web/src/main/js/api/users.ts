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
import axios from 'axios';
import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { getJSON } from '~sonar-aligned/helpers/request';
import { HttpStatus, axiosToCatch, parseJSON, post } from '../helpers/request';
import { IdentityProvider, Paging } from '../types/types';
import {
  ChangePasswordResults,
  CurrentUser,
  HomePage,
  NoticeType,
  RestUserBase,
  RestUserDetailed,
} from '../types/users';

const USERS_ENDPOINT = '/api/v2/users-management/users';

export function getCurrentUser(): Promise<CurrentUser> {
  return getJSON('/api/users/current', undefined, { bypassRedirect: true });
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

export function getUserGroups(data: {
  login: string;
  organization?: string;
  p?: number;
  ps?: number;
  q?: string;
  selected?: string;
}): Promise<{ paging: Paging; groups: UserGroup[] }> {
  return getJSON('/api/users/groups', data);
}

export function getIdentityProviders(): Promise<{ identityProviders: IdentityProvider[] }> {
  return getJSON('/api/users/identity_providers').catch(throwGlobalError);
}

export function getUsers<T extends RestUserBase>(data: {
  organization?: string;
  active?: boolean;
  groupId?: string;
  'groupId!'?: string;
  managed?: boolean;
  pageIndex?: number;
  pageSize?: number;
  q: string;
  sonarLintLastConnectionDateFrom?: string;
  sonarLintLastConnectionDateTo?: string;
  sonarQubeLastConnectionDateFrom?: string;
  sonarQubeLastConnectionDateTo?: string;
}) {
  return axios.get<{ page: Paging; users: T[] }>(USERS_ENDPOINT, {
    params: data,
  });
}

export function postUser(data: {
  email?: string;
  login: string;
  name: string;
  password?: string;
  scmAccounts: string[];
}) {
  return axiosToCatch.post<RestUserDetailed>(USERS_ENDPOINT, data);
}

export function updateUser(
  id: string,
  data: Partial<Pick<RestUserDetailed, 'email' | 'name' | 'scmAccounts'>>,
) {
  return axiosToCatch.patch<RestUserDetailed>(`${USERS_ENDPOINT}/${id}`, data);
}

export function deleteUser({ id, anonymize }: { anonymize?: boolean; id: string }) {
  return axios.delete(`${USERS_ENDPOINT}/${id}`, { params: { anonymize } });
}

export function setHomePage(homepage: HomePage): Promise<void | Response> {
  return post('/api/users/set_homepage', homepage).catch(throwGlobalError);
}

export function skipOnboarding(): Promise<void | Response> {
  return post('/api/users/onboarded').catch(throwGlobalError);
}
