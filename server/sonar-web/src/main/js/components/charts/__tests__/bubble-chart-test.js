/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import TestUtils from 'react-addons-test-utils';
import { expect } from 'chai';

import { BubbleChart } from '../bubble-chart';

describe('Bubble Chart', function () {

  it('should display bubbles', function () {
    const items = [
      { x: 1, y: 10, size: 7 },
      { x: 2, y: 30, size: 5 },
      { x: 3, y: 20, size: 2 }
    ];
    const chart = TestUtils.renderIntoDocument(<BubbleChart items={items} width={100} height={100}/>);
    expect(TestUtils.scryRenderedDOMComponentsWithClass(chart, 'bubble-chart-bubble')).to.have.length(3);
  });

  it('should display grid', function () {
    const items = [
      { x: 1, y: 10, size: 7 },
      { x: 2, y: 30, size: 5 },
      { x: 3, y: 20, size: 2 }
    ];
    const chart = TestUtils.renderIntoDocument(<BubbleChart items={items} width={100} height={100}/>);
    expect(TestUtils.scryRenderedDOMComponentsWithTag(chart, 'line')).to.not.be.empty;
  });

  it('should display ticks', function () {
    const items = [
      { x: 1, y: 10, size: 7 },
      { x: 2, y: 30, size: 5 },
      { x: 3, y: 20, size: 2 }
    ];
    const chart = TestUtils.renderIntoDocument(<BubbleChart items={items} width={100} height={100}/>);
    expect(TestUtils.scryRenderedDOMComponentsWithClass(chart, 'bubble-chart-tick')).to.not.be.empty;
  });

});
