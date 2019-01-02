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
import MembersListItem from '../MembersListItem';
import { click } from '../../../helpers/testUtils';

const organization = { key: 'foo', name: 'Foo' };
const admin = { login: 'admin', name: 'Admin Istrator', avatar: '', groupCount: 3 };
const john = { login: 'john', name: 'John Doe', avatar: '7daf6c79d4802916d83f6266e24850af' };

it('should not render actions and groups for non admin', () => {
  const wrapper = shallow(
    <MembersListItem
      member={admin}
      organization={organization}
      organizationGroups={[]}
      removeMember={jest.fn()}
      updateMemberGroups={jest.fn()}
    />
  );
  expect(wrapper).toMatchSnapshot();
});

it('should render actions and groups for admin', () => {
  const wrapper = shallow(
    <MembersListItem
      member={admin}
      organization={{ ...organization, actions: { admin: true } }}
      organizationGroups={[]}
      removeMember={jest.fn()}
      updateMemberGroups={jest.fn()}
    />
  );
  expect(wrapper).toMatchSnapshot();
});

it('should groups at 0 if the groupCount field is not defined (just added user)', () => {
  const wrapper = shallow(
    <MembersListItem
      member={john}
      organization={{ ...organization, actions: { admin: true } }}
      organizationGroups={[]}
      removeMember={jest.fn()}
      updateMemberGroups={jest.fn()}
    />
  );
  expect(wrapper).toMatchSnapshot();
});

it('should open groups form', () => {
  const wrapper = shallow(
    <MembersListItem
      member={admin}
      organization={{ ...organization, actions: { admin: true } }}
      organizationGroups={[]}
      removeMember={jest.fn()}
      updateMemberGroups={jest.fn()}
    />
  );

  click(wrapper.find('ActionsDropdownItem').first());
  expect(wrapper.find('ManageMemberGroupsForm').exists()).toBe(true);

  wrapper.find('ManageMemberGroupsForm').prop<Function>('onClose')();
  wrapper.update();
  expect(wrapper.find('ManageMemberGroupsForm').exists()).toBe(false);
});

it('should open remove member form', () => {
  const wrapper = shallow(
    <MembersListItem
      member={admin}
      organization={{ ...organization, actions: { admin: true } }}
      organizationGroups={[]}
      removeMember={jest.fn()}
      updateMemberGroups={jest.fn()}
    />
  );

  click(wrapper.find('ActionsDropdownItem').last());
  expect(wrapper.find('RemoveMemberForm').exists()).toBe(true);

  wrapper.find('RemoveMemberForm').prop<Function>('onClose')();
  wrapper.update();
  expect(wrapper.find('RemoveMemberForm').exists()).toBe(false);
});
