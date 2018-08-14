/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { mount } from 'enzyme';
import MouseOverHandler from '../MouseOverHandler';

jest.useFakeTimers();

it('should trigger after delay', () => {
  const onOver = jest.fn();
  const wrapper = mount(
    <MouseOverHandler delay={1000} onOver={onOver}>
      <div />
    </MouseOverHandler>
  );

  const node = wrapper.getDOMNode();

  event(node, 'mouseenter');
  expect(onOver).not.toBeCalled();

  jest.runTimersToTime(500);
  expect(onOver).not.toBeCalled();

  jest.runTimersToTime(1000);
  expect(onOver).toBeCalled();
});

it('should not trigger when mouse is out', () => {
  const onOver = jest.fn();
  const wrapper = mount(
    <MouseOverHandler delay={1000} onOver={onOver}>
      <div />
    </MouseOverHandler>
  );

  const node = wrapper.getDOMNode();

  event(node, 'mouseenter');
  expect(onOver).not.toBeCalled();

  jest.runTimersToTime(500);
  event(node, 'mouseleave');

  jest.runTimersToTime(1000);
  expect(onOver).not.toBeCalled();
});

it('should detach events', () => {
  const onOver = jest.fn();
  const wrapper = mount(
    <MouseOverHandler delay={1000} onOver={onOver}>
      <div />
    </MouseOverHandler>
  );

  const node = wrapper.getDOMNode();

  event(node, 'mouseenter');
  expect(onOver).not.toBeCalled();

  wrapper.unmount();

  jest.runTimersToTime(1000);
  expect(onOver).not.toBeCalled();
});

function event(node: Element, eventName: string) {
  const event = new MouseEvent(eventName);
  node.dispatchEvent(event);
}
