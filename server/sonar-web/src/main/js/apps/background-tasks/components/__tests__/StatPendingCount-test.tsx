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
import { Props, StatPendingCount } from '../StatPendingCount';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should not render', () => {
  expect(shallowRender({ pendingCount: undefined }).type()).toBeNull();
});

it('should not show cancel pending button', () => {
  expect(
    shallowRender({ pendingCount: 0 })
      .find('ConfirmButton')
      .exists()
  ).toBe(false);
  expect(
    shallowRender({ isSystemAdmin: false })
      .find('ConfirmButton')
      .exists()
  ).toBe(false);
});

it('should trigger cancelling pending', () => {
  const onCancelAllPending = jest.fn();
  const result = shallowRender({ onCancelAllPending });
  expect(onCancelAllPending).not.toBeCalled();
  result.find('ConfirmButton').prop<Function>('onConfirm')();
  expect(onCancelAllPending).toBeCalled();
});

function shallowRender(props: Partial<Props> = {}) {
  return shallow(
    <StatPendingCount
      isSystemAdmin={true}
      onCancelAllPending={jest.fn()}
      pendingCount={5}
      {...props}
    />
  );
}
