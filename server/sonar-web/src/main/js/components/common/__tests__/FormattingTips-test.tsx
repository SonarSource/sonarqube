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
import { click } from '../../../helpers/testUtils';
import FormattingTips from '../FormattingTips';

const originalOpen = window.open;

beforeAll(() => {
  Object.defineProperty(window, 'open', {
    writable: true,
    value: jest.fn(),
  });
});

afterAll(() => {
  Object.defineProperty(window, 'open', {
    writable: true,
    value: originalOpen,
  });
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should correctly open a new window', () => {
  const wrapper = shallowRender();
  expect(window.open).not.toHaveBeenCalled();
  click(wrapper.find('a'));
  expect(window.open).toHaveBeenCalled();
});

function shallowRender(props: Partial<FormattingTips['props']> = {}) {
  return shallow<FormattingTips>(<FormattingTips {...props} />);
}
