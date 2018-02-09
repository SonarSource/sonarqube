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
import List from '../List';

it('should render', () => {
  const metrics = [
    { id: '3', key: 'foo', name: 'Foo', type: 'INT' },
    { id: '4', domain: 'Coverage', key: 'bar', name: 'Bar', type: 'INT' }
  ];
  expect(
    shallow(<List metrics={metrics} onDelete={jest.fn()} onEdit={jest.fn()} />)
  ).toMatchSnapshot();
});

it('should render no results', () => {
  expect(shallow(<List metrics={[]} onDelete={jest.fn()} onEdit={jest.fn()} />)).toMatchSnapshot();
});
