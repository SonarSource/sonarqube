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
import * as React from 'react';
import { mockPullRequest } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockMeasureEnhanced, mockMetric } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { ComponentQualifier } from '../../../../types/component';
import { IssueType } from '../../../../types/issues';
import { MetricKey } from '../../../../types/metrics';
import { IssueLabel, IssueLabelProps } from '../IssueLabel';

it('should render correctly for bugs', async () => {
  const measures = [
    mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.bugs }) }),
    mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_bugs }) }),
  ];

  const rtl = renderIssueLabel({ measures });
  expect(
    await screen.findByRole('link', {
      name: 'overview.see_list_of_x_y_issues.1.0.metric.bugs.name',
    }),
  ).toBeInTheDocument();

  rtl.unmount();

  renderIssueLabel({ measures, useDiffMetric: true });

  expect(
    await screen.findByRole('link', {
      name: 'overview.see_list_of_x_y_issues.1.0.metric.new_bugs.name',
    }),
  ).toBeInTheDocument();
});

it('should render correctly for hotspots with tooltip', async () => {
  const helpTooltip = 'tooltip text';
  const type = IssueType.SecurityHotspot;
  const measures = [
    mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.security_hotspots }) }),
    mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_security_hotspots }) }),
  ];

  renderIssueLabel({
    helpTooltip,
    measures,
    type,
  });

  expect(
    await screen.findByRole('link', {
      name: 'overview.see_list_of_x_y_issues.1.0.metric.security_hotspots.name',
    }),
  ).toBeInTheDocument();

  expect(screen.getByText('tooltip text')).toBeInTheDocument();
});

it('should render correctly for a re-indexing Application', () => {
  const type = IssueType.SecurityHotspot;
  const measures = [
    mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.security_hotspots }) }),
    mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_security_hotspots }) }),
  ];

  renderIssueLabel({
    component: mockComponent({ needIssueSync: true, qualifier: ComponentQualifier.Application }),
    measures,
    type,
  });

  expect(
    screen.queryByRole('link', {
      name: 'overview.see_list_of_x_y_issues.1.0.metric.security_hotspots.name',
    }),
  ).not.toBeInTheDocument();
});

function renderIssueLabel(props: Partial<IssueLabelProps> = {}) {
  return renderComponent(
    <IssueLabel
      branchLike={mockPullRequest()}
      component={mockComponent()}
      measures={[]}
      type={IssueType.Bug}
      {...props}
    />,
  );
}
