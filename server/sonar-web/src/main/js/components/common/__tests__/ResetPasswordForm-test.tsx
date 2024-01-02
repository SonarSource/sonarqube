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
import { changePassword } from '../../../api/users';
import { mockLoggedInUser } from '../../../helpers/testMocks';
import { mockEvent, waitAndUpdate } from '../../../helpers/testUtils';
import ResetPasswordForm from '../ResetPasswordForm';

jest.mock('../../../api/users', () => ({
  changePassword: jest.fn().mockResolvedValue({}),
}));

it('should trigger on password change prop', () => {
  const onPasswordChange = jest.fn();
  const wrapper = shallowRender({ onPasswordChange });
  wrapper.instance().handleSuccessfulChange();
  expect(onPasswordChange).not.toHaveBeenCalled();
  wrapper.instance().oldPassword = { value: '' } as HTMLInputElement;
  wrapper.instance().password = { value: '' } as HTMLInputElement;
  wrapper.instance().passwordConfirmation = { value: '' } as HTMLInputElement;
  wrapper.instance().handleSuccessfulChange();
  expect(onPasswordChange).toHaveBeenCalled();
});

it('should not trigger password change', () => {
  const wrapper = shallowRender();
  wrapper.instance().oldPassword = { value: 'testold' } as HTMLInputElement;
  wrapper.instance().password = { value: 'test', focus: () => {} } as HTMLInputElement;
  wrapper.instance().passwordConfirmation = { value: 'test1' } as HTMLInputElement;
  wrapper.instance().handleChangePassword(mockEvent());
  expect(changePassword).not.toHaveBeenCalled();
  expect(wrapper.state().errors).toBeDefined();
});

it('should trigger password change', async () => {
  const user = mockLoggedInUser();
  const wrapper = shallowRender({ user });
  wrapper.instance().handleChangePassword(mockEvent());
  await waitAndUpdate(wrapper);
  expect(changePassword).not.toHaveBeenCalled();

  wrapper.instance().oldPassword = { value: 'testold' } as HTMLInputElement;
  wrapper.instance().password = { value: 'test' } as HTMLInputElement;
  wrapper.instance().passwordConfirmation = { value: 'test' } as HTMLInputElement;
  wrapper.instance().handleChangePassword(mockEvent());
  await waitAndUpdate(wrapper);

  expect(changePassword).toHaveBeenCalledWith({
    login: user.login,
    password: 'test',
    previousPassword: 'testold',
  });
});

it('renders correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

function shallowRender(props?: Partial<ResetPasswordForm['props']>) {
  return shallow<ResetPasswordForm>(<ResetPasswordForm user={mockLoggedInUser()} {...props} />);
}
