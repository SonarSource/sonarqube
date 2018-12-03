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
import { Location } from 'history';
import { App } from '../App';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { getMeasuresAndMeta } from '../../../../api/measures';

jest.mock('../../../../api/metrics', () => ({
  getAllMetrics: jest.fn().mockResolvedValue([
    {
      id: '1',
      key: 'lines_to_cover',
      type: 'INT',
      name: 'Lines to Cover',
      domain: 'Coverage'
    },
    {
      id: '2',
      key: 'coverage',
      type: 'PERCENT',
      name: 'Coverage',
      domain: 'Coverage'
    },
    {
      id: '3',
      key: 'duplicated_lines_density',
      type: 'PERCENT',
      name: 'Duplicated Lines (%)',
      domain: 'Duplications'
    },
    {
      id: '4',
      key: 'new_bugs',
      type: 'INT',
      name: 'New Bugs',
      domain: 'Reliability'
    }
  ])
}));

jest.mock('../../../../api/measures', () => ({
  getMeasuresAndMeta: jest.fn()
}));

const COMPONENT = { key: 'foo', name: 'Foo', qualifier: 'TRK' };

const PROPS: App['props'] = {
  branchLike: { isMain: true, name: 'master' },
  component: COMPONENT,
  location: { pathname: '/component_measures', query: { metric: 'coverage' } } as Location,
  params: {},
  router: { push: jest.fn() } as any,
  routes: []
};

beforeEach(() => {
  (getMeasuresAndMeta as jest.Mock).mockResolvedValue({
    component: { measures: [{ metric: 'coverage', value: '80.0' }] },
    periods: [{ index: '1' }]
  });
});

it('should render correctly', async () => {
  const wrapper = shallow(<App {...PROPS} />);
  expect(wrapper.find('.spinner')).toHaveLength(1);
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should render a measure overview', async () => {
  const wrapper = shallow(
    <App
      {...PROPS}
      location={{ pathname: '/component_measures', query: { metric: 'Reliability' } } as Location}
    />
  );
  expect(wrapper.find('.spinner')).toHaveLength(1);
  await waitAndUpdate(wrapper);
  expect(wrapper.find('MeasureOverviewContainer')).toHaveLength(1);
});

it('should render a message when there are no measures', async () => {
  (getMeasuresAndMeta as jest.Mock).mockResolvedValue({
    component: { measures: [] },
    periods: [{ index: '1' }]
  });
  const wrapper = shallow(<App {...PROPS} />);
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should not render drilldown for estimated duplications', async () => {
  const pullRequest = { base: 'master', branch: 'feature-x', key: '5', title: '' };
  const wrapper = shallow(<App {...PROPS} branchLike={pullRequest} />);
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});
