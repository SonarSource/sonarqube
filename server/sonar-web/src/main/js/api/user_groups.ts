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
import { getJSON, post, postJSON } from '../helpers/request';
import { Group, Paging, UserGroupMember } from '../types/types';

export function getUsersGroups(data: {
  f?: string;
  p?: number;
  ps?: number;
  q?: string;
  managed: boolean | undefined;
}): Promise<{ groups: Group[]; paging: Paging }> {
  return getJSON('/api/user_groups/search', data).catch(throwGlobalError);
}

export function getUsersInGroup(data: {
  name?: string;
  p?: number;
  ps?: number;
  q?: string;
  selected?: string;
}): Promise<{
  paging: Paging;
  users: UserGroupMember[];
}> {
  return getJSON('/api/user_groups/users', data).catch(throwGlobalError);
}

export function addUserToGroup(data: { name: string; login?: string }) {
  return post('/api/user_groups/add_user', data).catch(throwGlobalError);
}

export function removeUserFromGroup(data: { name: string; login?: string }) {
  return post('/api/user_groups/remove_user', data).catch(throwGlobalError);
}

export function createGroup(data: { description?: string; name: string }): Promise<Group> {
  return postJSON('/api/user_groups/create', data).then((r) => r.group, throwGlobalError);
}

export function updateGroup(data: { description?: string; currentName: string; name?: string }) {
  return post('/api/user_groups/update', data).catch(throwGlobalError);
}

export function deleteGroup(data: { name: string }) {
  return post('/api/user_groups/delete', data).catch(throwGlobalError);
}
