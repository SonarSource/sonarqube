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
/* eslint-disable import/order */
import * as React from 'react';
import { shallow } from 'enzyme';
import { UsersApp } from '../UsersApp';
import { waitAndUpdate } from '../../../helpers/testUtils';
import { Location } from '../../../components/hoc/withRouter';

jest.mock('../../../api/users', () => ({
  getIdentityProviders: jest.fn(() =>
    Promise.resolve({
      identityProviders: [
        {
          backgroundColor: 'blue',
          iconPath: 'icon/path',
          key: 'foo',
          name: 'Foo Provider'
        }
      ]
    })
  ),
  searchUsers: jest.fn(() =>
    Promise.resolve({
      paging: {
        pageIndex: 1,
        pageSize: 1,
        total: 2
      },
      users: [
        {
          login: 'luke',
          name: 'Luke',
          active: true,
          scmAccounts: [],
          local: false
        }
      ]
    })
  )
}));

const getIdentityProviders = require('../../../api/users').getIdentityProviders as jest.Mock<any>;
const searchUsers = require('../../../api/users').searchUsers as jest.Mock<any>;

const currentUser = { isLoggedIn: true, login: 'luke' };
const location = { pathname: '', query: {} } as Location;

beforeEach(() => {
  getIdentityProviders.mockClear();
  searchUsers.mockClear();
});

it('should render correctly', async () => {
  const wrapper = getWrapper();
  expect(wrapper).toMatchSnapshot();
  expect(getIdentityProviders).toHaveBeenCalled();
  expect(searchUsers).toHaveBeenCalled();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

function getWrapper(props: Partial<UsersApp['props']> = {}) {
  return shallow(
    <UsersApp
      currentUser={currentUser}
      location={location}
      organizationsEnabled={true}
      router={{ push: jest.fn() }}
      {...props}
    />
  );
}
