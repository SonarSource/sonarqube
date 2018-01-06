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
import App from '../App';

const METRICS = {
  lines_to_cover: {
    key: 'lines_to_cover',
    type: 'INT',
    name: 'Lines to Cover',
    domain: 'Coverage'
  },
  coverage: { key: 'coverage', type: 'PERCENT', name: 'Coverage', domain: 'Coverage' },
  duplicated_lines_density: {
    key: 'duplicated_lines_density',
    type: 'PERCENT',
    name: 'Duplicated Lines (%)',
    domain: 'Duplications'
  },
  new_bugs: { key: 'new_bugs', type: 'INT', name: 'New Bugs', domain: 'Reliability' }
};

const PROPS = {
  branch: { isMain: true, name: 'master' },
  component: { key: 'foo' },
  location: { pathname: '/component_measures', query: { metric: 'coverage' } },
  fetchMeasures: () => Promise.resolve({ measures: [] }),
  fetchMetrics: () => {},
  metrics: METRICS,
  metricsKey: ['lines_to_cover', 'coverage', 'duplicated_lines_density', 'new_bugs'],
  router: { push: () => {} }
};

it('should render correctly', () => {
  const wrapper = shallow(<App {...PROPS} />);
  expect(wrapper.find('.spinner')).toHaveLength(1);
  wrapper.setState({ loading: false });
  expect(wrapper).toMatchSnapshot();
});

it('should render a measure overview', () => {
  const wrapper = shallow(
    <App
      {...PROPS}
      location={{ pathname: '/component_measures', query: { metric: 'Reliability' } }}
    />
  );
  expect(wrapper.find('.spinner')).toHaveLength(1);
  wrapper.setState({ loading: false });
  expect(wrapper.find('MeasureOverviewContainer')).toHaveLength(1);
});
