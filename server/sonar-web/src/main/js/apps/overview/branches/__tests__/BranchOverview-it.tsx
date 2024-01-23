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
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { getMeasuresWithPeriodAndMetrics } from '../../../../api/measures';
import AlmSettingsServiceMock from '../../../../api/mocks/AlmSettingsServiceMock';
import { QualityGatesServiceMock } from '../../../../api/mocks/QualityGatesServiceMock';
import { getProjectActivity } from '../../../../api/projectActivity';
import { getQualityGateProjectStatus } from '../../../../api/quality-gates';
import CurrentUserContextProvider from '../../../../app/components/current-user/CurrentUserContextProvider';
import { getActivityGraph, saveActivityGraph } from '../../../../components/activity-graph/utils';
import { isDiffMetric } from '../../../../helpers/measures';
import { mockMainBranch } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockAnalysis, mockAnalysisEvent } from '../../../../helpers/mocks/project-activity';
import {
  mockQualityGateApplicationStatus,
  mockQualityGateProjectStatus,
} from '../../../../helpers/mocks/quality-gates';
import { mockLoggedInUser, mockPeriod } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { byRole } from '../../../../helpers/testSelector';
import { ComponentQualifier } from '../../../../types/component';
import { MetricKey, MetricType } from '../../../../types/metrics';
import {
  Analysis,
  GraphType,
  ProjectAnalysisEventCategory,
} from '../../../../types/project-activity';
import { CaycStatus, Measure, Metric, Paging } from '../../../../types/types';
import BranchOverview, { BRANCH_OVERVIEW_ACTIVITY_GRAPH, NO_CI_DETECTED } from '../BranchOverview';

jest.mock('../../../../api/measures', () => {
  const { mockMeasure, mockMetric } = jest.requireActual('../../../../helpers/testMocks');
  return {
    getMeasuresWithPeriodAndMetrics: jest.fn((_, metricKeys: string[]) => {
      const metrics: Metric[] = [];
      const measures: Measure[] = [];
      metricKeys.forEach((key) => {
        if (key === 'unknown_metric') {
          return;
        }

        let type;
        if (/(coverage|duplication)$/.test(key)) {
          type = MetricType.Percent;
        } else if (/_rating$/.test(key)) {
          type = MetricType.Rating;
        } else {
          type = MetricType.Integer;
        }
        metrics.push(mockMetric({ key, id: key, name: key, type }));
        measures.push(
          mockMeasure({
            metric: key,
            ...(isDiffMetric(key) ? { leak: '1' } : { period: undefined }),
          }),
        );
      });
      return Promise.resolve({
        component: {
          measures,
          name: 'foo',
        },
        metrics,
      });
    }),
  };
});

jest.mock('../../../../api/time-machine', () => {
  const { MetricKey } = jest.requireActual('../../../../types/metrics');
  return {
    getAllTimeMachineData: jest.fn().mockResolvedValue({
      measures: [
        { metric: MetricKey.bugs, history: [{ date: '2019-01-05', value: '2.0' }] },
        { metric: MetricKey.vulnerabilities, history: [{ date: '2019-01-05', value: '0' }] },
        { metric: MetricKey.sqale_index, history: [{ date: '2019-01-01', value: '1.0' }] },
        {
          metric: MetricKey.duplicated_lines_density,
          history: [{ date: '2019-01-02', value: '1.0' }],
        },
        { metric: MetricKey.ncloc, history: [{ date: '2019-01-03', value: '10000' }] },
        { metric: MetricKey.coverage, history: [{ date: '2019-01-04', value: '95.5' }] },
      ],
    }),
  };
});

jest.mock('../../../../api/projectActivity', () => {
  const { mockAnalysis } = jest.requireActual('../../../../helpers/mocks/project-activity');
  return {
    getProjectActivity: jest.fn().mockResolvedValue({
      analyses: [
        mockAnalysis({ detectedCI: 'Cirrus CI' }),
        mockAnalysis(),
        mockAnalysis(),
        mockAnalysis(),
        mockAnalysis(),
      ],
    }),
  };
});

