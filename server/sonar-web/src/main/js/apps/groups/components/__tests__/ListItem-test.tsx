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
import * as React from 'react';
import { shallow } from 'enzyme';
import ListItem from '../ListItem';

it('should delete group', () => {
  const group = { id: 3, name: 'Foo', membersCount: 5 };
  const onDelete = jest.fn();
  const wrapper = shallow(
    <ListItem
      group={group}
      onDelete={onDelete}
      onEdit={jest.fn()}
      onEditMembers={jest.fn()}
      organization="org"
    />
  );
  expect(wrapper).toMatchSnapshot();

  wrapper.find('ConfirmButton').prop<Function>('onConfirm')();
  expect(onDelete).toBeCalledWith('Foo');
});

it('should render default group', () => {
  const group = { default: true, id: 3, name: 'Foo', membersCount: 5 };
  const wrapper = shallow(
    <ListItem
      group={group}
      onDelete={jest.fn()}
      onEdit={jest.fn()}
      onEditMembers={jest.fn()}
      organization="org"
    />
  );
  expect(wrapper).toMatchSnapshot();
});
