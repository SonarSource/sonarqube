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
import Sidebar from '../Sidebar';

it('should display two facets', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should correctly toggle facets', () => {
  const wrapper = shallowRender();
  expect(wrapper.state('openFacets').bugs).toBeUndefined();
  (wrapper.instance() as Sidebar).toggleFacet('bugs');
  expect(wrapper.state('openFacets').bugs).toBeTruthy();
  (wrapper.instance() as Sidebar).toggleFacet('bugs');
  expect(wrapper.state('openFacets').bugs).toBeFalsy();
});

function shallowRender(props = {}) {
  return shallow<Sidebar>(
    <Sidebar
      measures={[
        {
          metric: {
            id: '1',
            key: 'lines_to_cover',
            type: 'INT',
            name: 'Lines to Cover',
            domain: 'Coverage'
          },
          value: '431',
          periods: [{ index: 1, value: '70' }],
          leak: '70'
        },
        {
          metric: {
            id: '2',
            key: 'coverage',
            type: 'PERCENT',
            name: 'Coverage',
            domain: 'Coverage'
          },
          value: '99.3',
          periods: [{ index: 1, value: '0.0999999999999943' }],
          leak: '0.0999999999999943'
        },
        {
          metric: {
            id: '3',
            key: 'duplicated_lines_density',
            type: 'PERCENT',
            name: 'Duplicated Lines (%)',
            domain: 'Duplications'
          },
          value: '3.2',
          periods: [{ index: 1, value: '0.0' }],
          leak: '0.0'
        }
      ]}
      selectedMetric="duplicated_lines_density"
      showFullMeasures={true}
      updateQuery={() => {}}
      {...props}
    />
  );
}
