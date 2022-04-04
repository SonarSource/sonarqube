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
import { ComponentQualifier } from '../../../../types/component';
import SimpleBubbleChart from '../SimpleBubbleChart';

it('renders', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should handle filtering', () => {
  const wrapper = shallowRender();

  wrapper.instance().handleRatingFilterClick(2);

  expect(wrapper.state().ratingFilters).toEqual({ 2: true });
});

function shallowRender(overrides: Partial<SimpleBubbleChart['props']> = {}) {
  const project1 = mockProject({
    measures: { complexity: '17.2', coverage: '53.5', ncloc: '1734', security_rating: '2' }
  });
  const app = mockProject({
    key: 'app',
    measures: { complexity: '23.1', coverage: '87.3', ncloc: '32478', security_rating: '1' },
    name: 'App',
    qualifier: ComponentQualifier.Application
  });
  return shallow<SimpleBubbleChart>(
    <SimpleBubbleChart
      colorMetric="security_rating"
      helpText="foobar"
      projects={[app, project1]}
      sizeMetric={{ key: 'ncloc', type: 'INT' }}
      xMetric={{ key: 'complexity', type: 'INT' }}
      yMetric={{ key: 'coverage', type: 'PERCENT' }}
      {...overrides}
    />
  );
}
