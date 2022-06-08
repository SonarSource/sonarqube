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
import { shallow } from 'enzyme';
import * as React from 'react';
import { KeyboardKeys } from '../../../helpers/keycodes';
import { keydown } from '../../../helpers/testUtils';
import EscKeydownHandler from '../EscKeydownHandler';

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

it('should correctly trigger the keydown handler when hitting Esc', () => {
  const onKeydown = jest.fn();
  shallowRender({ onKeydown });
  jest.runAllTimers();
  keydown({ key: KeyboardKeys.Escape });
  expect(onKeydown).toBeCalled();
});

function shallowRender(props: Partial<EscKeydownHandler['props']> = {}) {
  return shallow<EscKeydownHandler>(
    <EscKeydownHandler onKeydown={jest.fn()} {...props}>
      <span>Hi there</span>
    </EscKeydownHandler>
  );
}
