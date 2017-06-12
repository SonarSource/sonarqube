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
//@flow
import { getJSON, post } from '../helpers/request';

export function getCurrentUser() {
  const url = '/api/users/current';
  return getJSON(url);
}

export function changePassword(login: string, password: string, previousPassword?: string) {
  const url = '/api/users/change_password';
  const data: { login: string, password: string, previousPassword?: string } = { login, password };
  if (previousPassword != null) {
    data.previousPassword = previousPassword;
  }
  return post(url, data);
}

export function getUserGroups(login: string, organization?: string) {
  const url = '/api/users/groups';
  const data: { login: string, organization?: string, q?: string } = { login };
  if (organization) {
    data.organization = organization;
  }
  return getJSON(url, data);
}

export function getIdentityProviders() {
  const url = '/api/users/identity_providers';
  return getJSON(url);
}

export function searchUsers(query: string, pageSize?: number) {
  const url = '/api/users/search';
  const data: { q: string, ps?: number } = { q: query };
  if (pageSize != null) {
    data.ps = pageSize;
  }
  return getJSON(url, data);
}

export function skipOnboarding(): Promise<void> {
  return post('/api/users/skip_onboarding_tutorial');
}
