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
import { shallow } from 'enzyme';
import * as React from 'react';
import { click } from 'sonar-ui-common/helpers/testUtils';
import UserListItem from '../UserListItem';

jest.mock('../../../../components/intl/DateFromNow');
jest.mock('../../../../components/intl/DateTimeFormatter');

const user: T.User = {
  active: true,
  lastConnectionDate: '2019-01-18T15:06:33+0100',
  local: false,
  login: 'obi',
  name: 'One',
  scmAccounts: []
};

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render correctly without last connection date', () => {
  expect(shallowRender({})).toMatchSnapshot();
});

it('should display a change password button', () => {
  expect(
    shallowRender({ organizationsEnabled: true })
      .find('UserGroups')
      .exists()
  ).toBeFalsy();
});

it('should open the correct forms', () => {
  const wrapper = shallowRender();
  click(wrapper.find('.js-user-tokens'));
  expect(wrapper.find('TokensFormModal').exists()).toBeTruthy();
});

function shallowRender(props: Partial<UserListItem['props']> = {}) {
  return shallow(
    <UserListItem
      isCurrentUser={false}
      onUpdateUsers={jest.fn()}
      organizationsEnabled={false}
      updateTokensCount={jest.fn()}
      user={user}
      {...props}
    />
  );
}
