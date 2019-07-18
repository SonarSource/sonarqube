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
import { shallow } from 'enzyme';
import * as React from 'react';
import FacetMeasureValue from '../FacetMeasureValue';

const MEASURE = {
  metric: {
    id: '1',
    key: 'bugs',
    type: 'INT',
    name: 'Bugs',
    domain: 'Reliability'
  },
  value: '5',
  periods: [{ index: 1, value: '5' }],
  leak: '5'
};
const LEAK_MEASURE = {
  metric: {
    id: '2',
    key: 'new_bugs',
    type: 'INT',
    name: 'New Bugs',
    domain: 'Reliability'
  },
  periods: [{ index: 1, value: '5' }],
  leak: '5'
};

it('should display measure value', () => {
  expect(shallow(<FacetMeasureValue displayLeak={true} measure={MEASURE} />)).toMatchSnapshot();
});

it('should display leak measure value', () => {
  expect(
    shallow(<FacetMeasureValue displayLeak={true} measure={LEAK_MEASURE} />)
  ).toMatchSnapshot();
  expect(
    shallow(<FacetMeasureValue displayLeak={false} measure={LEAK_MEASURE} />)
  ).toMatchSnapshot();
});
