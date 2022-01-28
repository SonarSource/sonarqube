/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { mockMainBranch } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockMeasureEnhanced, mockMetric } from '../../../../helpers/testMocks';
import { ComponentQualifier } from '../../../../types/component';
import { IssueType } from '../../../../types/issues';
import { MetricKey } from '../../../../types/metrics';
import MeasuresPanelIssueMeasureRow, {
  MeasuresPanelIssueMeasureRowProps
} from '../MeasuresPanelIssueMeasureRow';

it('should render correctly for projects', () => {
  expect(shallowRender({ type: IssueType.Bug })).toMatchSnapshot('Bug');
  expect(shallowRender({ type: IssueType.CodeSmell })).toMatchSnapshot('Code Smell');
  expect(shallowRender({ type: IssueType.SecurityHotspot })).toMatchSnapshot('Hotspot');
  expect(shallowRender({ type: IssueType.Vulnerability })).toMatchSnapshot('Vulnerabilty');
  expect(shallowRender({ isNewCodeTab: false })).toMatchSnapshot('Overview');
});

it('should render correctly for apps', () => {
  const app = mockComponent({ qualifier: ComponentQualifier.Application });

  expect(shallowRender({ component: app })).toMatchSnapshot('new code');
  expect(shallowRender({ component: app, isNewCodeTab: false })).toMatchSnapshot('overview');
});

function shallowRender(props: Partial<MeasuresPanelIssueMeasureRowProps> = {}) {
  return shallow<MeasuresPanelIssueMeasureRowProps>(
    <MeasuresPanelIssueMeasureRow
      branchLike={mockMainBranch()}
      component={mockComponent()}
      isNewCodeTab={true}
      measures={[
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.coverage }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_coverage }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.bugs }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_bugs }) })
      ]}
      type={IssueType.Bug}
      {...props}
    />
  );
}
