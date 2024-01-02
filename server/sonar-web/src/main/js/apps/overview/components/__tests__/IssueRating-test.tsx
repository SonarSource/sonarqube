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
import { shallow } from 'enzyme';
import * as React from 'react';
import { mockPullRequest } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockMeasureEnhanced, mockMetric } from '../../../../helpers/testMocks';
import { IssueType } from '../../../../types/issues';
import { MetricKey } from '../../../../types/metrics';
import { IssueRating, IssueRatingProps } from '../IssueRating';

it('should render correctly for bugs', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ useDiffMetric: true })).toMatchSnapshot();
});

it('should render correctly for code smells', () => {
  expect(shallowRender({ type: IssueType.CodeSmell })).toMatchSnapshot();
  expect(shallowRender({ type: IssueType.CodeSmell, useDiffMetric: true })).toMatchSnapshot();
});

it('should render correctly for vulnerabilities', () => {
  expect(shallowRender({ type: IssueType.Vulnerability })).toMatchSnapshot();
  expect(shallowRender({ type: IssueType.Vulnerability, useDiffMetric: true })).toMatchSnapshot();
});

it('should render correctly if no values are present', () => {
  expect(
    shallowRender({
      measures: [mockMeasureEnhanced({ metric: mockMetric({ key: 'NONE' }) })],
    })
  ).toMatchSnapshot();
});

function shallowRender(props: Partial<IssueRatingProps> = {}) {
  return shallow(
    <IssueRating
      branchLike={mockPullRequest()}
      component={mockComponent()}
      measures={[
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_reliability_rating }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.reliability_rating }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_maintainability_rating }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.sqale_rating }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_security_rating }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.security_rating }) }),
      ]}
      type={IssueType.Bug}
      {...props}
    />
  );
}
