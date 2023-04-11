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
import { cloneDeep } from 'lodash';
import { mockClusterSysInfo, mockIdentityProvider, mockUser } from '../../helpers/testMocks';
import { IdentityProvider, Paging, SysInfoCluster } from '../../types/types';
import { User } from '../../types/users';
import { getSystemInfo } from '../system';
import { createUser, getIdentityProviders, searchUsers } from '../users';

const DEFAULT_USERS = [
  mockUser({
    managed: true,
    login: 'bob.marley',
    name: 'Bob Marley',
    lastConnectionDate: '2023-06-27T17:08:59+0200',
    sonarLintLastConnectionDate: '2023-06-27T17:08:59+0200',
  }),
  mockUser({
    managed: false,
    login: 'alice.merveille',
    name: 'Alice Merveille',
    lastConnectionDate: '2023-06-27T17:08:59+0200',
    sonarLintLastConnectionDate: '2023-05-27T17:08:59+0200',
  }),
  mockUser({
    managed: false,
    login: 'charlie.cox',
    name: 'Charlie Cox',
    lastConnectionDate: '2023-06-25T17:08:59+0200',
    sonarLintLastConnectionDate: '2023-06-20T12:10:59+0200',
  }),
  mockUser({
    managed: true,
    login: 'denis.villeneuve',
    name: 'Denis Villeneuve',
    lastConnectionDate: '2023-06-20T15:08:59+0200',
    sonarLintLastConnectionDate: '2023-05-25T10:08:59+0200',
  }),
  mockUser({
    managed: true,
    login: 'eva.green',
    name: 'Eva Green',
    lastConnectionDate: '2023-05-27T17:08:59+0200',
  }),
  mockUser({
    managed: false,
    login: 'franck.grillo',
    name: 'Franck Grillo',
  }),
];

export default class UsersServiceMock {
  isManaged = true;
  users = cloneDeep(DEFAULT_USERS);
  constructor() {
    jest.mocked(getSystemInfo).mockImplementation(this.handleGetSystemInfo);
    jest.mocked(getIdentityProviders).mockImplementation(this.handleGetIdentityProviders);
    jest.mocked(searchUsers).mockImplementation((p) => this.handleSearchUsers(p));
    jest.mocked(createUser).mockImplementation(this.handleCreateUser);
  }

  setIsManaged(managed: boolean) {
    this.isManaged = managed;
  }

  getFilteredUsers = (filterParams: {
    managed: boolean;
    q: string;
    lastConnectedAfter?: string;
    lastConnectedBefore?: string;
    slLastConnectedAfter?: string;
    slLastConnectedBefore?: string;
  }) => {
    const {
      managed,
      q,
      lastConnectedAfter,
      lastConnectedBefore,
      slLastConnectedAfter,
      slLastConnectedBefore,
    } = filterParams;

    return this.users.filter((user) => {
      if (this.isManaged && managed !== undefined && user.managed !== managed) {
        return false;
      }

      if (q && (!user.login.includes(q) || (user.name && !user.name.includes(q)))) {
        return false;
      }

      if (
        lastConnectedAfter &&
        (user.lastConnectionDate === undefined ||
          isBefore(new Date(user.lastConnectionDate), new Date(lastConnectedAfter)))
      ) {
        return false;
      }

      if (
        lastConnectedBefore &&
        user.lastConnectionDate !== undefined &&
        isAfter(new Date(user.lastConnectionDate), new Date(lastConnectedBefore))
      ) {
        return false;
      }

      if (
        slLastConnectedAfter &&
        (user.sonarLintLastConnectionDate === undefined ||
          isBefore(new Date(user.sonarLintLastConnectionDate), new Date(slLastConnectedAfter)))
      ) {
        return false;
      }

      if (
        slLastConnectedBefore &&
        user.sonarLintLastConnectionDate !== undefined &&
        isAfter(new Date(user.sonarLintLastConnectionDate), new Date(slLastConnectedBefore))
      ) {
        return false;
      }

      return true;
    });
  };

  handleSearchUsers = (data: any): Promise<{ paging: Paging; users: User[] }> => {
    let paging = {
      pageIndex: 1,
      pageSize: 0,
      total: 10,
    };

    if (data.p !== undefined && data.p !== paging.pageIndex) {
      paging = { pageIndex: 2, pageSize: 2, total: 10 };
      const users = [
        mockUser({
          name: `Local User ${this.users.length + 4}`,
          login: `local-user-${this.users.length + 4}`,
        }),
        mockUser({
          name: `Local User ${this.users.length + 5}`,
          login: `local-user-${this.users.length + 5}`,
        }),
      ];

      return this.reply({ paging, users });
    }

    const users = this.getFilteredUsers(data);
    return this.reply({
      paging: {
        pageIndex: 1,
        pageSize: users.length,
        total: 10,
      },
      users,
    });
  };

  handleCreateUser = (data: {
    email?: string;
    local?: boolean;
    login: string;
    name: string;
    password?: string;
    scmAccount: string[];
  }) => {
    const { email, local, login, name, scmAccount } = data;
    const newUser = mockUser({
      email,
      local,
      login,
      name,
      scmAccounts: scmAccount,
    });
    this.users.push(newUser);
    return this.reply(undefined);
  };

  handleGetIdentityProviders = (): Promise<{ identityProviders: IdentityProvider[] }> => {
    return this.reply({ identityProviders: [mockIdentityProvider()] });
  };

  handleGetSystemInfo = (): Promise<SysInfoCluster> => {
    return this.reply(
      mockClusterSysInfo(
        this.isManaged
          ? {
              System: {
                'High Availability': true,
                'Server ID': 'asd564-asd54a-5dsfg45',
                'External Users and Groups Provisioning': 'Okta',
              },
            }
          : {}
      )
    );
  };

  reset = () => {
    this.isManaged = true;
    this.users = cloneDeep(DEFAULT_USERS);
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
