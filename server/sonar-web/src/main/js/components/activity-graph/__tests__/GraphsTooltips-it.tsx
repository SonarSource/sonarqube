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
import { MetricKey } from '~sonar-aligned/types/metrics';
import { parseDate } from '../../../helpers/dates';
import {
  mockAnalysisEvent,
  mockHistoryItem,
  mockMeasureHistory,
} from '../../../helpers/mocks/project-activity';
import { mockMetric } from '../../../helpers/testMocks';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { GraphType, MeasureHistory } from '../../../types/project-activity';
import { Metric } from '../../../types/types';
import { GraphsTooltips, Props } from '../GraphsTooltips';
import { generateSeries, getDisplayedHistoryMetrics } from '../utils';

it.each([
  [GraphType.issues, [[MetricKey.violations, 1]]],
  [
    GraphType.coverage,
    [
      ['metric.coverage.name', '75.0%'],
      ['metric.uncovered_lines.name', 8],
    ],
  ],
  [GraphType.duplications, [['metric.duplicated_lines_density.name', '3.0%']]],
  [GraphType.custom, [[MetricKey.bugs, 1]]],
])(
  'renders correctly for graph of type %s',
  (graph, metrics: Array<[string, number, string] | [string, number]>) => {
    renderGraphsTooltips({ graph });

    // Render events.
    expect(screen.getByText('event.category.QUALITY_GATE', { exact: false })).toBeInTheDocument();

    // Measures table.
    metrics.forEach(([key, n, rating]) => {
      expect(
        screen.getByRole('row', {
          // eslint-disable-next-line jest/no-conditional-in-test
          name: rating ? `${n} ${key} ${rating}` : `${n} ${key}`,
        }),
      ).toBeInTheDocument();
    });
  },
);

function renderGraphsTooltips(props: Partial<Props> = {}) {
  const graph = (props.graph as GraphType) || GraphType.coverage;
  const measuresHistory: MeasureHistory[] = [];
  const date = props.selectedDate || parseDate('2016-01-01T00:00:00+0200');
  const metrics: Metric[] = [];

  (
    [
      [MetricKey.violations, '1'],
      [MetricKey.bugs, '1'],
      [MetricKey.lines_to_cover, '10'],
      [MetricKey.uncovered_lines, '8'],
      [MetricKey.coverage, '75'],
      [MetricKey.duplicated_lines_density, '3'],
    ] as Array<[MetricKey, string]>
  ).forEach(([metric, value]) => {
    measuresHistory.push(
      mockMeasureHistory({
        metric,
        history: [mockHistoryItem({ date, value })],
      }),
    );

    metrics.push(
      mockMetric({
        key: metric,
        type: metric.includes('_density') || metric === MetricKey.coverage ? 'PERCENT' : 'INT',
      }),
    );
  });

  const series = generateSeries(
    measuresHistory,
    graph,
    metrics,
    getDisplayedHistoryMetrics(graph, graph === GraphType.custom ? [MetricKey.bugs] : []),
  );

  return renderComponent(
    <GraphsTooltips
      events={[mockAnalysisEvent({ key: '1' })]}
      graph={graph}
      graphWidth={100}
      measuresHistory={measuresHistory}
      selectedDate={date}
      series={series}
      tooltipIdx={0}
      tooltipPos={0}
      formatValue={(n: number | string) => String(n)}
      {...props}
    />,
  );
}
