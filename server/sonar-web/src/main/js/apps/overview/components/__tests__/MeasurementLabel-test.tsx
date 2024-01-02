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
import { mockPullRequest } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockMeasureEnhanced, mockMetric } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { MetricKey } from '../../../../types/metrics';
import { MeasurementType } from '../../utils';
import MeasurementLabel from '../MeasurementLabel';

it('should render correctly for coverage', async () => {
  renderMeasurementLabel();
  expect(await screen.findByText('metric.coverage.name')).toBeInTheDocument();

  renderMeasurementLabel({
    measures: [
      mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.coverage }) }),
      mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.lines_to_cover }) }),
    ],
  });
  expect(await screen.findByText('metric.coverage.name')).toBeInTheDocument();
  expect(await screen.findByText('overview.coverage_on_X_lines')).toBeInTheDocument();

  renderMeasurementLabel({
    measures: [
      mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_coverage }) }),
      mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_lines_to_cover }) }),
    ],
    useDiffMetric: true,
  });
  expect(screen.getByRole('link', { name: /.*new_coverage.*/ })).toBeInTheDocument();
  expect(await screen.findByText('overview.coverage_on_X_lines')).toBeInTheDocument();
  expect(await screen.findByText('overview.coverage_on_X_new_lines')).toBeInTheDocument();
});

it('should render correctly for duplications', async () => {
  renderMeasurementLabel({
    measures: [
      mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.duplicated_lines_density }) }),
    ],
    type: MeasurementType.Duplication,
  });
  expect(
    screen.getByRole('link', {
      name: 'overview.see_more_details_on_x_of_y.1.0%.metric.duplicated_lines_density.name',
    }),
  ).toBeInTheDocument();
  expect(await screen.findByText('metric.duplicated_lines_density.short_name')).toBeInTheDocument();

  renderMeasurementLabel({
    measures: [
      mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.duplicated_lines_density }) }),
      mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.ncloc }) }),
    ],
    type: MeasurementType.Duplication,
  });
  expect(await screen.findByText('metric.duplicated_lines_density.short_name')).toBeInTheDocument();
  expect(await screen.findByText('overview.duplications_on_X_lines')).toBeInTheDocument();

  renderMeasurementLabel({
    measures: [
      mockMeasureEnhanced({
        metric: mockMetric({ key: MetricKey.new_duplicated_lines_density }),
      }),
      mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_lines }) }),
    ],
    type: MeasurementType.Duplication,
    useDiffMetric: true,
  });

  expect(
    screen.getByRole('link', {
      name: 'overview.see_more_details_on_x_of_y.1.0%.metric.new_duplicated_lines_density.name',
    }),
  ).toBeInTheDocument();
  expect(await screen.findByText('overview.duplications_on_X_new_lines')).toBeInTheDocument();
});

it('should render correctly with no value', async () => {
  renderMeasurementLabel({ measures: [] });
  expect(await screen.findByText('metric.coverage.name')).toBeInTheDocument();
});

function renderMeasurementLabel(props: Partial<MeasurementLabel['props']> = {}) {
  return renderComponent(
    <MeasurementLabel
      branchLike={mockPullRequest()}
      component={mockComponent()}
      measures={[mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.coverage }) })]}
      type={MeasurementType.Coverage}
      {...props}
    />,
  );
}
