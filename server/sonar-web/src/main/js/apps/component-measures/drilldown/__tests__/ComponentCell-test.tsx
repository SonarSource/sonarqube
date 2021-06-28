/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { mockComponentMeasure, mockMetric } from '../../../../helpers/testMocks';
import { MetricKey } from '../../../../types/metrics';
import { enhanceComponent } from '../../utils';
import ComponentCell from '../ComponentCell';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({}, MetricKey.security_hotspots)).toMatchSnapshot('security review domain');

  const metric = mockMetric({ key: MetricKey.bugs });
  expect(
    shallowRender({
      component: enhanceComponent(
        mockComponentMeasure(false, { refKey: 'project-key' }),
        { key: metric.key },
        { [metric.key]: metric }
      )
    })
  ).toMatchSnapshot('ref component');
});

function shallowRender(
  overrides: Partial<ComponentCell['props']> = {},
  metricKey = MetricKey.bugs
) {
  const metric = mockMetric({ key: metricKey });
  const component = enhanceComponent(
    mockComponentMeasure(true, {
      measures: [{ metric: metric.key, value: '1', bestValue: false }]
    }),
    metric,
    { [metric.key]: metric }
  );

  return shallow<ComponentCell>(
    <ComponentCell
      component={component}
      metric={metric}
      onClick={jest.fn()}
      rootComponent={mockComponentMeasure(false)}
      view="list"
      {...overrides}
    />
  );
}
