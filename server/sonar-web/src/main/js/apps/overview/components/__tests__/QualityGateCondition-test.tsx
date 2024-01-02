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
import { screen } from '@testing-library/react';
import * as React from 'react';
import { mockBranch } from '../../../../helpers/mocks/branch-like';
import { mockQualityGateStatusConditionEnhanced } from '../../../../helpers/mocks/quality-gates';
import { mockMetric } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { MetricKey, MetricType } from '../../../../types/metrics';
import { QualityGateStatusConditionEnhanced } from '../../../../types/quality-gates';
import QualityGateCondition from '../QualityGateCondition';

it.each([
  [quickMock(MetricKey.reliability_rating)],
  [quickMock(MetricKey.security_rating)],
  [quickMock(MetricKey.sqale_rating)],
  [quickMock(MetricKey.new_reliability_rating, 'RATING', true)],
  [quickMock(MetricKey.new_security_rating, 'RATING', true)],
  [quickMock(MetricKey.new_maintainability_rating, 'RATING', true)],
  [quickMock(MetricKey.security_hotspots_reviewed)],
  [quickMock(MetricKey.new_security_hotspots_reviewed, 'RATING', true)],
])('should render correclty', async (condition) => {
  renderQualityGateCondition({ condition });
  expect(
    await screen.findByText(`metric.${condition.measure.metric.name}.name`),
  ).toBeInTheDocument();

  expect(
    await screen.findByText(`quality_gates.operator.${condition.op}`, { exact: false }),
  ).toBeInTheDocument();
  // if (condition.measure.metric.type === 'RATING') {
  //   expect(await screen.findByText('.rating', { exact: false })).toBeInTheDocument();
  // }
});

it('should show the count when metric is not rating', async () => {
  renderQualityGateCondition({ condition: quickMock(MetricKey.open_issues, MetricType.Integer) });
  expect(await screen.findByText('3 metric.open_issues.name')).toBeInTheDocument();
});

it('should work with branch', async () => {
  const condition = quickMock(MetricKey.new_maintainability_rating);
  renderQualityGateCondition({ branchLike: mockBranch(), condition });

  expect(await screen.findByText('metric.new_maintainability_rating.name')).toBeInTheDocument();
  expect(
    await screen.findByText('quality_gates.operator.GT.rating', { exact: false }),
  ).toBeInTheDocument();
});

function renderQualityGateCondition(props: Partial<QualityGateCondition['props']>) {
  return renderComponent(
    <QualityGateCondition
      component={{ key: 'abcd-key' }}
      condition={mockQualityGateStatusConditionEnhanced()}
      {...props}
    />,
  );
}

function quickMock(
  metric: MetricKey,
  type = 'RATING',
  addPeriod = false,
): QualityGateStatusConditionEnhanced {
  return mockQualityGateStatusConditionEnhanced({
    error: '1',
    measure: {
      metric: mockMetric({
        key: metric,
        name: metric,
        type,
      }),
      value: '3',
      ...(addPeriod ? { period: { value: '3', index: 1 } } : {}),
    },
    metric,
    ...(addPeriod ? { period: 1 } : {}),
  });
}
