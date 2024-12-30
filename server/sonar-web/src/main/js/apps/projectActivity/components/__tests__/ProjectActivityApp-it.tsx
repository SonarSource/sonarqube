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

import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { keyBy, times } from 'lodash';
import { Route } from 'react-router-dom';
import { byLabelText, byRole, byTestId, byText } from '~sonar-aligned/helpers/testSelector';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import ApplicationServiceMock from '../../../../api/mocks/ApplicationServiceMock';
import { ModeServiceMock } from '../../../../api/mocks/ModeServiceMock';
import { ProjectActivityServiceMock } from '../../../../api/mocks/ProjectActivityServiceMock';
import { TimeMachineServiceMock } from '../../../../api/mocks/TimeMachineServiceMock';
import { mockBranchList } from '../../../../api/mocks/data/branches';
import { DEPRECATED_ACTIVITY_METRICS } from '../../../../helpers/constants';
import { parseDate } from '../../../../helpers/dates';
import { mockComponent } from '../../../../helpers/mocks/component';
import {
  mockAnalysis,
  mockAnalysisEvent,
  mockHistoryItem,
  mockMeasureHistory,
} from '../../../../helpers/mocks/project-activity';
import { get } from '../../../../helpers/storage';
import { mockMetric } from '../../../../helpers/testMocks';
import { renderAppWithComponentContext } from '../../../../helpers/testReactTestingUtils';
import { Mode } from '../../../../types/mode';
import {
  ApplicationAnalysisEventCategory,
  GraphType,
  ProjectAnalysisEventCategory,
} from '../../../../types/project-activity';
import ProjectActivityAppContainer from '../ProjectActivityApp';

jest.mock('../../../../api/projectActivity');

jest.mock('../../../../helpers/storage', () => ({
  ...jest.requireActual('../../../../helpers/storage'),
  get: jest.fn(),
  save: jest.fn(),
}));

jest.mock('../../../../api/branches', () => ({
  getBranches: () => {
    isBranchReady = true;
    return Promise.resolve(mockBranchList());
  },
}));

const applicationHandler = new ApplicationServiceMock();
const projectActivityHandler = new ProjectActivityServiceMock();
const timeMachineHandler = new TimeMachineServiceMock();
const modeHandler = new ModeServiceMock();

let isBranchReady = false;

beforeEach(() => {
  isBranchReady = false;

  jest.clearAllMocks();
  applicationHandler.reset();
  projectActivityHandler.reset();
  timeMachineHandler.reset();
  modeHandler.reset();

  timeMachineHandler.setMeasureHistory(
    [
      MetricKey.violations,
      MetricKey.bugs,
      MetricKey.reliability_rating,
      MetricKey.code_smells,
      MetricKey.sqale_rating,
      MetricKey.security_hotspots_reviewed,
      MetricKey.security_review_rating,
      MetricKey.software_quality_maintainability_issues,
    ].map((metric) =>
      mockMeasureHistory({
        metric,
        history: projectActivityHandler.getAnalysesList().map(({ date }) =>
          mockHistoryItem({
            value: '3',
            date: parseDate(date),
          }),
        ),
      }),
    ),
  );
});

