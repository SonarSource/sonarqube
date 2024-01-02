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
import { getMeasuresWithMetrics } from '../../../../api/measures';
import { mockPullRequest } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockQualityGateStatusCondition } from '../../../../helpers/mocks/quality-gates';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { PR_METRICS } from '../../utils';
import { PullRequestOverview } from '../PullRequestOverview';

jest.mock('../../../../api/measures', () => {
  const { mockMeasure, mockMetric } = jest.requireActual('../../../../helpers/testMocks');
  return {
    getMeasuresWithMetrics: jest.fn().mockResolvedValue({
      component: {
        measures: [
          mockMeasure({ metric: 'new_bugs' }),
          mockMeasure({ metric: 'new_vulnerabilities' }),
          mockMeasure({ metric: 'new_code_smells' }),
          mockMeasure({ metric: 'new_security_hotspots' }),
        ],
      },
      metrics: [
        mockMetric({ key: 'new_bugs', name: 'new_bugs', id: 'new_bugs' }),
        mockMetric({
          key: 'new_vulnerabilities',
          name: 'new_vulnerabilities',
          id: 'new_vulnerabilities',
        }),
        mockMetric({ key: 'new_code_smells', name: 'new_code_smells', id: 'new_code_smells' }),
        mockMetric({
          key: 'new_security_hotspots',
          name: 'new_security_hotspots',
          id: 'new_security_hotspots',
        }),
      ],
    }),
  };
});

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly for a passed QG', async () => {
  const fetchBranchStatus = jest.fn();

  const wrapper = shallowRender({ fetchBranchStatus, status: 'OK' });

  wrapper.setProps({ conditions: [] });

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  expect(wrapper.find('QualityGateConditions').exists()).toBe(false);

  expect(getMeasuresWithMetrics).toHaveBeenCalled();
  expect(fetchBranchStatus).toHaveBeenCalled();
});

it('should render correctly if conditions are ignored', async () => {
  const wrapper = shallowRender({ conditions: [], ignoredConditions: true });
  await waitAndUpdate(wrapper);
  expect(wrapper.find('Alert').exists()).toBe(true);
});

it('should render correctly for a failed QG', async () => {
  const wrapper = shallowRender({
    status: 'ERROR',
    conditions: [
      mockQualityGateStatusCondition({
        error: '1.0',
        level: 'OK',
        metric: 'new_bugs',
        period: 1,
      }),
      mockQualityGateStatusCondition({
        error: '1.0',
        metric: 'new_code_smells',
        period: 1,
      }),
    ],
  });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should correctly fetch all required metrics for a passing QG', async () => {
  const wrapper = shallowRender({ conditions: [] });
  await waitAndUpdate(wrapper);
  expect(getMeasuresWithMetrics).toHaveBeenCalledWith('my-project', PR_METRICS, expect.any(Object));
});

it('should correctly fetch all required metrics for a failing QG', async () => {
  const wrapper = shallowRender({
    conditions: [mockQualityGateStatusCondition({ level: 'ERROR', metric: 'foo' })],
  });
  await waitAndUpdate(wrapper);
  expect(getMeasuresWithMetrics).toHaveBeenCalledWith(
    'my-project',
    [...PR_METRICS, 'foo'],
    expect.any(Object)
  );
});

function shallowRender(props: Partial<PullRequestOverview['props']> = {}) {
  return shallow(
    <PullRequestOverview
      branchLike={mockPullRequest()}
      component={mockComponent()}
      fetchBranchStatus={jest.fn()}
      {...props}
    />
  );
}
