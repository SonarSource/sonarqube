/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { changePassword } from '../../../../api/users';
import { mockUser } from '../../../../helpers/testMocks';
import { mockEvent, waitAndUpdate } from '../../../../helpers/testUtils';
import { ChangePasswordResults } from '../../../../types/users';
import PasswordForm from '../PasswordForm';

const password = 'new password asdf';

jest.mock('../../../../api/users', () => ({
  changePassword: jest.fn(() => Promise.resolve()),
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should handle password change', async () => {
  const onClose = jest.fn();
  const wrapper = shallowRender({ onClose });

  wrapper.setState({ newPassword: password, confirmPassword: password });
  wrapper.instance().handleChangePassword(mockEvent({ preventDefault: jest.fn() }));

  await waitAndUpdate(wrapper);

  expect(onClose).toHaveBeenCalled();
});

it('should handle password change error when new password is same as old', async () => {
  const wrapper = shallowRender();

  jest.mocked(changePassword).mockRejectedValue(ChangePasswordResults.NewPasswordSameAsOld);
  wrapper.setState({ newPassword: password, confirmPassword: password });
  wrapper.instance().mounted = true;
  wrapper.instance().handleChangePassword(mockEvent({ preventDefault: jest.fn() }));

  await waitAndUpdate(wrapper);

  expect(wrapper.state().errorTranslationKey).toBe('user.new_password_same_as_old');
});

it('should handle password change error when old password is incorrect', async () => {
  const wrapper = shallowRender();

  jest.mocked(changePassword).mockRejectedValue(ChangePasswordResults.OldPasswordIncorrect);

  wrapper.setState({ newPassword: password, confirmPassword: password });
  wrapper.instance().mounted = true;
  wrapper.instance().handleChangePassword(mockEvent({ preventDefault: jest.fn() }));

  await waitAndUpdate(wrapper);

  expect(wrapper.state().errorTranslationKey).toBe('user.old_password_incorrect');
});

it('should handle form changes', () => {
  const wrapper = shallowRender();

  wrapper.instance().handleConfirmPasswordChange(mockEvent({ currentTarget: { value: 'pwd' } }));
  expect(wrapper.state().confirmPassword).toBe('pwd');

  wrapper.instance().handleNewPasswordChange(mockEvent({ currentTarget: { value: 'pwd' } }));
  expect(wrapper.state().newPassword).toBe('pwd');

  wrapper.instance().handleOldPasswordChange(mockEvent({ currentTarget: { value: 'pwd' } }));
  expect(wrapper.state().oldPassword).toBe('pwd');
});

function shallowRender(props: Partial<PasswordForm['props']> = {}) {
  return shallow<PasswordForm>(
    <PasswordForm isCurrentUser={true} onClose={jest.fn()} user={mockUser()} {...props} />
  );
}
