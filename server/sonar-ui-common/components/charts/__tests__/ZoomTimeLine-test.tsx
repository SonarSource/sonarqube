/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import testTheme from '../../../config/jest/testTheme';
import ZoomTimeLine from '../ZoomTimeLine';

const series = [
  {
    data: [
      {
        x: new Date('2020-01-01'),
        y: 'beginning',
      },
      {
        x: new Date('2020-02-01'),
        y: 'end',
      },
    ],
    name: 'foo',
    translatedName: 'foo-translated',
    type: 'bar',
  },
];

it('should draw a graph with lines', () => {
  const wrapper = shallowRender();
  expect(wrapper.find('.line-chart-grid').exists()).toBe(true);
  expect(wrapper.find('.line-chart-path').exists()).toBe(true);
  expect(wrapper.find('.chart-zoom-tick').exists()).toBe(true);
  expect(wrapper.find('.line-chart-area').exists()).toBe(false);
});

it('should be zoomable', () => {
  expect(shallowRender().find('.chart-zoom').exists()).toBe(true);
});

it('should render a leak period', () => {
  expect(
    shallowRender({ leakPeriodDate: new Date('2020-01-01') })
      .find('ContextConsumer')
      .dive()
      .find(`rect[fill="${testTheme.colors.leakPrimaryColor}"]`)
      .exists()
  ).toBe(true);
});

it('should render areas under the graph lines', () => {
  expect(shallowRender({ showAreas: true }).find('.line-chart-area').exists()).toBe(true);
});

function shallowRender(props: Partial<ZoomTimeLine['props']> = {}) {
  return shallow<ZoomTimeLine>(
    <ZoomTimeLine
      width={300}
      series={series}
      updateZoom={jest.fn()}
      metricType="RATING"
      height={300}
      {...props}
    />
  );
}
