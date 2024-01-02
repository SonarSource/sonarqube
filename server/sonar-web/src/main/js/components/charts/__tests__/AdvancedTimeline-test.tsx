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
import { shallow } from 'enzyme';
import * as React from 'react';
import { Chart } from '../../../types/types';
import AdvancedTimeline from '../AdvancedTimeline';

const newCodeLegendClass = '.new-code-legend';

// Replace scaleTime with scaleUtc to avoid timezone-dependent snapshots
jest.mock('d3-scale', () => {
  const { scaleUtc, ...others } = jest.requireActual('d3-scale');

  return {
    ...others,
    scaleTime: scaleUtc,
  };
});

jest.mock('lodash', () => {
  const lodash = jest.requireActual('lodash');
  return { ...lodash, throttle: (f: any) => f };
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ disableZoom: false, updateZoom: () => {} })).toMatchSnapshot(
    'Zoom enabled'
  );
  expect(shallowRender({ formatYTick: (t) => `Nicer tick ${t}` })).toMatchSnapshot('format y tick');
  expect(shallowRender({ width: undefined })).toMatchSnapshot('no width');
  expect(shallowRender({ height: undefined })).toMatchSnapshot('no height');
  expect(shallowRender({ showAreas: undefined })).toMatchSnapshot('no areas');
});

it('should render leak correctly', () => {
  const wrapper = shallowRender({ leakPeriodDate: new Date('2019-10-02') });

  const leakNode = wrapper.find('.leak-chart-rect');
  expect(leakNode.exists()).toBe(true);
  expect(leakNode.getElement().props.width).toBe(15);
});

it('should render leak legend correctly', () => {
  const wrapper = shallowRender({
    displayNewCodeLegend: true,
    leakPeriodDate: new Date('2019-10-02'),
  });

  const leakNode = wrapper;
  expect(leakNode.find(newCodeLegendClass).exists()).toBe(true);
  expect(leakNode.find(newCodeLegendClass).props().textAnchor).toBe('start');
  expect(leakNode).toMatchSnapshot();
});

it('should render leak legend correctly for small leak', () => {
  const wrapper = shallowRender({
    displayNewCodeLegend: true,
    leakPeriodDate: new Date('2020-02-06'),
    series: [
      mockData(1, '2020-02-01'),
      mockData(2, '2020-02-02'),
      mockData(3, '2020-02-03'),
      mockData(4, '2020-02-04'),
      mockData(5, '2020-02-05'),
      mockData(6, '2020-02-06'),
      mockData(7, '2020-02-07'),
    ],
  });

  const leakNode = wrapper;
  expect(leakNode.find(newCodeLegendClass).exists()).toBe(true);
  expect(leakNode.find(newCodeLegendClass).props().textAnchor).toBe('end');
});

it('should set leakLegendTextWidth correctly', () => {
  const wrapper = shallowRender();

  wrapper.instance().setLeakLegendTextWidth({
    getBoundingClientRect: () => ({ width: 12 } as DOMRect),
  } as SVGTextElement);

  expect(wrapper.state().leakLegendTextWidth).toBe(12);

  wrapper.instance().setLeakLegendTextWidth(null);

  expect(wrapper.state().leakLegendTextWidth).toBe(12);
});

it('should render old leak correctly', () => {
  const wrapper = shallowRender({ leakPeriodDate: new Date('2014-10-02') });

  const leakNode = wrapper.find('.leak-chart-rect');
  expect(leakNode.exists()).toBe(true);
  expect(leakNode.getElement().props.width).toBe(30);
});

it('should find date to display based on mouse location', () => {
  const wrapper = shallowRender();

  wrapper.instance().updateTooltipPos(0);
  expect(wrapper.state().selectedDateIdx).toBeUndefined();

  wrapper.instance().handleMouseEnter();
  wrapper.instance().updateTooltipPos(10);
  expect(wrapper.state().selectedDateIdx).toBe(1);
});

it('should update timeline when width changes', () => {
  const updateTooltip = jest.fn();
  const wrapper = shallowRender({ selectedDate: new Date('2019-10-02'), updateTooltip });
  const { xScale, selectedDateXPos } = wrapper.state();

  wrapper.setProps({ width: 200 });
  expect(wrapper.state().xScale).not.toBe(xScale);
  expect(wrapper.state().xScale).toEqual(expect.any(Function));
  expect(wrapper.state().selectedDateXPos).not.toBe(selectedDateXPos);
  expect(wrapper.state().selectedDateXPos).toEqual(expect.any(Number));
  expect(updateTooltip).toHaveBeenCalled();
});

