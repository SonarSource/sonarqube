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
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import { MetricKey } from '~sonar-aligned/types/metrics';
import BranchesServiceMock from '../../../../api/mocks/BranchesServiceMock';
import { fetchQualityGate, getQualityGateProjectStatus } from '../../../../api/quality-gates';
import CurrentUserContextProvider from '../../../../app/components/current-user/CurrentUserContextProvider';
import { mockPullRequest } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import {
  mockQualityGate,
  mockQualityGateProjectCondition,
} from '../../../../helpers/mocks/quality-gates';
import { mockLoggedInUser } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { ComponentPropsType } from '../../../../helpers/testUtils';
import { CaycStatus } from '../../../../types/types';
import { NoticeType } from '../../../../types/users';
import PullRequestOverview from '../PullRequestOverview';

jest.mock('../../../../api/ce', () => ({
  getAnalysisStatus: jest.fn().mockResolvedValue({ component: { warnings: [] } }),
}));

jest.mock('../../../../api/measures', () => {
  const { mockMeasure, mockMetric } = jest.requireActual('../../../../helpers/testMocks');
  const { ComponentQualifier } = jest.requireActual('~sonar-aligned/types/component');
  const { MetricKey, MetricType } = jest.requireActual('~sonar-aligned/types/metrics');
  return {
    ...jest.requireActual('../../../../sonar-aligned/types/metrics'),
    getMeasuresWithPeriodAndMetrics: jest.fn().mockResolvedValue({
      component: {
        key: '',
        name: '',
        qualifier: ComponentQualifier.Project,
        measures: [
          mockMeasure({
            metric: MetricKey.new_coverage,
          }),
          mockMeasure({
            metric: MetricKey.coverage,
          }),
          mockMeasure({
            metric: MetricKey.duplicated_lines,
          }),
          mockMeasure({
            metric: MetricKey.new_bugs,
          }),
          mockMeasure({
            metric: MetricKey.new_lines,
          }),
          mockMeasure({
            metric: MetricKey.new_violations,
          }),
          mockMeasure({
            metric: MetricKey.pull_request_fixed_issues,
          }),
        ],
      },
      metrics: [
        mockMetric({ key: MetricKey.new_coverage }),
        mockMetric({ key: MetricKey.coverage }),
        mockMetric({ key: MetricKey.duplicated_lines }),
        mockMetric({ key: MetricKey.new_lines, type: MetricType.ShortInteger }),
        mockMetric({ key: MetricKey.new_bugs, type: MetricType.Integer }),
        mockMetric({ key: MetricKey.new_violations }),
        mockMetric({ key: MetricKey.pull_request_fixed_issues }),
      ],
    }),
  };
});

jest.mock('../../../../api/quality-gates', () => {
  const { mockQualityGateProjectStatus, mockQualityGateApplicationStatus, mockQualityGate } =
    jest.requireActual('../../../../helpers/mocks/quality-gates');
  const { MetricKey } = jest.requireActual('../../../../sonar-aligned/types/metrics');
  return {
    getQualityGateProjectStatus: jest.fn().mockResolvedValue(
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
    ),
    getApplicationQualityGate: jest.fn().mockResolvedValue(mockQualityGateApplicationStatus()),
    getGateForProject: jest.fn().mockResolvedValue(mockQualityGate({ isBuiltIn: true })),
    fetchQualityGate: jest.fn().mockResolvedValue(mockQualityGate({ isBuiltIn: true })),
  };
});

const branchesHandler = new BranchesServiceMock();

afterEach(() => {
  branchesHandler.reset();
});

