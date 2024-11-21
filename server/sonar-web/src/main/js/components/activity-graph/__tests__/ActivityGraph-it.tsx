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
import { times } from 'lodash';
import * as React from 'react';
import {
  byLabelText,
  byPlaceholderText,
  byRole,
  byText,
} from '~sonar-aligned/helpers/testSelector';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import { modeHandler } from '../../../apps/issues/test-utils';
import { CCT_SOFTWARE_QUALITY_METRICS } from '../../../helpers/constants';
import { parseDate } from '../../../helpers/dates';
import { mockHistoryItem, mockMeasureHistory } from '../../../helpers/mocks/project-activity';
import { mockMetric } from '../../../helpers/testMocks';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { ComponentPropsType } from '../../../helpers/testUtils';
import { Mode } from '../../../types/mode';
import { GraphType, MeasureHistory } from '../../../types/project-activity';
import { Metric } from '../../../types/types';
import GraphsHeader from '../GraphsHeader';
import GraphsHistory from '../GraphsHistory';
import { generateSeries, getDisplayedHistoryMetrics, splitSeriesInGraphs } from '../utils';

const MAX_GRAPHS = 2;
const MAX_SERIES_PER_GRAPH = 3;
const HISTORY_COUNT = 10;
const START_DATE = '2016-01-01T00:00:00+0200';

describe('rendering', () => {
  it('should render correctly when loading', async () => {
    renderActivityGraph({ loading: true });
    expect(await screen.findByText('loading')).toBeInTheDocument();
  });

  it.each([
    [Mode.MQR, MetricKey.software_quality_maintainability_issues],
    [Mode.Standard, MetricKey.code_smells],
  ])('should show the correct legend items in %s mode', async (mode, metric) => {
    modeHandler.setMode(mode);
    const { ui, user } = getPageObject();
    renderActivityGraph();

    // Static legend items, which aren't interactive.
    expect(ui.legendRemoveMetricBtn(MetricKey.violations).query()).not.toBeInTheDocument();
    expect(ui.getLegendItem(MetricKey.violations)).toBeInTheDocument();

    // Switch to custom graph.
    await ui.changeGraphType(GraphType.custom);
    await ui.openAddMetrics();
    await ui.clickOnMetric(metric);
    await ui.clickOnMetric(MetricKey.test_failures);
    await user.keyboard('{Escape}');

    // These legend items are interactive (interaction tested below).
    expect(ui.legendRemoveMetricBtn(metric).get()).toBeInTheDocument();
    expect(ui.legendRemoveMetricBtn(MetricKey.test_failures).get()).toBeInTheDocument();

    // Shows warning for metrics with no data.
    const li = ui.getLegendItem(MetricKey.test_failures);
    // eslint-disable-next-line jest/no-conditional-in-test
    if (li) {
      li.focus();
    }
    expect(ui.noDataWarningTooltip.get()).toBeInTheDocument();
  });
});

describe('data table modal', () => {
  it('shows the same data in a table', async () => {
    const { ui } = getPageObject();
    renderActivityGraph();

    await ui.openDataTable();
    expect(ui.dataTable.get()).toBeInTheDocument();
    expect(ui.dataTableColHeaders.getAll()).toHaveLength(3);
    expect(ui.dataTableRows.getAll()).toHaveLength(HISTORY_COUNT + 1);

    // Change graph type and dates, check table updates correctly.
    await ui.closeDataTable();
    await ui.changeGraphType(GraphType.coverage);

    await ui.openDataTable(true);
    expect(ui.dataTable.get()).toBeInTheDocument();
    expect(ui.dataTableColHeaders.getAll()).toHaveLength(4);
    expect(ui.dataTableRows.getAll()).toHaveLength(HISTORY_COUNT + 1);
  });

  it('shows the same data in a table when filtered by date', async () => {
    const { ui } = getPageObject();
    renderActivityGraph({
      graphStartDate: parseDate('2017-01-01'),
      graphEndDate: parseDate('2019-01-01'),
    });

    await ui.openDataTable();
    expect(ui.dataTable.get()).toBeInTheDocument();
    expect(ui.dataTableColHeaders.getAll()).toHaveLength(3);
    expect(ui.dataTableRows.getAll()).toHaveLength(2);
  });
});

