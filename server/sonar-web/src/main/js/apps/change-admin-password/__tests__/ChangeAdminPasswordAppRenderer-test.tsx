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
import { mockLocation } from '../../../helpers/testMocks';
import { change, submit } from '../../../helpers/testUtils';
import ChangeAdminPasswordAppRenderer, {
  ChangeAdminPasswordAppRendererProps
} from '../ChangeAdminPasswordAppRenderer';
import { DEFAULT_ADMIN_PASSWORD } from '../constants';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ canAdmin: false })).toMatchSnapshot('access denied');
  expect(shallowRender({ canSubmit: false })).toMatchSnapshot('cannot submit');
  expect(shallowRender({ submitting: true })).toMatchSnapshot('submitting');
  expect(
    shallowRender({
      passwordValue: DEFAULT_ADMIN_PASSWORD,
      confirmPasswordValue: DEFAULT_ADMIN_PASSWORD
    })
  ).toMatchSnapshot('trying to use default admin password');
  expect(shallowRender({ success: true })).toMatchSnapshot('success');
});

it('should correctly react to input changes', () => {
  const onConfirmPasswordChange = jest.fn();
  const onPasswordChange = jest.fn();
  const wrapper = shallowRender({ onConfirmPasswordChange, onPasswordChange });

  change(wrapper.find('#user-password'), 'new pass');
  change(wrapper.find('#confirm-user-password'), 'confirm pass');
  expect(onPasswordChange).toBeCalledWith('new pass');
  expect(onConfirmPasswordChange).toBeCalledWith('confirm pass');
});

it('should correctly submit the form', () => {
  const onSubmit = jest.fn();
  const wrapper = shallowRender({ onSubmit });

  submit(wrapper.find('form'));
  expect(onSubmit).toBeCalled();
});

function shallowRender(props: Partial<ChangeAdminPasswordAppRendererProps> = {}) {
  return shallow<ChangeAdminPasswordAppRendererProps>(
    <ChangeAdminPasswordAppRenderer
      canAdmin={true}
      canSubmit={true}
      passwordValue="password"
      confirmPasswordValue="confirm"
      onConfirmPasswordChange={jest.fn()}
      onPasswordChange={jest.fn()}
      onSubmit={jest.fn()}
      submitting={false}
      success={false}
      location={mockLocation()}
      {...props}
    />
  );
}
