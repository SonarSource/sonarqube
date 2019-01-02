/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as React from 'react';
import { shallow } from 'enzyme';
import ComponentMeasure from '../ComponentMeasure';

const METRIC = { id: '1', key: 'coverage', type: 'PERCENT', name: 'Coverage' };
const LEAK_METRIC = { id: '2', key: 'new_coverage', type: 'PERCENT', name: 'Coverage on New Code' };
const COMPONENT = { key: 'foo', name: 'Foo', qualifier: 'TRK' };
const COMPONENT_MEASURE = {
  ...COMPONENT,
  measures: [{ value: '3.0', periods: [{ index: 1, value: '10.0' }], metric: METRIC.key }]
};

it('renders correctly', () => {
  expect(
    shallow(<ComponentMeasure component={COMPONENT_MEASURE} metric={METRIC} />)
  ).toMatchSnapshot();
});

it('renders correctly for leak values', () => {
  expect(
    shallow(
      <ComponentMeasure
        component={{
          ...COMPONENT,
          measures: [
            { value: '3.0', periods: [{ index: 1, value: '10.0' }], metric: LEAK_METRIC.key }
          ]
        }}
        metric={LEAK_METRIC}
      />
    )
  ).toMatchSnapshot();
});

it('renders correctly when no measure found', () => {
  expect(shallow(<ComponentMeasure component={COMPONENT} metric={METRIC} />)).toMatchSnapshot();
});
