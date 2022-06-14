/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { select } from 'd3-selection';
import { zoom } from 'd3-zoom';
import { shallow } from 'enzyme';
import * as React from 'react';
import { Link } from 'react-router';
import { AutoSizer, AutoSizerProps } from 'react-virtualized/dist/commonjs/AutoSizer';
import { mockComponentMeasureEnhanced } from '../../../helpers/mocks/component';
import { mockHtmlElement } from '../../../helpers/mocks/dom';
import { click, mockEvent } from '../../../helpers/testUtils';
import { ComponentMeasureEnhanced } from '../../../types/types';
import BubbleChart from '../BubbleChart';

jest.mock('react-virtualized/dist/commonjs/AutoSizer', () => ({
  AutoSizer: ({ children }: AutoSizerProps) => children({ width: 100, height: NaN })
}));

jest.mock('d3-selection', () => ({
  event: { transform: { x: 10, y: 10, k: 20 } },
  select: jest.fn().mockReturnValue({ call: jest.fn() })
}));

jest.mock('d3-zoom', () => ({
  ...jest.requireActual('d3-zoom'),
  zoom: jest.fn()
}));

beforeEach(jest.clearAllMocks);

it('should display bubbles', () => {
  const wrapper = shallowRender();
  wrapper
    .find(AutoSizer)
    .dive()
    .find('Bubble')
    .forEach(bubble => {
      expect(bubble.dive()).toMatchSnapshot();
    });
});

it('should render bubble links', () => {
  const wrapper = shallowRender({
    items: [
      { x: 1, y: 10, size: 7, link: 'foo' },
      { x: 2, y: 30, size: 5, link: 'bar' }
    ]
  });
  wrapper
    .find(AutoSizer)
    .dive()
    .find('Bubble')
    .forEach(bubble => {
      expect(bubble.dive()).toMatchSnapshot();
    });
});

it('should render bubbles with click handlers', () => {
  const onBubbleClick = jest.fn();
  const wrapper = shallowRender({ onBubbleClick });
  wrapper
    .find(AutoSizer)
    .dive()
    .find('Bubble')
    .forEach(bubble => {
      click(bubble.dive().find('circle'));
      expect(bubble.dive()).toMatchSnapshot();
    });
  expect(onBubbleClick).toBeCalledTimes(2);
  expect(onBubbleClick).toHaveBeenLastCalledWith(mockComponentMeasureEnhanced());
});

it('should correctly handle zooming', () => {
  class ZoomBehaviorMock {
    on = () => this;
    scaleExtent = () => this;
    translateExtent = () => this;
  }

  const call = jest.fn();
  const zoomBehavior = new ZoomBehaviorMock();
  (select as jest.Mock).mockReturnValueOnce({ call });
  (zoom as jest.Mock).mockReturnValueOnce(zoomBehavior);

  return new Promise<void>((resolve, reject) => {
    const wrapper = shallowRender({ padding: [5, 5, 5, 5] });
    wrapper.instance().boundNode(
      mockHtmlElement<SVGSVGElement>({
        getBoundingClientRect: () => ({ width: 100, height: 100 } as DOMRect)
      })
    );

    // Call zoom event handler.
    wrapper.instance().zoomed();
    expect(wrapper.state().transform).toEqual({
      x: 105,
      y: 105,
      k: 20
    });

    // Reset Zoom levels.
    const resetZoomClick = wrapper
      .find('div.bubble-chart-zoom')
      .find(Link)
      .props().onClick;
    if (!resetZoomClick) {
      reject();
      return;
    }

    const stopPropagation = jest.fn();
    const preventDefault = jest.fn();
    resetZoomClick(mockEvent({ stopPropagation, preventDefault }));
    expect(stopPropagation).toBeCalled();
    expect(preventDefault).toBeCalled();
    expect(call).toHaveBeenCalledWith(zoomBehavior);

    resolve();
  });
});

function shallowRender(props: Partial<BubbleChart<ComponentMeasureEnhanced>['props']> = {}) {
  return shallow<BubbleChart<ComponentMeasureEnhanced>>(
    <BubbleChart
      height={100}
      items={[
        { x: 1, y: 10, size: 7, data: mockComponentMeasureEnhanced() },
        { x: 2, y: 30, size: 5, data: mockComponentMeasureEnhanced() }
      ]}
      padding={[0, 0, 0, 0]}
      {...props}
    />
  );
}
