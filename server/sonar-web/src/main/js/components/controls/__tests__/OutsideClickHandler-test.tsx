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
import { mount, shallow } from 'enzyme';
import * as React from 'react';
import OutsideClickHandler from '../OutsideClickHandler';

beforeAll(() => {
  jest.useFakeTimers();
});

afterAll(() => {
  jest.runOnlyPendingTimers();
  jest.useRealTimers();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should register for click event', () => {
  const addEventListener = jest.spyOn(window, 'addEventListener');
  const removeEventListener = jest.spyOn(window, 'removeEventListener');

  const wrapper = shallowRender();

  jest.runAllTimers();

  expect(addEventListener).toHaveBeenCalledWith('click', expect.anything());

  wrapper.instance().componentWillUnmount();

  expect(removeEventListener).toHaveBeenCalledWith('click', expect.anything());
});

it('should call event handler on click on window', () => {
  const onClickOutside = jest.fn();

  const map: { [key: string]: EventListener } = {};
  window.addEventListener = jest.fn((event, callback) => {
    map[event] = callback as EventListener;
  });

  mount(
    <div id="outside-element">
      <OutsideClickHandler onClickOutside={onClickOutside}>
        <div id="children" />
      </OutsideClickHandler>
    </div>
  );

  jest.runAllTimers();

  map['click'](new Event('click'));
  expect(onClickOutside).toHaveBeenCalled();
});

function shallowRender(props: Partial<OutsideClickHandler['props']> = {}) {
  return shallow<OutsideClickHandler>(
    <OutsideClickHandler onClickOutside={jest.fn()} {...props}>
      <div id="children" />
    </OutsideClickHandler>
  );
}
