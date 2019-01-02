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
import Tooltip, { TooltipInner } from '../Tooltip';

jest.useFakeTimers();
jest.mock('react-dom', () => {
  const actual = require.requireActual('react-dom');
  return Object.assign({}, actual, {
    findDOMNode: () => undefined
  });
});

it('should render', () => {
  expect(
    shallow(
      <TooltipInner overlay={<span id="overlay" />} visible={false}>
        <div id="tooltip" />
      </TooltipInner>
    )
  ).toMatchSnapshot();
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
  const wrapper = shallow(
    <TooltipInner onHide={onHide} onShow={onShow} overlay={<span id="overlay" />}>
      <div id="tooltip" />
    </TooltipInner>
  );
  wrapper.find('#tooltip').simulate('mouseenter');
  jest.runOnlyPendingTimers();
  wrapper.update();
  expect(wrapper.find('TooltipPortal').exists()).toBe(true);
  expect(onShow).toBeCalled();

  wrapper.find('#tooltip').simulate('mouseleave');
  jest.runOnlyPendingTimers();
  wrapper.update();
  expect(wrapper.find('TooltipPortal').exists()).toBe(false);
  expect(onHide).toBeCalled();
});

it('should not open when mouse goes away quickly', () => {
  const onShow = jest.fn();
  const onHide = jest.fn();
  const wrapper = shallow(
    <TooltipInner onHide={onHide} onShow={onShow} overlay={<span id="overlay" />}>
      <div id="tooltip" />
    </TooltipInner>
  );

  wrapper.find('#tooltip').simulate('mouseenter');
  wrapper.find('#tooltip').simulate('mouseleave');
  jest.runOnlyPendingTimers();
  wrapper.update();

  expect(wrapper.find('TooltipPortal').exists()).toBe(false);
});

it('should not render tooltip without overlay', () => {
  const wrapper = shallow(
    <Tooltip overlay={undefined}>
      <div id="tooltip" />
    </Tooltip>
  );
  expect(wrapper.type()).toBe('div');
});

it('should not render empty tooltips', () => {
  expect(
    shallow(
      <Tooltip overlay={undefined} visible={true}>
        <div id="tooltip" />
      </Tooltip>
    )
  ).toMatchSnapshot();
  expect(
    shallow(
      <Tooltip overlay="" visible={true}>
        <div id="tooltip" />
      </Tooltip>
    )
  ).toMatchSnapshot();
});
