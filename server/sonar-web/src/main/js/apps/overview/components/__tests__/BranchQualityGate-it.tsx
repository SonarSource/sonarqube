/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { mockPullRequest } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockQualityGateStatusConditionEnhanced } from '../../../../helpers/mocks/quality-gates';
import { mockMeasureEnhanced, mockMetric } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { byLabelText, byRole } from '../../../../helpers/testSelector';
import { MetricKey, MetricType } from '../../../../types/metrics';
import { FCProps } from '../../../../types/misc';
import { Status } from '../../utils';
import BranchQualityGate from '../BranchQualityGate';

it('renders failed QG', () => {
  renderBranchQualityGate();

  // Maintainability rating condition
  const maintainabilityRatingLink = byRole('link', {
    name: 'overview.failed_condition.x_rating_requiredmetric_domain.Maintainability metric.type.ratingE A',
  }).get();
  expect(maintainabilityRatingLink).toBeInTheDocument();
  expect(maintainabilityRatingLink).toHaveAttribute(
    'href',
    '/project/issues?resolved=false&types=CODE_SMELL&pullRequest=1001&sinceLeakPeriod=true&id=my-project',
  );

  // Security Hotspots rating condition
  const securityHotspotsRatingLink = byRole('link', {
    name: 'overview.failed_condition.x_rating_requiredmetric_domain.Security Review metric.type.ratingE A',
  }).get();
  expect(securityHotspotsRatingLink).toBeInTheDocument();
  expect(securityHotspotsRatingLink).toHaveAttribute(
    'href',
    '/security_hotspots?id=my-project&pullRequest=1001',
  );

  // New code smells
  const codeSmellsLink = byRole('link', {
    name: 'overview.failed_condition.x_required 5 Code Smells ≤ 1',
  }).get();
  expect(codeSmellsLink).toBeInTheDocument();
  expect(codeSmellsLink).toHaveAttribute(
    'href',
    '/project/issues?resolved=false&types=CODE_SMELL&pullRequest=1001&id=my-project',
  );

  // Conditions to cover
  const conditionToCoverLink = byRole('link', {
    name: 'overview.failed_condition.x_required 5 Conditions to cover ≥ 10',
  }).get();
  expect(conditionToCoverLink).toBeInTheDocument();
  expect(conditionToCoverLink).toHaveAttribute(
    'href',
    '/component_measures?id=my-project&metric=conditions_to_cover&pullRequest=1001&view=list',
  );

  expect(byLabelText('overview.quality_gate_x.overview.gate.ERROR').get()).toBeInTheDocument();
});

it('renders passed QG', () => {
  renderBranchQualityGate({ failedConditions: [], status: Status.OK });

  expect(byLabelText('overview.quality_gate_x.overview.gate.OK').get()).toBeInTheDocument();
  expect(byRole('link').query()).not.toBeInTheDocument();
});

function renderBranchQualityGate(props: Partial<FCProps<typeof BranchQualityGate>> = {}) {
  return renderComponent(
    <BranchQualityGate
      status={Status.ERROR}
      branchLike={mockPullRequest()}
      component={mockComponent()}
      failedConditions={[
        mockQualityGateStatusConditionEnhanced({
          actual: '5.0',
          error: '1.0',
          metric: MetricKey.new_maintainability_rating,
          measure: mockMeasureEnhanced({
            metric: mockMetric({
              domain: 'Maintainability',
              key: MetricKey.new_maintainability_rating,
              name: 'Maintainability rating',
              type: MetricType.Rating,
            }),
          }),
        }),
        mockQualityGateStatusConditionEnhanced({
          actual: '5.0',
          error: '1.0',
          metric: MetricKey.new_security_review_rating,
          measure: mockMeasureEnhanced({
            metric: mockMetric({
              domain: 'Security Review',
              key: MetricKey.new_security_review_rating,
              name: 'Security Review Rating',
              type: MetricType.Rating,
            }),
          }),
        }),
        mockQualityGateStatusConditionEnhanced({
          actual: '5',
          error: '1',
          metric: MetricKey.new_code_smells,
          measure: mockMeasureEnhanced({
            metric: mockMetric({
              domain: 'Maintainability',
              key: MetricKey.new_code_smells,
              name: 'Code Smells',
              type: MetricType.ShortInteger,
            }),
          }),
        }),
        mockQualityGateStatusConditionEnhanced({
          actual: '5',
          error: '10',
          op: 'up',
          metric: MetricKey.conditions_to_cover,
          measure: mockMeasureEnhanced({
            metric: mockMetric({
              key: MetricKey.conditions_to_cover,
              name: 'Conditions to cover',
              type: MetricType.ShortInteger,
            }),
          }),
        }),
      ]}
      {...props}
    />,
  );
}
