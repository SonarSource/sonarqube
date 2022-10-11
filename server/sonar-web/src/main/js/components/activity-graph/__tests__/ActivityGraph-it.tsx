/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import userEvent from '@testing-library/user-event';
import { UserEvent } from '@testing-library/user-event/dist/types/setup';
import { times } from 'lodash';
import * as React from 'react';
import selectEvent from 'react-select-event';
import { byLabelText, byPlaceholderText, byRole, byText } from 'testing-library-selector';
import { parseDate } from '../../../helpers/dates';
import {
  mockAnalysisEvent,
  mockHistoryItem,
  mockMeasureHistory,
  mockParsedAnalysis
} from '../../../helpers/mocks/project-activity';
import { mockMetric } from '../../../helpers/testMocks';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { MetricKey } from '../../../types/metrics';
import { GraphType, MeasureHistory } from '../../../types/project-activity';
import { Metric } from '../../../types/types';
import GraphsHeader from '../GraphsHeader';
import GraphsHistory from '../GraphsHistory';
import { generateSeries, getDisplayedHistoryMetrics, splitSeriesInGraphs } from '../utils';

const ui = {
  // Graph types.
  graphTypeSelect: byLabelText('project_activity.graphs.choose_type'),

  // Add metrics.
  addMetricBtn: byRole('button', { name: 'project_activity.graphs.custom.add' }),
  bugsCheckbox: byRole('checkbox', { name: MetricKey.bugs }),
  newBugsCheckbox: byRole('checkbox', { name: MetricKey.new_bugs }),
  burnedBudgetCheckbox: byRole('checkbox', { name: MetricKey.burned_budget }),
  vulnerabilityCheckbox: byRole('checkbox', { name: MetricKey.vulnerabilities }),
  hiddenOptionsAlert: byText('project_activity.graphs.custom.type_x_message', {
    exact: false
  }),
  maxOptionsAlert: byText('project_activity.graphs.custom.add_metric_info'),
  filterMetrics: byPlaceholderText('search.search_for_metrics'),

  // Graphs.
  graphs: byLabelText('project_activity.graphs.explanation_x', { exact: false }),
  noDataText: byText('project_activity.graphs.custom.no_history'),

  // Date filters.
  fromDateInput: byLabelText('from_date'),
  toDateInput: byLabelText('to_date'),
  submitDatesBtn: byRole('button', { name: 'Submit dates' }),

  // Data in table.
  openInTableBtn: byRole('button', { name: 'project_activity.graphs.open_in_table' }),
  closeDataTableBtn: byRole('button', { name: 'close' }),
  dataTable: byRole('table'),
  dataTableRows: byRole('row'),
  dataTableColHeaders: byRole('columnheader'),
  onlyFirst100Text: byText('project_activity.graphs.data_table.max_lines_warning.100'),
  noDataTableText: byText('project_activity.graphs.data_table.no_data_warning_check_dates_x', {
    exact: false
  })
};

it('should correctly handle adding/removing custom metrics', async () => {
  const user = userEvent.setup();
  renderActivityGraph();

  // Change graph type to "Custom".
  await changeGraphType(GraphType.custom);

  // Open the "Add metrics" dropdown button; select some metrics.
  await toggleAddMetrics(user);

  // We should not see DATA type or New Code metrics.
  expect(ui.newBugsCheckbox.query()).not.toBeInTheDocument();
  expect(ui.burnedBudgetCheckbox.query()).not.toBeInTheDocument();

  // Select 3 Int types.
  await clickOnMetric(user, MetricKey.bugs);
  await clickOnMetric(user, MetricKey.code_smells);
  await clickOnMetric(user, MetricKey.confirmed_issues);
  // Select 1 Percent type.
  await clickOnMetric(user, MetricKey.coverage);

  // We should see 2 graphs, correctly labelled.
  expect(ui.graphs.getAll()).toHaveLength(2);

  // We cannot select anymore Int types. It should hide options, and show an alert.
  expect(ui.vulnerabilityCheckbox.query()).not.toBeInTheDocument();
  expect(ui.hiddenOptionsAlert.get()).toBeInTheDocument();

  // Select 2 more Percent types.
  await clickOnMetric(user, MetricKey.duplicated_lines_density);
  await clickOnMetric(user, MetricKey.test_success_density);

  // We cannot select anymore options. It should disable all remaining options, and
  // show a different alert.
  expect(ui.maxOptionsAlert.get()).toBeInTheDocument();
  // See https://github.com/testing-library/jest-dom/issues/144 for why we cannot
  // use isDisabled().
  expect(ui.vulnerabilityCheckbox.get()).toHaveAttribute('aria-disabled', 'true');

  // Disable a few options.
  await clickOnMetric(user, MetricKey.bugs);
  await clickOnMetric(user, MetricKey.code_smells);
  await clickOnMetric(user, MetricKey.coverage);

  // Search for option.
  await searchForMetric(user, 'bug');
  expect(ui.bugsCheckbox.get()).toBeInTheDocument();
  expect(ui.vulnerabilityCheckbox.query()).not.toBeInTheDocument();
  toggleAddMetrics(user);

  // Disable final metrics by clicking on the legend items.
  await removeMetric(user, MetricKey.confirmed_issues);
  await removeMetric(user, MetricKey.duplicated_lines_density);
  await removeMetric(user, MetricKey.test_success_density);

  // Should show message that there's no data to be rendered.
  expect(ui.noDataText.get()).toBeInTheDocument();
});

