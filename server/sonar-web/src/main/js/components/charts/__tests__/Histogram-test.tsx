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
import Histogram from '../Histogram';

it('renders', () => {
  expect(shallow(<Histogram bars={[100, 75, 150]} height={75} width={100} />)).toMatchSnapshot();
});

it('renders with yValues', () => {
  expect(
    shallow(
      <Histogram
        bars={[100, 75, 150]}
        height={75}
        width={100}
        yValues={['100.0', '75.0', '150.0']}
      />
    )
  ).toMatchSnapshot();
});

it('renders with yValues and yTicks', () => {
  expect(
    shallow(
      <Histogram
        bars={[100, 75, 150]}
        height={75}
        width={100}
        yTicks={['a', 'b', 'c']}
        yValues={['100.0', '75.0', '150.0']}
      />
    )
  ).toMatchSnapshot();
});

it('renders with yValues, yTicks and yTooltips', () => {
  expect(
    shallow(
      <Histogram
        bars={[100, 75, 150]}
        height={75}
        width={100}
        yTicks={['a', 'b', 'c']}
        yTooltips={['a - 100', 'b - 75', 'c - 150']}
        yValues={['100.0', '75.0', '150.0']}
      />
    )
  ).toMatchSnapshot();
});
