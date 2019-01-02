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
import PermissionCell from '../PermissionCell';

const permissionItem = {
  id: 'foobar',
  name: 'Foobar',
  permissions: ['bar']
};

const permission = { key: 'baz', name: 'Baz', description: '' };
const permissionGroup = {
  category: 'admin',
  permissions: [
    { key: 'foo', name: 'Foo', description: '' },
    { key: 'bar', name: 'Bar', description: '' }
  ]
};
it('should display unchecked checkbox', () => {
  expect(
    shallow(
      <PermissionCell
        loading={[]}
        onCheck={jest.fn()}
        permission={permission}
        permissionItem={permissionItem}
      />
    )
  ).toMatchSnapshot();
});

it('should display multiple checkboxes with one checked', () => {
  expect(
    shallow(
      <PermissionCell
        loading={[]}
        onCheck={jest.fn()}
        permission={permissionGroup}
        permissionItem={permissionItem}
      />
    )
  ).toMatchSnapshot();
});

it('should display disabled checkbox', () => {
  expect(
    shallow(
      <PermissionCell
        loading={['baz']}
        onCheck={jest.fn()}
        permission={permission}
        permissionItem={permissionItem}
      />
    )
  ).toMatchSnapshot();
});

it('should display selected checkbox', () => {
  expect(
    shallow(
      <PermissionCell
        loading={[]}
        onCheck={jest.fn()}
        permission={permission}
        permissionItem={permissionItem}
        selectedPermission="baz"
      />
    )
  ).toMatchSnapshot();
});
