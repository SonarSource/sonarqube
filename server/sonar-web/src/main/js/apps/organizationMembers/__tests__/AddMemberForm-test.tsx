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
import { click, submit } from '../../../helpers/testUtils';
import AddMemberForm from '../AddMemberForm';
import { searchMembers } from '../../../api/organizations';

jest.mock('../../../api/organizations', () => ({
  searchMembers: jest.fn().mockResolvedValue({ paging: {}, users: [] })
}));

const memberLogins = ['admin'];

it('should render and open the modal', () => {
  const wrapper = shallow(
    <AddMemberForm
      addMember={jest.fn()}
      memberLogins={memberLogins}
      organization={{ key: 'foo', name: 'Foo' }}
    />
  );
  expect(wrapper).toMatchSnapshot();
  wrapper.setState({ open: true });
  expect(wrapper).toMatchSnapshot();
});

it('should correctly handle user interactions', () => {
  const wrapper = shallow(
    <AddMemberForm
      addMember={jest.fn()}
      memberLogins={memberLogins}
      organization={{ key: 'foo', name: 'Foo' }}
    />
  );
  click(wrapper.find('Button'));
  expect(wrapper.state('open')).toBeTruthy();
  (wrapper.instance() as AddMemberForm).closeForm();
  expect(wrapper.state('open')).toBeFalsy();
});

it('should search users', () => {
  const wrapper = shallow(
    <AddMemberForm
      addMember={jest.fn()}
      memberLogins={memberLogins}
      organization={{ key: 'foo', name: 'Foo' }}
    />
  );
  click(wrapper.find('Button'));

  wrapper.find('UsersSelectSearch').prop<Function>('searchUsers')('foo', 100);
  expect(searchMembers).lastCalledWith({
    organization: 'foo',
    ps: 100,
    q: 'foo',
    selected: 'deselected'
  });

  wrapper.find('UsersSelectSearch').prop<Function>('searchUsers')('', 100);
  expect(searchMembers).lastCalledWith({
    organization: 'foo',
    ps: 100,
    selected: 'deselected'
  });
});

it('should select user', () => {
  const addMember = jest.fn();
  const user = { login: 'luke', name: 'Luke' };
  const wrapper = shallow(
    <AddMemberForm
      addMember={addMember}
      memberLogins={memberLogins}
      organization={{ key: 'foo', name: 'Foo' }}
    />
  );
  click(wrapper.find('Button'));

  wrapper.find('UsersSelectSearch').prop<Function>('handleValueChange')(user);
  submit(wrapper.find('form'));
  expect(addMember).toBeCalledWith(user);
});
