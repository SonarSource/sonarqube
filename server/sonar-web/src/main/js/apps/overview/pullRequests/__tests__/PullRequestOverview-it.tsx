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
import { screen } from '@testing-library/react';
import * as React from 'react';
import CurrentUserContextProvider from '../../../../app/components/current-user/CurrentUserContextProvider';
import { mockPullRequest } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockQualityGateStatusCondition } from '../../../../helpers/mocks/quality-gates';
import { mockLoggedInUser, mockMetric, mockPeriod } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { ComponentQualifier } from '../../../../types/component';
import { MetricKey } from '../../../../types/metrics';
import { PullRequestOverview } from '../PullRequestOverview';

jest.mock('../../../../api/measures', () => {
  return {
    ...jest.requireActual('../../../../types/metrics'),
    getMeasuresWithMetrics: jest.fn().mockResolvedValue({
      component: {
        key: '',
        name: '',
        qualifier: ComponentQualifier.Project,
        measures: [
          mockQualityGateStatusCondition({
            error: '1.0',
            metric: MetricKey.new_coverage,
            period: 1,
          }),
          mockQualityGateStatusCondition({
            error: '1.0',
            metric: MetricKey.duplicated_lines,
            period: 1,
          }),
          mockQualityGateStatusCondition({
            error: '3',
            metric: MetricKey.new_bugs,
            period: 1,
          }),
        ],
      },
      metrics: [
        mockMetric({ key: MetricKey.new_coverage }),
        mockMetric({
          key: MetricKey.duplicated_lines,
        }),
        mockMetric({
          key: MetricKey.new_bugs,
          type: 'INT',
        }),
      ],
      period: mockPeriod(),
    }),
  };
});

jest.mock('../../../../api/quality-gates', () => {
  const { mockQualityGateProjectStatus, mockQualityGateApplicationStatus } = jest.requireActual(
    '../../../../helpers/mocks/quality-gates'
  );
  const { MetricKey } = jest.requireActual('../../../../types/metrics');
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
      })
    ),
    getApplicationQualityGate: jest.fn().mockResolvedValue(mockQualityGateApplicationStatus()),
  };
});

it('should render correctly for a passed QG', async () => {
  renderPullRequestOverview({ status: 'OK', conditions: [] });

  expect(await screen.findByText('metric.level.OK')).toBeInTheDocument();
  expect(screen.queryByText('overview.failed_conditions')).not.toBeInTheDocument();
});

it('should render correctly if conditions are ignored', async () => {
  renderPullRequestOverview({ conditions: [], ignoredConditions: true });

  expect(await screen.findByText('overview.quality_gate.ignored_conditions')).toBeInTheDocument();
});

it('should render correctly for a failed QG', async () => {
  renderPullRequestOverview({
    status: 'ERROR',
    conditions: [
      mockQualityGateStatusCondition({
        error: '2.0',
        metric: MetricKey.new_coverage,
        period: 1,
      }),
      mockQualityGateStatusCondition({
        error: '1.0',
        metric: MetricKey.duplicated_lines,
        period: 1,
      }),
      mockQualityGateStatusCondition({
        error: '3',
        metric: MetricKey.new_bugs,
        period: 1,
      }),
    ],
  });

  expect(await screen.findByText('metric.level.ERROR')).toBeInTheDocument();

  expect(await screen.findByText('overview.failed_conditions')).toBeInTheDocument();

  expect(await screen.findByText('metric.new_coverage.name')).toBeInTheDocument();
  expect(await screen.findByText('quality_gates.operator.GT 2.0%')).toBeInTheDocument();

  expect(await screen.findByText('metric.duplicated_lines.name')).toBeInTheDocument();
  expect(await screen.findByText('quality_gates.operator.GT 1.0%')).toBeInTheDocument();

  expect(screen.getByText('quality_gates.operator.GT 3')).toBeInTheDocument();
});

function renderPullRequestOverview(props: Partial<PullRequestOverview['props']> = {}) {
  renderComponent(
    <CurrentUserContextProvider currentUser={mockLoggedInUser()}>
      <PullRequestOverview
        fetchBranchStatus={jest.fn()}
        branchLike={mockPullRequest()}
        component={mockComponent({
          breadcrumbs: [mockComponent({ key: 'foo' })],
          key: 'foo',
          name: 'Foo',
        })}
        {...props}
      />
    </CurrentUserContextProvider>
  );
}
