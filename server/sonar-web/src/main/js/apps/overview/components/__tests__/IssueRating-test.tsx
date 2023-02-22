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
import { screen } from '@testing-library/react';
import * as React from 'react';
import { mockPullRequest } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockMeasureEnhanced, mockMetric } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { IssueType } from '../../../../types/issues';
import { MetricKey } from '../../../../types/metrics';
import { IssueRating, IssueRatingProps } from '../IssueRating';

it('should render correctly for vulnerabilities', async () => {
  renderIssueRating({ type: IssueType.Vulnerability });
  expect(await screen.findByText('metric_domain.Security')).toBeInTheDocument();

  renderIssueRating({ type: IssueType.Vulnerability, useDiffMetric: true });
  const labels = await screen.findAllByText('metric_domain.Security');
  expect(labels).toHaveLength(2);
  const tooltips = await screen.findAllByText('metric.security_rating.tooltip.A');
  expect(tooltips).toHaveLength(2);
});

it('should render correctly if no values are present', async () => {
  renderIssueRating({
    measures: [mockMeasureEnhanced({ metric: mockMetric({ key: 'NONE' }) })],
  });
  expect(await screen.findByText('metric_domain.Reliability')).toBeInTheDocument();
});

function renderIssueRating(props: Partial<IssueRatingProps> = {}) {
  return renderComponent(
    <IssueRating
      branchLike={mockPullRequest()}
      component={mockComponent()}
      measures={[
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_reliability_rating }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.reliability_rating }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_maintainability_rating }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.sqale_rating }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_security_rating }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.security_rating }) }),
      ]}
      type={IssueType.Bug}
      {...props}
    />
  );
}