jest.mock('../../../../api/application', () => ({
  getApplicationDetails: jest.fn().mockResolvedValue({
    branches: [],
    key: 'key-1',
    name: 'app',
    projects: [
      {
        branch: 'foo',
        key: 'KEY-P1',
        name: 'P1',
      },
    ],
    visibility: 'Private',
  }),
  getApplicationLeak: jest.fn().mockResolvedValue([
    {
      date: '2017-01-05',
      project: 'foo',
      projectName: 'Foo',
    },
  ]),
}));

jest.mock('../../../../components/activity-graph/utils', () => {
  const { MetricKey } = jest.requireActual('../../../../types/metrics');
  const { GraphType } = jest.requireActual('../../../../types/project-activity');
  const original = jest.requireActual('../../../../components/activity-graph/utils');
  return {
    ...original,
    getActivityGraph: jest.fn(() => ({ graph: GraphType.coverage })),
    saveActivityGraph: jest.fn(),
    getHistoryMetrics: jest.fn(() => [MetricKey.lines_to_cover, MetricKey.uncovered_lines]),
  };
});

const almHandler = new AlmSettingsServiceMock();
let qualityGatesMock: QualityGatesServiceMock;

beforeAll(() => {
  qualityGatesMock = new QualityGatesServiceMock();
  qualityGatesMock.setQualityGateProjectStatus(
    mockQualityGateProjectStatus({
      status: 'ERROR',
      conditions: [
        {
          actualValue: '2',
          comparator: 'GT',
          errorThreshold: '1',
          metricKey: MetricKey.new_reliability_rating,
          periodIndex: 1,
          status: 'ERROR',
        },
        {
          actualValue: '5',
          comparator: 'GT',
          errorThreshold: '2.0',
          metricKey: MetricKey.bugs,
          periodIndex: 0,
          status: 'ERROR',
        },
        {
          actualValue: '2',
          comparator: 'GT',
          errorThreshold: '1.0',
          metricKey: 'unknown_metric',
          periodIndex: 0,
          status: 'ERROR',
        },
      ],
    }),
  );
  qualityGatesMock.setApplicationQualityGateStatus(mockQualityGateApplicationStatus());
});

afterEach(() => {
  jest.clearAllMocks();
  qualityGatesMock.reset();
  almHandler.reset();
});

describe('project overview', () => {
  it('should show a successful QG', async () => {
    const user = userEvent.setup();
    qualityGatesMock.setQualityGateProjectStatus(
      mockQualityGateProjectStatus({
        status: 'OK',
      }),
    );
    renderBranchOverview();

    // Meta info
    expect(await screen.findByText('master')).toBeInTheDocument();
    expect(screen.getByText('version-1.0')).toBeInTheDocument();

    // QG panel
    expect(screen.getByText('metric.level.OK')).toBeInTheDocument();
    expect(screen.getByText('overview.passed.clean_code')).toBeInTheDocument();
    expect(
      screen.queryByText('overview.quality_gate.conditions.cayc.warning'),
    ).not.toBeInTheDocument();

    //Measures panel
    expect(screen.getByText('metric.new_vulnerabilities.name')).toBeInTheDocument();
    expect(
      byRole('link', {
        name: 'overview.see_more_details_on_x_of_y.1.metric.accepted_issues.name',
      }).get(),
    ).toBeInTheDocument();

    // go to overall
    await user.click(screen.getByText('overview.overall_code'));

    expect(screen.getByText('metric.vulnerabilities.name')).toBeInTheDocument();
    expect(
      byRole('link', {
        name: 'overview.see_more_details_on_x_of_y.1.metric.high_impact_accepted_issues.name',
      }).get(),
    ).toBeInTheDocument();
  });

  it('should show a successful non-compliant QG', async () => {
    jest
      .mocked(getQualityGateProjectStatus)
      .mockResolvedValueOnce(
        mockQualityGateProjectStatus({ status: 'OK', caycStatus: CaycStatus.NonCompliant }),
      );

    renderBranchOverview();

    expect(await screen.findByText('metric.level.OK')).toBeInTheDocument();
    expect(
      screen.queryByText('overview.quality_gate.conditions.cayc.warning'),
    ).not.toBeInTheDocument();
  });

  it('should show a successful non-compliant QG as admin', async () => {
    jest
      .mocked(getQualityGateProjectStatus)
      .mockResolvedValueOnce(
        mockQualityGateProjectStatus({ status: 'OK', caycStatus: CaycStatus.NonCompliant }),
      );
    qualityGatesMock.setIsAdmin(true);
    qualityGatesMock.setGetGateForProjectName('Non Cayc QG');

    renderBranchOverview();

    await screen.findByText('metric.level.OK');
    expect(
      await screen.findByText('overview.quality_gate.conditions.cayc.warning'),
    ).toBeInTheDocument();
  });

  it('should show a failed QG', async () => {
    qualityGatesMock.setQualityGateProjectStatus(
      mockQualityGateProjectStatus({
        status: 'ERROR',
        conditions: [
          {
            actualValue: '2',
            comparator: 'GT',
            errorThreshold: '1',
            metricKey: MetricKey.new_reliability_rating,
            periodIndex: 1,
            status: 'ERROR',
          },
          {
            actualValue: '5',
            comparator: 'GT',
            errorThreshold: '2.0',
            metricKey: MetricKey.bugs,
            periodIndex: 0,
            status: 'ERROR',
          },
          {
            actualValue: '2',
            comparator: 'GT',
            errorThreshold: '1.0',
            metricKey: 'unknown_metric',
            periodIndex: 0,
            status: 'ERROR',
          },
        ],
      }),
    );

    renderBranchOverview();

    expect(await screen.findByText('metric.level.ERROR')).toBeInTheDocument();
    expect(screen.getAllByText(/overview.X_conditions_failed/)).toHaveLength(2);
  });

  it('should correctly show a project as empty', async () => {
    jest.mocked(getMeasuresWithPeriodAndMetrics).mockResolvedValueOnce({
      component: { key: '', name: '', qualifier: ComponentQualifier.Project, measures: [] },
      metrics: [],
      period: mockPeriod(),
    });

    renderBranchOverview();

    expect(await screen.findByText('overview.project.main_branch_empty')).toBeInTheDocument();
  });
});