it('should render correctly when loading', async () => {
  renderActivityGraph({ loading: true });
  expect(await screen.findByLabelText('loading')).toBeInTheDocument();
});

it('shows the same data in a table', async () => {
  const user = userEvent.setup();
  renderActivityGraph();

  await user.click(ui.openInTableBtn.get());
  expect(ui.dataTable.get()).toBeInTheDocument();
  expect(ui.dataTableColHeaders.getAll()).toHaveLength(5);
  expect(ui.dataTableRows.getAll()).toHaveLength(101);
  expect(screen.getByText('event.category.QUALITY_GATE', { exact: false })).toBeInTheDocument();
  expect(screen.getByText('event.category.VERSION', { exact: false })).toBeInTheDocument();
  expect(
    screen.getByText('event.category.DEFINITION_CHANGE', { exact: false })
  ).toBeInTheDocument();
  expect(ui.onlyFirst100Text.get()).toBeInTheDocument();

  // Change graph type and dates, check table updates correctly.
  await user.click(ui.closeDataTableBtn.get());
  await changeGraphType(GraphType.coverage);

  await user.click(ui.openInTableBtn.get());
  expect(ui.dataTable.get()).toBeInTheDocument();
  expect(ui.dataTableColHeaders.getAll()).toHaveLength(4);
  expect(ui.dataTableRows.getAll()).toHaveLength(101);
});

it('shows the same data in a table when filtered by date', async () => {
  const user = userEvent.setup();
  renderActivityGraph({
    graphStartDate: parseDate('2017-01-01'),
    graphEndDate: parseDate('2019-01-01')
  });

  await user.click(ui.openInTableBtn.get());
  expect(ui.dataTable.get()).toBeInTheDocument();
  expect(ui.dataTableColHeaders.getAll()).toHaveLength(5);
  expect(ui.dataTableRows.getAll()).toHaveLength(2);
  expect(ui.onlyFirst100Text.query()).not.toBeInTheDocument();
});

async function changeGraphType(type: GraphType) {
  await selectEvent.select(ui.graphTypeSelect.get(), [`project_activity.graphs.${type}`]);
}

async function toggleAddMetrics(user: UserEvent) {
  await user.click(ui.addMetricBtn.get());
}

async function clickOnMetric(user: UserEvent, name: MetricKey) {
  await user.click(screen.getByRole('checkbox', { name }));
}

async function searchForMetric(user: UserEvent, text: string) {
  await user.type(ui.filterMetrics.get(), text);
}

async function removeMetric(user: UserEvent, metric: MetricKey) {
  await user.click(
    screen.getByRole('button', { name: `project_activity.graphs.custom.remove_metric.${metric}` })
  );
}

