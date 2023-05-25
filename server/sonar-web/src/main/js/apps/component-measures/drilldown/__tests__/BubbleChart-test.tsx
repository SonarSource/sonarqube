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
import userEvent from '@testing-library/user-event';
import { MetricsEnum } from 'design-system';
import { keyBy } from 'lodash';
import React from 'react';
import { AutoSizerProps } from 'react-virtualized';
import { byRole, byText } from 'testing-library-selector';
import { mockComponentMeasure } from '../../../../helpers/mocks/component';
import { mockMeasure, mockMetric, mockPaging } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { MetricKey, MetricType } from '../../../../types/metrics';
import { enhanceComponent } from '../../utils';
import BubbleChart from '../BubbleChart';

jest.mock('react-virtualized/dist/commonjs/AutoSizer', () => ({
  AutoSizer: ({ children }: AutoSizerProps) => children({ width: 100, height: NaN }),
}));

jest.mock('d3-zoom', () => ({
  zoom: jest.fn().mockReturnValue({ scaleExtent: jest.fn().mockReturnValue({ on: jest.fn() }) }),
}));

jest.mock('d3-selection', () => ({
  select: jest.fn().mockReturnValue({ call: jest.fn() }),
}));

const metrics = keyBy(
  [
    mockMetric({ key: MetricKey.ncloc, type: MetricType.Integer }),
    mockMetric({ key: MetricKey.security_remediation_effort, type: MetricType.Data }),
    mockMetric({ key: MetricKey.vulnerabilities, type: MetricType.Integer }),
    mockMetric({ key: MetricKey.security_rating, type: MetricType.Rating }),
  ],
  (m) => m.key
);

const ui = {
  show500Files: byText(/component_measures.legend.only_first_500_files/),
  bubbles: byRole('link', { name: '' }),
  filterCheckbox: (name: string) => byRole('checkbox', { name }),
};

it('should render correctly', async () => {
  renderBubbleChart();

  expect(await ui.show500Files.find()).toBeInTheDocument();
  expect(ui.bubbles.getAll()).toHaveLength(1);
});

it('should filter by rating', async () => {
  const user = userEvent.setup();
  renderBubbleChart();

  expect(await ui.bubbles.findAll()).toHaveLength(1);
  Object.keys(MetricsEnum).forEach((rating) => {
    expect(ui.filterCheckbox(rating).get()).toBeInTheDocument();
  });

  await user.click(ui.filterCheckbox(MetricsEnum.C).get());
  expect(ui.bubbles.getAll()).toHaveLength(1);
});

function renderBubbleChart(overrides: Partial<BubbleChart['props']> = {}) {
  return renderComponent(
    <BubbleChart
      componentKey="foo"
      components={[
        enhanceComponent(
          mockComponentMeasure(true, {
            measures: [
              mockMeasure({ value: '236', metric: MetricKey.ncloc }),
              mockMeasure({ value: '10', metric: MetricKey.security_remediation_effort }),
              mockMeasure({ value: '3', metric: MetricKey.vulnerabilities }),
              mockMeasure({ value: '2', metric: MetricKey.security_rating }),
            ],
          }),
          metrics[MetricKey.vulnerabilities],
          metrics
        ),
      ]}
      domain="Security"
      metrics={metrics}
      paging={mockPaging({ total: 1000 })}
      updateSelected={jest.fn()}
      {...overrides}
    />
  );
}
