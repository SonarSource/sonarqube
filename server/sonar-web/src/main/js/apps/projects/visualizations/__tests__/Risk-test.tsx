/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { mockProject } from '../../../../helpers/mocks/projects';
import Risk from '../Risk';

it('renders', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should handle filtering', () => {
  const wrapper = shallowRender();

  wrapper.instance().handleRatingFilterClick(2);

  expect(wrapper.state().ratingFilters).toEqual({ 2: true });
});

function shallowRender(overrides: Partial<Risk['props']> = {}) {
  const project1 = mockProject({
    key: 'foo',
    measures: {
      complexity: '17.2',
      coverage: '53.5',
      ncloc: '1734',
      sqale_index: '1',
      reliability_rating: '3',
      security_rating: '2'
    },
    name: 'Foo'
  });
  const project2 = mockProject({
    key: 'bar',
    name: 'Bar',
    measures: {}
  });
  return shallow<Risk>(<Risk helpText="foobar" projects={[project1, project2]} {...overrides} />);
}
