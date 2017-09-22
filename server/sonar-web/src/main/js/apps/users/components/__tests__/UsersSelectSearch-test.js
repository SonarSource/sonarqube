/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import { shallow } from 'enzyme';
import UsersSelectSearch from '../UsersSelectSearch';

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
const onSearch = jest.fn(() => {
  return Promise.resolve(users);
});
const onChange = jest.fn();

it('should render correctly', () => {
  const wrapper = shallow(
    <UsersSelectSearch
      selectedUser={selectedUser}
      excludedUsers={excludedUsers}
      isLoading={false}
      handleValueChange={onChange}
      searchUsers={onSearch}
    />
  );
  expect(wrapper).toMatchSnapshot();
  const searchResult = wrapper.instance().filterSearchResult({ users });
  expect(searchResult).toMatchSnapshot();
  expect(wrapper.setState({ searchResult })).toMatchSnapshot();
});
