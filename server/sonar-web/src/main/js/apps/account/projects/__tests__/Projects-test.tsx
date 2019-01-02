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
import { shallow } from 'enzyme';
import Projects from '../Projects';
import ProjectCard from '../ProjectCard';
import ListFooter from '../../../../components/controls/ListFooter';

const PROJECTS = [
  { key: 'key1', links: [], name: 'name1' },
  { key: 'key2', links: [], name: 'name2' }
];
it('should render list of ProjectCards', () => {
  const output = shallow(
    <Projects loadMore={() => true} loading={false} projects={PROJECTS} total={5} />
  );

  expect(output.find(ProjectCard).length).toBe(2);
});

it('should render ListFooter', () => {
  const loadMore = jest.fn();

  const footer = shallow(
    <Projects loadMore={loadMore} loading={false} projects={PROJECTS} total={5} />
  ).find(ListFooter);

  expect(footer.length).toBe(1);
  expect(footer.prop('count')).toBe(2);
  expect(footer.prop('total')).toBe(5);
  expect(footer.prop('loadMore')).toBe(loadMore);
});

it('should render when no results', () => {
  const output = shallow(
    <Projects loadMore={() => true} loading={false} projects={[]} total={0} />
  );

  expect(output.find('.js-no-results').length).toBe(1);
  expect(output.find(ProjectCard).length).toBe(0);
  expect(output.find(ListFooter).length).toBe(0);
});
