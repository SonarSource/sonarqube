/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import { getJSON, post, RequestData } from '../helpers/request';

export function getCurrentUser(): Promise<any> {
  return getJSON('/api/users/current');
}

export function changePassword(
  login: string,
  password: string,
  previousPassword?: string
): Promise<void> {
  const data: RequestData = { login, password };
  if (previousPassword != null) {
    data.previousPassword = previousPassword;
  }
  return post('/api/users/change_password', data);
}

export function getUserGroups(login: string, organization?: string): Promise<any> {
  const data: RequestData = { login };
  if (organization) {
    data.organization = organization;
  }
  return getJSON('/api/users/groups', data);
}

export function getIdentityProviders(): Promise<any> {
  return getJSON('/api/users/identity_providers');
}

export function searchUsers(query: string, pageSize?: number): Promise<any> {
  const data: RequestData = { q: query };
  if (pageSize != null) {
    data.ps = pageSize;
  }
  return getJSON('/api/users/search', data);
}

export function skipOnboarding(): Promise<void> {
  return post('/api/users/skip_onboarding_tutorial');
}
