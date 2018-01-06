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
import Sidebar from '../Sidebar';

const MEASURES = [
  {
    metric: {
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
      key: 'duplicated_lines_density',
      type: 'PERCENT',
      name: 'Duplicated Lines (%)',
      domain: 'Duplications'
    },
    value: '3.2',
    periods: [{ index: 1, value: '0.0' }],
    leak: '0.0'
  }
];

const PROPS = {
  measures: MEASURES,
  selectedMetric: 'duplicated_lines_density',
  updateQuery: () => {}
};

it('should display two facets', () => {
  expect(shallow(<Sidebar {...PROPS} />)).toMatchSnapshot();
});

it('should correctly toggle facets', () => {
  const wrapper = shallow(<Sidebar {...PROPS} />);
  expect(wrapper.state('openFacets').bugs).toBeUndefined();
  wrapper.instance().toggleFacet('bugs');
  expect(wrapper.state('openFacets').bugs).toBeTruthy();
  wrapper.instance().toggleFacet('bugs');
  expect(wrapper.state('openFacets').bugs).toBeFalsy();
});
