/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { mockBranch } from '../../../../helpers/mocks/branch-like';
import { mockQualityGateStatusConditionEnhanced } from '../../../../helpers/mocks/quality-gates';
import { mockMetric } from '../../../../helpers/testMocks';
import { QualityGateStatusConditionEnhanced } from '../../../../types/quality-gates';
import QualityGateCondition from '../QualityGateCondition';

it('open_issues', () => {
  const condition = quickMock('open_issues', 'INT');
  expect(shallowRender({ condition })).toMatchSnapshot();
});

it('new_open_issues', () => {
  const condition = quickMock('new_open_issues', 'INT', true);
  expect(shallowRender({ condition })).toMatchSnapshot();
});

it('reliability_rating', () => {
  const condition = quickMock('reliability_rating');
  expect(shallowRender({ condition })).toMatchSnapshot();
});

it('security_rating', () => {
  const condition = quickMock('security_rating');
  expect(shallowRender({ condition })).toMatchSnapshot();
});

it('sqale_rating', () => {
  const condition = quickMock('sqale_rating');
  expect(shallowRender({ condition })).toMatchSnapshot();
});

it('new_reliability_rating', () => {
  const condition = quickMock('new_reliability_rating', 'RATING', true);
  expect(shallowRender({ condition })).toMatchSnapshot();
});

it('new_security_rating', () => {
  const condition = quickMock('new_security_rating', 'RATING', true);
  expect(shallowRender({ condition })).toMatchSnapshot();
});

it('new_maintainability_rating', () => {
  const condition = quickMock('new_maintainability_rating', 'RATING', true);
  expect(shallowRender({ condition })).toMatchSnapshot();
});

it('should work with branch', () => {
  const condition = quickMock('new_maintainability_rating');
  expect(
    shallow(
      <QualityGateCondition
        branchLike={mockBranch()}
        component={{ key: 'abcd-key' }}
        condition={condition}
      />
    )
  ).toMatchSnapshot();
});

function shallowRender(props = {}) {
  return shallow(
    <QualityGateCondition
      component={{ key: 'abcd-key' }}
      condition={mockQualityGateStatusConditionEnhanced()}
      {...props}
    />
  );
}

function quickMock(
  metric: string,
  type = 'RATING',
  addPeriod = false
): QualityGateStatusConditionEnhanced {
  return mockQualityGateStatusConditionEnhanced({
    error: '1',
    measure: {
      metric: mockMetric({
        key: metric,
        name: metric,
        type
      }),
      value: '3',
      ...(addPeriod ? { periods: [{ value: '3', index: 1 }] } : {})
    },
    metric,
    ...(addPeriod ? { period: 1 } : {})
  });
}
