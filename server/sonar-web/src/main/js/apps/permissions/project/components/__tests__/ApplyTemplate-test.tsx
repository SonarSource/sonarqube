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
import ApplyTemplate from '../ApplyTemplate';

jest.mock('../../../../../api/permissions', () => ({
  getPermissionTemplates: jest.fn().mockResolvedValue({
    permissionTemplates: [
      {
        id: 'tmp1',
        name: 'SonarSource projects',
        createdAt: '2015-11-27T15:20:32+0100',
        permissions: [
          { key: 'admin', usersCount: 0, groupsCount: 3 },
          { key: 'codeviewer', usersCount: 0, groupsCount: 6 }
        ]
      }
    ],
    defaultTemplates: [{ templateId: 'tmp1', qualifier: 'TRK' }],
    permissions: [
      { key: 'admin', name: 'Administer', description: 'Administer access' },
      { key: 'codeviewer', name: 'See Source Code', description: 'View code' }
    ]
  })
}));

it('render correctly', async () => {
  const wrapper = shallow(
    <ApplyTemplate onClose={jest.fn()} organization="foo" project={{ key: 'foo', name: 'Foo' }} />
  );
  expect(wrapper).toMatchSnapshot();
  await waitAndUpdate(wrapper);
  expect(wrapper.dive()).toMatchSnapshot();
});