describe('rendering', () => {
  it('should render issues as default graph', async () => {
    const { ui } = getPageObject();
    renderProjectActivityAppContainer();

    await ui.appLoaded();
    expect(ui.graphTypeSelect.get()).toHaveValue('project_activity.graphs.issues');
  });

  it('should render new code legend for applications', async () => {
    const { ui } = getPageObject();

    renderProjectActivityAppContainer(
      mockComponent({
        qualifier: ComponentQualifier.Application,
        breadcrumbs: [
          { key: 'breadcrumb', name: 'breadcrumb', qualifier: ComponentQualifier.Application },
        ],
      }),
    );
    await ui.appLoaded();
    expect(ui.newCodeLegend.get()).toBeInTheDocument();
  });

  it('should render new code legend for projects', async () => {
    const { ui } = getPageObject();

    renderProjectActivityAppContainer(
      mockComponent({
        qualifier: ComponentQualifier.Project,
        breadcrumbs: [
          { key: 'breadcrumb', name: 'breadcrumb', qualifier: ComponentQualifier.Project },
        ],
        leakPeriodDate: parseDate('2017-03-01T22:00:00.000Z').toDateString(),
      }),
    );

    await ui.appLoaded();
    expect(ui.newCodeLegend.get()).toBeInTheDocument();
  });

  it.each([ComponentQualifier.Portfolio, ComponentQualifier.SubPortfolio])(
    'should not render new code legend for %s',
    async (qualifier) => {
      const { ui } = getPageObject();

      renderProjectActivityAppContainer(
        mockComponent({
          qualifier,
          breadcrumbs: [{ key: 'breadcrumb', name: 'breadcrumb', qualifier }],
        }),
      );

      await ui.appLoaded({ doNotWaitForBranch: true });
      expect(ui.newCodeLegend.query()).not.toBeInTheDocument();
    },
  );

  it('should correctly show the baseline marker', async () => {
    const { ui } = getPageObject();

    renderProjectActivityAppContainer(
      mockComponent({
        leakPeriodDate: parseDate('2017-03-01T22:00:00.000Z').toDateString(),
        breadcrumbs: [
          { key: 'breadcrumb', name: 'breadcrumb', qualifier: ComponentQualifier.Project },
        ],
      }),
    );

    await ui.appLoaded();
    expect(ui.baseline.get()).toBeInTheDocument();
  });

  it('should correctly show the baseline marker when first new code analysis is not present but baseline analysis is present', async () => {
    const { ui } = getPageObject();

    renderProjectActivityAppContainer(
      mockComponent({
        leakPeriodDate: parseDate('2017-03-03T22:00:00.000Z').toDateString(),
        breadcrumbs: [
          { key: 'breadcrumb', name: 'breadcrumb', qualifier: ComponentQualifier.Project },
        ],
      }),
    );

    await ui.appLoaded();
    expect(ui.baseline.get()).toBeInTheDocument();
  });

  it('should not show the baseline marker when first new code analysis and baseline analysis is not present', async () => {
    const { ui } = getPageObject();

    renderProjectActivityAppContainer(
      mockComponent({
        leakPeriodDate: parseDate('2017-03-10T22:00:00.000Z').toDateString(),
        breadcrumbs: [
          { key: 'breadcrumb', name: 'breadcrumb', qualifier: ComponentQualifier.Project },
        ],
      }),
    );

    await ui.appLoaded();
    expect(ui.baseline.query()).not.toBeInTheDocument();
  });

  it('should only show certain security hotspot-related metrics for a project', async () => {
    const { ui } = getPageObject();

    renderProjectActivityAppContainer(
      mockComponent({
        breadcrumbs: [
          { key: 'breadcrumb', name: 'breadcrumb', qualifier: ComponentQualifier.Project },
        ],
      }),
    );

    await ui.changeGraphType(GraphType.custom);
    await ui.openMetricsDropdown();
    expect(ui.metricCheckbox(MetricKey.security_hotspots_reviewed).get()).toBeInTheDocument();
    expect(ui.metricCheckbox(MetricKey.security_review_rating).query()).not.toBeInTheDocument();
  });

  it.each([ComponentQualifier.Portfolio, ComponentQualifier.SubPortfolio])(
    'should only show certain security hotspot-related metrics for a %s',
    async (qualifier) => {
      const { ui } = getPageObject();

      renderProjectActivityAppContainer(
        mockComponent({
          qualifier,
          breadcrumbs: [{ key: 'breadcrumb', name: 'breadcrumb', qualifier }],
        }),
      );

      await ui.changeGraphType(GraphType.custom);
      await ui.openMetricsDropdown();
      expect(ui.metricCheckbox(MetricKey.security_review_rating).get()).toBeInTheDocument();

      expect(
        ui.metricCheckbox(MetricKey.security_hotspots_reviewed).query(),
      ).not.toBeInTheDocument();
    },
  );

  it('should render graph gap info message', async () => {
    timeMachineHandler.setMeasureHistory([
      mockMeasureHistory({
        metric: MetricKey.code_smells,
        history: projectActivityHandler.getAnalysesList().map(({ date }) =>
          mockHistoryItem({
            // eslint-disable-next-line jest/no-conditional-in-test
            value: '2',
            date: parseDate(date),
          }),
        ),
      }),
      mockMeasureHistory({
        metric: MetricKey.software_quality_maintainability_issues,
        history: projectActivityHandler.getAnalysesList().map(({ date }, index) =>
          mockHistoryItem({
            // eslint-disable-next-line jest/no-conditional-in-test
            value: index === 0 ? '3' : undefined,
            date: parseDate(date),
          }),
        ),
      }),
    ]);
    const { ui } = getPageObject();
    renderProjectActivityAppContainer(
      mockComponent({
        breadcrumbs: [
          { key: 'breadcrumb', name: 'breadcrumb', qualifier: ComponentQualifier.Application },
        ],
      }),
    );

    await ui.changeGraphType(GraphType.custom);
    await ui.openMetricsDropdown();
    await ui.toggleMetric(MetricKey.software_quality_maintainability_issues);
    expect(ui.gapInfoMessage.get()).toBeInTheDocument();
  });

  it('should not render graph gap info message if no gaps', async () => {
    const { ui } = getPageObject();
    renderProjectActivityAppContainer(
      mockComponent({
        breadcrumbs: [
          { key: 'breadcrumb', name: 'breadcrumb', qualifier: ComponentQualifier.Application },
        ],
      }),
    );

    await ui.changeGraphType(GraphType.custom);
    await ui.openMetricsDropdown();
    await ui.toggleMetric(MetricKey.software_quality_maintainability_issues);
    expect(ui.gapInfoMessage.query()).not.toBeInTheDocument();
  });
});

