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
import { mount } from 'enzyme';
import EditMembers from '../EditMembers';
import { click, waitAndUpdate } from '../../../../helpers/testUtils';

it('should edit members', async () => {
  const group = { id: 3, name: 'Foo', membersCount: 5 };
  const onEdit = jest.fn();

  const wrapper = mount(<EditMembers group={group} onEdit={onEdit} organization="org" />);
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('ButtonIcon'));
  expect(wrapper).toMatchSnapshot();

  await waitAndUpdate(wrapper);
  click(wrapper.find('ResetButtonLink'));
  expect(onEdit).toBeCalled();
  expect(wrapper).toMatchSnapshot();
});
