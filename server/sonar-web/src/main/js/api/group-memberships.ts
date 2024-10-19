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
import { GroupMembership, Paging } from '../types/types';

const GROUPS_MEMBERSHIPS_ENDPOINT = '/api/v2/authorizations/group-memberships';

export function getGroupMemberships(data: {
  groupId?: string;
  pageIndex?: number;
  pageSize?: number;
  userId?: string;
}) {
  return axios.get<{ groupMemberships: GroupMembership[]; page: Paging }>(
    GROUPS_MEMBERSHIPS_ENDPOINT,
    { params: data },
  );
}

export function addGroupMembership(data: { organization: string; groupId: string; userId: string }) {
  return axios.post<GroupMembership>(GROUPS_MEMBERSHIPS_ENDPOINT, data);
}

export function removeGroupMembership(id: string) {
  return axios.delete(`${GROUPS_MEMBERSHIPS_ENDPOINT}/${id}`);
}
