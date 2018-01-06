/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import React from 'react';
import { shallow } from 'enzyme';
import { click, mockEvent } from '../../../../../helpers/testUtils';
import ManageMemberGroupsForm from '../ManageMemberGroupsForm';

jest.mock('react-dom');

const member = { login: 'admin', name: 'Admin Istrator', avatar: '', groupCount: 3 };
const organization = { name: 'MyOrg', key: 'myorg' };
const organizationGroups = [
  {
    id: '7',
    name: 'professionals',
    description: '',
    membersCount: 12
  },
  {
    id: '11',
    name: 'pull-request-analysers',
    description: 'Technical accounts',
    membersCount: 3
  },
  {
    id: '1',
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
      organization={organization}
      organizationGroups={organizationGroups}
      updateMemberGroups={updateFunc}
    />
  );
  const instance = wrapper.instance();
  instance.loadUserGroups = jest.fn(() => {
    instance.setState({ loading: false, userGroups });
  });
  return { wrapper, instance };
}

it('should render and open the modal', () => {
  const wrapper = shallow(
    <ManageMemberGroupsForm
      member={member}
      organization={organization}
      organizationGroups={organizationGroups}
      updateMemberGroups={jest.fn()}
    />
  );
  expect(wrapper).toMatchSnapshot();
  wrapper.setState({ open: true });
  expect(wrapper.first().getElements()).toMatchSnapshot();
});

it('should correctly handle user interactions', () => {
  const form = getMountedForm();
  form.wrapper.find('ActionsDropdownItem').prop('onClick')();
  expect(form.wrapper.state('open')).toBeTruthy();
  expect(form.instance.loadUserGroups).toBeCalled();
  expect(form.wrapper.state()).toMatchSnapshot();
});

it('should correctly select the groups', () => {
  const form = getMountedForm();
  form.instance.openForm(mockEvent);
  expect(form.instance.isGroupSelected(11)).toBeTruthy();
  expect(form.instance.isGroupSelected(7)).toBeFalsy();
  form.instance.onCheck(11, false);
  form.instance.onCheck(7, true);
  expect(form.wrapper.state('userGroups')).toMatchSnapshot();
  expect(form.instance.isGroupSelected(11)).toBeFalsy();
  expect(form.instance.isGroupSelected(7)).toBeTruthy();
});

it('should correctly handle the submit event and close the modal', () => {
  const updateMemberGroups = jest.fn();
  const form = getMountedForm(updateMemberGroups);
  form.instance.openForm(mockEvent);
  form.instance.onCheck(11, false);
  form.instance.onCheck(7, true);
  form.instance.handleSubmit(mockEvent);
  expect(updateMemberGroups.mock.calls).toMatchSnapshot();
  expect(form.wrapper.state()).toMatchSnapshot();
});

it('should reset the selected groups when the modal is opened', () => {
  const form = getMountedForm();
  form.instance.openForm(mockEvent);
  form.instance.onCheck(11, false);
  form.instance.onCheck(7, true);
  expect(form.wrapper.state()).toMatchSnapshot();
  form.instance.closeForm();
  form.instance.openForm(mockEvent);
  expect(form.wrapper.state()).toMatchSnapshot();
});
