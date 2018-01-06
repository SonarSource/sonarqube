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
import React from 'react';
import { shallow } from 'enzyme';
import { UnconnectedProjects } from '../Projects';

const projects = [{ key: 'foo', name: 'Foo' }, { key: 'bar', name: 'Bar' }];

const newProject = { key: 'qux', name: 'Qux' };

it('should render projects', () => {
  const wrapper = shallow(<UnconnectedProjects projects={projects} />);
  expect(wrapper).toMatchSnapshot();

  // let's add a new project
  wrapper.setState({ addedProjects: [newProject] });
  expect(wrapper).toMatchSnapshot();

  // let's say we saved it, so it's passed back in `props`
  wrapper.setProps({ projects: [...projects, newProject] });
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.state()).toMatchSnapshot();
});