it.each([
  [
    Mode.MQR,
    MetricKey.software_quality_reliability_issues,
    MetricKey.software_quality_maintainability_issues,
    MetricKey.software_quality_security_issues,
  ],
  [Mode.Standard, MetricKey.bugs, MetricKey.code_smells, MetricKey.vulnerabilities],
])(
  'should correctly handle adding/removing custom metrics in $s mode',
  async (mode, bugs, codeSmells, vulnerabilities) => {
    modeHandler.setMode(mode);
    const { ui } = getPageObject();
    renderActivityGraph();

    // Change graph type to "Custom".
    await ui.changeGraphType(GraphType.custom);

    // Open the "Add metrics" dropdown button; select some metrics.
    await ui.openAddMetrics();

    // We should not see DATA type or New Code metrics.
    expect(ui.metricCheckbox(`new_${bugs}`).query()).not.toBeInTheDocument();
    expect(ui.burnedBudgetCheckbox.query()).not.toBeInTheDocument();

    // Select 3 Int types.
    await ui.clickOnMetric(bugs);
    await ui.clickOnMetric(codeSmells);
    await ui.clickOnMetric(MetricKey.accepted_issues);
    // Select 1 Percent type.
    await ui.clickOnMetric(MetricKey.coverage);

    // We should see 2 graphs, correctly labelled.
    expect(ui.graphs.getAll()).toHaveLength(2);

    // We cannot select anymore Int types. It should hide options, and show an alert.
    expect(ui.metricCheckbox(vulnerabilities).query()).not.toBeInTheDocument();
    expect(ui.hiddenOptionsAlert.get()).toBeInTheDocument();

    // Select 2 more Percent types.
    await ui.clickOnMetric(MetricKey.duplicated_lines_density);
    await ui.clickOnMetric(MetricKey.test_success_density);

    // We cannot select anymore options. It should disable all remaining options, and
    // show a different alert.
    expect(ui.maxOptionsAlert.get()).toBeInTheDocument();
    expect(ui.metricCheckbox(vulnerabilities).get()).toBeDisabled();

    // Disable a few options.
    await ui.clickOnMetric(bugs);
    await ui.clickOnMetric(codeSmells);
    await ui.clickOnMetric(MetricKey.coverage);

    // Search for option and select it
    await ui.searchForMetric('condition');
    expect(ui.metricCheckbox(MetricKey.branch_coverage).query()).not.toBeInTheDocument();
    await ui.clickOnMetric(MetricKey.conditions_to_cover);

    // Disable percentage metrics by clicking on the legend items.
    await ui.removeMetric(MetricKey.duplicated_lines_density);
    await ui.removeMetric(MetricKey.test_success_density);

    // We should see 1 graph
    expect(ui.graphs.getAll()).toHaveLength(1);

    // Disable final number metrics
    await ui.removeMetric(MetricKey.accepted_issues);
    await ui.removeMetric(MetricKey.conditions_to_cover);

    // Should show message that there's no data to be rendered.
    expect(ui.noDataText.get()).toBeInTheDocument();
  },
);

function getPageObject() {
  const user = userEvent.setup();
  const ui = {
    // Graph types.
    graphTypeSelect: byLabelText('project_activity.graphs.choose_type'),

    // Add/remove metrics.
    addMetricBtn: byRole('button', { name: 'project_activity.graphs.custom.add' }),
    maintainabilityIssuesCheckbox: byRole('checkbox', {
      name: MetricKey.software_quality_maintainability_issues,
    }),
    newBugsCheckbox: byRole('checkbox', { name: MetricKey.new_bugs }),
    burnedBudgetCheckbox: byRole('checkbox', { name: MetricKey.burned_budget }),
    hiddenOptionsAlert: byText('project_activity.graphs.custom.type_x_message', {
      exact: false,
    }),
    maxOptionsAlert: byText('project_activity.graphs.custom.add_metric_info'),
    filterMetrics: byPlaceholderText('search.search_for_metrics'),
    metricCheckbox: (name: string) => byRole('checkbox', { name }),
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

    // Data in table.
    openInTableRegion: byRole('region', { name: /project_activity.graphs.open_in_table/ }),
    closeDataTableBtn: byRole('button', { name: 'close' }),
    dataTable: byRole('table'),
    dataTableRows: byRole('row'),
    dataTableColHeaders: byRole('columnheader'),
    noDataTableText: byText('project_activity.graphs.data_table.no_data_warning_check_dates_x', {
      exact: false,
    }),
  };

  return {
    user,
    ui: {
      ...ui,
      async changeGraphType(type: GraphType) {
        await user.click(ui.graphTypeSelect.get());
        const optionForType = await screen.findByText(`project_activity.graphs.${type}`);
        await user.click(optionForType);
      },
      async openAddMetrics() {
        await user.click(ui.addMetricBtn.get());
      },
      async searchForMetric(text: string) {
        await user.type(ui.filterMetrics.get(), text);
      },
      async clickOnMetric(name: MetricKey) {
        await user.click(ui.metricCheckbox(name).get());
      },
      async removeMetric(metric: MetricKey) {
        await user.click(ui.legendRemoveMetricBtn(metric).get());
      },
      async openDataTable(tabFromGraphSelection = false) {
        // tab to graph selection and close popup
        if (!tabFromGraphSelection) {
          await user.tab();
          await user.keyboard('{escape}');
        }
        // tab to graph region and open data table
        await user.tab();
        await user.keyboard('{enter}');
      },
      async closeDataTable() {
        await user.click(ui.closeDataTableBtn.get());
      },
    },
  };
}