describe('CRUD', () => {
  it('should correctly create, update, and delete "VERSION" events', async () => {
    const { ui } = getPageObject();
    const initialValue = '1.1-SNAPSHOT';
    const updatedValue = '1.1--SNAPSHOT';

    renderProjectActivityAppContainer(
      mockComponent({
        breadcrumbs: [
          { key: 'breadcrumb', name: 'breadcrumb', qualifier: ComponentQualifier.Project },
        ],
        configuration: { showHistory: true },
      }),
    );

    await ui.appLoaded();

    await ui.addVersionEvent('1.1.0.1', initialValue);
    expect(screen.getAllByText(initialValue).length).toBeGreaterThan(0);

    await ui.updateEvent(`VERSION ${initialValue}`, updatedValue);
    expect(screen.getAllByText(updatedValue).length).toBeGreaterThan(0);

    await ui.deleteEvent(`VERSION ${updatedValue}`);
    expect(screen.queryByText(updatedValue)).not.toBeInTheDocument();
  });

  it('should correctly create, update, and delete "OTHER" events', async () => {
    const { ui } = getPageObject();
    const initialValue = 'Custom event name';
    const updatedValue = 'Custom event updated name';

    renderProjectActivityAppContainer(
      mockComponent({
        breadcrumbs: [
          { key: 'breadcrumb', name: 'breadcrumb', qualifier: ComponentQualifier.Project },
        ],
        configuration: { showHistory: true },
      }),
    );

    await ui.appLoaded();

    await ui.addCustomEvent('1.1.0.1', initialValue);
    expect(screen.getAllByText(initialValue).length).toBeGreaterThan(0);

    await ui.updateEvent(`OTHER ${initialValue}`, updatedValue);
    expect(screen.getAllByText(updatedValue).length).toBeGreaterThan(0);

    await ui.deleteEvent(`OTHER ${updatedValue}`);
    expect(screen.queryByText(updatedValue)).not.toBeInTheDocument();
  });

  it('should correctly allow deletion of specific analyses', async () => {
    const { ui } = getPageObject();

    renderProjectActivityAppContainer(
      mockComponent({
        breadcrumbs: [
          { key: 'breadcrumb', name: 'breadcrumb', qualifier: ComponentQualifier.Project },
        ],
        configuration: { showHistory: true },
      }),
    );

    await ui.appLoaded();

    // Most recent analysis is not deletable.
    await ui.openCogMenu('1.1.0.2');
    expect(ui.deleteAnalysisBtn.query()).not.toBeInTheDocument();

    await ui.deleteAnalysis('1.1.0.1');
    expect(screen.queryByText('1.1.0.1')).not.toBeInTheDocument();
  });
});

