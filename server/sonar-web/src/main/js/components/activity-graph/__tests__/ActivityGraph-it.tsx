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
import userEvent from '@testing-library/user-event';
import { UserEvent } from '@testing-library/user-event/dist/types/setup/setup';
import { times } from 'lodash';
import * as React from 'react';
import selectEvent from 'react-select-event';
import { byLabelText, byPlaceholderText, byRole, byText } from 'testing-library-selector';
import { parseDate } from '../../../helpers/dates';
import { mockHistoryItem, mockMeasureHistory } from '../../../helpers/mocks/project-activity';
import { mockMetric } from '../../../helpers/testMocks';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { MetricKey } from '../../../types/metrics';
import { GraphType, MeasureHistory } from '../../../types/project-activity';
import { Metric } from '../../../types/types';
import GraphsHeader from '../GraphsHeader';
import GraphsHistory from '../GraphsHistory';
import { generateSeries, getDisplayedHistoryMetrics, splitSeriesInGraphs } from '../utils';

const MAX_GRAPHS = 2;
const MAX_SERIES_PER_GRAPH = 3;
const HISTORY_COUNT = 10;
const START_DATE = '2016-01-01T00:00:00+0200';

it('should render correctly when loading', async () => {
  renderActivityGraph({ loading: true });
  expect(await screen.findByText('loading')).toBeInTheDocument();
});

it('should show the correct legend items', async () => {
  const user = userEvent.setup();
  const ui = getPageObject(user);
  renderActivityGraph();

  // Static legend items, which aren't interactive.
  expect(ui.legendRemoveMetricBtn(MetricKey.bugs).query()).not.toBeInTheDocument();
  expect(ui.getLegendItem(MetricKey.bugs)).toBeInTheDocument();

  // Switch to custom graph.
  await ui.changeGraphType(GraphType.custom);
  await ui.openAddMetrics();
  await ui.clickOnMetric(MetricKey.bugs);
  await ui.clickOnMetric(MetricKey.test_failures);
  await user.keyboard('{Escape}');

  // These legend items are interactive (interaction tested below).
  expect(ui.legendRemoveMetricBtn(MetricKey.bugs).get()).toBeInTheDocument();
  expect(ui.legendRemoveMetricBtn(MetricKey.test_failures).get()).toBeInTheDocument();

  // Shows warning for metrics with no data.
  const li = ui.getLegendItem(MetricKey.test_failures);
  // eslint-disable-next-line jest/no-conditional-in-test
  if (li) {
    li.focus();
  }
  expect(ui.noDataWarningTooltip.get()).toBeInTheDocument();
});

it('should correctly handle adding/removing custom metrics', async () => {
  const ui = getPageObject(userEvent.setup());
  renderActivityGraph();

  // Change graph type to "Custom".
  await ui.changeGraphType(GraphType.custom);

  // Open the "Add metrics" dropdown button; select some metrics.
  await ui.openAddMetrics();

  // We should not see DATA type or New Code metrics.
  expect(ui.newBugsCheckbox.query()).not.toBeInTheDocument();
  expect(ui.burnedBudgetCheckbox.query()).not.toBeInTheDocument();

  // Select 3 Int types.
  await ui.clickOnMetric(MetricKey.bugs);
  await ui.clickOnMetric(MetricKey.code_smells);
  await ui.clickOnMetric(MetricKey.confirmed_issues);
  // Select 1 Percent type.
  await ui.clickOnMetric(MetricKey.coverage);

  // We should see 2 graphs, correctly labelled.
  expect(ui.graphs.getAll()).toHaveLength(2);

  // We cannot select anymore Int types. It should hide options, and show an alert.
  expect(ui.vulnerabilityCheckbox.query()).not.toBeInTheDocument();
  expect(ui.hiddenOptionsAlert.get()).toBeInTheDocument();

  // Select 2 more Percent types.
  await ui.clickOnMetric(MetricKey.duplicated_lines_density);
  await ui.clickOnMetric(MetricKey.test_success_density);

  // We cannot select anymore options. It should disable all remaining options, and
  // show a different alert.
  expect(ui.maxOptionsAlert.get()).toBeInTheDocument();
  // See https://github.com/testing-library/jest-dom/issues/144 for why we cannot
  // use isDisabled().
  expect(ui.vulnerabilityCheckbox.get()).toHaveAttribute('aria-disabled', 'true');

  // Disable a few options.
  await ui.clickOnMetric(MetricKey.bugs);
  await ui.clickOnMetric(MetricKey.code_smells);
  await ui.clickOnMetric(MetricKey.coverage);

  // Search for option.
  await ui.searchForMetric('bug');
  expect(ui.bugsCheckbox.get()).toBeInTheDocument();
  expect(ui.vulnerabilityCheckbox.query()).not.toBeInTheDocument();

  // Disable final metrics by clicking on the legend items.
  await ui.removeMetric(MetricKey.confirmed_issues);
  await ui.removeMetric(MetricKey.duplicated_lines_density);
  await ui.removeMetric(MetricKey.test_success_density);

  // Should show message that there's no data to be rendered.
  expect(ui.noDataText.get()).toBeInTheDocument();
});

