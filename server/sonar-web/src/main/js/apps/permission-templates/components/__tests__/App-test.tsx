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
import { App } from '../App';
import { mockLocation, mockOrganization } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';

jest.mock('../../../../api/permissions', () => ({
  getPermissionTemplates: jest.fn().mockResolvedValue({
    permissionTemplates: [
      {
        id: '1',
        name: 'Default template',
        description: 'Default permission template of organization test',
        createdAt: '2019-02-07T17:23:26+0100',
        updatedAt: '2019-02-07T17:23:26+0100',
        permissions: [
          { key: 'admin', usersCount: 0, groupsCount: 1, withProjectCreator: false },
          { key: 'codeviewer', usersCount: 0, groupsCount: 1, withProjectCreator: false }
        ]
      }
    ],
    defaultTemplates: [{ templateId: '1', qualifier: 'TRK' }],
    permissions: [
      { key: 'admin', name: 'Administer', description: 'Admin permission' },
      { key: 'codeviewer', name: 'See Source Code', description: 'Code viewer permission' }
    ]
  })
}));

it('should render correctly', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<App['props']> = {}) {
  return shallow(
    <App
      location={mockLocation()}
      organization={mockOrganization()}
      topQualifiers={['TRK']}
      {...props}
    />
  );
}
