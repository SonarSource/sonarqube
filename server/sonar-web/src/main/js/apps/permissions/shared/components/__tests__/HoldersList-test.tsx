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
import HoldersList from '../HoldersList';

const permissions = [
  { key: 'foo', name: 'Foo', description: '' },
  {
    category: 'admin',
    permissions: [
      { key: 'bar', name: 'Bar', description: '' },
      { key: 'baz', name: 'Baz', description: '' }
    ]
  }
];

const groups = [
  { id: 'foobar', name: 'Foobar', permissions: ['bar'] },
  { id: 'barbaz', name: 'Barbaz', permissions: ['bar'] },
  { id: 'abc', name: 'abc', permissions: [] }
];

const users = [
  { login: 'foobar', name: 'Foobar', permissions: ['bar'] },
  { login: 'barbaz', name: 'Barbaz', permissions: ['bar'] },
  { login: 'bcd', name: 'bcd', permissions: [] }
];

const elementsContainer = (
  <HoldersList
    groups={groups}
    onSelectPermission={jest.fn(() => Promise.resolve())}
    onToggleGroup={jest.fn(() => Promise.resolve())}
    onToggleUser={jest.fn(() => Promise.resolve())}
    permissions={permissions}
    selectedPermission="bar"
    users={users}
  />
);

it('should display users and groups', () => {
  expect(shallow(elementsContainer)).toMatchSnapshot();
});
