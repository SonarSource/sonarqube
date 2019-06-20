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
import GroupsForm, { SearchParams } from '../GroupsForm';
import SelectList, { Filter } from '../../../../components/SelectList/SelectList';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { getUserGroups } from '../../../../api/users';
import { addUserToGroup, removeUserFromGroup } from '../../../../api/user_groups';
import { mockUser } from '../../../../helpers/testMocks';

jest.mock('../../../../api/users', () => ({
  getUserGroups: jest.fn().mockResolvedValue({
    paging: { pageIndex: 1, pageSize: 10, total: 1 },
    groups: [
      {
        id: 1001,
        name: 'test1',
        description: 'test1',
        selected: true
      },
      {
        id: 1002,
        name: 'test2',
        description: 'test2',
        selected: true
      },
      {
        id: 1003,
        name: 'test3',
        description: 'test3',
        selected: false
      }
    ]
  })
}));

jest.mock('../../../../api/user_groups', () => ({
  addUserToGroup: jest.fn().mockResolvedValue({}),
  removeUserFromGroup: jest.fn().mockResolvedValue({})
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper).toMatchSnapshot();
  expect(getUserGroups).toHaveBeenCalledWith(
    expect.objectContaining({
      p: 1
    })
  );

  wrapper.setState({ listHasBeenTouched: true });
  expect(wrapper.find(SelectList).props().needReload).toBe(true);

  wrapper.setState({ lastSearchParams: { selected: Filter.All } as SearchParams });
  expect(wrapper.find(SelectList).props().needReload).toBe(false);
});

it('should handle reload properly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().handleReload();
  expect(getUserGroups).toHaveBeenCalledWith(
    expect.objectContaining({
      p: 1
    })
  );
  expect(wrapper.state().listHasBeenTouched).toBe(false);
});

it('should handle search reload properly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().handleSearch('foo', Filter.Selected);
  expect(getUserGroups).toHaveBeenCalledWith(
    expect.objectContaining({
      p: 1,
      q: 'foo',
      selected: Filter.Selected
    })
  );
  expect(wrapper.state().listHasBeenTouched).toBe(false);
});

it('should handle load more properly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().handleLoadMore();
  expect(getUserGroups).toHaveBeenCalledWith(
    expect.objectContaining({
      p: 2
    })
  );
  expect(wrapper.state().listHasBeenTouched).toBe(false);
});

it('should handle selection properly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().handleSelect('toto');
  await waitAndUpdate(wrapper);
  expect(addUserToGroup).toHaveBeenCalledWith(
    expect.objectContaining({
      name: 'toto'
    })
  );
  expect(wrapper.state().listHasBeenTouched).toBe(true);
});

it('should handle deselection properly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().handleUnselect('tata');
  await waitAndUpdate(wrapper);
  expect(removeUserFromGroup).toHaveBeenCalledWith(
    expect.objectContaining({
      name: 'tata'
    })
  );
  expect(wrapper.state().listHasBeenTouched).toBe(true);
});

function shallowRender(props: Partial<GroupsForm['props']> = {}) {
  return shallow<GroupsForm>(
    <GroupsForm onClose={jest.fn()} onUpdateUsers={jest.fn()} user={mockUser()} {...props} />
  );
}
