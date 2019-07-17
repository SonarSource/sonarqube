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
import { getMeasures } from '../../../../api/measures';
import {
  mockComponent,
  mockPullRequest,
  mockQualityGateStatusCondition
} from '../../../../helpers/testMocks';
import { ReviewApp } from '../ReviewApp';

jest.mock('../../../../api/measures', () => {
  const { mockMeasure } = require.requireActual('../../../../helpers/testMocks');
  return {
    getMeasures: jest
      .fn()
      .mockResolvedValue([
        mockMeasure({ metric: 'new_bugs ' }),
        mockMeasure({ metric: 'new_vulnerabilities' }),
        mockMeasure({ metric: 'new_code_smells' }),
        mockMeasure({ metric: 'new_security_hotspots' })
      ])
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

  expect(getMeasures).toBeCalled();
  expect(fetchBranchStatus).toBeCalled();
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
        period: 1
      }),
      mockQualityGateStatusCondition({
        error: '1.0',
        metric: 'new_code_smells',
        period: 1
      })
    ]
  });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  expect(wrapper.find('QualityGateConditions').exists()).toBe(true);
});

it('should correctly handle a WS failure', async () => {
  (getMeasures as jest.Mock).mockRejectedValue({});
  const fetchBranchStatus = jest.fn().mockRejectedValue({});
  const wrapper = shallowRender({ fetchBranchStatus });

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<ReviewApp['props']> = {}) {
  return shallow(
    <ReviewApp
      branchLike={mockPullRequest()}
      component={mockComponent()}
      fetchBranchStatus={jest.fn()}
      {...props}
    />
  );
}
