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
import React from 'react';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import { mockQualityGateStatusConditionEnhanced } from '../../../../helpers/mocks/quality-gates';
import { mockMetric } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { QualityGateStatusConditionEnhanced } from '../../../../types/quality-gates';
import QualityGateCondition from '../QualityGateCondition';
import QualityGateSimplifiedCondition from '../QualityGateSimplifiedCondition';

it('should show simplified condition', async () => {
  renderQualityGateCondition({
    condition: quickMock(MetricKey.new_violations, MetricType.Integer),
  });
  expect(await screen.findByText('metric.new_violations.name')).toBeInTheDocument();
});

function renderQualityGateCondition(props: Partial<QualityGateCondition['props']>) {
  return renderComponent(
    <QualityGateSimplifiedCondition
      component={{ key: 'abcd-key' }}
      condition={mockQualityGateStatusConditionEnhanced()}
      {...props}
    />,
  );
}

function quickMock(
  metric: MetricKey,
  type = MetricType.Rating,
  addPeriod = false,
  value = '3',
): QualityGateStatusConditionEnhanced {
  return mockQualityGateStatusConditionEnhanced({
    error: '1',
    measure: {
      metric: mockMetric({
        key: metric,
        name: metric,
        type,
      }),
      value,
      ...(addPeriod ? { period: { value, index: 1 } } : {}),
    },
    metric,
    ...(addPeriod ? { period: 1 } : {}),
  });
}
