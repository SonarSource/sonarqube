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

import { cloneDeep } from 'lodash';
import { mockGroupMembership } from '../../helpers/testMocks';
import { GroupMembership } from '../../types/types';
import {
  addGroupMembership,
  getGroupMemberships,
  removeGroupMembership,
} from '../group-memberships';

jest.mock('../group-memberships');

export default class GroupMembershipsServiceMock {
  memberships: GroupMembership[];

  defaultMemberships = [];

  constructor() {
    this.memberships = cloneDeep(this.defaultMemberships);

    jest.mocked(getGroupMemberships).mockImplementation(this.handleGetGroupMemberships);
    jest.mocked(addGroupMembership).mockImplementation(this.handleAddGroupMembership);
    jest.mocked(removeGroupMembership).mockImplementation(this.handleRemoveGroupMembership);
  }

  handleAddGroupMembership: typeof addGroupMembership = ({
    userId,
    groupId,
  }: {
    groupId: string;
    userId: string;
  }): Promise<GroupMembership> => {
    const newMembership = mockGroupMembership({ userId, groupId });
    this.memberships.push(newMembership);
    return this.reply(newMembership);
  };

  handleRemoveGroupMembership: typeof removeGroupMembership = (id: string) => {
    if (!this.memberships.some((g) => g.id === id)) {
      return Promise.reject();
    }

    this.memberships = this.memberships.filter((g) => g.id !== id);
    return this.reply(undefined);
  };

  handleGetGroupMemberships: typeof getGroupMemberships = ({
    userId,
    groupId,
    pageSize = 50,
    pageIndex = 1,
  }) => {
    const allMemberships = this.memberships
      .filter((m) => userId === undefined || m.userId === userId)
      .filter((m) => groupId === undefined || m.groupId === groupId);
    const groupMemberships = allMemberships.slice((pageIndex - 1) * pageSize, pageIndex * pageSize);
    return this.reply({
      page: { pageIndex, pageSize, total: allMemberships.length },
      groupMemberships,
    });
  };

  reset() {
    this.memberships = cloneDeep(this.defaultMemberships);
  }

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
