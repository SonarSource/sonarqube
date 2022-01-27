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
import { createCondition, updateCondition } from '../../../../api/quality-gates';
import { mockQualityGate } from '../../../../helpers/mocks/quality-gates';
import { mockCondition, mockMetric } from '../../../../helpers/testMocks';
import { MetricKey } from '../../../../types/metrics';
import ConditionModal from '../ConditionModal';

jest.mock('../../../../api/quality-gates', () => ({
  createCondition: jest.fn().mockResolvedValue({}),
  updateCondition: jest.fn().mockResolvedValue({})
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ metric: mockMetric() })).toMatchSnapshot();
});

it('should correctly handle a metric selection', () => {
  const wrapper = shallowRender();
  const metric = mockMetric();

  expect(wrapper.find('withMetricsContext(MetricSelectComponent)').prop('metric')).toBeUndefined();

  wrapper.instance().handleMetricChange(metric);
  expect(wrapper.find('withMetricsContext(MetricSelectComponent)').prop('metric')).toEqual(metric);
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

it('should handle submission', async () => {
  const onAddCondition = jest.fn();
  const wrapper = shallowRender({ onAddCondition });

  wrapper.setState({ metric: mockMetric() });

  await wrapper.instance().handleFormSubmit();

  expect(createCondition).toBeCalled();
  expect(updateCondition).not.toBeCalled();

  jest.clearAllMocks();

  wrapper.setProps({ condition: mockCondition() });
  await wrapper.instance().handleFormSubmit();

  expect(createCondition).not.toBeCalled();
  expect(updateCondition).toBeCalled();
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
