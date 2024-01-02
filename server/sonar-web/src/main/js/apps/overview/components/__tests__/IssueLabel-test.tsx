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
import { IssueLabel, IssueLabelProps } from '../IssueLabel';

it('should render correctly for bugs', () => {
  const measures = [
    mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.bugs }) }),
    mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_bugs }) }),
  ];
  expect(shallowRender({ measures })).toMatchSnapshot();
  expect(shallowRender({ measures, useDiffMetric: true })).toMatchSnapshot();
});

it('should render correctly for code smells', () => {
  const type = IssueType.CodeSmell;
  const measures = [
    mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.code_smells }) }),
    mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_code_smells }) }),
  ];
  expect(shallowRender({ measures, type })).toMatchSnapshot();
  expect(shallowRender({ measures, type, useDiffMetric: true })).toMatchSnapshot();
});

it('should render correctly for vulnerabilities', () => {
  const type = IssueType.Vulnerability;
  const measures = [
    mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.vulnerabilities }) }),
    mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_vulnerabilities }) }),
  ];
  expect(shallowRender({ measures, type })).toMatchSnapshot();
  expect(shallowRender({ measures, type, useDiffMetric: true })).toMatchSnapshot();
});

it('should render correctly for hotspots', () => {
  const helpTooltip = 'tooltip text';
  const type = IssueType.SecurityHotspot;
  const measures = [
    mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.security_hotspots }) }),
    mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_security_hotspots }) }),
  ];
  expect(
    shallowRender({
      helpTooltip,
      measures,
      type,
    })
  ).toMatchSnapshot();
  expect(
    shallowRender({
      helpTooltip,
      measures,
      type,
      useDiffMetric: true,
    })
  ).toMatchSnapshot();
});

it('should render correctly if no values are present', () => {
  expect(shallowRender()).toMatchSnapshot();
});

function shallowRender(props: Partial<IssueLabelProps> = {}) {
  return shallow(
    <IssueLabel
      branchLike={mockPullRequest()}
      component={mockComponent()}
      measures={[]}
      type={IssueType.Bug}
      {...props}
    />
  );
}