function renderActivityGraph(
  graphsHistoryProps: Partial<GraphsHistory['props']> = {},
  graphsHeaderProps: Partial<GraphsHeader['props']> = {}
) {
  const MAX_GRAPHS = 2;
  const MAX_SERIES_PER_GRAPH = 3;
  const HISTORY_COUNT = 100;

  function ActivityGraph() {
    const [selectedMetrics, setSelectedMetrics] = React.useState<string[]>([]);
    const [graph, setGraph] = React.useState(GraphType.issues);
    const [selectedDate, setSelectedDate] = React.useState<Date | undefined>(undefined);
    const [fromDate, setFromDate] = React.useState<Date | undefined>(undefined);
    const [toDate, setToDate] = React.useState<Date | undefined>(undefined);

    const measuresHistory: MeasureHistory[] = [];
    const metrics: Metric[] = [];
    [
      MetricKey.bugs,
      MetricKey.code_smells,
      MetricKey.confirmed_issues,
      MetricKey.vulnerabilities,
      MetricKey.blocker_violations,
      MetricKey.lines_to_cover,
      MetricKey.uncovered_lines,
      MetricKey.coverage,
      MetricKey.duplicated_lines_density,
      MetricKey.test_success_density
    ].forEach(metric => {
      const history = times(HISTORY_COUNT, i => {
        const date = parseDate('2016-01-01T00:00:00+0200');
        date.setDate(date.getDate() + i);
        return mockHistoryItem({ date, value: i.toString() });
      });
      history.push(
        mockHistoryItem({ date: parseDate('2018-10-27T12:21:15+0200') }),
        mockHistoryItem({ date: parseDate('2020-10-27T16:33:50+0200') })
      );
      measuresHistory.push(mockMeasureHistory({ metric, history }));
      metrics.push(
        mockMetric({
          key: metric,
          name: metric,
          type: metric.includes('_density') || metric === MetricKey.coverage ? 'PERCENT' : 'INT'
        })
      );
    });

    // The following should be filtered out, and not be suggested as options.
    metrics.push(
      mockMetric({ key: MetricKey.new_bugs, name: MetricKey.new_bugs, type: 'INT' }),
      mockMetric({ key: MetricKey.burned_budget, name: MetricKey.burned_budget, type: 'DATA' })
    );

    const series = generateSeries(
      measuresHistory,
      graph,
      metrics,
      getDisplayedHistoryMetrics(graph, selectedMetrics)
    );
    const graphs = splitSeriesInGraphs(series, MAX_GRAPHS, MAX_SERIES_PER_GRAPH);
    const metricsTypeFilter =
      graphs.length < MAX_GRAPHS
        ? undefined
        : graphs.filter(graph => graph.length < MAX_SERIES_PER_GRAPH).map(graph => graph[0].type);

    const addCustomMetric = (metricKey: string) => {
      setSelectedMetrics([...selectedMetrics, metricKey]);
    };

    const removeCustomMetric = (metricKey: string) => {
      setSelectedMetrics(selectedMetrics.filter(m => m !== metricKey));
    };

    const updateGraph = (graphType: string) => {
      setGraph(graphType as GraphType);
    };

    const updateSelectedDate = (date?: Date) => {
      setSelectedDate(date);
    };

    const updateFromToDates = (from?: Date, to?: Date) => {
      setFromDate(from);
      setToDate(to);
    };

    return (
      <>
        <GraphsHeader
          addCustomMetric={addCustomMetric}
          graph={graph}
          metrics={metrics}
          metricsTypeFilter={metricsTypeFilter}
          removeCustomMetric={removeCustomMetric}
          selectedMetrics={selectedMetrics}
          updateGraph={updateGraph}
          {...graphsHeaderProps}
        />
        <GraphsHistory
          analyses={[
            mockParsedAnalysis({
              date: parseDate('2018-10-27T12:21:15+0200'),
              events: [
                mockAnalysisEvent({ key: '1' }),
                mockAnalysisEvent({
                  key: '2',
                  category: 'VERSION',
                  description: undefined,
                  qualityGate: undefined
                }),
                mockAnalysisEvent({
                  key: '3',
                  category: 'DEFINITION_CHANGE',
                  definitionChange: {
                    projects: [{ changeType: 'ADDED', key: 'foo', name: 'Foo' }]
                  },
                  qualityGate: undefined
                })
              ]
            })
          ]}
          graph={graph}
          graphEndDate={toDate}
          graphStartDate={fromDate}
          graphs={graphs}
          loading={false}
          measuresHistory={[]}
          removeCustomMetric={removeCustomMetric}
          selectedDate={selectedDate}
          series={series}
          updateGraphZoom={updateFromToDates}
          updateSelectedDate={updateSelectedDate}
          {...graphsHistoryProps}
        />
      </>
    );
  }

  return renderComponent(<ActivityGraph />);
}
