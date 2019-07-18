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
import { shallow } from 'enzyme';
import * as React from 'react';
import StatPendingTime, { Props } from '../StatPendingTime';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should not render', () => {
  expect(shallowRender({ pendingCount: undefined }).type()).toBeNull();
  expect(shallowRender({ pendingCount: 0 }).type()).toBeNull();
  expect(shallowRender({ pendingTime: undefined }).type()).toBeNull();
});

it('should not render when pending time is too small', () => {
  expect(
    shallowRender({ pendingTime: 0 })
      .find('.emphasised-measure')
      .exists()
  ).toBe(false);
  expect(
    shallowRender({ pendingTime: 900 })
      .find('.emphasised-measure')
      .exists()
  ).toBe(false);
});

function shallowRender(props: Partial<Props> = {}) {
  return shallow(<StatPendingTime pendingCount={5} pendingTime={15420} {...props} />);
}
