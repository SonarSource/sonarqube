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
import Tooltip, { TooltipInner, TooltipProps } from '../Tooltip';

beforeAll(() => {
  jest.useFakeTimers();
});

afterAll(() => {
  jest.runOnlyPendingTimers();
  jest.useRealTimers();
});

jest.mock('react-dom', () => {
  const actual = jest.requireActual('react-dom');
  return Object.assign({}, actual, {
    findDOMNode: jest.fn().mockReturnValue(undefined),
  });
});

jest.mock('lodash', () => {
  const actual = jest.requireActual('lodash');
  return Object.assign({}, actual, {
    uniqueId: jest.fn((prefix) => `${prefix}1`),
  });
});

beforeEach(jest.clearAllMocks);

it('should render', () => {
  expect(shallowRenderTooltipInner()).toMatchSnapshot();
  expect(
    shallow(
      <TooltipInner overlay={<span id="overlay" />} visible={true}>
        <div id="tooltip" />
      </TooltipInner>,
      { disableLifecycleMethods: true }
    )
  ).toMatchSnapshot();
});

it('should open & close', () => {
  const onShow = jest.fn();
  const onHide = jest.fn();
  const wrapper = shallowRenderTooltipInner({ onHide, onShow });

  wrapper.find('#tooltip').simulate('pointerenter');
  jest.runOnlyPendingTimers();
  wrapper.update();
  expect(wrapper.find('TooltipPortal').exists()).toBe(true);
  expect(onShow).toHaveBeenCalled();

  wrapper.find('#tooltip').simulate('pointerleave');
  jest.runOnlyPendingTimers();
  wrapper.update();
  expect(wrapper.find('TooltipPortal').exists()).toBe(false);
  expect(onHide).toHaveBeenCalled();

  onShow.mockReset();
  onHide.mockReset();

  wrapper.find('#tooltip').simulate('focus');
  expect(wrapper.find('TooltipPortal').exists()).toBe(true);
  expect(onShow).toHaveBeenCalled();

  wrapper.find('#tooltip').simulate('blur');
  expect(wrapper.find('TooltipPortal').exists()).toBe(false);
  expect(onHide).toHaveBeenCalled();
});

it('should not open when pointer goes away quickly', () => {
  const onShow = jest.fn();
  const onHide = jest.fn();
  const wrapper = shallowRenderTooltipInner({ onHide, onShow });

  wrapper.find('#tooltip').simulate('pointerenter');
  wrapper.find('#tooltip').simulate('pointerleave');
  jest.runOnlyPendingTimers();
  wrapper.update();

  expect(wrapper.find('TooltipPortal').exists()).toBe(false);
});

it('should not render tooltip without overlay', () => {
  const wrapper = shallowRenderTooltip();
  expect(wrapper.type()).toBe('div');
});

it('should not render empty tooltips', () => {
  expect(shallowRenderTooltip()).toMatchSnapshot();
  expect(shallowRenderTooltip()).toMatchSnapshot();
});

it('should adjust arrow position', () => {
  const wrapper = shallowRenderTooltipInner();

  expect(wrapper.instance().adjustArrowPosition('left', { leftFix: 10, topFix: 20 })).toEqual({
    marginTop: -20,
  });
  expect(wrapper.instance().adjustArrowPosition('right', { leftFix: 10, topFix: 20 })).toEqual({
    marginTop: -20,
  });
  expect(wrapper.instance().adjustArrowPosition('top', { leftFix: 10, topFix: 20 })).toEqual({
    marginLeft: -10,
  });
  expect(wrapper.instance().adjustArrowPosition('bottom', { leftFix: 10, topFix: 20 })).toEqual({
    marginLeft: -10,
  });
});

function shallowRenderTooltip() {
  return shallow<TooltipProps>(
    <Tooltip overlay={undefined}>
      <div id="tooltip" />
    </Tooltip>
  );
}

function shallowRenderTooltipInner(props?: Partial<TooltipProps>) {
  return shallow<TooltipInner>(
    <TooltipInner overlay={<span id="overlay" />} {...props}>
      <div id="tooltip" />
    </TooltipInner>
  );
}
