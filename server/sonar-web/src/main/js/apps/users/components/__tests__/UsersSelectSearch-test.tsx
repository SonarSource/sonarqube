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
import UsersSelectSearch, {
  UsersSelectSearchOption,
  UsersSelectSearchValue
} from '../UsersSelectSearch';

const selectedUser = {
  login: 'admin',
  name: 'Administrator',
  avatar: '7daf6c79d4802916d83f6266e24850af'
};
const users = [
  { login: 'admin', name: 'Administrator', email: 'admin@admin.ch' },
  { login: 'test', name: 'Tester', email: 'tester@testing.ch' },
  { login: 'foo', name: 'Foo Bar', email: 'foo@bar.ch' }
];
const excludedUsers = ['admin'];

describe('UsersSelectSearch', () => {
  it('should render correctly', () => {
    const onSearch = jest.fn().mockResolvedValue({ users });
    const wrapper = shallow(
      <UsersSelectSearch
        excludedUsers={excludedUsers}
        handleValueChange={jest.fn()}
        searchUsers={onSearch}
        selectedUser={selectedUser}
      />
    );
    expect(wrapper).toMatchSnapshot();
    const searchResult = (wrapper.instance() as UsersSelectSearch).filterSearchResult({ users });
    expect(searchResult).toMatchSnapshot();
    expect(wrapper.setState({ searchResult })).toMatchSnapshot();
  });
});

describe('UsersSelectSearchOption', () => {
  it('should render correctly without all parameters', () => {
    const wrapper = shallow(
      <UsersSelectSearchOption onFocus={jest.fn()} onSelect={jest.fn()} option={selectedUser}>
        {selectedUser.name}
      </UsersSelectSearchOption>
    );
    expect(wrapper).toMatchSnapshot();
  });

  it('should render correctly with email instead of hash', () => {
    const wrapper = shallow(
      <UsersSelectSearchOption onFocus={jest.fn()} onSelect={jest.fn()} option={users[0]}>
        {users[0].name}
      </UsersSelectSearchOption>
    );
    expect(wrapper).toMatchSnapshot();
  });
});

describe('UsersSelectSearchValue', () => {
  it('should render correctly with a user', () => {
    const wrapper = shallow(
      <UsersSelectSearchValue value={selectedUser}>{selectedUser.name}</UsersSelectSearchValue>
    );
    expect(wrapper).toMatchSnapshot();
  });

  it('should render correctly with email instead of hash', () => {
    const wrapper = shallow(
      <UsersSelectSearchValue value={users[0]}>{users[0].name}</UsersSelectSearchValue>
    );
    expect(wrapper).toMatchSnapshot();
  });

  it('should render correctly without value', () => {
    const wrapper = shallow(<UsersSelectSearchValue />);
    expect(wrapper).toMatchSnapshot();
  });
});
