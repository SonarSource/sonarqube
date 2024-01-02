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
import { scaleBand } from 'd3-scale';
import { shallow } from 'enzyme';
import * as React from 'react';
import Histogram from '../Histogram';

jest.mock('d3-scale', () => {
  const d3 = jest.requireActual('d3-scale');
  return {
    ...d3,
    scaleBand: jest.fn(d3.scaleBand),
  };
});

beforeEach(jest.clearAllMocks);

it('renders correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ alignTicks: true })).toMatchSnapshot('align ticks');
  expect(shallowRender({ yValues: ['100.0', '75.0', '150.0'] })).toMatchSnapshot('with yValues');
  expect(
    shallowRender({ yTicks: ['a', 'b', 'c'], yValues: ['100.0', '75.0', '150.0'] })
  ).toMatchSnapshot('with yValues and yTicks');
  expect(
    shallowRender({
      yTicks: ['a', 'b', 'c'],
      yTooltips: ['a - 100', 'b - 75', 'c - 150'],
      yValues: ['100.0', '75.0', '150.0'],
    })
  ).toMatchSnapshot('with yValues, yTicks and yTooltips');
});

it('correctly handles yScale() returning undefined', () => {
  const yScale = () => undefined;
  yScale.bandwidth = () => 1;

  (scaleBand as jest.Mock).mockReturnValueOnce({
    domain: () => ({ rangeRound: () => yScale }),
  });

  expect(
    shallowRender({ yValues: ['100.0', '75.0', '150.0'], yTicks: ['a', 'b', 'c'] })
  ).toMatchSnapshot();
});

function shallowRender(props: Partial<Histogram['props']> = {}) {
  return shallow<Histogram>(<Histogram bars={[100, 75, 150]} height={75} width={100} {...props} />);
}