describe('data loading', () => {
  function getMock(namespace: string) {
    // eslint-disable-next-line jest/no-conditional-in-test
    return namespace.includes('.custom') ? 'bugs,code_smells' : GraphType.custom;
  }

  it('should load all analyses', async () => {
    const count = 1000;

    projectActivityHandler.setAnalysesList(
      times(count, (i) => {
        return mockAnalysis({
          key: `analysis-${i}`,
          date: '2016-01-01T00:00:00+0200',
        });
      }),
    );

    const { ui } = getPageObject();
    renderProjectActivityAppContainer();
    await ui.appLoaded();

    expect(ui.activityItem.getAll().length).toBe(count);
  });

  it('should reload custom graph from local storage', async () => {
    jest.mocked(get).mockImplementationOnce(getMock).mockImplementationOnce(getMock);
    const { ui } = getPageObject();
    renderProjectActivityAppContainer();
    await ui.appLoaded();

    expect(ui.graphTypeSelect.get()).toHaveValue('project_activity.graphs.custom');
  });

  it('should correctly fetch the top level component when dealing with sub portfolios', async () => {
    const { ui } = getPageObject();

    renderProjectActivityAppContainer(
      mockComponent({
        key: 'unknown',
        qualifier: ComponentQualifier.SubPortfolio,
        breadcrumbs: [
          { key: 'foo', name: 'foo', qualifier: ComponentQualifier.Portfolio },
          { key: 'unknown', name: 'unknown', qualifier: ComponentQualifier.SubPortfolio },
        ],
      }),
    );

    await ui.appLoaded({ doNotWaitForBranch: true });

    // If it didn't fail, it means we correctly queried for project "foo".
    expect(ui.activityItem.getAll().length).toBe(4);
  });
});

describe('filtering', () => {
  it('should correctly filter by event category', async () => {
    projectActivityHandler.setAnalysesList([
      mockAnalysis({
        key: `analysis-1`,
        events: [],
      }),
      mockAnalysis({
        key: `analysis-2`,
        events: [
          mockAnalysisEvent({ key: '1', category: ProjectAnalysisEventCategory.QualityGate }),
        ],
      }),
      mockAnalysis({
        key: `analysis-3`,
        events: [mockAnalysisEvent({ key: '2', category: ProjectAnalysisEventCategory.Version })],
      }),
      mockAnalysis({
        key: `analysis-4`,
        events: [mockAnalysisEvent({ key: '3', category: ProjectAnalysisEventCategory.Version })],
      }),
      mockAnalysis({
        key: `analysis-5`,
        events: [mockAnalysisEvent({ key: '4', category: ProjectAnalysisEventCategory.SqUpgrade })],
      }),
      mockAnalysis({
        key: `analysis-6`,
        events: [mockAnalysisEvent({ key: '5', category: ProjectAnalysisEventCategory.Version })],
      }),
      mockAnalysis({
        key: `analysis-7`,
        events: [mockAnalysisEvent({ key: '6', category: ProjectAnalysisEventCategory.SqUpgrade })],
      }),
    ]);

    const { ui } = getPageObject();
    renderProjectActivityAppContainer();
    await ui.appLoaded();

    await ui.filterByCategory(ProjectAnalysisEventCategory.Version);
    expect(ui.activityItem.getAll().length).toBe(3);

    await ui.filterByCategory(ProjectAnalysisEventCategory.QualityGate);
    expect(ui.activityItem.getAll().length).toBe(1);

    await ui.filterByCategory(ProjectAnalysisEventCategory.SqUpgrade);
    expect(ui.activityItem.getAll().length).toBe(2);
  });

  it('should correctly filter by date range', async () => {
    projectActivityHandler.setAnalysesList(
      times(20, (i) => {
        const date = parseDate('2016-01-01T00:00:00.000Z');
        date.setDate(date.getDate() + i);
        return mockAnalysis({
          key: `analysis-${i}`,
          date: date.toDateString(),
        });
      }),
    );

    const { ui } = getPageObject();
    renderProjectActivityAppContainer();
    await ui.appLoaded();

    expect(ui.activityItem.getAll().length).toBe(20);

    await ui.setDateRange('2016-01-10');
    expect(ui.activityItem.getAll().length).toBe(11);
    await ui.resetDateFilters();

    expect(ui.activityItem.getAll().length).toBe(20);

    await ui.setDateRange('2016-01-10', '2016-01-11');
    expect(ui.activityItem.getAll().length).toBe(2);
    await ui.resetDateFilters();

    await ui.setDateRange(undefined, '2016-01-08');
    expect(ui.activityItem.getAll().length).toBe(8);
  });
});

