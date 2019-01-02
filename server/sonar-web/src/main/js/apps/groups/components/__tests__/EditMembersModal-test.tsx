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
/* eslint-disable import/first, import/order */
import * as React from 'react';
import { shallow } from 'enzyme';
import EditMembersModal from '../EditMembersModal';
import { waitAndUpdate } from '../../../../helpers/testUtils';

jest.mock('../../../../api/user_groups', () => ({
  getUsersInGroup: jest.fn().mockResolvedValue({
    paging: { pageIndex: 0, pageSize: 10, total: 0 },
    users: [
      {
        login: 'foo',
        name: 'bar',
        selected: true
      }
    ]
  })
}));

const getUsersInGroup = require('../../../../api/user_groups').getUsersInGroup as jest.Mock<any>;

const group = { id: 1, name: 'foo', membersCount: 1 };

it('should render modal', async () => {
  getUsersInGroup.mockClear();

  const wrapper = shallow(<EditMembersModal group={group} onClose={() => {}} organization="bar" />);
  expect(wrapper).toMatchSnapshot();

  await waitAndUpdate(wrapper);
  expect(getUsersInGroup).toHaveBeenCalledTimes(1);
  expect(wrapper).toMatchSnapshot();
});
