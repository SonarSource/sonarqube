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
import { select } from 'd3-selection';
import { D3ZoomEvent, zoom } from 'd3-zoom';
import { shallow } from 'enzyme';
import * as React from 'react';
import { AutoSizer, AutoSizerProps } from 'react-virtualized/dist/commonjs/AutoSizer';
import Link from '../../../components/common/Link';
import { mockComponentMeasureEnhanced } from '../../../helpers/mocks/component';
import { mockHtmlElement } from '../../../helpers/mocks/dom';
import { click, mockEvent } from '../../../helpers/testUtils';
import { ComponentMeasureEnhanced } from '../../../types/types';
import BubbleChart from '../BubbleChart';

jest.mock('react-virtualized/dist/commonjs/AutoSizer', () => ({
  AutoSizer: ({ children }: AutoSizerProps) => children({ width: 100, height: NaN }),
}));

jest.mock('d3-selection', () => ({
  select: jest.fn().mockReturnValue({ call: jest.fn() }),
}));

jest.mock('d3-zoom', () => {
  return {
    zoomidentity: { k: 1, tx: 0, ty: 0 },
    zoom: jest.fn(),
  };
});

beforeEach(jest.clearAllMocks);

it('should display bubbles', () => {
  const wrapper = shallowRender();
  wrapper
    .find(AutoSizer)
    .dive()
    .find('Bubble')
    .forEach((bubble) => {
      expect(bubble.dive()).toMatchSnapshot();
    });
});

it('should render bubble links', () => {
  const wrapper = shallowRender({
    items: [
      { x: 1, y: 10, size: 7, color: { fill: 'blue', stroke: 'blue' } },
      { x: 2, y: 30, size: 5, color: { fill: 'green', stroke: 'green' } },
    ],
  });
  wrapper
    .find(AutoSizer)
    .dive()
    .find('Bubble')
    .forEach((bubble) => {
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
    .forEach((bubble) => {
      click(bubble.dive().find('a'));
      expect(bubble.dive()).toMatchSnapshot();
    });
  expect(onBubbleClick).toHaveBeenCalledTimes(2);
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
        getBoundingClientRect: () => ({ width: 100, height: 100 } as DOMRect),
      })
    );

    // Call zoom event handler.
    const mockZoomEvent = { transform: { x: 10, y: 10, k: 20 } } as D3ZoomEvent<
      SVGSVGElement,
      void
    >;
    wrapper.instance().zoomed(mockZoomEvent);
    expect(wrapper.state().transform).toEqual({
      x: 105,
      y: 105,
      k: 20,
    });

    // Reset Zoom levels.
    const resetZoomClick = wrapper.find('div.bubble-chart-zoom').find(Link).props().onClick;
    if (!resetZoomClick) {
      reject();
      return;
    }

    const stopPropagation = jest.fn();
    const preventDefault = jest.fn();
    resetZoomClick(mockEvent({ stopPropagation, preventDefault }));
    expect(stopPropagation).toHaveBeenCalled();
    expect(preventDefault).toHaveBeenCalled();
    expect(call).toHaveBeenCalledWith(zoomBehavior);

    resolve();
  });
});

function shallowRender(props: Partial<BubbleChart<ComponentMeasureEnhanced>['props']> = {}) {
  return shallow<BubbleChart<ComponentMeasureEnhanced>>(
    <BubbleChart
      height={100}
      items={[
        {
          x: 1,
          y: 10,
          size: 7,
          data: mockComponentMeasureEnhanced(),
          color: { fill: 'blue', stroke: 'blue' },
        },
        {
          x: 2,
          y: 30,
          size: 5,
          data: mockComponentMeasureEnhanced(),
          color: { fill: 'red', stroke: 'red' },
        },
      ]}
      padding={[0, 0, 0, 0]}
      {...props}
    />
  );
}
