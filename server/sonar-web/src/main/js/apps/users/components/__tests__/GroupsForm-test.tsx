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
import SelectList, { SelectListFilter } from 'sonar-ui-common/components/controls/SelectList';
import { click, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { getUserGroups } from '../../../../api/users';
import { addUserToGroup, removeUserFromGroup } from '../../../../api/user_groups';
import { mockUser } from '../../../../helpers/testMocks';
import GroupsForm from '../GroupsForm';

const user = mockUser();

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
  wrapper
    .find(SelectList)
    .props()
    .onSearch({
      query: '',
      filter: SelectListFilter.Selected,
      page: 1,
      pageSize: 100
    });
  await waitAndUpdate(wrapper);

  expect(wrapper.instance().mounted).toBe(true);
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.instance().renderElement('test1')).toMatchSnapshot();
  expect(wrapper.instance().renderElement('test_foo')).toMatchSnapshot();

  expect(getUserGroups).toHaveBeenCalledWith(
    expect.objectContaining({
      login: user.login,
      organization: undefined,
      p: 1,
      ps: 100,
      q: undefined,
      selected: SelectListFilter.Selected
    })
  );
  expect(wrapper.state().needToReload).toBe(false);

  wrapper.instance().componentWillUnmount();
  expect(wrapper.instance().mounted).toBe(false);
});

it('should handle selection properly', async () => {
  const wrapper = shallowRender();
  wrapper.instance().handleSelect('toto');
  await waitAndUpdate(wrapper);

  expect(addUserToGroup).toHaveBeenCalledWith(
    expect.objectContaining({
      login: user.login,
      name: 'toto'
    })
  );
  expect(wrapper.state().needToReload).toBe(true);
});

it('should handle deselection properly', async () => {
  const wrapper = shallowRender();
  wrapper.instance().handleUnselect('tata');
  await waitAndUpdate(wrapper);

  expect(removeUserFromGroup).toHaveBeenCalledWith(
    expect.objectContaining({
      login: user.login,
      name: 'tata'
    })
  );
  expect(wrapper.state().needToReload).toBe(true);
});

it('should close modal properly', () => {
  const spyOnClose = jest.fn();
  const spyOnUpdateUsers = jest.fn();
  const wrapper = shallowRender({ onClose: spyOnClose, onUpdateUsers: spyOnUpdateUsers });
  click(wrapper.find('.js-modal-close'));

  expect(spyOnClose).toHaveBeenCalled();
  expect(spyOnUpdateUsers).toHaveBeenCalled();
});

function shallowRender(props: Partial<GroupsForm['props']> = {}) {
  return shallow<GroupsForm>(
    <GroupsForm onClose={jest.fn()} onUpdateUsers={jest.fn()} user={user} {...props} />
  );
}
