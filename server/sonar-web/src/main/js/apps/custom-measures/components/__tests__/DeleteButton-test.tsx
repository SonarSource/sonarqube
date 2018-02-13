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
import DeleteButton from '../DeleteButton';

it('should delete custom measure', () => {
  const measure = {
    createdAt: '2017-01-01',
    description: 'my custom measure',
    id: '1',
    metric: { key: 'custom', name: 'custom-metric', type: 'STRING' },
    projectKey: 'foo',
    user: { active: true, login: 'user', name: 'user' },
    value: 'custom-value'
  };
  const onDelete = jest.fn();
  const wrapper = shallow(<DeleteButton measure={measure} onDelete={onDelete} />);
  expect(wrapper).toMatchSnapshot();

  wrapper.find('ConfirmButton').prop<Function>('onConfirm')('1');
  expect(onDelete).toBeCalledWith('1');
});
