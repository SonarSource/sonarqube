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
import { searchMembers } from '../../../../api/organizations';
import { searchUsers } from '../../../../api/users';
import { isSonarCloud } from '../../../../helpers/system';
import { mockLoggedInUser, mockUser } from '../../../../helpers/testMocks';
import { SetAssigneePopup } from '../SetAssigneePopup';

jest.mock('../../../../helpers/system', () => ({
  isSonarCloud: jest.fn().mockReturnValue(false)
}));

jest.mock('../../../../api/organizations', () => {
  const { mockUser } = jest.requireActual('../../../../helpers/testMocks');
  return {
    searchMembers: jest.fn().mockResolvedValue({
      users: [mockUser(), mockUser({ active: false, login: 'foo', name: undefined })]
    })
  };
});

jest.mock('../../../../api/users', () => {
  const { mockUser } = jest.requireActual('../../../../helpers/testMocks');
  return { searchUsers: jest.fn().mockResolvedValue({ users: [mockUser()] }) };
});

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should allow to search for a user on SQ', async () => {
  const wrapper = shallowRender();
  wrapper.find('SearchBox').prop<Function>('onChange')('o');
  await waitAndUpdate(wrapper);
  expect(searchUsers).toBeCalledWith({ q: 'o', ps: 10 });
  expect(wrapper.state('users')).toEqual([mockUser()]);
});

it('should allow to search for a user on SC', async () => {
  (isSonarCloud as jest.Mock).mockReturnValueOnce(true);
  const wrapper = shallowRender();
  wrapper.find('SearchBox').prop<Function>('onChange')('o');
  await waitAndUpdate(wrapper);
  expect(searchMembers).toBeCalledWith({ organization: 'foo', q: 'o', ps: 10 });
  expect(wrapper.state('users')).toEqual([mockUser()]);
});

function shallowRender(props: Partial<SetAssigneePopup['props']> = {}) {
  return shallow(
    <SetAssigneePopup
      currentUser={mockLoggedInUser()}
      issue={{ projectOrganization: 'foo' }}
      onSelect={jest.fn()}
      {...props}
    />
  );
}
