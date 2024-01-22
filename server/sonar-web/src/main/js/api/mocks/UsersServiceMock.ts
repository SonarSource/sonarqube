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
import { isAfter, isBefore } from 'date-fns';
import { cloneDeep, isEmpty, isUndefined, omitBy } from 'lodash';
import { HttpStatus } from '../../helpers/request';
import { mockIdentityProvider, mockLoggedInUser, mockRestUser } from '../../helpers/testMocks';
import { IdentityProvider } from '../../types/types';
import {
  ChangePasswordResults,
  LoggedInUser,
  NoticeType,
  RestUserDetailed,
} from '../../types/users';
import {
  changePassword,
  deleteUser,
  dismissNotice,
  getCurrentUser,
  getIdentityProviders,
  getUsers,
  postUser,
  updateUser,
} from '../users';
import GroupMembershipsServiceMock from './GroupMembersipsServiceMock';

jest.mock('../users');

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

const DEFAULT_PASSWORD = 'test';

export default class UsersServiceMock {
  isManaged = true;
  users = cloneDeep(DEFAULT_USERS);
  currentUser = mockLoggedInUser();
  password = DEFAULT_PASSWORD;
  groupMembershipsServiceMock?: GroupMembershipsServiceMock = undefined;
  constructor(groupMembershipsServiceMock?: GroupMembershipsServiceMock) {
    this.groupMembershipsServiceMock = groupMembershipsServiceMock;
    jest.mocked(getIdentityProviders).mockImplementation(this.handleGetIdentityProviders);
    jest.mocked(getUsers).mockImplementation(this.handleGetUsers);
    jest.mocked(postUser).mockImplementation(this.handlePostUser);
    jest.mocked(updateUser).mockImplementation(this.handleUpdateUser);
    jest.mocked(changePassword).mockImplementation(this.handleChangePassword);
    jest.mocked(deleteUser).mockImplementation(this.handleDeactivateUser);
    jest.mocked(dismissNotice).mockImplementation(this.handleDismissNotification);
    jest.mocked(getCurrentUser).mockImplementation(this.handleGetCurrentUser);
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

      if (q && !user.login.includes(q) && !user.name?.includes(q) && !user.email?.includes(q)) {
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

  handleDismissNotification: typeof dismissNotice = (noticeType: NoticeType) => {
    if (Object.values(NoticeType).includes(noticeType)) {
      return this.reply(true);
    }

    return Promise.reject();
  };

  setCurrentUser = (user: LoggedInUser) => {
    this.currentUser = user;
  };

  handleGetCurrentUser: typeof getCurrentUser = () => {
    return this.reply(this.currentUser);
  };

  reset = () => {
    this.isManaged = true;
    this.users = cloneDeep(DEFAULT_USERS);
    this.password = DEFAULT_PASSWORD;
    this.currentUser = mockLoggedInUser();
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
