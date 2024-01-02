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
import Radio from '../../../components/controls/Radio';
import Select from '../../../components/controls/Select';
import { mockQualityGate } from '../../../helpers/mocks/quality-gates';
import { mockCondition } from '../../../helpers/testMocks';
import { submit } from '../../../helpers/testUtils';
import { MetricKey } from '../../../types/metrics';
import { USE_SYSTEM_DEFAULT } from '../constants';
import ProjectQualityGateAppRenderer, {
  ProjectQualityGateAppRendererProps,
} from '../ProjectQualityGateAppRenderer';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ loading: true })).toMatchSnapshot('loading');
  expect(shallowRender({ submitting: true })).toMatchSnapshot('submitting');
  expect(
    shallowRender({
      currentQualityGate: mockQualityGate({ id: '2', isDefault: true }),
      selectedQualityGateId: USE_SYSTEM_DEFAULT,
    })
  ).toMatchSnapshot('always use system default');
  expect(shallowRender({ selectedQualityGateId: '3' })).toMatchSnapshot('show new code warning');
  expect(
    shallowRender({
      selectedQualityGateId: '5',
    })
  ).toMatchSnapshot('show warning');
  expect(
    shallowRender({
      selectedQualityGateId: USE_SYSTEM_DEFAULT,
    })
  ).toMatchSnapshot('show warning if not using default');
  expect(shallowRender({ allQualityGates: undefined }).type()).toBeNull(); // no quality gates
});

it('should render select options correctly', () => {
  return new Promise<void>((resolve) => {
    const wrapper = shallowRender();
    const render = wrapper.find(Select).props().components.Option;

    expect(render).toBeDefined();

    expect(render({ data: { value: '1', label: 'Gate 1' } })).toMatchSnapshot('default');
    resolve();
  });
});

it('should correctly handle changes', () => {
  const wrapper = shallowRender();
  const onSelect = jest.fn((selectedQualityGateId) => {
    wrapper.setProps({ selectedQualityGateId });
  });
  wrapper.setProps({ onSelect });

  wrapper.find(Radio).at(0).props().onCheck(USE_SYSTEM_DEFAULT);
  expect(onSelect).toHaveBeenLastCalledWith(USE_SYSTEM_DEFAULT);

  wrapper.find(Radio).at(1).props().onCheck('1');
  expect(onSelect).toHaveBeenLastCalledWith('1');

  wrapper.find(Select).props().onChange!({ value: '2' });
  expect(onSelect).toHaveBeenLastCalledWith('2');
});

it('should correctly handle form submission', () => {
  const onSubmit = jest.fn();
  const wrapper = shallowRender({ onSubmit });
  submit(wrapper.find('form'));
  expect(onSubmit).toHaveBeenCalled();
});

function shallowRender(props: Partial<ProjectQualityGateAppRendererProps> = {}) {
  const conditions = [mockCondition(), mockCondition({ metric: MetricKey.new_bugs })];
  const conditionsEmptyOnNew = [mockCondition({ metric: MetricKey.bugs })];
  return shallow<ProjectQualityGateAppRendererProps>(
    <ProjectQualityGateAppRenderer
      allQualityGates={[
        mockQualityGate({ conditions }),
        mockQualityGate({ id: '2', isDefault: true, conditions }),
        mockQualityGate({ id: '3', isDefault: true, conditions: conditionsEmptyOnNew }),
      ]}
      currentQualityGate={mockQualityGate({ id: '1' })}
      loading={false}
      onSelect={jest.fn()}
      onSubmit={jest.fn()}
      selectedQualityGateId="1"
      submitting={false}
      {...props}
    />
  );
}