it('should update tootlips when selected date changes', () => {
  const updateTooltip = jest.fn();

  const wrapper = shallowRender({ selectedDate: new Date('2019-10-01'), updateTooltip });
  const { xScale, selectedDateXPos } = wrapper.state();
  const selectedDate = new Date('2019-10-02');

  wrapper.setProps({ selectedDate });
  expect(wrapper.state().xScale).toBe(xScale);
  expect(wrapper.state().selectedDate).toBe(selectedDate);
  expect(wrapper.state().selectedDateXPos).not.toBe(selectedDateXPos);
  expect(wrapper.state().selectedDateXPos).toEqual(expect.any(Number));
  expect(updateTooltip).toHaveBeenCalled();
});

it('should handle scroll correcly', () => {
  let updateZoom = jest.fn();
  let preventDefault = jest.fn();
  let wrapper = shallowRender({ updateZoom });
  wrapper.instance().handleWheel({
    preventDefault,
    deltaX: 1,
    deltaY: -2,
    deltaZ: 0,
    pageX: 100,
    pageY: 1,
    currentTarget: {
      getBoundingClientRect: () => ({
        bottom: 0,
        height: 100,
        width: 50,
        left: 0,
        right: 0,
        top: 10,
        x: 12,
        y: 23,
        toJSON: () => '',
      }),
    } as any as SVGElement,
  } as any as React.WheelEvent<SVGElement>);
  expect(preventDefault).toHaveBeenCalled();
  expect(updateZoom).toHaveBeenCalledWith(new Date('2019-10-01T06:24:00.000Z'), undefined);

  updateZoom = jest.fn();
  preventDefault = jest.fn();
  wrapper = shallowRender({ updateZoom });
  wrapper.instance().handleWheel({
    preventDefault,
    deltaX: 1,
    deltaY: 2,
    deltaZ: 0,
    pageX: 100,
    pageY: 1,
    deltaMode: 25,
    currentTarget: {
      getBoundingClientRect: () => ({
        bottom: 0,
        height: 100,
        width: 50,
        left: 0,
        right: 0,
        top: 10,
        x: 12,
        y: 23,
        toJSON: () => '',
      }),
    } as any as SVGElement,
  } as any as React.WheelEvent<SVGElement>);
  expect(preventDefault).toHaveBeenCalled();
  expect(updateZoom).toHaveBeenCalledWith(undefined, new Date('2019-10-02T20:48:00.000Z'));
});

it('should handle mouse out correcly', () => {
  const updateTooltip = jest.fn();
  const wrapper = shallowRender({ updateTooltip: undefined });
  wrapper.setState({
    mouseOver: true,
    selectedDate: new Date(),
    selectedDateXPos: 1,
    selectedDateIdx: 1,
  });
  wrapper.instance().handleMouseOut();
  expect(wrapper.state().mouseOver).toBe(true);

  wrapper.setProps({ updateTooltip });
  wrapper.instance().handleMouseOut();
  expect(wrapper.state().mouseOver).toBe(false);
  expect(wrapper.state().selectedDate).toBeUndefined();
  expect(wrapper.state().selectedDateXPos).toBeUndefined();
  expect(wrapper.state().selectedDateIdx).toBeUndefined();
  wrapper.instance().handleMouseOut();
});

it('should handle click correcly', () => {
  const updateSelectedDate = jest.fn();
  const wrapper = shallowRender({ updateSelectedDate });
  wrapper.setState({ selectedDate: new Date() });

  wrapper.instance().handleClick();
  expect(updateSelectedDate).toHaveBeenCalledWith(wrapper.state().selectedDate);

  wrapper.setProps({ updateSelectedDate: undefined });
  updateSelectedDate.mockClear();
  wrapper.instance().handleClick();
  expect(updateSelectedDate).not.toHaveBeenCalled();
});

function shallowRender(props?: Partial<AdvancedTimeline['props']>) {
  return shallow<AdvancedTimeline>(
    <AdvancedTimeline
      height={100}
      maxYTicksCount={10}
      metricType="TEST_METRIC"
      series={[
        {
          name: 'test-1',
          type: 'test-type-1',
          translatedName: '',
          data: [
            {
              x: new Date('2019-10-01'),
              y: 1,
            },
            {
              x: new Date('2019-10-02'),
              y: 2,
            },
          ],
        },
        {
          name: 'test-2',
          type: 'test-type-2',
          translatedName: '',
          data: [
            {
              x: new Date('2019-10-03'),
              y: 3,
            },
          ],
        },
      ]}
      width={100}
      zoomSpeed={1}
      {...props}
    />
  );
}

function mockData(i: number, date: string): Chart.Serie {
  return {
    name: `t${i}`,
    type: 'type',
    translatedName: '',
    data: [{ x: new Date(date), y: i }],
  };
}
