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
import { keyBy } from 'lodash';
import * as React from 'react';
import { getComponentLeaves } from '../../../../api/components';
import { mockBranch } from '../../../../helpers/mocks/branch-like';
import {
  mockComponentMeasure,
  mockComponentMeasureEnhanced,
} from '../../../../helpers/mocks/component';
import {
  mockMeasure,
  mockMeasureEnhanced,
  mockMetric,
  mockPeriod,
} from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { MetricKey } from '../../../../types/metrics';
import { BUBBLES_FETCH_LIMIT } from '../../utils';
import MeasureOverview from '../MeasureOverview';

jest.mock('../../../../api/components', () => ({
  getComponentLeaves: jest
    .fn()
    .mockResolvedValue({ components: [], paging: { total: 200, pageIndex: 1, pageSize: 100 } }),
}));

beforeEach(jest.clearAllMocks);

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ loading: true })).toMatchSnapshot('loading');
  expect(shallowRender({ leakPeriod: mockPeriod(), branchLike: mockBranch() })).toMatchSnapshot(
    'has leak period'
  );
  expect(shallowRender({ component: mockComponentMeasure(true) })).toMatchSnapshot('is file');
});

it('should correctly enhance leaf components', async () => {
  (getComponentLeaves as jest.Mock).mockResolvedValueOnce({
    components: [
      mockComponentMeasure(false, { measures: [mockMeasure({ metric: MetricKey.bugs })] }),
    ],
  });
  const updateLoading = jest.fn();
  const wrapper = shallowRender({ updateLoading });

  expect(updateLoading).toHaveBeenCalledWith({ bubbles: true });
  expect(getComponentLeaves).toHaveBeenCalledWith(
    'foo',
    [
      MetricKey.ncloc,
      MetricKey.reliability_remediation_effort,
      MetricKey.bugs,
      MetricKey.reliability_rating,
    ],
    expect.objectContaining({ metricSort: MetricKey.bugs, s: 'metric', ps: BUBBLES_FETCH_LIMIT })
  );

  await waitAndUpdate(wrapper);

  expect(wrapper.state().components).toEqual([
    mockComponentMeasureEnhanced({
      measures: [
        mockMeasureEnhanced({
          leak: '1.0',
          metric: mockMetric({ key: MetricKey.bugs, type: 'INT' }),
        }),
      ],
    }),
  ]);
  expect(updateLoading).toHaveBeenLastCalledWith({ bubbles: false });
});

it('should not enhance file components', () => {
  shallowRender({ component: mockComponentMeasure(true) });
  expect(getComponentLeaves).not.toHaveBeenCalled();
});

it('should correctly flag itself as (un)mounted', () => {
  const wrapper = shallowRender();
  const instance = wrapper.instance();
  expect(instance.mounted).toBe(true);
  wrapper.unmount();
  expect(instance.mounted).toBe(false);
});

function shallowRender(props: Partial<MeasureOverview['props']> = {}) {
  return shallow<MeasureOverview>(
    <MeasureOverview
      component={mockComponentMeasure()}
      domain="Reliability"
      loading={false}
      metrics={keyBy(
        [
          mockMetric({ key: MetricKey.ncloc, type: 'INT' }),
          mockMetric({
            key: MetricKey.reliability_remediation_effort,
            type: 'INT',
          }),
          mockMetric({ key: MetricKey.bugs, type: 'INT' }),
          mockMetric({
            key: MetricKey.reliability_rating,
            type: 'DATA',
          }),
        ],
        (m) => m.key
      )}
      updateLoading={jest.fn()}
      updateSelected={jest.fn()}
      rootComponent={mockComponentMeasure()}
      {...props}
    />
  );
}