describe('graph interactions', () => {
  it('should allow analyses to be clicked to see details for the analysis', async () => {
    const { ui } = getPageObject();
    renderProjectActivityAppContainer();
    await ui.appLoaded();

    expect(ui.issuesPopupCell.query()).not.toBeInTheDocument();

    await ui.showDetails('1.1.0.1');

    expect(ui.issuesPopupCell.get()).toBeInTheDocument();
  });

  it.each([
    [Mode.MQR, MetricKey.software_quality_maintainability_issues],
    [Mode.Standard, MetricKey.code_smells],
  ])('should correctly handle customizing the graph in %s mode', async (mode, metric) => {
    modeHandler.setMode(mode);
    const { ui } = getPageObject();
    renderProjectActivityAppContainer();
    await ui.appLoaded();

    await ui.changeGraphType(GraphType.custom);

    expect(ui.noDataText.get()).toBeInTheDocument();

    // Add metrics.
    await ui.openMetricsDropdown();
    await ui.toggleMetric(metric);
    await ui.toggleMetric(MetricKey.security_hotspots_reviewed);
    await ui.closeMetricsDropdown();

    expect(ui.graphs.getAll()).toHaveLength(2);

    // Remove metrics.
    await ui.openMetricsDropdown();
    await ui.toggleMetric(metric);
    await ui.toggleMetric(MetricKey.security_hotspots_reviewed);
    await ui.closeMetricsDropdown();

    expect(ui.noDataText.get()).toBeInTheDocument();

    await ui.changeGraphType(GraphType.issues);

    expect(ui.graphs.getAll()).toHaveLength(1);
  });
});

