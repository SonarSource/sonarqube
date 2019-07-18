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
import { click } from 'sonar-ui-common/helpers/testUtils';
import { mockOrganization, mockOrganizationWithAdminActions } from '../../../helpers/testMocks';
import MembersListItem from '../MembersListItem';

it('should not render actions and groups for non admin', () => {
  expect(shallowRender({ organization: mockOrganization() })).toMatchSnapshot();
});

it('should render actions and groups for admin', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should show groups at 0 if the groupCount field is not defined (just added user)', () => {
  expect(
    shallowRender({
      member: { login: 'john', name: 'John Doe', avatar: '7daf6c79d4802916d83f6266e24850af' }
    })
  ).toMatchSnapshot();
});

it('should not display the remove member action', () => {
  expect(shallowRender({ removeMember: undefined }).find('ActionsDropdown')).toMatchSnapshot();
});

it('should open groups form', () => {
  const wrapper = shallowRender();

  click(wrapper.find('ActionsDropdownItem').first());
  expect(wrapper.find('ManageMemberGroupsForm').exists()).toBe(true);

  wrapper.find('ManageMemberGroupsForm').prop<Function>('onClose')();
  wrapper.update();
  expect(wrapper.find('ManageMemberGroupsForm').exists()).toBe(false);
});

it('should open remove member form', () => {
  const wrapper = shallowRender();

  click(wrapper.find('ActionsDropdownItem').last());
  expect(wrapper.find('RemoveMemberForm').exists()).toBe(true);

  wrapper.find('RemoveMemberForm').prop<Function>('onClose')();
  wrapper.update();
  expect(wrapper.find('RemoveMemberForm').exists()).toBe(false);
});

function shallowRender(props: Partial<MembersListItem['props']> = {}) {
  return shallow(
    <MembersListItem
      member={{ login: 'admin', name: 'Admin Istrator', avatar: '', groupCount: 3 }}
      organization={mockOrganizationWithAdminActions()}
      organizationGroups={[]}
      removeMember={jest.fn()}
      updateMemberGroups={jest.fn()}
      {...props}
    />
  );
}
