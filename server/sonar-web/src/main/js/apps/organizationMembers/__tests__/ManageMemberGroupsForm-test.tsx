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
import { mockEvent } from '../../../helpers/testUtils';
import ManageMemberGroupsForm from '../ManageMemberGroupsForm';

const member = { login: 'admin', name: 'Admin Istrator', avatar: '', groupCount: 3 };
const organization = { name: 'MyOrg', key: 'myorg' };
const organizationGroups = [
  {
    id: 7,
    name: 'professionals',
    description: '',
    membersCount: 12
  },
  {
    id: 11,
    name: 'pull-request-analysers',
    description: 'Technical accounts',
    membersCount: 3
  },
  {
    id: 1,
    name: 'sonar-administrators',
    description: 'System administrators',
    membersCount: 17
  }
];
const userGroups = {
  11: { id: 11, name: 'pull-request-analysers', description: 'Technical accounts', selected: true }
};

function getMountedForm(updateFunc = jest.fn()) {
  const wrapper = shallow(
    <ManageMemberGroupsForm
      member={member}
      onClose={jest.fn()}
      organization={organization}
      organizationGroups={organizationGroups}
      updateMemberGroups={updateFunc}
    />,
    { disableLifecycleMethods: true }
  );
  const instance = wrapper.instance();
  wrapper.setState({ loading: false, userGroups });
  return { wrapper, instance };
}

it('should render', () => {
  const wrapper = shallow(
    <ManageMemberGroupsForm
      member={member}
      onClose={jest.fn()}
      organization={organization}
      organizationGroups={organizationGroups}
      updateMemberGroups={jest.fn()}
    />
  );
  expect(wrapper).toMatchSnapshot();
});

it('should correctly select the groups', () => {
  const form = getMountedForm();
  const instance = form.instance as ManageMemberGroupsForm;
  expect(instance.isGroupSelected('11')).toBeTruthy();
  expect(instance.isGroupSelected('7')).toBeFalsy();
  instance.onCheck('11', false);
  instance.onCheck('7', true);
  expect(form.wrapper.state('userGroups')).toMatchSnapshot();
  expect(instance.isGroupSelected('11')).toBeFalsy();
  expect(instance.isGroupSelected('7')).toBeTruthy();
});

it('should correctly handle the submit event and close the modal', () => {
  const updateMemberGroups = jest.fn();
  const form = getMountedForm(updateMemberGroups);
  const instance = form.instance as ManageMemberGroupsForm;
  instance.onCheck('11', false);
  instance.onCheck('7', true);
  instance.handleSubmit(mockEvent as any);
  expect(updateMemberGroups.mock.calls).toMatchSnapshot();
  expect(form.wrapper.state()).toMatchSnapshot();
});
