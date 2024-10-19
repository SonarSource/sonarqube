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
import { MetricKey } from '~sonar-aligned/types/metrics';
import { mockQualityGateStatusCondition } from '../../../../helpers/mocks/quality-gates';
import { mockCurrentUser } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { SonarLintPromotion, SonarLintPromotionProps } from '../SonarLintPromotion';

it('should render correctly', () => {
  renderSonarLintPromotion();
  expect(
    screen.queryByText('overview.fix_failed_conditions_with_sonarlint'),
  ).not.toBeInTheDocument();

  renderSonarLintPromotion({ currentUser: mockCurrentUser({ usingSonarLintConnectedMode: true }) });
  expect(
    screen.queryByText('overview.fix_failed_conditions_with_sonarlint'),
  ).not.toBeInTheDocument();
});

it.each(
  [
    MetricKey.new_blocker_violations,
    MetricKey.new_critical_violations,
    MetricKey.new_info_violations,
    MetricKey.new_violations,
    MetricKey.new_major_violations,
    MetricKey.new_minor_violations,
    MetricKey.new_code_smells,
    MetricKey.new_bugs,
    MetricKey.new_vulnerabilities,
    MetricKey.new_security_rating,
    MetricKey.new_maintainability_rating,
    MetricKey.new_reliability_rating,
  ].map(Array.of),
)('should show message for %s', async (metric) => {
  renderSonarLintPromotion({
    qgConditions: [mockQualityGateStatusCondition({ metric: metric as MetricKey })],
  });

  expect(
    await screen.findByText('overview.fix_failed_conditions_with_sonarlint'),
  ).toBeInTheDocument();
});

function renderSonarLintPromotion(props: Partial<SonarLintPromotionProps> = {}) {
  return renderComponent(<SonarLintPromotion currentUser={mockCurrentUser()} {...props} />);
}
