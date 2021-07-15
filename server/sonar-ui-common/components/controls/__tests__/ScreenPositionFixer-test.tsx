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
import { mount } from 'enzyme';
import * as React from 'react';
import { resizeWindowTo, setNodeRect } from '../../../helpers/testUtils';
import ScreenPositionFixer from '../ScreenPositionFixer';

jest.mock('lodash', () => {
  const lodash = require.requireActual('lodash');
  lodash.throttle = (fn: any) => () => fn();
  return lodash;
});

jest.mock('react-dom', () => ({
  findDOMNode: jest.fn(),
}));

beforeEach(() => {
  setNodeRect({ left: 50, top: 50 });
  resizeWindowTo(1000, 1000);
});

it('should fix position', () => {
  const children = jest.fn(() => <div />);
  mountRender({ children });

  setNodeRect({ left: 50, top: 50 });
  resizeWindowTo(75, 1000);
  expect(children).toHaveBeenLastCalledWith({ leftFix: -29, topFix: 0 });

  resizeWindowTo(1000, 75);
  expect(children).toHaveBeenLastCalledWith({ leftFix: 0, topFix: -29 });

  setNodeRect({ left: -10, top: 50 });
  resizeWindowTo(1000, 1000);
  expect(children).toHaveBeenLastCalledWith({ leftFix: 14, topFix: 0 });

  setNodeRect({ left: 50, top: -10 });
  resizeWindowTo();
  expect(children).toHaveBeenLastCalledWith({ leftFix: 0, topFix: 14 });
});

it('should render two times', () => {
  const children = jest.fn(() => <div />);
  mountRender({ children });
  expect(children).toHaveBeenCalledTimes(2);
  expect(children).toHaveBeenCalledWith({});
  expect(children).toHaveBeenLastCalledWith({ leftFix: 0, topFix: 0 });
});

it('should re-position when `ready` turns to `true`', () => {
  const children = jest.fn(() => <div />);
  const wrapper = mountRender({ children, ready: false });
  expect(children).toHaveBeenCalledTimes(2);
  wrapper.setProps({ ready: true });
  // 2 + 1 (props change) + 1 (new measurement)
  expect(children).toHaveBeenCalledTimes(4);
});

it('should re-position when window is resized', () => {
  const children = jest.fn(() => <div />);
  const wrapper = mountRender({ children });
  expect(children).toHaveBeenCalledTimes(2);

  resizeWindowTo();
  // 2 + 1 (new measurement)
  expect(children).toHaveBeenCalledTimes(3);

  wrapper.unmount();
  resizeWindowTo();
  expect(children).toHaveBeenCalledTimes(3);
});

function mountRender(props: ScreenPositionFixer['props']) {
  return mount(<ScreenPositionFixer {...props} />);
}
