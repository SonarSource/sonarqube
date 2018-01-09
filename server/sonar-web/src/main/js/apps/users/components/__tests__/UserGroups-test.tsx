/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { click } from '../../../../helpers/testUtils';
import UserGroups from '../UserGroups';

const user = {
  login: 'obi',
  name: 'One',
  active: true,
  scmAccounts: [],
  local: false
};

const groups = ['foo', 'bar', 'baz', 'plop'];

it('should render correctly', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('should show all groups', () => {
  const wrapper = getWrapper();
  expect(wrapper.find('li')).toHaveLength(3);
  click(wrapper.find('.js-user-more-groups'));
  expect(wrapper.find('li')).toHaveLength(5);
});

it('should open the groups form', () => {
  const wrapper = getWrapper();
  click(wrapper.find('.js-user-groups'));
  expect(wrapper.find('GroupsForm').exists()).toBeTruthy();
});

function getWrapper(props = {}) {
  return shallow(<UserGroups groups={groups} onUpdateUsers={jest.fn()} user={user} {...props} />);
}
