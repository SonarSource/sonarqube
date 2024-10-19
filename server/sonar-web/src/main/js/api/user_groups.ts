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
import { Group, Paging } from '../types/types';

const GROUPS_ENDPOINT = '/api/v2/authorizations/groups';

export function getUsersGroups(params: {
  organization?: string;
  managed?: boolean;
  pageIndex?: number;
  pageSize?: number;
  q?: string;
}): Promise<{ groups: Group[]; page: Paging }> {
  return axios.get(GROUPS_ENDPOINT, { params });
}

export function createGroup(data: { organization?: string; description?: string; name: string }): Promise<Group> {
  return axios.post(GROUPS_ENDPOINT, data).then((r) => r.group);
}

export function updateGroup(
  id: string,
  data: {
    organization?: string;
    description?: string;
    name?: string;
  },
) {
  return axios.patch(`${GROUPS_ENDPOINT}/${id}`, data);
}

export function deleteGroup(id: string) {
  return axios.delete(`${GROUPS_ENDPOINT}/${id}`);
}