describe('ratings', () => {
  it('should combine old and new rating + gaps', async () => {
    timeMachineHandler.setMeasureHistory([
      mockMeasureHistory({
        metric: MetricKey.reliability_rating,
        history: [
          mockHistoryItem({
            value: '5',
            date: new Date('2022-01-11'),
          }),
          mockHistoryItem({
            value: '2',
            date: new Date('2022-01-12'),
          }),
          mockHistoryItem({
            value: '2',
            date: new Date('2022-01-13'),
          }),
          mockHistoryItem({
            value: '2',
            date: new Date('2022-01-14'),
          }),
        ],
      }),
      mockMeasureHistory({
        metric: MetricKey.software_quality_reliability_rating,
        history: [
          mockHistoryItem({
            value: undefined,
            date: new Date('2022-01-11'),
          }),
          mockHistoryItem({
            value: '3',
            date: new Date('2022-01-12'),
          }),
          mockHistoryItem({
            value: undefined,
            date: new Date('2022-01-13'),
          }),
          mockHistoryItem({
            value: '3',
            date: new Date('2022-01-14'),
          }),
        ],
      }),
    ]);
    const { ui } = getPageObject();
    renderProjectActivityAppContainer();

    await ui.changeGraphType(GraphType.custom);
    await ui.openMetricsDropdown();
    await ui.toggleMetric(MetricKey.software_quality_reliability_rating);
    await ui.closeMetricsDropdown();

    expect(await ui.graphs.findAll()).toHaveLength(1);
    expect(ui.metricChangedInfoBtn.get()).toBeInTheDocument();
    expect(ui.gapInfoMessage.get()).toBeInTheDocument();
  });

  it('should not show old rating if new one was always there', async () => {
    timeMachineHandler.setMeasureHistory([
      mockMeasureHistory({
        metric: MetricKey.reliability_rating,
        history: [
          mockHistoryItem({
            value: '5',
            date: new Date('2022-01-11'),
          }),
          mockHistoryItem({
            value: '2',
            date: new Date('2022-01-12'),
          }),
        ],
      }),
      mockMeasureHistory({
        metric: MetricKey.software_quality_reliability_rating,
        history: [
          mockHistoryItem({
            value: '4',
            date: new Date('2022-01-11'),
          }),
          mockHistoryItem({
            value: '3',
            date: new Date('2022-01-12'),
          }),
        ],
      }),
    ]);
    const { ui } = getPageObject();
    renderProjectActivityAppContainer();

    await ui.changeGraphType(GraphType.custom);
    await ui.openMetricsDropdown();
    await ui.toggleMetric(MetricKey.software_quality_reliability_rating);
    await ui.closeMetricsDropdown();

    expect(await ui.graphs.findAll()).toHaveLength(1);
    expect(ui.metricChangedInfoBtn.query()).not.toBeInTheDocument();
    expect(ui.gapInfoMessage.query()).not.toBeInTheDocument();
  });

  it('should not show change info button if no new metrics', async () => {
    timeMachineHandler.setMeasureHistory([
      mockMeasureHistory({
        metric: MetricKey.reliability_rating,
        history: [
          mockHistoryItem({
            value: '5',
            date: new Date('2022-01-11'),
          }),
          mockHistoryItem({
            value: '2',
            date: new Date('2022-01-12'),
          }),
          mockHistoryItem({
            value: '2',
            date: new Date('2022-01-13'),
          }),
        ],
      }),
    ]);
    const { ui } = getPageObject();
    renderProjectActivityAppContainer();

    await ui.changeGraphType(GraphType.custom);
    await ui.openMetricsDropdown();
    await ui.toggleMetric(MetricKey.software_quality_reliability_rating);
    await ui.closeMetricsDropdown();

    expect(await ui.graphs.findAll()).toHaveLength(1);
    expect(ui.metricChangedInfoBtn.query()).not.toBeInTheDocument();
    expect(ui.gapInfoMessage.query()).not.toBeInTheDocument();
  });

  it('should not show gaps message and metric change button in legacy mode', async () => {
    modeHandler.setMode(Mode.Standard);
    timeMachineHandler.setMeasureHistory([
      mockMeasureHistory({
        metric: MetricKey.reliability_rating,
        history: [
          mockHistoryItem({
            value: '5',
            date: new Date('2022-01-11'),
          }),
          mockHistoryItem({
            value: '2',
            date: new Date('2022-01-12'),
          }),
          mockHistoryItem({
            value: '2',
            date: new Date('2022-01-13'),
          }),
          mockHistoryItem({
            value: '2',
            date: new Date('2022-01-14'),
          }),
        ],
      }),
      mockMeasureHistory({
        metric: MetricKey.software_quality_reliability_rating,
        history: [
          mockHistoryItem({
            value: undefined,
            date: new Date('2022-01-11'),
          }),
          mockHistoryItem({
            value: '4',
            date: new Date('2022-01-12'),
          }),
          mockHistoryItem({
            value: undefined,
            date: new Date('2022-01-13'),
          }),
          mockHistoryItem({
            value: '3',
            date: new Date('2022-01-14'),
          }),
        ],
      }),
    ]);
    const { ui } = getPageObject();
    renderProjectActivityAppContainer();

    await ui.changeGraphType(GraphType.custom);
    await ui.openMetricsDropdown();
    await ui.toggleMetric(MetricKey.reliability_rating);
    await ui.closeMetricsDropdown();

    expect(await ui.graphs.findAll()).toHaveLength(1);
    expect(ui.metricChangedInfoBtn.query()).not.toBeInTheDocument();
    expect(ui.gapInfoMessage.query()).not.toBeInTheDocument();
  });
});

