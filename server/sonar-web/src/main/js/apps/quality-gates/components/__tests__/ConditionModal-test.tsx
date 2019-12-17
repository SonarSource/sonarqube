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
import { mockQualityGate } from '../../../../helpers/mocks/quality-gates';
import { mockMetric } from '../../../../helpers/testMocks';
import { MetricKey } from '../../../../types/metrics';
import ConditionModal from '../ConditionModal';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ metric: mockMetric() })).toMatchSnapshot();
});

it('should correctly handle a metric selection', () => {
  const wrapper = shallowRender();
  const metric = mockMetric();

  expect(wrapper.find('MetricSelect').prop('metric')).toBeUndefined();

  wrapper.instance().handleMetricChange(metric);
  expect(wrapper.find('MetricSelect').prop('metric')).toEqual(metric);
});

it('should correctly switch scope', () => {
  const wrapper = shallowRender({
    metrics: [
      mockMetric({ key: MetricKey.new_coverage }),
      mockMetric({
        key: MetricKey.new_duplicated_lines
      }),
      mockMetric(),
      mockMetric({ key: MetricKey.duplicated_lines })
    ]
  });
  expect(wrapper).toMatchSnapshot();

  wrapper.instance().handleScopeChange('overall');
  expect(wrapper).toMatchSnapshot();

  wrapper.instance().handleScopeChange('new');
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<ConditionModal['props']> = {}) {
  return shallow<ConditionModal>(
    <ConditionModal
      header="header"
      metrics={[
        mockMetric({ key: MetricKey.new_coverage }),
        mockMetric({ key: MetricKey.new_duplicated_lines })
      ]}
      onAddCondition={jest.fn()}
      onClose={jest.fn()}
      qualityGate={mockQualityGate()}
      {...props}
    />
  );
}
