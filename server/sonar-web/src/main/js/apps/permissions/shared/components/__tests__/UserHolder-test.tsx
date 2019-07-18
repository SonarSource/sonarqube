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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { isSonarCloud } from '../../../../../helpers/system';
import UserHolder from '../UserHolder';

jest.mock('../../../../../helpers/system', () => ({ isSonarCloud: jest.fn() }));

const user = {
  email: 'john.doe@sonarsource.com',
  login: 'john doe',
  name: 'John Doe',
  permissions: ['bar']
};

const userHolder = (
  <UserHolder
    key="foo"
    onToggle={jest.fn(() => Promise.resolve())}
    permissions={[
      {
        category: 'admin',
        permissions: [
          { key: 'foo', name: 'Foo', description: '' },
          { key: 'bar', name: 'Bar', description: '' }
        ]
      },
      { key: 'baz', name: 'Baz', description: '' }
    ]}
    selectedPermission="bar"
    user={user}
  />
);

it('should render correctly', () => {
  expect(shallow(userHolder)).toMatchSnapshot();
});

it('should disabled PermissionCell checkboxes when waiting for promise to return', async () => {
  const wrapper = shallow<UserHolder>(userHolder);
  expect(wrapper.state().loading).toEqual([]);

  wrapper.instance().handleCheck(true, 'baz');
  wrapper.update();
  expect(wrapper.state().loading).toEqual(['baz']);

  wrapper.instance().handleCheck(true, 'bar');
  wrapper.update();
  expect(wrapper.state().loading).toEqual(['baz', 'bar']);

  await waitAndUpdate(wrapper);
  expect(wrapper.state().loading).toEqual([]);
});

it('should show user details for SonarQube', () => {
  (isSonarCloud as jest.Mock).mockReturnValue(false);

  const wrapper = shallow<UserHolder>(userHolder);
  expect(wrapper.find('.display-inline-block.text-middle')).toMatchSnapshot();
});

it('should show user details for SonarCloud', () => {
  (isSonarCloud as jest.Mock).mockReturnValue(true);

  const wrapper = shallow<UserHolder>(userHolder);
  expect(wrapper.find('.display-inline-block.text-middle')).toMatchSnapshot();
});
