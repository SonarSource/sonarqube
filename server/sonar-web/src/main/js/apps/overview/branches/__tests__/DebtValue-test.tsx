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
import { mockMainBranch } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockMeasureEnhanced, mockMetric } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { MetricKey } from '../../../../types/metrics';
import { DebtValue, DebtValueProps } from '../DebtValue';

it('should render correctly', () => {
  renderDebtValue();

  expect(
    screen.getByLabelText(
      'overview.see_more_details_on_x_of_y.work_duration.x_minutes.1.sqale_index'
    )
  ).toBeInTheDocument();

  expect(screen.getByText('sqale_index')).toBeInTheDocument();
});

it('should render diff metric correctly', () => {
  renderDebtValue({ useDiffMetric: true });

  expect(
    screen.getByLabelText(
      'overview.see_more_details_on_x_of_y.work_duration.x_minutes.1.new_technical_debt'
    )
  ).toBeInTheDocument();

  expect(screen.getByText('new_technical_debt')).toBeInTheDocument();
});

it('should handle missing measure', () => {
  renderDebtValue({ measures: [] });

  expect(screen.getByLabelText('no_data')).toBeInTheDocument();
  expect(screen.getByText('metric.sqale_index.name')).toBeInTheDocument();
});

function renderDebtValue(props: Partial<DebtValueProps> = {}) {
  return renderComponent(
    <DebtValue
      branchLike={mockMainBranch()}
      component={mockComponent()}
      measures={[
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.sqale_index }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_technical_debt }) }),
      ]}
      {...props}
    />
  );
}
