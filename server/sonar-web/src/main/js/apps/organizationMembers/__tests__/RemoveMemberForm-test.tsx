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
import RemoveMemberForm from '../RemoveMemberForm';
import { mockEvent } from '../../../helpers/testMocks';

const member = { login: 'admin', name: 'Admin Istrator', avatar: '', groupCount: 3 };
const organization = { key: 'myorg', name: 'MyOrg' };

it('should render ', () => {
  const wrapper = shallow(
    <RemoveMemberForm
      member={member}
      onClose={jest.fn()}
      organization={organization}
      removeMember={jest.fn()}
    />
  );
  expect(wrapper).toMatchSnapshot();
});

it('should correctly handle user interactions', () => {
  const removeMember = jest.fn();
  const wrapper = shallow<RemoveMemberForm>(
    <RemoveMemberForm
      member={member}
      onClose={jest.fn()}
      organization={organization}
      removeMember={removeMember}
    />
  );
  wrapper.instance().handleSubmit(mockEvent());
  expect(removeMember).toBeCalledWith({
    avatar: '',
    groupCount: 3,
    login: 'admin',
    name: 'Admin Istrator'
  });
});
