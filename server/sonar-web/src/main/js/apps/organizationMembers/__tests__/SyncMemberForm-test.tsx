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
import { SyncMemberForm } from '../SyncMemberForm';
import { setOrganizationMemberSync, syncMembers } from '../../../api/organizations';
import { mockOrganizationWithAlm } from '../../../helpers/testMocks';
import { waitAndUpdate } from '../../../helpers/testUtils';

jest.mock('../../../api/organizations', () => ({
  setOrganizationMemberSync: jest.fn().mockResolvedValue(undefined),
  syncMembers: jest.fn().mockResolvedValue(undefined)
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should allow to switch to automatic mode', async () => {
  const fetchOrganization = jest.fn();
  const refreshMembers = jest.fn().mockResolvedValue({});
  const wrapper = shallowRender({ fetchOrganization, refreshMembers });
  expect(wrapper).toMatchSnapshot();

  wrapper.setState({ membersSync: true });
  wrapper.find('ConfirmButton').prop<Function>('onConfirm')();
  expect(setOrganizationMemberSync).toHaveBeenCalledWith({ organization: 'foo', enabled: true });

  await waitAndUpdate(wrapper);
  expect(fetchOrganization).toHaveBeenCalledWith('foo');
  expect(syncMembers).toHaveBeenCalledWith('foo');
  expect(refreshMembers).toBeCalled();
});

it('should allow to switch to manual mode', async () => {
  const fetchOrganization = jest.fn();
  const wrapper = shallowRender({
    fetchOrganization,
    organization: mockOrganizationWithAlm({}, { membersSync: true })
  });
  expect(wrapper).toMatchSnapshot();

  wrapper.setState({ membersSync: false });
  wrapper.find('ConfirmButton').prop<Function>('onConfirm')();
  expect(setOrganizationMemberSync).toHaveBeenCalledWith({ organization: 'foo', enabled: false });

  await waitAndUpdate(wrapper);
  expect(fetchOrganization).toHaveBeenCalledWith('foo');
  expect(syncMembers).not.toHaveBeenCalled();
});

function shallowRender(props: Partial<SyncMemberForm['props']> = {}) {
  return shallow<SyncMemberForm>(
    <SyncMemberForm
      buttonText="configure"
      fetchOrganization={jest.fn()}
      hasOtherMembers={true}
      organization={mockOrganizationWithAlm()}
      refreshMembers={jest.fn().mockResolvedValue({})}
      {...props}
    />
  );
}