describe('application overview', () => {
  const component = mockComponent({
    breadcrumbs: [mockComponent({ key: 'foo', qualifier: ComponentQualifier.Application })],
    qualifier: ComponentQualifier.Application,
  });

  it('should show failed conditions for every project', async () => {
    renderBranchOverview({ component });
    expect(await screen.findByText('Foo')).toBeInTheDocument();
    expect(screen.getByText('Bar')).toBeInTheDocument();
  });

  it("should show projects that don't have a compliant quality gate", async () => {
    const appStatus = mockQualityGateApplicationStatus({
      projects: [
        {
          key: '1',
          name: 'first project',
          conditions: [],
          caycStatus: CaycStatus.NonCompliant,
          status: 'OK',
        },
        {
          key: '2',
          name: 'second',
          conditions: [],
          caycStatus: CaycStatus.Compliant,
          status: 'OK',
        },
        {
          key: '3',
          name: 'number 3',
          conditions: [],
          caycStatus: CaycStatus.NonCompliant,
          status: 'OK',
        },
        {
          key: '4',
          name: 'four',
          conditions: [
            {
              comparator: 'GT',
              metric: MetricKey.bugs,
              status: 'ERROR',
              value: '3',
              errorThreshold: '0',
            },
          ],
          caycStatus: CaycStatus.NonCompliant,
          status: 'ERROR',
        },
      ],
    });
    qualityGatesMock.setApplicationQualityGateStatus(appStatus);

    renderBranchOverview({ component });
    expect(
      await screen.findByText('overview.quality_gate.application.non_cayc.projects_x.3'),
    ).toBeInTheDocument();
    expect(screen.getByText('first project')).toBeInTheDocument();
    expect(screen.queryByText('second')).not.toBeInTheDocument();
    expect(screen.getByText('number 3')).toBeInTheDocument();
  });

  it('should correctly show an app as empty', async () => {
    jest.mocked(getMeasuresWithPeriodAndMetrics).mockResolvedValueOnce({
      component: { key: '', name: '', qualifier: ComponentQualifier.Application, measures: [] },
      metrics: [],
      period: mockPeriod(),
    });

    renderBranchOverview({ component });

    expect(await screen.findByText('portfolio.app.empty')).toBeInTheDocument();
  });
});

