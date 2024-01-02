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
import BoxedTabs from '../../../../components/controls/BoxedTabs';
import { mockBranch, mockMainBranch } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import {
  mockLocation,
  mockMeasureEnhanced,
  mockMetric,
  mockPeriod,
} from '../../../../helpers/testMocks';
import { ComponentQualifier } from '../../../../types/component';
import { MetricKey } from '../../../../types/metrics';
import { MeasuresPanel, MeasuresPanelProps, MeasuresPanelTabs } from '../MeasuresPanel';

jest.mock('react', () => {
  return {
    ...jest.requireActual('react'),
    useEffect: jest.fn().mockImplementation((f) => f()),
  };
});

it('should render correctly for projects', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('default');
  wrapper.find(BoxedTabs).prop<Function>('onSelect')(MeasuresPanelTabs.Overall);
  expect(wrapper).toMatchSnapshot('overall');
});

it('should render correctly for applications', () => {
  const wrapper = shallowRender({
    component: mockComponent({ qualifier: ComponentQualifier.Application }),
  });
  expect(wrapper).toMatchSnapshot('default');
  wrapper.find(BoxedTabs).prop<Function>('onSelect')(MeasuresPanelTabs.Overall);
  expect(wrapper).toMatchSnapshot('overall');
});

it('should render correctly if there is no new code measures', () => {
  const wrapper = shallowRender({
    measures: [
      mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.coverage }) }),
      mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.bugs }) }),
    ],
  });
  wrapper.find(BoxedTabs).prop<Function>('onSelect')(MeasuresPanelTabs.New);
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly if branch is misconfigured', () => {
  const wrapper = shallowRender({
    branch: mockBranch({ name: 'own-reference' }),
    measures: [
      mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.coverage }) }),
      mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.bugs }) }),
    ],
    period: mockPeriod({ date: undefined, mode: 'REFERENCE_BRANCH', parameter: 'own-reference' }),
  });
  wrapper.find(BoxedTabs).prop<Function>('onSelect')(MeasuresPanelTabs.New);
  expect(wrapper).toMatchSnapshot('hide settings');

  wrapper.setProps({ component: mockComponent({ configuration: { showSettings: true } }) });
  expect(wrapper).toMatchSnapshot('show settings');
});

it('should render correctly if there is no coverage', () => {
  expect(
    shallowRender({
      measures: [
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.bugs }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_bugs }) }),
      ],
    })
  ).toMatchSnapshot();
});

it('should render correctly if the data is still loading', () => {
  expect(shallowRender({ loading: true })).toMatchSnapshot();
});

it('should render correctly when code scope is overall code', () => {
  expect(
    shallowRender({
      location: mockLocation({ pathname: '/dashboard', query: { code_scope: 'overall' } }),
    })
  ).toMatchSnapshot();
});

it('should render correctly when code scope is new code', () => {
  expect(
    shallowRender({
      location: mockLocation({ pathname: '/dashboard', query: { code_scope: 'new' } }),
    })
  ).toMatchSnapshot();
});

function shallowRender(props: Partial<MeasuresPanelProps> = {}) {
  return shallow<MeasuresPanelProps>(
    <MeasuresPanel
      branch={mockMainBranch()}
      component={mockComponent()}
      measures={[
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.coverage }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_coverage }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.bugs }) }),
        mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.new_bugs }) }),
      ]}
      location={mockLocation()}
      {...props}
    />
  );
}
