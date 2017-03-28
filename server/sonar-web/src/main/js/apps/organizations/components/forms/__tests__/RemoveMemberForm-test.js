/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import { shallow, mount } from 'enzyme';
import { click } from '../../../../../helpers/testUtils';
import RemoveMemberForm from '../RemoveMemberForm';

const member = {};
const removeMember = jest.fn();
const organization = { name: 'MyOrg' };

it('should render and open the modal', () => {
  const wrapper = shallow(
    <RemoveMemberForm member={member} removeMember={removeMember} organization={organization} />
  );
  expect(wrapper).toMatchSnapshot();
  wrapper.setState({ open: true });
  expect(wrapper).toMatchSnapshot();
});

it('should correctly handle user interactions', () => {
  const wrapper = mount(
    <RemoveMemberForm member={member} removeMember={removeMember} organization={organization} />
  );
  click(wrapper.find('a'));
  expect(wrapper.state('open')).toBeTruthy();
});