it.each([
  ['no analysis', [], true],
  ['1 analysis, no CI data', [mockAnalysis()], false],
  ['1 analysis, no CI detected', [mockAnalysis({ detectedCI: NO_CI_DETECTED })], false],
  ['1 analysis, CI detected', [mockAnalysis({ detectedCI: 'Cirrus CI' })], true],
])(
  "should correctly flag a project that wasn't analyzed using a CI (%s)",
  async (_, analyses, expected) => {
    (getProjectActivity as jest.Mock).mockResolvedValueOnce({ analyses });

    renderBranchOverview();

    // wait for loading
    await screen.findByText('overview.quality_gate.status');

    expect(screen.queryByText('overview.project.next_steps.set_up_ci') === null).toBe(expected);
  },
);

it.each([
  [
    'no upgrade event',
    [
      mockAnalysis({
        events: [mockAnalysisEvent({ category: ProjectAnalysisEventCategory.Other })],
      }),
    ],
    false,
  ],
  [
    'upgrade event too old',
    [
      mockAnalysis({
        date: '2023-04-02T12:10:30+0200',
        events: [mockAnalysisEvent({ category: ProjectAnalysisEventCategory.SqUpgrade })],
      }),
    ],
    false,
  ],
  [
    'upgrade event too far down in the list',
    [
      mockAnalysis({
        date: '2023-04-13T08:10:30+0200',
      }),
      mockAnalysis({
        date: '2023-04-13T09:10:30+0200',
      }),
      mockAnalysis({
        date: '2023-04-13T10:10:30+0200',
      }),
      mockAnalysis({
        date: '2023-04-13T11:10:30+0200',
      }),
      mockAnalysis({
        date: '2023-04-13T12:10:30+0200',
        events: [mockAnalysisEvent({ category: ProjectAnalysisEventCategory.SqUpgrade })],
      }),
    ],
    false,
  ],
  [
    'upgrade event without QP update event',
    [
      mockAnalysis({
        date: '2023-04-13T12:10:30+0200',
        events: [mockAnalysisEvent({ category: ProjectAnalysisEventCategory.SqUpgrade })],
      }),
    ],
    false,
  ],
  [
    'upgrade event with QP update event',
    [
      mockAnalysis({
        date: '2023-04-13T12:10:30+0200',
        events: [
          mockAnalysisEvent({ category: ProjectAnalysisEventCategory.SqUpgrade }),
          mockAnalysisEvent({ category: ProjectAnalysisEventCategory.QualityProfile }),
        ],
      }),
    ],
    true,
  ],
])(
  'should correctly display message about SQ upgrade updating QPs',
  async (_, analyses, expected) => {
    jest.useFakeTimers({
      advanceTimers: true,
      now: new Date('2023-04-25T12:00:00+0200'),
    });

    jest.mocked(getProjectActivity).mockResolvedValueOnce({
      analyses,
    } as { analyses: Analysis[]; paging: Paging });

    renderBranchOverview();

    await screen.findByText('overview.quality_gate.status');

    await waitFor(() =>
      expect(
        screen.queryByText(/overview.quality_profiles_update_after_sq_upgrade.message/) !== null,
      ).toBe(expected),
    );

    jest.useRealTimers();
  },
);

it('should correctly handle graph type storage', async () => {
  renderBranchOverview();

  expect(getActivityGraph).toHaveBeenCalledWith(BRANCH_OVERVIEW_ACTIVITY_GRAPH, 'foo');

  const dropdownButton = await screen.findByLabelText('project_activity.graphs.choose_type');

  await userEvent.click(dropdownButton);

  const issuesItem = await screen.findByRole('menuitem', {
    name: `project_activity.graphs.${GraphType.issues}`,
  });

  expect(issuesItem).toBeInTheDocument();

  await userEvent.click(issuesItem);

  expect(saveActivityGraph).toHaveBeenCalledWith(
    BRANCH_OVERVIEW_ACTIVITY_GRAPH,
    'foo',
    GraphType.issues,
  );
});

function renderBranchOverview(props: Partial<BranchOverview['props']> = {}) {
  return renderComponent(
    <CurrentUserContextProvider currentUser={mockLoggedInUser()}>
      <BranchOverview
        branch={mockMainBranch()}
        component={mockComponent({
          breadcrumbs: [mockComponent({ key: 'foo' })],
          key: 'foo',
          name: 'Foo',
          version: 'version-1.0',
        })}
        {...props}
      />
    </CurrentUserContextProvider>,
  );
}
