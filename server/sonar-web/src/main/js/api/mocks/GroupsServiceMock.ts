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
import {
  mockGroup,
  mockIdentityProvider,
  mockPaging,
  mockUserGroupMember,
} from '../../helpers/testMocks';
import { Group, IdentityProvider, Paging, Provider } from '../../types/types';
import { createGroup, deleteGroup, getUsersGroups, updateGroup } from '../user_groups';

jest.mock('../user_groups');

export default class GroupsServiceMock {
  provider: Provider | undefined;
  paging: Paging;
  groups: Group[];
  readOnlyGroups = [
    mockGroup({ name: 'managed-group', managed: true, id: '1' }),
    mockGroup({ name: 'local-group', managed: false, id: '2' }),
  ];

  defaultUsers = [
    mockUserGroupMember({ name: 'alice', login: 'alice.dev' }),
    mockUserGroupMember({ name: 'bob', login: 'bob.dev' }),
    mockUserGroupMember({ selected: false }),
  ];

  constructor() {
    this.groups = cloneDeep(this.readOnlyGroups);
    this.paging = mockPaging({
      pageIndex: 1,
      pageSize: 2,
      total: 200,
    });

    jest.mocked(getUsersGroups).mockImplementation((p) => this.handleSearchUsersGroups(p));
    jest.mocked(createGroup).mockImplementation((g) => this.handleCreateGroup(g));
    jest.mocked(deleteGroup).mockImplementation((id) => this.handleDeleteGroup(id));
    jest.mocked(updateGroup).mockImplementation((id, data) => this.handleUpdateGroup(id, data));
  }

  reset() {
    this.groups = cloneDeep(this.readOnlyGroups);
  }

  setPaging(paging: Partial<Paging>) {
    this.paging = { ...this.paging, ...paging };
  }

  handleCreateGroup = (group: { name: string; description?: string }): Promise<Group> => {
    const newGroup = mockGroup(group);
    this.groups.push(newGroup);
    return this.reply(newGroup);
  };

  handleDeleteGroup: typeof deleteGroup = (id: string) => {
    if (!this.groups.some((g) => g.id === id)) {
      return Promise.reject();
    }

    const groupToDelete = this.groups.find((g) => g.id === id);
    if (groupToDelete?.managed) {
      return Promise.reject();
    }

    this.groups = this.groups.filter((g) => g.id !== id);
    return this.reply(undefined);
  };

  handleUpdateGroup: typeof updateGroup = (id, data): Promise<Record<string, never>> => {
    const group = this.groups.find((g) => g.id === id);
    if (group === undefined) {
      return Promise.reject();
    }

    if (data.description !== undefined) {
      group.description = data.description;
    }

    if (data.name !== undefined) {
      group.name = data.name;
    }

    return this.reply({});
  };

  handleSearchUsersGroups = (
    params: Parameters<typeof getUsersGroups>[0],
  ): Promise<{ groups: Group[]; page: Paging }> => {
    const { paging: page } = this;
    if (params.pageIndex !== undefined && params.pageIndex !== page.pageIndex) {
      this.setPaging({ pageIndex: page.pageIndex++ });
      const groups = [
        mockGroup({ name: `local-group ${this.groups.length + 4}` }),
        mockGroup({ name: `local-group ${this.groups.length + 5}` }),
      ];

      return this.reply({ page, groups });
    }
    if (params.managed === undefined) {
      return this.reply({
        page,
        groups: this.groups.filter((g) => (params?.q ? g.name.includes(params.q) : true)),
      });
    }
    const groups = this.groups.filter((group) => group.managed === params.managed);
    return this.reply({
      page,
      groups: groups.filter((g) => (params?.q ? g.name.includes(params.q) : true)),
    });
  };

  handleGetIdentityProviders = (): Promise<{ identityProviders: IdentityProvider[] }> => {
    return this.reply({ identityProviders: [mockIdentityProvider()] });
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
