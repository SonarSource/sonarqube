/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { mockPermissionUser } from '../../../../../helpers/mocks/permissions';
import { waitAndUpdate } from '../../../../../helpers/testUtils';
import UserHolder from '../UserHolder';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ user: mockPermissionUser({ login: '<creator>' }) })).toMatchSnapshot(
    'creator'
  );
});

it('should disabled PermissionCell checkboxes when waiting for promise to return', async () => {
  const wrapper = shallowRender();
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

function shallowRender(props: Partial<UserHolder['props']> = {}) {
  return shallow<UserHolder>(
    <UserHolder
      onToggle={jest.fn().mockResolvedValue(null)}
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
      user={mockPermissionUser({ email: 'john.doe@sonarsource.com', name: 'John Doe' })}
      {...props}
    />
  );
}
