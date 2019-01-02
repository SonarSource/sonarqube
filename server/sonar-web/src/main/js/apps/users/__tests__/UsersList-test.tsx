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
import * as React from 'react';
import { shallow } from 'enzyme';
import UsersList from '../UsersList';

const users = [
  {
    login: 'luke',
    name: 'Luke',
    active: true,
    scmAccounts: [],
    local: false
  },
  {
    login: 'obi',
    name: 'One',
    active: true,
    scmAccounts: [],
    local: false
  }
];

it('should render correctly', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('should show a group column', () => {
  const wrapper = getWrapper({ organizationsEnabled: false });
  expect(wrapper.find('th').filterWhere(elem => elem.text() === 'my_profile.groups')).toHaveLength(
    1
  );
});

function getWrapper(props = {}) {
  return shallow(
    <UsersList
      currentUser={{ isLoggedIn: true, login: 'luke' }}
      identityProviders={[
        {
          backgroundColor: 'blue',
          iconPath: 'icon/path',
          key: 'foo',
          name: 'Foo Provider'
        }
      ]}
      onUpdateUsers={jest.fn()}
      organizationsEnabled={true}
      updateTokensCount={jest.fn()}
      users={users}
      {...props}
    />
  );
}
