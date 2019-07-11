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
import EditMembersModal, { SearchParams } from '../EditMembersModal';
import SelectList, { Filter } from '../../../../components/SelectList/SelectList';
import { getUsersInGroup, addUserToGroup, removeUserFromGroup } from '../../../../api/user_groups';

jest.mock('../../../../api/user_groups', () => ({
  getUsersInGroup: jest.fn().mockResolvedValue({
    paging: { pageIndex: 1, pageSize: 10, total: 1 },
    users: [
      {
        login: 'foo',
        name: 'bar',
        selected: true
      }
    ]
  }),
  addUserToGroup: jest.fn().mockResolvedValue({}),
  removeUserFromGroup: jest.fn().mockResolvedValue({})
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render modal properly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper).toMatchSnapshot();
  expect(getUsersInGroup).toHaveBeenCalledWith(
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
  expect(getUsersInGroup).toHaveBeenCalledWith(
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
  expect(getUsersInGroup).toHaveBeenCalledWith(
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
  expect(getUsersInGroup).toHaveBeenCalledWith(
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
      login: 'toto'
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
      login: 'tata'
    })
  );
  expect(wrapper.state().listHasBeenTouched).toBe(true);
});

function shallowRender(props: Partial<EditMembersModal['props']> = {}) {
  return shallow<EditMembersModal>(
    <EditMembersModal
      group={{ id: 1, name: 'foo', membersCount: 1 }}
      onClose={jest.fn()}
      organization="bar"
      {...props}
    />
  );
}
