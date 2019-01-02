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
import { getJSON, post, postJSON } from '../helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';

export function searchUsersGroups(data: {
  f?: string;
  organization?: string;
  p?: number;
  ps?: number;
  q?: string;
}): Promise<{ groups: T.Group[]; paging: T.Paging }> {
  return getJSON('/api/user_groups/search', data).catch(throwGlobalError);
}

export interface GroupUser {
  login: string;
  name: string;
  selected: boolean;
}

export function getUsersInGroup(data: {
  id?: number;
  name?: string;
  organization?: string;
  p?: number;
  ps?: number;
  q?: string;
  selected?: string;
}): Promise<{ paging: T.Paging; users: GroupUser[] }> {
  return getJSON('/api/user_groups/users', data).catch(throwGlobalError);
}

export function addUserToGroup(data: {
  id?: string;
  name?: string;
  login?: string;
  organization?: string;
}) {
  return post('/api/user_groups/add_user', data).catch(throwGlobalError);
}

export function removeUserFromGroup(data: {
  id?: string;
  name?: string;
  login?: string;
  organization?: string;
}) {
  return post('/api/user_groups/remove_user', data).catch(throwGlobalError);
}

export function createGroup(data: {
  description?: string;
  organization: string | undefined;
  name: string;
}): Promise<T.Group> {
  return postJSON('/api/user_groups/create', data).then(r => r.group, throwGlobalError);
}

export function updateGroup(data: { description?: string; id: number; name?: string }) {
  return post('/api/user_groups/update', data).catch(throwGlobalError);
}

export function deleteGroup(data: { name: string; organization: string | undefined }) {
  return post('/api/user_groups/delete', data).catch(throwGlobalError);
}
