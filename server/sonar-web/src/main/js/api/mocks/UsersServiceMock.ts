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

import { isAfter, isBefore } from 'date-fns';
import { cloneDeep, isEmpty, isUndefined, omitBy } from 'lodash';
import { HttpStatus } from '../../helpers/request';
import { mockIdentityProvider, mockRestUser } from '../../helpers/testMocks';
import { IdentityProvider } from '../../types/types';
import { ChangePasswordResults, RestUserDetailed } from '../../types/users';
import { addUserToGroup, removeUserFromGroup } from '../legacy-group-membership';
import {
  UserGroup,
  changePassword,
  deleteUser,
  dismissNotice,
  getIdentityProviders,
  getUserGroups,
  getUsers,
  postUser,
  updateUser,
} from '../users';
import GroupMembershipsServiceMock from './GroupMembersipsServiceMock';

jest.mock('../users');
jest.mock('../legacy-group-membership');

const DEFAULT_USERS = [
  mockRestUser({
    managed: true,
    login: 'bob.marley',
    name: 'Bob Marley',
    sonarQubeLastConnectionDate: '2023-06-27T17:08:59+0200',
    sonarLintLastConnectionDate: '2023-06-27T17:08:59+0200',
    id: '1',
  }),
  mockRestUser({
    managed: false,
    login: 'alice.merveille',
    name: 'Alice Merveille',
    sonarQubeLastConnectionDate: '2023-06-27T17:08:59+0200',
    sonarLintLastConnectionDate: '2023-05-27T17:08:59+0200',
    email: 'alice.merveille@wonderland.com',
    id: '2',
  }),
  mockRestUser({
    managed: false,
    local: false,
    login: 'charlie.cox',
    name: 'Charlie Cox',
    sonarQubeLastConnectionDate: '2023-06-25T17:08:59+0200',
    sonarLintLastConnectionDate: '2023-06-20T12:10:59+0200',
    externalProvider: 'test',
    externalLogin: 'ExternalTest',
    id: '3',
  }),
  mockRestUser({
    managed: true,
    local: false,
    externalProvider: 'test2',
    externalLogin: 'UnknownExternalProvider',
    login: 'denis.villeneuve',
    name: 'Denis Villeneuve',
    sonarQubeLastConnectionDate: '2023-06-20T15:08:59+0200',
    sonarLintLastConnectionDate: '2023-05-25T10:08:59+0200',
    id: '4',
  }),
  mockRestUser({
    managed: true,
    login: 'eva.green',
    name: 'Eva Green',
    sonarQubeLastConnectionDate: '2023-05-27T17:08:59+0200',
    id: '5',
  }),
  mockRestUser({
    managed: false,
    login: 'franck.grillo',
    name: 'Franck Grillo',
    id: '6',
  }),
];

const DEFAULT_GROUPS: UserGroup[] = [
  {
    id: 1001,
    name: 'test1',
    description: 'test1',
    selected: true,
    default: true,
  },
  {
    id: 1002,
    name: 'test2',
    description: 'test2',
    selected: true,
    default: false,
  },
  {
    id: 1003,
    name: 'test3',
    description: 'test3',
    selected: true,
    default: false,
  },
  {
    id: 1004,
    name: 'test4',
    description: 'test4',
    selected: false,
    default: false,
  },
];

const DEFAULT_PASSWORD = 'test';

export default class UsersServiceMock {
  isManaged = true;
  users = cloneDeep(DEFAULT_USERS);
  groups = cloneDeep(DEFAULT_GROUPS);
  password = DEFAULT_PASSWORD;
  groupMembershipsServiceMock?: GroupMembershipsServiceMock = undefined;
  constructor(groupMembershipsServiceMock?: GroupMembershipsServiceMock) {
    this.groupMembershipsServiceMock = groupMembershipsServiceMock;
    jest.mocked(getIdentityProviders).mockImplementation(this.handleGetIdentityProviders);
    jest.mocked(getUsers).mockImplementation(this.handleGetUsers);
    jest.mocked(postUser).mockImplementation(this.handlePostUser);
    jest.mocked(updateUser).mockImplementation(this.handleUpdateUser);
    jest.mocked(getUserGroups).mockImplementation(this.handleGetUserGroups);
    jest.mocked(addUserToGroup).mockImplementation(this.handleAddUserToGroup);
    jest.mocked(removeUserFromGroup).mockImplementation(this.handleRemoveUserFromGroup);
    jest.mocked(changePassword).mockImplementation(this.handleChangePassword);
    jest.mocked(deleteUser).mockImplementation(this.handleDeactivateUser);
    jest.mocked(dismissNotice).mockResolvedValue({});
  }

