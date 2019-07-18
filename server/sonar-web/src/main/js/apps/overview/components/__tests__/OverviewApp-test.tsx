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
import { getAllTimeMachineData } from '../../../../api/time-machine';
import {
  mockComponent,
  mockLongLivingBranch,
  mockMainBranch,
  mockMeasure,
  mockMetric
} from '../../../../helpers/testMocks';
import { OverviewApp } from '../OverviewApp';

jest.mock('../../../../api/measures', () => {
  const { mockMeasure, mockMetric } = getMockHelpers();
  return {
    getMeasuresAndMeta: jest.fn().mockResolvedValue({
      component: {
        measures: [mockMeasure({ metric: 'ncloc' }), mockMeasure({ metric: 'coverage' })],
        name: 'foo'
      },
      metrics: [mockMetric({ key: 'ncloc' }), mockMetric()]
    })
  };
});

jest.mock('sonar-ui-common/helpers/dates', () => ({
  parseDate: jest.fn(date => date)
}));

jest.mock('../../../../api/time-machine', () => ({
  getAllTimeMachineData: jest.fn().mockResolvedValue({
    measures: [
      { metric: 'bugs', history: [{ date: '2019-01-05', value: '2.0' }] },
      { metric: 'vulnerabilities', history: [{ date: '2019-01-05', value: '0' }] },
      { metric: 'sqale_index', history: [{ date: '2019-01-01', value: '1.0' }] },
      { metric: 'duplicated_lines_density', history: [{ date: '2019-01-02', value: '1.0' }] },
      { metric: 'ncloc', history: [{ date: '2019-01-03', value: '10000' }] },
      { metric: 'coverage', history: [{ date: '2019-01-04', value: '95.5' }] }
    ]
  })
}));

it('should render correctly', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(getMeasuresAndMeta).toBeCalled();
  expect(getAllTimeMachineData).toBeCalled();
});

it('should show the correct message if the application is empty or has no lines of code', async () => {
  (getMeasuresAndMeta as jest.Mock).mockResolvedValue({
    component: {
      measures: [mockMeasure({ metric: 'projects' })],
      name: 'foo'
    },
    metrics: [mockMetric({ key: 'projects' })]
  });

  const wrapper = shallowRender({
    component: mockComponent({ key: 'foo', name: 'foo', qualifier: 'APP' })
  });
  await waitAndUpdate(wrapper);
  expect(wrapper.find('h3').text()).toBe('portfolio.app.no_lines_of_code');

  (getMeasuresAndMeta as jest.Mock).mockResolvedValue({
    component: {
      measures: [],
      name: 'bar'
    },
    metrics: []
  });
  wrapper.setProps({ component: mockComponent({ key: 'bar', name: 'bar', qualifier: 'APP' }) });
  await waitAndUpdate(wrapper);
  expect(wrapper.find('h3').text()).toBe('portfolio.app.empty');
});

it('should show the correct message if the project is empty', async () => {
  (getMeasuresAndMeta as jest.Mock).mockResolvedValue({
    component: {
      measures: [],
      name: 'foo'
    },
    metrics: []
  });
  const wrapper = shallowRender({ branchLike: mockMainBranch() });

  await waitAndUpdate(wrapper);
  expect(wrapper.find('h3').text()).toBe('overview.project.main_branch_empty');

  wrapper.setProps({ branchLike: mockLongLivingBranch({ name: 'branch-foo' }) });
  await waitAndUpdate(wrapper);
  expect(wrapper.find('h3').text()).toBe('overview.project.branch_X_empty.branch-foo');

  wrapper.setProps({ branchLike: undefined });
  await waitAndUpdate(wrapper);
  expect(wrapper.find('h3').text()).toBe('overview.project.empty');
});

it('should show the correct message if the project has no lines of code', async () => {
  (getMeasuresAndMeta as jest.Mock).mockResolvedValue({
    component: {
      measures: [mockMeasure({ metric: 'bugs' })],
      name: 'foo'
    },
    metrics: [mockMetric({ key: 'bugs' })]
  });
  const wrapper = shallowRender({ branchLike: mockMainBranch() });

  await waitAndUpdate(wrapper);
  expect(wrapper.find('h3').text()).toBe('overview.project.main_branch_no_lines_of_code');

  wrapper.setProps({ branchLike: mockLongLivingBranch({ name: 'branch-foo' }) });
  await waitAndUpdate(wrapper);
  expect(wrapper.find('h3').text()).toBe('overview.project.branch_X_no_lines_of_code.branch-foo');

  wrapper.setProps({ branchLike: undefined });
  await waitAndUpdate(wrapper);
  expect(wrapper.find('h3').text()).toBe('overview.project.no_lines_of_code');
});

function getMockHelpers() {
  // We use this little "force-requiring" instead of an import statement in
  // order to prevent a hoisting race condition while mocking. If we want to use
  // a mock helper in a Jest mock, we have to require it like this. Otherwise,
  // we get errors like:
  //     ReferenceError: testMocks_1 is not defined
  return require.requireActual('../../../../helpers/testMocks');
}

function shallowRender(props: Partial<OverviewApp['props']> = {}) {
  return shallow(
    <OverviewApp
      branchLike={mockMainBranch()}
      component={mockComponent({ name: 'foo' })}
      fetchMetrics={jest.fn()}
      metrics={{ coverage: mockMetric() }}
      onComponentChange={jest.fn()}
      {...props}
    />
  );
}
