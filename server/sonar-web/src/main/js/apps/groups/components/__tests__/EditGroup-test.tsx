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
import EditGroup from '../EditGroup';

it('should edit group', () => {
  const group = { id: 3, name: 'Foo', membersCount: 5 };
  const onEdit = jest.fn();
  const newDescription = 'bla bla';
  let onClick: any;

  const wrapper = shallow(
    <EditGroup group={group} onEdit={onEdit}>
      {props => {
        ({ onClick } = props);
        return <button />;
      }}
    </EditGroup>
  );
  expect(wrapper).toMatchSnapshot();

  onClick();
  wrapper.update();
  expect(wrapper).toMatchSnapshot();

  // change name
  wrapper.find('Form').prop<Function>('onSubmit')({ name: 'Bar', description: newDescription });
  expect(onEdit).lastCalledWith({ description: newDescription, id: 3, name: 'Bar' });

  // change description
  wrapper.find('Form').prop<Function>('onSubmit')({
    name: group.name,
    description: newDescription
  });
  expect(onEdit).lastCalledWith({ description: newDescription, id: group.id });

  wrapper.find('Form').prop<Function>('onClose')();
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});
