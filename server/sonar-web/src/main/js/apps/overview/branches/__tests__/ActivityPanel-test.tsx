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
import GraphsHistory from '../../../../components/activity-graph/GraphsHistory';
import { parseDate } from '../../../../helpers/dates';
import { mockMainBranch } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockAnalysis, mockAnalysisEvent } from '../../../../helpers/mocks/project-activity';
import { mockMeasure, mockMetric } from '../../../../helpers/testMocks';
import { GraphType } from '../../../../types/project-activity';
import { ActivityPanel, ActivityPanelProps } from '../ActivityPanel';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ loading: true, analyses: undefined })).toMatchSnapshot();
});

it('should correctly pass the leak period start date', () => {
  // Leak period start is more recent than the oldest historic measure.
  let { leakPeriodDate } = shallowRender({
    leakPeriodDate: parseDate('2017-08-27T16:33:50+0200'),
  })
    .find(GraphsHistory)
    .props();

  expect(leakPeriodDate!.getTime()).toBe(1503844430000); /* 2017-08-27T16:33:50+0200 */

  // Leak period start is older than the oldest historic measure.
  ({ leakPeriodDate } = shallowRender({ leakPeriodDate: parseDate('2015-08-27T16:33:50+0200') })
    .find(GraphsHistory)
    .props());

  expect(leakPeriodDate!.getTime()).toBe(1477578830000); /* 2016-10-27T16:33:50+0200 */
});

function shallowRender(props: Partial<ActivityPanelProps> = {}) {
  return shallow(
    <ActivityPanel
      analyses={[mockAnalysis({ events: [mockAnalysisEvent()] }), mockAnalysis()]}
      branchLike={mockMainBranch()}
      component={mockComponent()}
      graph={GraphType.issues}
      loading={false}
      measuresHistory={[mockMeasure()].map((m) => ({
        ...m,
        history: [{ date: parseDate('2016-10-27T16:33:50+0200'), value: '20' }],
      }))}
      metrics={[mockMetric({ key: 'bugs' })]}
      onGraphChange={jest.fn()}
      {...props}
    />
  );
}