function renderActivityGraph(
  graphsHistoryProps: Partial<GraphsHistory['props']> = {},
  graphsHeaderProps: Partial<ComponentPropsType<typeof GraphsHeader>> = {},
) {
  function ActivityGraph() {
    const [selectedMetrics, setSelectedMetrics] = React.useState<string[]>([]);
    const [graph, setGraph] = React.useState(graphsHistoryProps.graph || GraphType.issues);

    const measuresHistory: MeasureHistory[] = [];
    const metrics: Metric[] = [];
    [
      MetricKey.accepted_issues,
      MetricKey.violations,
      MetricKey.bugs,
      MetricKey.code_smells,
      MetricKey.vulnerabilities,
      MetricKey.software_quality_maintainability_issues,
      MetricKey.software_quality_reliability_issues,
      MetricKey.software_quality_security_issues,
      MetricKey.blocker_violations,
      MetricKey.lines_to_cover,
      MetricKey.uncovered_lines,
      MetricKey.coverage,
      MetricKey.duplicated_lines_density,
      MetricKey.test_success_density,
      MetricKey.branch_coverage,
      MetricKey.conditions_to_cover,
    ].forEach((metric) => {
      const history = times(HISTORY_COUNT - 2, (i) => {
        const date = parseDate(START_DATE);
        date.setDate(date.getDate() + i);
        return mockHistoryItem({ date, value: i.toString() });
      });
      history.push(
        mockHistoryItem({
          date: parseDate('2018-10-27T12:21:15+0200'),
          value: CCT_SOFTWARE_QUALITY_METRICS.includes(metric)
            ? JSON.stringify({ total: 2286 })
            : '2286',
        }),
        mockHistoryItem({ date: parseDate('2020-10-27T16:33:50+0200') }),
      );
      measuresHistory.push(mockMeasureHistory({ metric, history }));
      metrics.push(
        mockMetric({
          key: metric,
          type:
            metric.includes('_density') || metric === MetricKey.coverage
              ? MetricType.Percent
              : MetricType.Integer,
        }),
      );
    });

    // The following should be filtered out, and not be suggested as options.
    metrics.push(
      mockMetric({ key: MetricKey.new_bugs, type: MetricType.Integer }),
      mockMetric({ key: MetricKey.burned_budget, type: MetricType.Data }),
    );

    // The following will not be filtered out, but has no values.
    metrics.push(mockMetric({ key: MetricKey.test_failures, type: MetricType.Integer }));
    measuresHistory.push(
      mockMeasureHistory({
        metric: MetricKey.test_failures,
        history: times(HISTORY_COUNT, (i) => {
          const date = parseDate(START_DATE);
          date.setDate(date.getDate() + i);
          return mockHistoryItem({ date, value: undefined });
        }),
      }),
    );

    const series = generateSeries(
      measuresHistory,
      graph,
      metrics,
      getDisplayedHistoryMetrics(graph, selectedMetrics),
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

    return (
      <>
        <GraphsHeader
          onAddCustomMetric={addCustomMetric}
          graph={graph}
          metrics={metrics}
          metricsTypeFilter={metricsTypeFilter}
          onRemoveCustomMetric={removeCustomMetric}
          selectedMetrics={selectedMetrics}
          onUpdateGraph={updateGraph}
          {...graphsHeaderProps}
        />
        <GraphsHistory
          analyses={[]}
          graph={graph}
          graphs={graphs}
          loading={false}
          measuresHistory={[]}
          removeCustomMetric={removeCustomMetric}
          series={series}
          {...graphsHistoryProps}
        />
      </>
    );
  }

  return renderComponent(<ActivityGraph />);
}
