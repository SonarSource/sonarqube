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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { getMeasuresAndMeta } from '../../../../api/measures';
import {
  mockComponent,
  mockIssue,
  mockLocation,
  mockMainBranch,
  mockPullRequest,
  mockRouter
} from '../../../../helpers/testMocks';
import { App } from '../App';

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

beforeEach(() => {
  (getMeasuresAndMeta as jest.Mock).mockResolvedValue({
    component: { measures: [{ metric: 'coverage', value: '80.0' }] },
    periods: [{ index: '1' }]
  });
});

it('should render correctly', async () => {
  const wrapper = shallowRender();
  expect(wrapper.find('.spinner')).toHaveLength(1);
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should render a measure overview', async () => {
  const wrapper = shallowRender({
    location: mockLocation({ pathname: '/component_measures', query: { metric: 'Reliability' } })
  });
  expect(wrapper.find('.spinner')).toHaveLength(1);
  await waitAndUpdate(wrapper);
  expect(wrapper.find('MeasureOverviewContainer')).toHaveLength(1);
});

it('should render a message when there are no measures', async () => {
  (getMeasuresAndMeta as jest.Mock).mockResolvedValue({
    component: { measures: [] },
    periods: [{ index: '1' }]
  });
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should not render drilldown for estimated duplications', async () => {
  const wrapper = shallowRender({ branchLike: mockPullRequest({ title: '' }) });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should refresh branch status if issues are updated', async () => {
  const fetchBranchStatus = jest.fn();
  const branchLike = mockPullRequest();
  const wrapper = shallowRender({ branchLike, fetchBranchStatus });
  const instance = wrapper.instance();
  await waitAndUpdate(wrapper);

  instance.handleIssueChange(mockIssue());
  expect(fetchBranchStatus).toBeCalledWith(branchLike, 'foo');
});

function shallowRender(props: Partial<App['props']> = {}) {
  return shallow<App>(
    <App
      branchLike={mockMainBranch()}
      component={mockComponent({ key: 'foo', name: 'Foo' })}
      fetchBranchStatus={jest.fn()}
      location={mockLocation({ pathname: '/component_measures', query: { metric: 'coverage' } })}
      params={{}}
      router={mockRouter()}
      routes={[]}
      {...props}
    />
  );
}
