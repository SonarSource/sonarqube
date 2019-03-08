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
import { ReviewApp } from '../ReviewApp';
import { getMeasures } from '../../../../api/measures';
import { getQualityGateProjectStatus } from '../../../../api/quality-gates';
import { mockComponent, mockPullRequest } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';

jest.mock('../../../../api/measures', () => {
  const { mockMeasure } = getMockHelpers();
  return {
    getMeasures: jest
      .fn()
      .mockResolvedValue([
        mockMeasure({ metric: 'new_bugs ' }),
        mockMeasure({ metric: 'new_vulnerabilities' }),
        mockMeasure({ metric: 'new_code_smells' })
      ])
  };
});

jest.mock('../../../../api/quality-gates', () => ({
  getQualityGateProjectStatus: jest.fn()
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly for a passed QG', async () => {
  const { mockQualityGateProjectStatus } = getMockHelpers();
  (getQualityGateProjectStatus as jest.Mock).mockResolvedValue(mockQualityGateProjectStatus());
  const registerBranchStatus = jest.fn();
  const wrapper = shallowRender({ registerBranchStatus });

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  expect(wrapper.find('QualityGateConditions').exists()).toBe(false);

  expect(getMeasures).toBeCalled();
  expect(getQualityGateProjectStatus).toBeCalled();
  expect(registerBranchStatus).toBeCalled();
});

it('should render correctly for a failed QG', async () => {
  const { mockQualityGateProjectStatus } = getMockHelpers();
  (getQualityGateProjectStatus as jest.Mock).mockResolvedValue(
    mockQualityGateProjectStatus({
      status: 'ERROR',
      conditions: [
        {
          status: 'OK',
          metricKey: 'new_bugs',
          comparator: 'GT',
          periodIndex: 1,
          errorThreshold: '1.0',
          actualValue: '0'
        },
        {
          status: 'ERROR',
          metricKey: 'new_code_smells',
          comparator: 'GT',
          periodIndex: 1,
          errorThreshold: '1.0',
          actualValue: '10'
        }
      ]
    })
  );

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  expect(wrapper.find('QualityGateConditions').exists()).toBe(true);
});

it('should correctly refresh data if certain props change', () => {
  const wrapper = shallowRender();

  jest.clearAllMocks();
  wrapper.setProps({
    component: mockComponent({ key: 'foo' })
  });
  expect(getMeasures).toBeCalled();
  expect(getQualityGateProjectStatus).toBeCalled();

  jest.clearAllMocks();
  wrapper.setProps({
    branchLike: mockPullRequest({ key: '1002' })
  });
  expect(getMeasures).toBeCalled();
  expect(getQualityGateProjectStatus).toBeCalled();
});

it('should correctly handle a WS failure', async () => {
  (getMeasures as jest.Mock).mockRejectedValue({});
  (getQualityGateProjectStatus as jest.Mock).mockRejectedValue({});
  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

function getMockHelpers() {
  // We use this little "force-requiring" instead of an import statement in
  // order to prevent a hoisting race condition while mocking. If we want to use
  // a mock helper in a Jest mock, we have to require it like this. Otherwise,
  // we get errors like:
  //     ReferenceError: testMocks_1 is not defined
  return require.requireActual('../../../../helpers/testMocks');
}

function shallowRender(props: Partial<ReviewApp['props']> = {}) {
  return shallow(
    <ReviewApp
      branchLike={mockPullRequest()}
      component={mockComponent()}
      registerBranchStatus={jest.fn()}
      {...props}
    />
  );
}
