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
import axios from 'axios';
import { throwGlobalError } from '../helpers/error';
import { axiosToCatch, getJSON, post } from '../helpers/request';
import { Group, Paging, UserGroupMember } from '../types/types';

const GROUPS_ENDPOINT = '/api/v2/authorizations/groups';

export function getUsersGroups(params: {
  q?: string;
  managed: boolean | undefined;
  pageIndex?: number;
  pageSize?: number;
}): Promise<{ groups: Group[]; page: Paging }> {
  return axios.get(GROUPS_ENDPOINT, { params });
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
  return axios.post(GROUPS_ENDPOINT, data).then((r) => r.group);
}

export function updateGroup(
  id: string,
  data: {
    name?: string;
    description?: string;
  },
) {
  return axiosToCatch.patch(`${GROUPS_ENDPOINT}/${id}`, data);
}

export function deleteGroup(id: string) {
  return axios.delete(`${GROUPS_ENDPOINT}/${id}`);
}
