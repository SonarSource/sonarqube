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
import { times } from 'lodash';
import * as React from 'react';
import { parseDate } from '../../../helpers/dates';
import {
  mockAnalysisEvent,
  mockHistoryItem,
  mockMeasureHistory,
  mockParsedAnalysis,
} from '../../../helpers/mocks/project-activity';
import { mockMetric } from '../../../helpers/testMocks';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { MetricKey } from '../../../types/metrics';
import {
  GraphType,
  MeasureHistory,
  ProjectAnalysisEventCategory,
} from '../../../types/project-activity';
import { Metric } from '../../../types/types';
import DataTableModal, { DataTableModalProps, MAX_DATA_TABLE_ROWS } from '../DataTableModal';
import { generateSeries, getDisplayedHistoryMetrics } from '../utils';

it('should render correctly if there are no series', () => {
  renderDataTableModal({ series: [] });
  expect(
    screen.getByText('project_activity.graphs.data_table.no_data_warning'),
  ).toBeInTheDocument();
});

it('should render correctly if there are events', () => {
  renderDataTableModal({
    analyses: [
      mockParsedAnalysis({
        date: parseDate('2016-01-01T00:00:00+0200'),
        events: [
          mockAnalysisEvent({ key: '1', category: ProjectAnalysisEventCategory.QualityGate }),
        ],
      }),
    ],
  });
  expect(screen.getByText('event.category.QUALITY_GATE', { exact: false })).toBeInTheDocument();
});

it('should render correctly if there is too much data', () => {
  renderDataTableModal({ series: mockSeries(MAX_DATA_TABLE_ROWS + 1) });
  expect(
    screen.getByText(`project_activity.graphs.data_table.max_lines_warning.${MAX_DATA_TABLE_ROWS}`),
  ).toBeInTheDocument();
});

it('should render correctly if there is no data and we have a start date', () => {
  renderDataTableModal({ graphStartDate: parseDate('3022-01-01') });
  expect(
    screen.getByText('project_activity.graphs.data_table.no_data_warning_check_dates_x', {
      exact: false,
    }),
  ).toBeInTheDocument();
});

it('should render correctly if there is no data and we have an end date', () => {
  renderDataTableModal({ graphEndDate: parseDate('2015-01-01') });
  expect(
    screen.getByText('project_activity.graphs.data_table.no_data_warning_check_dates_y', {
      exact: false,
    }),
  ).toBeInTheDocument();
});

it('should render correctly if there is no data and we have a date range', () => {
  renderDataTableModal({
    graphEndDate: parseDate('2015-01-01'),
    graphStartDate: parseDate('2014-01-01'),
  });
  expect(
    screen.getByText('project_activity.graphs.data_table.no_data_warning_check_dates_x_y', {
      exact: false,
    }),
  ).toBeInTheDocument();
});

function renderDataTableModal(props: Partial<DataTableModalProps> = {}) {
  return renderComponent(
    <DataTableModal analyses={[]} series={mockSeries()} onClose={jest.fn()} {...props} />,
  );
}

function mockSeries(n = 10) {
  const measuresHistory: MeasureHistory[] = [];
  const metrics: Metric[] = [];
  [MetricKey.bugs, MetricKey.code_smells, MetricKey.vulnerabilities].forEach((metric) => {
    const history = times(n, (i) => {
      const date = parseDate('2016-01-01T00:00:00+0200');
      date.setDate(date.getDate() + 365 * i);
      return mockHistoryItem({ date, value: i.toString() });
    });
    measuresHistory.push(mockMeasureHistory({ metric, history }));
    metrics.push(
      mockMetric({
        key: metric,
        name: metric,
        type: 'INT',
      }),
    );
  });

  return generateSeries(
    measuresHistory,
    GraphType.issues,
    metrics,
    getDisplayedHistoryMetrics(GraphType.issues, [
      MetricKey.bugs,
      MetricKey.code_smells,
      MetricKey.vulnerabilities,
    ]),
  );
}
