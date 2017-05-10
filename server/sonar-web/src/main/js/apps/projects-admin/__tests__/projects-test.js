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
import { shallow } from 'enzyme';
import Projects from '../projects';
import Checkbox from '../../../components/controls/Checkbox';

it('should render list of projects with no selection', () => {
  const projects = [
    { id: '1', key: 'a', name: 'A', qualifier: 'TRK' },
    { id: '2', key: 'b', name: 'B', qualifier: 'TRK' }
  ];

  const result = shallow(
    <Projects
      organization={{ key: 'foo' }}
      projects={projects}
      selection={[]}
      refresh={jest.fn()}
    />
  );
  expect(result.find('tr').length).toBe(2);
  expect(result.find(Checkbox).filterWhere(n => n.prop('checked')).length).toBe(0);
});

it('should render list of projects with one selected', () => {
  const projects = [
    { id: '1', key: 'a', name: 'A', qualifier: 'TRK' },
    { id: '2', key: 'b', name: 'B', qualifier: 'TRK' }
  ];
  const selection = ['a'];

  const result = shallow(
    <Projects
      organization={{ key: 'foo' }}
      projects={projects}
      selection={selection}
      refresh={jest.fn()}
    />
  );
  expect(result.find('tr').length).toBe(2);
  expect(result.find(Checkbox).filterWhere(n => n.prop('checked')).length).toBe(1);
});