function getPageObject() {
  const user = userEvent.setup();

  const ui = {
    // Graph types.
    graphTypeSelect: byLabelText('project_activity.graphs.choose_type'),

    // Graphs.
    graphs: byLabelText('project_activity.graphs.explanation_x', { exact: false }),
    noDataText: byText('project_activity.graphs.custom.no_history'),
    gapInfoMessage: byText('project_activity.graphs.data_table.data_gap', { exact: false }),
    metricChangedInfoBtn: byRole('button', {
      name: 'project_activity.graphs.rating_split.info_icon',
    }),

    // Add metrics.
    addMetricBtn: byRole('button', { name: 'project_activity.graphs.custom.add' }),
    metricCheckbox: (name: MetricKey) =>
      byRole('checkbox', {
        name: DEPRECATED_ACTIVITY_METRICS.includes(name) ? `${name} (deprecated)` : name,
      }),

    // Graph legend.
    newCodeLegend: byText('hotspot.filters.period.since_leak_period'),

    // Filtering.
    categorySelect: byLabelText('project_activity.filter_events'),
    resetDatesBtn: byRole('button', { name: 'project_activity.reset_dates' }),
    fromDateInput: byLabelText('start_date'),
    toDateInput: byLabelText('end_date'),

    // Analysis interactions.
    activityItem: byLabelText(/project_activity.show_analysis_X_on_graph/),
    cogBtn: (id: string) => byRole('button', { name: `project_activity.analysis_X_actions.${id}` }),
    seeDetailsBtn: (time: string) =>
      byLabelText(`project_activity.show_analysis_X_on_graph.${time}`),
    addCustomEventBtn: byRole('menuitem', { name: 'project_activity.add_custom_event' }),
    addVersionEvenBtn: byRole('menuitem', { name: 'project_activity.add_version' }),
    deleteAnalysisBtn: byRole('menuitem', { name: 'project_activity.delete_analysis' }),
    editEventBtn: (event: string) =>
      byRole('button', { name: `project_activity.events.tooltip.edit.${event}` }),
    deleteEventBtn: (event: string) =>
      byRole('button', { name: `project_activity.events.tooltip.delete.${event}` }),

    // Event modal.
    nameInput: byLabelText('name'),
    saveBtn: byRole('button', { name: 'save' }),
    changeBtn: byRole('button', { name: 'change_verb' }),
    deleteBtn: byRole('button', { name: 'delete' }),

    // Misc.
    loading: byText('loading'),
    baseline: byText('project_activity.new_code_period_start'),
    issuesPopupCell: byRole('cell', { name: `metric.${MetricKey.violations}.name` }),
    monthSelector: byTestId('month-select'),
    yearSelector: byTestId('year-select'),
  };

  return {
    user,
    ui: {
      ...ui,
      async appLoaded({ doNotWaitForBranch }: { doNotWaitForBranch?: boolean } = {}) {
        await waitFor(() => {
          expect(byText('loading').query()).not.toBeInTheDocument();
        });

        expect(await ui.graphs.findAll()).toHaveLength(1);

        if (!doNotWaitForBranch) {
          await waitFor(() => {
            expect(isBranchReady).toBe(true);
          });
        }
      },

      async changeGraphType(type: GraphType) {
        await user.click(await ui.graphTypeSelect.find());
        const optionForType = await screen.findByText(`project_activity.graphs.${type}`);
        await user.click(optionForType);
      },

      async openMetricsDropdown() {
        await user.click(ui.addMetricBtn.get());
      },

      async toggleMetric(metric: MetricKey) {
        await user.click(ui.metricCheckbox(metric).get());
      },

      async closeMetricsDropdown() {
        await user.keyboard('{Escape}');
      },

      async openCogMenu(id: string) {
        await user.click(ui.cogBtn(id).get());
      },

      async deleteAnalysis(id: string) {
        await user.click(ui.cogBtn(id).get());
        await user.click(ui.deleteAnalysisBtn.get());
        await user.click(ui.deleteBtn.get());
      },

      async addVersionEvent(id: string, value: string) {
        await user.click(ui.cogBtn(id).get());
        await user.click(ui.addVersionEvenBtn.get());
        await user.type(ui.nameInput.get(), value);
        await user.click(ui.saveBtn.get());
      },

      async addCustomEvent(id: string, value: string) {
        await user.click(ui.cogBtn(id).get());
        await user.click(ui.addCustomEventBtn.get());
        await user.type(ui.nameInput.get(), value);
        await user.click(ui.saveBtn.get());
      },

      async updateEvent(event: string, value: string) {
        await user.click(ui.editEventBtn(event).get());
        await user.clear(ui.nameInput.get());
        await user.type(ui.nameInput.get(), value);
        await user.click(ui.changeBtn.get());
      },

      async deleteEvent(event: string) {
        await user.click(ui.deleteEventBtn(event).get());
        await user.click(ui.deleteBtn.get());
      },

      async showDetails(id: string) {
        await user.click(ui.seeDetailsBtn(id).get());
      },

      async filterByCategory(
        category: ProjectAnalysisEventCategory | ApplicationAnalysisEventCategory,
      ) {
        await user.click(ui.categorySelect.get());
        const optionForType = await screen.findByText(`event.category.${category}`);
        await user.click(optionForType);
      },

      async setDateRange(from?: string, to?: string) {
        if (from) {
          await this.selectDate(from, ui.fromDateInput.get());
        }

        if (to) {
          await this.selectDate(to, ui.toDateInput.get());
        }
      },

      async selectDate(date: string, datePickerSelector: HTMLElement) {
        const monthMap = [
          'Jan',
          'Feb',
          'Mar',
          'Apr',
          'May',
          'Jun',
          'Jul',
          'Aug',
          'Sep',
          'Oct',
          'Nov',
          'Dec',
        ];
        const parsedDate = parseDate(date);
        await user.click(datePickerSelector);
        const monthSelector = within(ui.monthSelector.get()).getByRole('combobox');

        await user.click(monthSelector);
        const selectedMonthElements = within(ui.monthSelector.get()).getAllByText(
          monthMap[parseDate(parsedDate).getMonth()],
        );
        await user.click(selectedMonthElements[selectedMonthElements.length - 1]);

        const yearSelector = within(ui.yearSelector.get()).getByRole('combobox');

        await user.click(yearSelector);
        const selectedYearElements = within(ui.yearSelector.get()).getAllByText(
          parseDate(parsedDate).getFullYear(),
        );
        await user.click(selectedYearElements[selectedYearElements.length - 1]);

        await user.click(
          screen.getByText(parseDate(parsedDate).getDate().toString(), { selector: 'button' }),
        );
      },

      async resetDateFilters() {
        await user.click(ui.resetDatesBtn.get());
      },
    },
  };
}

function renderProjectActivityAppContainer(
  component = mockComponent({
    breadcrumbs: [{ key: 'breadcrumb', name: 'breadcrumb', qualifier: ComponentQualifier.Project }],
  }),
) {
  return renderAppWithComponentContext(
    `project/activity?id=${component.key}`,
    () => <Route path="*" element={<ProjectActivityAppContainer />} />,
    {
      metrics: keyBy(
        [
          mockMetric({
            key: MetricKey.software_quality_maintainability_issues,
            type: MetricType.Integer,
          }),
          mockMetric({ key: MetricKey.bugs, type: MetricType.Integer }),
          mockMetric({ key: MetricKey.code_smells, type: MetricType.Integer }),
          mockMetric({ key: MetricKey.security_hotspots_reviewed }),
          mockMetric({ key: MetricKey.security_review_rating, type: MetricType.Rating }),
          mockMetric({ key: MetricKey.reliability_rating, type: MetricType.Rating }),
          mockMetric({
            key: MetricKey.software_quality_reliability_rating,
            type: MetricType.Rating,
          }),
        ],
        'key',
      ),
    },
    { component },
  );
}
