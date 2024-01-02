/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import {
  createGroup,
  deleteGroup,
  searchUsersGroups,
  updateGroup,
} from '../../../../api/user_groups';
import { mockGroup } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import App from '../App';

jest.mock('../../../../api/user_groups', () => ({
  createGroup: jest.fn().mockResolvedValue({
    default: false,
    description: 'Desc foo',
    id: 3,
    membersCount: 0,
    name: 'Foo',
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
        name: 'Owners',
      },
      {
        default: true,
        description: 'Members of organization foo',
        id: 2,
        membersCount: 2,
        name: 'Members',
      },
    ],
  }),
  updateGroup: jest.fn().mockResolvedValue({}),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  await waitAndUpdate(wrapper);
  expect(searchUsersGroups).toHaveBeenCalledWith({ q: '' });
  expect(wrapper).toMatchSnapshot();
});

it('should correctly handle creation', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state('groups')).toHaveLength(2);
  wrapper.instance().handleCreate({ description: 'Desc foo', name: 'foo' });
  await waitAndUpdate(wrapper);
  expect(createGroup).toHaveBeenCalled();
});

it('should correctly handle deletion', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state('groups')).toHaveLength(2);
  wrapper.setState({ groupToBeDeleted: mockGroup({ name: 'Members' }) });
  wrapper.instance().handleDelete();
  await waitAndUpdate(wrapper);
  expect(deleteGroup).toHaveBeenCalled();
  expect(wrapper.state().groupToBeDeleted).toBeUndefined();
});

it('should ignore deletion', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  wrapper.setState({ groupToBeDeleted: undefined });
  wrapper.instance().handleDelete();
  expect(deleteGroup).not.toHaveBeenCalled();
});

it('should correctly handle edition', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  wrapper.setState({ editedGroup: mockGroup({ id: 1, name: 'Owners' }) });
  wrapper.instance().handleEdit({ description: 'foo', name: 'bar' });
  await waitAndUpdate(wrapper);
  expect(updateGroup).toHaveBeenCalled();
  expect(wrapper.state('groups')).toContainEqual({
    default: false,
    description: 'foo',
    id: 1,
    membersCount: 1,
    name: 'bar',
  });
});

it('should ignore edition', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  wrapper.setState({ editedGroup: undefined });
  wrapper.instance().handleEdit({ description: 'nope', name: 'nuhuh' });
  expect(updateGroup).not.toHaveBeenCalled();
});

it('should fetch more groups', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  wrapper.find('ListFooter').prop<Function>('loadMore')();
  await waitAndUpdate(wrapper);
  expect(searchUsersGroups).toHaveBeenCalledWith({ p: 2, q: '' });
  expect(wrapper.state('groups')).toHaveLength(4);
});

it('should search for groups', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  wrapper.find('SearchBox').prop<Function>('onChange')('foo');
  expect(searchUsersGroups).toHaveBeenCalledWith({ q: 'foo' });
  expect(wrapper.state('query')).toBe('foo');
});

it('should handle edit modal', async () => {
  const editedGroup = mockGroup();

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().editedGroup).toBeUndefined();

  wrapper.instance().openEditForm(editedGroup);
  expect(wrapper.state().editedGroup).toEqual(editedGroup);

  wrapper.instance().closeEditForm();
  expect(wrapper.state().editedGroup).toBeUndefined();
});

it('should handle delete modal', async () => {
  const groupToBeDeleted = mockGroup();

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().groupToBeDeleted).toBeUndefined();

  wrapper.instance().openDeleteForm(groupToBeDeleted);
  expect(wrapper.state().groupToBeDeleted).toEqual(groupToBeDeleted);

  wrapper.instance().closeDeleteForm();
  expect(wrapper.state().groupToBeDeleted).toBeUndefined();
});

it('should refresh correctly', async () => {
  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);

  const query = 'preserve me';
  wrapper.setState({ paging: { pageIndex: 2, pageSize: 2, total: 5 }, query });

  (searchUsersGroups as jest.Mock).mockClear();

  wrapper.instance().refresh();
  await waitAndUpdate(wrapper);

  expect(searchUsersGroups).toHaveBeenNthCalledWith(1, { q: query });
  expect(searchUsersGroups).toHaveBeenNthCalledWith(2, { q: query, p: 2 });
});

function shallowRender(props: Partial<App['props']> = {}) {
  return shallow<App>(<App {...props} />);
}
