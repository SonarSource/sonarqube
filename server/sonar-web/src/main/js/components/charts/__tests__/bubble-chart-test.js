/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import { shallow } from 'enzyme';
import { BubbleChart, Bubble } from '../bubble-chart';

it('should display bubbles', () => {
  const items = [
    { x: 1, y: 10, size: 7 },
    { x: 2, y: 30, size: 5 },
    { x: 3, y: 20, size: 2 }
  ];
  const chart = shallow(<BubbleChart items={items} width={100} height={100}/>);
  expect(chart.find(Bubble).length).toBe(3);
});

it('should display grid', () => {
  const items = [
    { x: 1, y: 10, size: 7 },
    { x: 2, y: 30, size: 5 },
    { x: 3, y: 20, size: 2 }
  ];
  const chart = shallow(<BubbleChart items={items} width={100} height={100}/>);
  expect(chart.find('line').length).toBeGreaterThan(0);
});

it('should display ticks', () => {
  const items = [
    { x: 1, y: 10, size: 7 },
    { x: 2, y: 30, size: 5 },
    { x: 3, y: 20, size: 2 }
  ];
  const chart = shallow(<BubbleChart items={items} width={100} height={100}/>);
  expect(chart.find('.bubble-chart-tick').length).toBeGreaterThan(0);
});
