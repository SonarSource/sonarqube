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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { addUserToGroup, getUsersInGroup, removeUserFromGroup } from '../../../../api/user_groups';
import EditMembersModal from '../EditMembersModal';

const organization = 'orga';
const group = { id: 1, name: 'foo', membersCount: 1 };

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
  expect(wrapper.state().needToReload).toBe(false);

  expect(wrapper.instance().mounted).toBe(true);
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.instance().renderElement('test1')).toMatchSnapshot();
  expect(wrapper.instance().renderElement('test_foo')).toMatchSnapshot();

  expect(getUsersInGroup).toHaveBeenCalledWith(
    expect.objectContaining({
      name: group.name,
      organization,
      p: 1,
      ps: 100,
      q: undefined,
      selected: SelectListFilter.Selected
    })
  );

  wrapper.instance().componentWillUnmount();
  expect(wrapper.instance().mounted).toBe(false);
});

it('should handle selection properly', async () => {
  const wrapper = shallowRender();
  wrapper.instance().handleSelect('toto');
  await waitAndUpdate(wrapper);

  expect(addUserToGroup).toHaveBeenCalledWith(
    expect.objectContaining({
      name: group.name,
      organization,
      login: 'toto'
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
      name: group.name,
      organization,
      login: 'tata'
    })
  );
  expect(wrapper.state().needToReload).toBe(true);
});

function shallowRender(props: Partial<EditMembersModal['props']> = {}) {
  return shallow<EditMembersModal>(
    <EditMembersModal group={group} onClose={jest.fn()} organization={organization} {...props} />
  );
}
