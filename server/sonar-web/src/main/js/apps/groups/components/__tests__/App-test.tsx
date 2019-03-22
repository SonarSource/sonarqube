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
import App from '../App';
import { mockOrganization } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';

jest.mock('../../../../api/user_groups', () => ({
  createGroup: jest.fn(),
  deleteGroup: jest.fn(),
  searchUsersGroups: jest.fn().mockResolvedValue({
    paging: { pageIndex: 1, pageSize: 100, total: 2 },
    groups: [
      {
        default: false,
        description: 'Owners of organization foo',
        id: 1,
        membersCount: 1,
        name: 'Owners'
      },
      {
        default: true,
        description: 'Members of organization foo',
        id: 2,
        membersCount: 2,
        name: 'Members'
      }
    ]
  }),
  updateGroup: jest.fn()
}));

it('should render correctly', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<App['props']> = {}) {
  return shallow(<App organization={mockOrganization()} {...props} />);
}