describe('data table modal', () => {
  it('shows the same data in a table', async () => {
    const ui = getPageObject(userEvent.setup());
    renderActivityGraph();

    await ui.openDataTable();
    expect(ui.dataTable.get()).toBeInTheDocument();
    expect(ui.dataTableColHeaders.getAll()).toHaveLength(5);
    expect(ui.dataTableRows.getAll()).toHaveLength(HISTORY_COUNT + 1);

    // Change graph type and dates, check table updates correctly.
    await ui.closeDataTable();
    await ui.changeGraphType(GraphType.coverage);

    await ui.openDataTable();
    expect(ui.dataTable.get()).toBeInTheDocument();
    expect(ui.dataTableColHeaders.getAll()).toHaveLength(4);
    expect(ui.dataTableRows.getAll()).toHaveLength(HISTORY_COUNT + 1);
  });

  it('shows the same data in a table when filtered by date', async () => {
    const ui = getPageObject(userEvent.setup());
    renderActivityGraph({
      graphStartDate: parseDate('2017-01-01'),
      graphEndDate: parseDate('2019-01-01'),
    });

    await ui.openDataTable();
    expect(ui.dataTable.get()).toBeInTheDocument();
    expect(ui.dataTableColHeaders.getAll()).toHaveLength(5);
    expect(ui.dataTableRows.getAll()).toHaveLength(2);
  });
});

function getPageObject(user: UserEvent) {
  const ui = {
    // Graph types.
    graphTypeSelect: byLabelText('project_activity.graphs.choose_type'),

    // Add/remove metrics.
    addMetricBtn: byRole('button', { name: 'project_activity.graphs.custom.add' }),
    bugsCheckbox: byRole('checkbox', { name: MetricKey.bugs }),
    newBugsCheckbox: byRole('checkbox', { name: MetricKey.new_bugs }),
    burnedBudgetCheckbox: byRole('checkbox', { name: MetricKey.burned_budget }),
    vulnerabilityCheckbox: byRole('checkbox', { name: MetricKey.vulnerabilities }),
    hiddenOptionsAlert: byText('project_activity.graphs.custom.type_x_message', {
      exact: false,
    }),
    maxOptionsAlert: byText('project_activity.graphs.custom.add_metric_info'),
    filterMetrics: byPlaceholderText('search.search_for_metrics'),
    legendRemoveMetricBtn: (key: string) =>
      byRole('button', { name: `project_activity.graphs.custom.remove_metric.${key}` }),
    getLegendItem: (name: string) => {
      // This is due to a limitation in testing library, where we cannot get a listitem
      // role element by name.
      return screen.getAllByRole('listitem').find((item) => item.textContent === name);
    },
    noDataWarningTooltip: byLabelText('project_activity.graphs.custom.metric_no_history'),

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
    noDataTableText: byText('project_activity.graphs.data_table.no_data_warning_check_dates_x', {
      exact: false,
    }),
  };

  return {
    ...ui,
    async changeGraphType(type: GraphType) {
      await selectEvent.select(ui.graphTypeSelect.get(), [`project_activity.graphs.${type}`]);
    },
    async openAddMetrics() {
      await user.click(ui.addMetricBtn.get());
    },
    async searchForMetric(text: string) {
      await user.type(ui.filterMetrics.get(), text);
    },
    async clickOnMetric(name: MetricKey) {
      await user.click(screen.getByRole('checkbox', { name }));
    },
    async removeMetric(metric: MetricKey) {
      await user.click(ui.legendRemoveMetricBtn(metric).get());
    },
    async openDataTable() {
      await user.click(ui.openInTableBtn.get());
    },
    async closeDataTable() {
      await user.click(ui.closeDataTableBtn.get());
    },
  };
}

function renderActivityGraph(
  graphsHistoryProps: Partial<GraphsHistory['props']> = {},
  graphsHeaderProps: Partial<GraphsHeader['props']> = {}
) {
  function ActivityGraph() {
    const [selectedMetrics, setSelectedMetrics] = React.useState<string[]>([]);
    const [graph, setGraph] = React.useState(graphsHistoryProps.graph || GraphType.issues);
    const [selectedDate, setSelectedDate] = React.useState<Date | undefined>(
      graphsHistoryProps.selectedDate
    );
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
      MetricKey.test_success_density,
    ].forEach((metric) => {
      const history = times(HISTORY_COUNT - 2, (i) => {
        const date = parseDate(START_DATE);
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
          type: metric.includes('_density') || metric === MetricKey.coverage ? 'PERCENT' : 'INT',
        })
      );
    });

    // The following should be filtered out, and not be suggested as options.
    metrics.push(
      mockMetric({ key: MetricKey.new_bugs, type: 'INT' }),
      mockMetric({ key: MetricKey.burned_budget, type: 'DATA' })
    );

    // The following will not be filtered out, but has no values.
    metrics.push(mockMetric({ key: MetricKey.test_failures, type: 'INT' }));
    measuresHistory.push(
      mockMeasureHistory({
        metric: MetricKey.test_failures,
        history: times(HISTORY_COUNT, (i) => {
          const date = parseDate(START_DATE);
          date.setDate(date.getDate() + i);
          return mockHistoryItem({ date, value: undefined });
        }),
      })
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
        : graphs
            .filter((graph) => graph.length < MAX_SERIES_PER_GRAPH)
            .map((graph) => graph[0].type);

    const addCustomMetric = (metricKey: string) => {
      setSelectedMetrics([...selectedMetrics, metricKey]);
    };

    const removeCustomMetric = (metricKey: string) => {
      setSelectedMetrics(selectedMetrics.filter((m) => m !== metricKey));
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
          analyses={[]}
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
