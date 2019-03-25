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
import {
  createGroup,
  deleteGroup,
  searchUsersGroups,
  updateGroup
} from '../../../../api/user_groups';

jest.mock('../../../../api/user_groups', () => ({
  createGroup: jest.fn().mockResolvedValue({
    default: false,
    description: 'Desc foo',
    id: 3,
    membersCount: 0,
    name: 'Foo'
  }),
  deleteGroup: jest.fn().mockResolvedValue({}),
  searchUsersGroups: jest.fn().mockResolvedValue({
    paging: { pageIndex: 1, pageSize: 2, total: 4 },
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
  updateGroup: jest.fn().mockResolvedValue({})
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  await waitAndUpdate(wrapper);
  expect(searchUsersGroups).toHaveBeenCalledWith({ organization: 'foo', q: '' });
  expect(wrapper).toMatchSnapshot();
});

it('should correctly handle creation', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state('groups')).toHaveLength(2);
  wrapper.instance().handleCreate({ description: 'Desc foo', name: 'foo' });
  await waitAndUpdate(wrapper);
  expect(createGroup).toHaveBeenCalled();
  expect(wrapper.state('groups')).toHaveLength(3);
});

it('should correctly handle deletion', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state('groups')).toHaveLength(2);
  wrapper.instance().handleDelete('Members');
  await waitAndUpdate(wrapper);
  expect(deleteGroup).toHaveBeenCalled();
  expect(wrapper.state('groups')).toHaveLength(1);
});

it('should correctly handle edition', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  wrapper.instance().handleEdit({ id: 1, description: 'foo', name: 'bar' });
  await waitAndUpdate(wrapper);
  expect(updateGroup).toHaveBeenCalled();
  expect(wrapper.state('groups')).toContainEqual({
    default: false,
    description: 'foo',
    id: 1,
    membersCount: 1,
    name: 'bar'
  });
});

it('should fetch more groups', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  wrapper.find('ListFooter').prop<Function>('loadMore')();
  await waitAndUpdate(wrapper);
  expect(searchUsersGroups).toHaveBeenCalledWith({ organization: 'foo', p: 2, q: '' });
  expect(wrapper.state('groups')).toHaveLength(4);
});

it('should search for groups', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  wrapper.find('SearchBox').prop<Function>('onChange')('foo');
  expect(searchUsersGroups).toBeCalledWith({ organization: 'foo', q: 'foo' });
  expect(wrapper.state('query')).toBe('foo');
});

function shallowRender(props: Partial<App['props']> = {}) {
  return shallow<App>(<App organization={mockOrganization()} {...props} />);
}