  getFilteredRestUsers = (filterParams: Parameters<typeof getUsers>[0]) => {
    const {
      managed,
      q,
      sonarQubeLastConnectionDateFrom,
      sonarQubeLastConnectionDateTo,
      sonarLintLastConnectionDateFrom,
      sonarLintLastConnectionDateTo,
      groupId,
      'groupId!': groupIdExclude,
    } = filterParams;
    let { users } = this;
    if (groupId || groupIdExclude) {
      if (!this.groupMembershipsServiceMock) {
        throw new Error(
          'groupMembershipsServiceMock is not defined. Please provide GroupMembershipsServiceMock to UsersServiceMock constructor',
        );
      }
      const groupMemberships = this.groupMembershipsServiceMock?.memberships.filter(
        (m) => m.groupId === (groupId ?? groupIdExclude),
      );
      const userIds = groupMemberships?.map((m) => m.userId);
      users = users.filter((u) => (groupId ? userIds?.includes(u.id) : !userIds?.includes(u.id)));
    }

    return users.filter((user) => {
      if (this.isManaged && managed !== undefined && user.managed !== managed) {
        return false;
      }

      if (q && (!user.login.includes(q) || (user.name && !user.name.includes(q)))) {
        return false;
      }

      if (
        sonarQubeLastConnectionDateFrom &&
        (user.sonarQubeLastConnectionDate === null ||
          isBefore(
            new Date(user.sonarQubeLastConnectionDate),
            new Date(sonarQubeLastConnectionDateFrom),
          ))
      ) {
        return false;
      }

      if (
        sonarQubeLastConnectionDateTo &&
        user.sonarQubeLastConnectionDate &&
        isAfter(new Date(user.sonarQubeLastConnectionDate), new Date(sonarQubeLastConnectionDateTo))
      ) {
        return false;
      }

      if (
        sonarLintLastConnectionDateFrom &&
        (user.sonarLintLastConnectionDate === null ||
          isBefore(
            new Date(user.sonarLintLastConnectionDate),
            new Date(sonarLintLastConnectionDateFrom),
          ))
      ) {
        return false;
      }

      if (
        sonarLintLastConnectionDateTo &&
        user.sonarLintLastConnectionDate &&
        isAfter(new Date(user.sonarLintLastConnectionDate), new Date(sonarLintLastConnectionDateTo))
      ) {
        return false;
      }

      return true;
    });
  };

  handleGetUsers: typeof getUsers<RestUserDetailed> = (data) => {
    const pageIndex = data.pageIndex ?? 1;
    const pageSize = data.pageSize ?? 10;

    const users = this.getFilteredRestUsers(data);

    return this.reply({
      page: {
        pageIndex,
        pageSize,
        total: users.length,
      },
      users: users.slice((pageIndex - 1) * pageSize, pageIndex * pageSize),
    });
  };

  handlePostUser = (data: {
    email?: string;
    local?: boolean;
    login: string;
    name: string;
    password?: string;
    scmAccounts: string[];
  }) => {
    const { email, local, login, name, scmAccounts } = data;
    if (scmAccounts.some((a) => isEmpty(a.trim()))) {
      return Promise.reject({
        response: {
          status: HttpStatus.BadRequest,
          data: { message: 'Error: Empty SCM' },
        },
      });
    }
    const newUser = mockRestUser({
      email,
      local,
      login,
      name,
      scmAccounts,
    });
    this.users.push(newUser);
    return this.reply(newUser);
  };

  handleUpdateUser: typeof updateUser = (id, data) => {
    const { email, name, scmAccounts } = data;
    const user = this.users.find((u) => u.id === id);
    if (!user) {
      return Promise.reject('No such user');
    }
    Object.assign(user, {
      ...omitBy({ name, email, scmAccounts }, isUndefined),
    });
    return this.reply(user);
  };

  handleGetIdentityProviders = (): Promise<{ identityProviders: IdentityProvider[] }> => {
    return this.reply({
      identityProviders: [mockIdentityProvider({ key: 'test' })],
    });
  };

  handleGetUserGroups: typeof getUserGroups = (data) => {
    if (data.login !== 'alice.merveille') {
      return this.reply({
        paging: { pageIndex: 1, pageSize: 10, total: 0 },
        groups: [],
      });
    }
    const filteredGroups = this.groups
      .filter((g) => g.name.includes(data.q ?? ''))
      .filter((g) => {
        switch (data.selected) {
          case 'all':
            return true;
          case 'deselected':
            return !g.selected;
          default:
            return g.selected;
        }
      });

    return this.reply({
      paging: { pageIndex: 1, pageSize: 10, total: filteredGroups.length },
      groups: filteredGroups,
    });
  };

  handleAddUserToGroup: typeof addUserToGroup = ({ name }) => {
    this.groups = this.groups.map((g) => (g.name === name ? { ...g, selected: true } : g));
    return this.reply({});
  };

  handleRemoveUserFromGroup: typeof removeUserFromGroup = ({ name }) => {
    let isDefault = false;
    this.groups = this.groups.map((g) => {
      if (g.name === name) {
        if (g.default) {
          isDefault = true;
          return g;
        }
        return { ...g, selected: false };
      }
      return g;
    });
    return isDefault
      ? Promise.reject({
          errors: [{ msg: 'Cannot remove Default group' }],
        })
      : this.reply({});
  };

  handleChangePassword: typeof changePassword = (data) => {
    if (data.previousPassword !== this.password) {
      return Promise.reject(ChangePasswordResults.OldPasswordIncorrect);
    }
    if (data.password === this.password) {
      return Promise.reject(ChangePasswordResults.NewPasswordSameAsOld);
    }
    this.password = data.password;
    return this.reply({});
  };

  handleDeactivateUser: typeof deleteUser = (data) => {
    const index = this.users.findIndex((u) => u.id === data.id);
    const user = this.users.splice(index, 1)[0];
    user.active = false;
    return this.reply(undefined);
  };

  reset = () => {
    this.isManaged = true;
    this.users = cloneDeep(DEFAULT_USERS);
    this.groups = cloneDeep(DEFAULT_GROUPS);
    this.password = DEFAULT_PASSWORD;
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