it('should render links correctly', async () => {
  jest.mocked(getQualityGateProjectStatus).mockResolvedValueOnce({
    status: 'OK',
    conditions: [],
    caycStatus: CaycStatus.Compliant,
    ignoredConditions: false,
  });
  renderPullRequestOverview();

  await waitFor(async () => expect(await screen.findByText('metric.level.OK')).toBeInTheDocument());
  expect(screen.getByText('metric.level.OK')).toBeInTheDocument();

  expect(
    byRole('link', {
      name: 'overview.see_more_details_on_x_of_y.1.metric.new_violations.name',
    }).get(),
  ).toHaveAttribute(
    'href',
    '/project/issues?pullRequest=1001&issueStatuses=OPEN%2CCONFIRMED&id=foo',
  );

  expect(
    byRole('link', {
      name: 'overview.see_more_details_on_x_of_y.1.metric.pull_request_fixed_issues.name',
    }).get(),
  ).toHaveAttribute('href', '/project/issues?fixedInPullRequest=1001&id=foo');

  expect(
    screen.getByRole('link', { name: 'no_measure_value_x.metric.new_security_hotspots.name' }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('link', { name: 'no_measure_value_x.metric.new_accepted_issues.name' }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('link', {
      name: 'no_measure_value_x.metric.new_duplicated_lines_density.name',
    }),
  ).toBeInTheDocument();
});

it('should render correctly for a passed QG', async () => {
  jest.mocked(getQualityGateProjectStatus).mockResolvedValueOnce({
    status: 'OK',
    conditions: [],
    caycStatus: CaycStatus.Compliant,
    ignoredConditions: false,
  });
  renderPullRequestOverview();

  expect(await screen.findByText('metric.level.OK')).toBeInTheDocument();

  expect(screen.getByText('metric.new_lines.name')).toBeInTheDocument();
  expect(
    screen.getByText('component_measures.facet_category.overall_category.estimated'),
  ).toBeInTheDocument();
  expect(screen.getByText(/overview.last_analysis_x/)).toBeInTheDocument();
});

it('should render correctly if conditions are ignored', async () => {
  jest.mocked(getQualityGateProjectStatus).mockResolvedValueOnce({
    status: 'OK',
    conditions: [],
    caycStatus: CaycStatus.Compliant,
    ignoredConditions: true,
  });
  renderPullRequestOverview();

  await waitFor(async () =>
    expect(await screen.findByText('overview.quality_gate.ignored_conditions')).toBeInTheDocument(),
  );
});

it('should render correctly for a failed QG', async () => {
  jest.mocked(getQualityGateProjectStatus).mockResolvedValueOnce({
    status: 'ERROR',
    conditions: [
      mockQualityGateProjectCondition({
        errorThreshold: '2.0',
        metricKey: MetricKey.new_coverage,
        periodIndex: 1,
      }),
      mockQualityGateProjectCondition({
        errorThreshold: '1.0',
        metricKey: MetricKey.duplicated_lines,
        periodIndex: 1,
      }),
      mockQualityGateProjectCondition({
        errorThreshold: '3',
        metricKey: MetricKey.new_bugs,
        periodIndex: 1,
      }),
    ],
    caycStatus: CaycStatus.Compliant,
    ignoredConditions: true,
  });
  renderPullRequestOverview();

  expect(await byText('metric.level.ERROR').find()).toBeInTheDocument();

  expect(
    byRole('link', {
      name: '1 1 new_bugs quality_gates.operator.GT 3',
    }).get(),
  ).toBeInTheDocument();
  expect(
    byRole('link', {
      name: '1.0% new_coverage quality_gates.operator.GT 2.0%',
    }).get(),
  ).toBeInTheDocument();
});

it('renders SL promotion', async () => {
  const user = userEvent.setup();
  jest.mocked(getQualityGateProjectStatus).mockResolvedValueOnce({
    status: 'ERROR',
    conditions: [
      mockQualityGateProjectCondition({
        errorThreshold: '2.0',
        metricKey: MetricKey.new_coverage,
        periodIndex: 1,
      }),
    ],
    caycStatus: CaycStatus.Compliant,
    ignoredConditions: true,
  });
  renderPullRequestOverview();

  await waitFor(async () =>
    expect(
      await byRole('heading', { name: 'overview.sonarlint_ad.header' }).find(),
    ).toBeInTheDocument(),
  );

  // Close promotion
  await user.click(byRole('button', { name: 'overview.sonarlint_ad.close_promotion' }).get());

  expect(
    byRole('heading', { name: 'overview.sonarlint_ad.header' }).query(),
  ).not.toBeInTheDocument();
});

it('should render correctly 0 New issues onboarding', async () => {
  jest.mocked(getQualityGateProjectStatus).mockResolvedValueOnce({
    status: 'ERROR',
    conditions: [
      mockQualityGateProjectCondition({
        status: 'ERROR',
        errorThreshold: '0',
        metricKey: MetricKey.new_violations,
        actualValue: '1',
      }),
    ],
    caycStatus: CaycStatus.Compliant,
    ignoredConditions: false,
  });
  jest.mocked(fetchQualityGate).mockResolvedValueOnce(mockQualityGate({ isBuiltIn: true }));

  renderPullRequestOverview();

  expect(await byText('metric.level.ERROR').find()).toBeInTheDocument();
  expect(await byRole('alertdialog').find()).toBeInTheDocument();
});

it('should not render 0 New issues onboarding when user dismissed it', async () => {
  jest.mocked(getQualityGateProjectStatus).mockResolvedValueOnce({
    status: 'ERROR',
    conditions: [
      mockQualityGateProjectCondition({
        status: 'ERROR',
        errorThreshold: '0',
        metricKey: MetricKey.new_violations,
        actualValue: '1',
      }),
    ],
    caycStatus: CaycStatus.Compliant,
    ignoredConditions: false,
  });
  jest.mocked(fetchQualityGate).mockResolvedValueOnce(mockQualityGate({ isBuiltIn: true }));

  renderPullRequestOverview(
    {},
    mockLoggedInUser({
      dismissedNotices: { [NoticeType.OVERVIEW_ZERO_NEW_ISSUES_SIMPLIFICATION]: true },
    }),
  );

  expect(await byText('metric.level.ERROR').find()).toBeInTheDocument();

  expect(await byRole('alertdialog').query()).not.toBeInTheDocument();
});

function renderPullRequestOverview(
  props: Partial<ComponentPropsType<typeof PullRequestOverview>> = {},
  currentUser = mockLoggedInUser(),
) {
  renderComponent(
    <CurrentUserContextProvider currentUser={currentUser}>
      <PullRequestOverview
        pullRequest={mockPullRequest()}
        component={mockComponent({
          breadcrumbs: [mockComponent({ key: 'foo' })],
          key: 'foo',
          name: 'Foo',
        })}
        {...props}
      />
    </CurrentUserContextProvider>,
  );
}
