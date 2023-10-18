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
import { screen, waitFor } from '@testing-library/react';
import * as React from 'react';
import { getQualityGateProjectStatus } from '../../../../api/quality-gates';
import CurrentUserContextProvider from '../../../../app/components/current-user/CurrentUserContextProvider';
import { mockPullRequest } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockQualityGateProjectCondition } from '../../../../helpers/mocks/quality-gates';
import { mockLoggedInUser, mockMeasure, mockMetric } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { byLabelText, byRole } from '../../../../helpers/testSelector';
import { ComponentPropsType } from '../../../../helpers/testUtils';
import { ComponentQualifier } from '../../../../types/component';
import { MetricKey, MetricType } from '../../../../types/metrics';
import { CaycStatus } from '../../../../types/types';
import PullRequestOverview from '../PullRequestOverview';

jest.mock('../../../../api/measures', () => {
  return {
    ...jest.requireActual('../../../../types/metrics'),
    getMeasuresWithMetrics: jest.fn().mockResolvedValue({
      component: {
        key: '',
        name: '',
        qualifier: ComponentQualifier.Project,
        measures: [
          mockMeasure({
            metric: MetricKey.new_coverage,
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
        ],
      },
      metrics: [
        mockMetric({ key: MetricKey.new_coverage }),
        mockMetric({
          key: MetricKey.duplicated_lines,
        }),
        mockMetric({ key: MetricKey.new_lines, type: MetricType.ShortInteger }),
        mockMetric({
          key: MetricKey.new_bugs,
          type: MetricType.Integer,
        }),
      ],
    }),
  };
});

jest.mock('../../../../api/quality-gates', () => {
  const { mockQualityGateProjectStatus, mockQualityGateApplicationStatus } = jest.requireActual(
    '../../../../helpers/mocks/quality-gates',
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
      }),
    ),
    getApplicationQualityGate: jest.fn().mockResolvedValue(mockQualityGateApplicationStatus()),
  };
});

it('should render correctly for a passed QG', async () => {
  jest.mocked(getQualityGateProjectStatus).mockResolvedValueOnce({
    status: 'OK',
    conditions: [],
    caycStatus: CaycStatus.Compliant,
    ignoredConditions: false,
  });
  renderPullRequestOverview();

  await waitFor(async () => expect(await screen.findByText('metric.level.OK')).toBeInTheDocument());
  expect(screen.getByLabelText('overview.quality_gate_x.overview.gate.OK')).toBeInTheDocument();

  expect(screen.getByText('metric.new_lines.name')).toBeInTheDocument();
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

  await waitFor(async () =>
    expect(
      await byLabelText('overview.quality_gate_x.overview.gate.ERROR').find(),
    ).toBeInTheDocument(),
  );

  expect(
    byRole('link', {
      name: 'overview.failed_condition.x_required 10.0% duplicated_lines ≤ 1.0%',
    }).get(),
  ).toBeInTheDocument();
  expect(
    byRole('link', {
      name: 'overview.failed_condition.x_required 10 new_bugs ≤ 3',
    }).get(),
  ).toBeInTheDocument();
});

function renderPullRequestOverview(
  props: Partial<ComponentPropsType<typeof PullRequestOverview>> = {},
) {
  renderComponent(
    <CurrentUserContextProvider currentUser={mockLoggedInUser()}>
      <PullRequestOverview
        branchLike={mockPullRequest()}
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
