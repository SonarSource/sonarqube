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
import OrganizationMembers from '../OrganizationMembers';
import { waitAndUpdate } from '../../../helpers/testUtils';
import { searchMembers, addMember, removeMember } from '../../../api/organizations';
import { searchUsersGroups, addUserToGroup, removeUserFromGroup } from '../../../api/user_groups';

jest.mock('../../../api/organizations', () => ({
  addMember: jest.fn().mockResolvedValue({ login: 'bar', name: 'Bar', groupCount: 1 }),
  removeMember: jest.fn().mockResolvedValue(undefined),
  searchMembers: jest.fn().mockResolvedValue({
    paging: { pageIndex: 1, pageSize: 2, total: 3 },
    users: [
      { login: 'admin', name: 'Admin Istrator', avatar: '', groupCount: 3 },
      { login: 'john', name: 'John Doe', avatar: '7daf6c79d4802916d83f6266e24850af', groupCount: 1 }
    ]
  })
}));

jest.mock('../../../api/user_groups', () => ({
  addUserToGroup: jest.fn().mockResolvedValue(undefined),
  removeUserFromGroup: jest.fn().mockResolvedValue(undefined),
  searchUsersGroups: jest.fn().mockResolvedValue({
    paging: { pageIndex: 1, pageSize: 100, total: 2 },
    groups: [
      { id: 1, name: 'Members', description: '', membersCount: 2, default: true },
      { id: 2, name: 'Watchers', description: '', membersCount: 0, default: false }
    ]
  })
}));

const organization = { key: 'foo', name: 'Foo' };

beforeEach(() => {
  (searchMembers as jest.Mock).mockClear();
  (searchUsersGroups as jest.Mock).mockClear();
});

it('should fetch members and render for non-admin', async () => {
  const wrapper = shallow(<OrganizationMembers organization={organization} />);
  expect(wrapper).toMatchSnapshot();

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(searchMembers).toBeCalledWith({ organization: 'foo', ps: 50, q: undefined });
});

it('should fetch members and groups and render for admin', async () => {
  const wrapper = shallow(
    <OrganizationMembers organization={{ ...organization, actions: { admin: true } }} />
  );
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(searchMembers).toBeCalledWith({ organization: 'foo', ps: 50, q: undefined });
  expect(searchUsersGroups).toBeCalledWith({ organization: 'foo' });
});

it('should search users', async () => {
  const wrapper = shallow(
    <OrganizationMembers organization={{ ...organization, actions: { admin: true } }} />
  );
  await waitAndUpdate(wrapper);
  wrapper.find('MembersListHeader').prop<Function>('handleSearch')('user');
  expect(searchMembers).lastCalledWith({ organization: 'foo', ps: 50, q: 'user' });
});

it('should load more members', async () => {
  const wrapper = shallow(
    <OrganizationMembers organization={{ ...organization, actions: { admin: true } }} />
  );
  await waitAndUpdate(wrapper);
  wrapper.find('ListFooter').prop<Function>('loadMore')();
  expect(searchMembers).lastCalledWith({ organization: 'foo', p: 2, ps: 50, q: undefined });
});

it('should add new member', async () => {
  const wrapper = shallow(
    <OrganizationMembers organization={{ ...organization, actions: { admin: true } }} />
  );
  await waitAndUpdate(wrapper);
  wrapper.find('AddMemberForm').prop<Function>('addMember')({ login: 'bar' });
  await waitAndUpdate(wrapper);
  expect(
    wrapper
      .find('MembersList')
      .prop<T.OrganizationMember[]>('members')
      .find(m => m.login === 'bar')
  ).toBeDefined();
  expect(wrapper.find('ListFooter').prop('total')).toEqual(4);
  expect(addMember).toBeCalledWith({ login: 'bar', organization: 'foo' });
});

it('should remove member', async () => {
  const wrapper = shallow(
    <OrganizationMembers organization={{ ...organization, actions: { admin: true } }} />
  );
  await waitAndUpdate(wrapper);
  wrapper.find('MembersList').prop<Function>('removeMember')({ login: 'john' });
  await waitAndUpdate(wrapper);
  expect(
    wrapper
      .find('MembersList')
      .prop<T.OrganizationMember[]>('members')
      .find(m => m.login === 'john')
  ).toBeUndefined();
  expect(wrapper.find('ListFooter').prop('total')).toEqual(2);
  expect(removeMember).toBeCalledWith({ login: 'john', organization: 'foo' });
});

it('should update groups', async () => {
  const wrapper = shallow(
    <OrganizationMembers organization={{ ...organization, actions: { admin: true } }} />
  );
  await waitAndUpdate(wrapper);
  wrapper.find('MembersList').prop<Function>('updateMemberGroups')(
    { login: 'john' },
    ['cats', 'dogs'], // add to
    ['birds'] // remove from
  );
  await waitAndUpdate(wrapper);
  const john = wrapper
    .find('MembersList')
    .prop<T.OrganizationMember[]>('members')
    .find(m => m.login === 'john');
  expect(john && john.groupCount).toBe(2);
  expect(addUserToGroup).toHaveBeenCalledTimes(2);
  expect(addUserToGroup).toBeCalledWith({ login: 'john', name: 'cats', organization: 'foo' });
  expect(addUserToGroup).toBeCalledWith({ login: 'john', name: 'dogs', organization: 'foo' });
  expect(removeUserFromGroup).toHaveBeenCalledTimes(1);
  expect(removeUserFromGroup).toBeCalledWith({ login: 'john', name: 'birds', organization: 'foo' });
});
