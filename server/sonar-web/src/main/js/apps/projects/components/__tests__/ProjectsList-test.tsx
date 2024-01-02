/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { ListRowProps } from 'react-virtualized';
import { AutoSizer } from 'react-virtualized/dist/commonjs/AutoSizer';
import { WindowScroller } from 'react-virtualized/dist/commonjs/WindowScroller';
import ProjectsList from '../ProjectsList';

jest.mock('react-virtualized/dist/commonjs/AutoSizer');
jest.mock('react-virtualized/dist/commonjs/WindowScroller');

it('renders correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper.find(WindowScroller).dive().find(AutoSizer).dive()).toMatchSnapshot(
    'list element'
  );

  expect(
    wrapper.instance().renderRow({ index: 0, key: 'foo-key', style: {} } as ListRowProps)
  ).toMatchSnapshot('row element');
});

it('renders different types of "no projects"', () => {
  expect(shallowRender({ projects: [] })).toMatchSnapshot();
  expect(shallowRender({ projects: [], isFiltered: true })).toMatchSnapshot();
  expect(shallowRender({ projects: [], isFavorite: true })).toMatchSnapshot();
});

function shallowRender(props?: any) {
  return shallow<ProjectsList>(
    <ProjectsList
      cardType="overall"
      currentUser={{ isLoggedIn: true }}
      isFavorite={false}
      isFiltered={false}
      projects={[
        { key: 'foo', name: 'Foo' },
        { key: 'bar', name: 'Bar' },
      ]}
      {...props}
    />
  );
}
