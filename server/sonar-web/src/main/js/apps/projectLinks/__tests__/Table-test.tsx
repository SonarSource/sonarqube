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
import Table from '../Table';

it('should render', () => {
  const links = [
    { id: '1', type: 'homepage', url: 'http://example.com/homepage' },
    { id: '2', type: 'issue', url: 'http://example.com/issue' },
    { id: '3', name: 'foo', type: 'foo', url: 'http://example.com/foo' },
    { id: '4', name: 'bar', type: 'bar', url: 'http://example.com/bar' }
  ];
  expect(shallow(<Table links={links} onDelete={jest.fn()} />)).toMatchSnapshot();
});

it('should render empty', () => {
  expect(shallow(<Table links={[]} onDelete={jest.fn()} />)).toMatchSnapshot();
});
