/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import BoxedTabs from 'sonar-ui-common/components/controls/BoxedTabs';
import { mockMainBranch } from '../../../../helpers/mocks/branch-like';
import { mockComponent, mockMeasureEnhanced, mockMetric } from '../../../../helpers/testMocks';
import { ComponentQualifier } from '../../../../types/component';
import { MetricKey } from '../../../../types/metrics';
import { MeasuresPanel, MeasuresPanelProps, MeasuresPanelTabs } from '../MeasuresPanel';

it('should render correctly for projects', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  wrapper.find(BoxedTabs).prop<Function>('onSelect')(MeasuresPanelTabs.Overall);
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly for applications', () => {
  const wrapper = shallowRender({
    component: mockComponent({ qualifier: ComponentQualifier.Application })
  });
  expect(wrapper).toMatchSnapshot();
  wrapper.find(BoxedTabs).prop<Function>('onSelect')(MeasuresPanelTabs.Overall);
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly if there is no new code measures', () => {
  const wrapper = shallowRender({
    measures: [
      mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.coverage }) }),
      mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.bugs }) })
    ]
  });
  wrapper.find(BoxedTabs).prop<Function>('onSelect')(MeasuresPanelTabs.New);
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly if there is no coverage', () => {
  expect(
    shallowRender({
      measures: [
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.bugs }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_bugs }) })
      ]
    })
  ).toMatchSnapshot();
});

it('should render correctly if the data is still loading', () => {
  expect(shallowRender({ loading: true })).toMatchSnapshot();
});

function shallowRender(props: Partial<MeasuresPanelProps> = {}) {
  return shallow(
    <MeasuresPanel
      branchLike={mockMainBranch()}
      component={mockComponent()}
      measures={[
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.coverage }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_coverage }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.bugs }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_bugs }) })
      ]}
      {...props}
    />
  );
}
