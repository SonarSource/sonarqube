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
import ListIcon from '../../../../components/icons/ListIcon';
import { mockMetric } from '../../../../helpers/testMocks';
import { MetricKey } from '../../../../types/metrics';
import MeasureViewSelect from '../MeasureViewSelect';

it('should render correctly', () => {
  expect(
    shallowRender({ metric: mockMetric({ key: MetricKey.releasability_rating }) })
  ).toMatchSnapshot('has no list');
  expect(
    shallowRender({ metric: mockMetric({ key: MetricKey.alert_status, type: 'LEVEL' }) })
  ).toMatchSnapshot('has no tree');
  expect(shallowRender({ metric: mockMetric({ type: 'RATING' }) })).toMatchSnapshot(
    'has no treemap'
  );
});

it('should correctly trigger a selection change', () => {
  const handleViewChange = jest.fn();
  const wrapper = shallowRender({ handleViewChange });
  wrapper.instance().handleChange({ icon: <ListIcon />, label: 'List View', value: 'list' });
  expect(handleViewChange).toHaveBeenCalledWith('list');
});

function shallowRender(props: Partial<MeasureViewSelect['props']> = {}) {
  return shallow<MeasureViewSelect>(
    <MeasureViewSelect metric={mockMetric()} handleViewChange={jest.fn()} view="list" {...props} />
  );
}
