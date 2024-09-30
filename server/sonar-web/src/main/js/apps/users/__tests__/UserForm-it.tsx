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

import userEvent from '@testing-library/user-event';
import React from 'react';
import UsersServiceMock from '../../../api/mocks/UsersServiceMock';
import { mockRestUser } from '../../../helpers/testMocks';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { byLabelText, byRole, byTestId, byText } from '../../../sonar-aligned/helpers/testSelector';
import { FCProps } from '../../../types/misc';
import UserForm from '../components/UserForm';

const userHandler = new UsersServiceMock();

const ui = {
  loginInput: byRole('textbox', { name: /login/ }),
  userNameInput: byRole('textbox', { name: /name/ }),
  emailInput: byRole('textbox', { name: /email/ }),
  passwordInput: byLabelText(/^password/),
  scmAddButton: byRole('button', { name: 'add_verb' }),
  dialogSCMInputs: byRole('textbox', { name: /users.create_user.scm_account/ }),
  confirmPassword: byLabelText(/confirm_password\*/i),
  createButton: byRole('button', { name: 'create' }),
  updateButton: byRole('button', { name: 'update_verb' }),

  errorMinimum3Charatecters: byText('users.minimum_x_characters.3'),
  errorLoginAlreadyTaken: byText('users.login_already_used'),
  errorInvalidCharacter: byText('users.login_invalid_characters'),
  errorStartWithLetterOrNumber: byText('users.login_start_with_letter_or_number'),
  errorEmailInvalid: byText('users.email.invalid'),

  validCondition: byTestId('valid-condition'),
  failedCondition: byTestId('failed-condition'),

  condition1Uppercase: byText('user.password.condition.1_upper_case'),
  condition1Lowercase: byText('user.password.condition.1_lower_case'),
  condition1Number: byText('user.password.condition.1_number'),
  condition1SpecialCharacter: byText('user.password.condition.1_special_character'),
  condition12Characters: byText('user.password.condition.12_characters'),
};

beforeEach(() => {
  userHandler.reset();
});

describe('in non-managed mode', () => {
  describe('when creating', () => {
    it('should render correctly', async () => {
      renderUserForm();

      expect(await ui.loginInput.find()).toBeInTheDocument();
      expect(ui.userNameInput.get()).toBeInTheDocument();
      expect(ui.emailInput.get()).toBeInTheDocument();
      expect(ui.passwordInput.get()).toBeInTheDocument();
      expect(ui.scmAddButton.get()).toBeInTheDocument();
    });

    it('should have proper validation for login', async () => {
      const user = userEvent.setup();
      renderUserForm();

      expect(await ui.loginInput.find()).toHaveValue('');
      await user.type(ui.userNameInput.get(), 'Ken Samaras');
      await user.type(ui.emailInput.get(), 'nekfeu@screw.fr');
      await user.type(ui.passwordInput.get(), 'P@ssword12345');
      await user.type(ui.confirmPassword.get(), 'P@ssword12345');

      // Login should have at least 3 characters
      expect(ui.createButton.get()).toBeDisabled();
      await user.type(ui.loginInput.get(), 'a');
      expect(ui.errorMinimum3Charatecters.get()).toBeInTheDocument();
      expect(ui.createButton.get()).toBeDisabled();
      await user.type(ui.loginInput.get(), 'b');
      expect(ui.errorMinimum3Charatecters.get()).toBeInTheDocument();
      await user.type(ui.loginInput.get(), 'c');
      expect(ui.errorMinimum3Charatecters.query()).not.toBeInTheDocument();
      expect(ui.createButton.get()).toBeEnabled();
      await user.clear(ui.loginInput.get());

      // Login should not already be taken
      await user.type(ui.loginInput.get(), 'bob.marley');
      expect(ui.errorLoginAlreadyTaken.get()).toBeInTheDocument();
      expect(ui.createButton.get()).toBeDisabled();
      await user.clear(ui.loginInput.get());

      // login should only used valid characters
      await user.type(ui.loginInput.get(), 'abc!@#');
      expect(ui.errorInvalidCharacter.get()).toBeInTheDocument();
      expect(ui.createButton.get()).toBeDisabled();
      await user.clear(ui.loginInput.get());
      await user.type(ui.loginInput.get(), 'abc-_@.');
      expect(ui.errorInvalidCharacter.query()).not.toBeInTheDocument();
      expect(ui.createButton.get()).toBeEnabled();
      await user.clear(ui.loginInput.get());

      // login should start with a letter, a number or _
      await user.type(ui.loginInput.get(), '@abc');
      expect(ui.errorStartWithLetterOrNumber.get()).toBeInTheDocument();
      expect(ui.createButton.get()).toBeDisabled();
      await user.clear(ui.loginInput.get());
      await user.type(ui.loginInput.get(), '.abc');
      expect(ui.errorStartWithLetterOrNumber.get()).toBeInTheDocument();
      expect(ui.createButton.get()).toBeDisabled();
      await user.clear(ui.loginInput.get());
      await user.type(ui.loginInput.get(), '-abc');
      expect(ui.errorStartWithLetterOrNumber.get()).toBeInTheDocument();
      expect(ui.createButton.get()).toBeDisabled();
      await user.clear(ui.loginInput.get());
      await user.type(ui.loginInput.get(), '_abc');
      expect(ui.errorStartWithLetterOrNumber.query()).not.toBeInTheDocument();
      expect(ui.createButton.get()).toBeEnabled();
      await user.clear(ui.loginInput.get());
      await user.type(ui.loginInput.get(), '1abc');
      expect(ui.errorStartWithLetterOrNumber.query()).not.toBeInTheDocument();
      expect(ui.createButton.get()).toBeEnabled();
    });

    it('should have proper validation for email', async () => {
      const user = userEvent.setup();
      renderUserForm();

      expect(await ui.loginInput.find()).toHaveValue('');
      await user.type(ui.loginInput.get(), 'Nekfeu');
      await user.type(ui.userNameInput.get(), 'Ken Samaras');
      await user.type(ui.passwordInput.get(), 'P@ssword12345');
      await user.type(ui.confirmPassword.get(), 'P@ssword12345');

      // Email is not mandatory
      expect(ui.createButton.get()).toBeEnabled();

      // Email should be valid though
      await user.type(ui.emailInput.get(), 'nekfeu');
      expect(ui.createButton.get()).toBeDisabled();
      // just to loose focus...
      await user.click(ui.loginInput.get());
      expect(ui.errorEmailInvalid.get()).toBeInTheDocument();
      await user.type(ui.emailInput.get(), '@screw.fr');
      expect(ui.errorEmailInvalid.query()).not.toBeInTheDocument();
      expect(ui.createButton.get()).toBeEnabled();
    });

    it('should have proper validation for password', async () => {
      const user = userEvent.setup();
      renderUserForm();

      expect(await ui.loginInput.find()).toHaveValue('');
      await user.type(ui.loginInput.get(), 'Nekfeu');
      await user.type(ui.userNameInput.get(), 'Ken Samaras');
      await user.type(ui.emailInput.get(), 'nekfeu@screw.fr');
      expect(ui.createButton.get()).toBeDisabled();

      // Password should have at least 1 Uppercase
      await user.type(ui.passwordInput.get(), 'P');
      expect(ui.createButton.get()).toBeDisabled();
      expect(ui.validCondition.getAll()).toContain(ui.condition1Uppercase.get());
      expect(ui.failedCondition.getAll()).toContain(ui.condition1Lowercase.get());
      expect(ui.failedCondition.getAll()).toContain(ui.condition1Number.get());
      expect(ui.failedCondition.getAll()).toContain(ui.condition1SpecialCharacter.get());
      expect(ui.failedCondition.getAll()).toContain(ui.condition12Characters.get());

      // Password should have at least 1 Lowercase
      await user.type(ui.passwordInput.get(), 'assword');
      expect(ui.createButton.get()).toBeDisabled();
      expect(ui.validCondition.getAll()).toContain(ui.condition1Uppercase.get());
      expect(ui.validCondition.getAll()).toContain(ui.condition1Lowercase.get());
      expect(ui.failedCondition.getAll()).toContain(ui.condition1Number.get());
      expect(ui.failedCondition.getAll()).toContain(ui.condition1SpecialCharacter.get());
      expect(ui.failedCondition.getAll()).toContain(ui.condition12Characters.get());

      // Password should have at least 1 Number
      await user.type(ui.passwordInput.get(), '1');
      expect(ui.createButton.get()).toBeDisabled();
      expect(ui.validCondition.getAll()).toContain(ui.condition1Uppercase.get());
      expect(ui.validCondition.getAll()).toContain(ui.condition1Lowercase.get());
      expect(ui.validCondition.getAll()).toContain(ui.condition1Number.get());
      expect(ui.failedCondition.getAll()).toContain(ui.condition1SpecialCharacter.get());
      expect(ui.failedCondition.getAll()).toContain(ui.condition12Characters.get());

      // Password should have at least 1 Special Character
      await user.type(ui.passwordInput.get(), '$');
      expect(ui.createButton.get()).toBeDisabled();
      expect(ui.validCondition.getAll()).toContain(ui.condition1Uppercase.get());
      expect(ui.validCondition.getAll()).toContain(ui.condition1Lowercase.get());
      expect(ui.validCondition.getAll()).toContain(ui.condition1Number.get());
      expect(ui.validCondition.getAll()).toContain(ui.condition1SpecialCharacter.get());
      expect(ui.failedCondition.getAll()).toContain(ui.condition12Characters.get());

      // Password should have at least 12 characters
      await user.type(ui.passwordInput.get(), '74');
      expect(ui.passwordInput.get()).toHaveValue('Password1$74');
      expect(ui.createButton.get()).toBeDisabled();
      expect(ui.validCondition.getAll()).toContain(ui.condition1Uppercase.get());
      expect(ui.validCondition.getAll()).toContain(ui.condition1Lowercase.get());
      expect(ui.validCondition.getAll()).toContain(ui.condition1Number.get());
      expect(ui.validCondition.getAll()).toContain(ui.condition1SpecialCharacter.get());
      expect(ui.validCondition.getAll()).toContain(ui.condition12Characters.get());

      // Password should match
      await user.type(ui.confirmPassword.get(), 'Password1$');
      expect(ui.condition1Uppercase.query()).not.toBeInTheDocument();
      expect(ui.condition1Lowercase.query()).not.toBeInTheDocument();
      expect(ui.condition1Number.query()).not.toBeInTheDocument();
      expect(ui.condition1SpecialCharacter.query()).not.toBeInTheDocument();
      expect(ui.condition12Characters.query()).not.toBeInTheDocument();
      expect(ui.createButton.get()).toBeDisabled();
      await user.type(ui.confirmPassword.get(), '74');
      expect(ui.createButton.get()).toBeEnabled();
    });
  });

  describe('when updating', () => {
    it('should render correctly', async () => {
      renderUserForm({ user: mockRestUser({ login: 'nekfeu', name: 'Ken Samaras', email: '' }) });

      expect(await ui.userNameInput.get()).toBeInTheDocument();
      expect(ui.emailInput.get()).toBeInTheDocument();
      expect(ui.scmAddButton.get()).toBeInTheDocument();
    });

    it('should validate email', async () => {
      const user = userEvent.setup();
      renderUserForm({ user: mockRestUser({ login: 'nekfeu', name: 'Ken Samaras', email: '' }) });

      expect(await ui.userNameInput.find()).toHaveValue('Ken Samaras');
      expect(ui.emailInput.get()).toHaveValue('');
      expect(ui.updateButton.get()).toBeEnabled();

      await user.type(ui.emailInput.get(), 'nekfeu');
      expect(ui.updateButton.get()).toBeDisabled();
      // just to loose focus...
      await user.click(ui.userNameInput.get());
      expect(ui.errorEmailInvalid.get()).toBeInTheDocument();
      await user.type(ui.emailInput.get(), '@screw.fr');
      expect(ui.errorEmailInvalid.query()).not.toBeInTheDocument();
      expect(ui.updateButton.get()).toBeEnabled();
    });
  });
});

describe('in managed mode', () => {
  describe('when updating', () => {
    it('should render correctly', async () => {
      renderUserForm({
        isInstanceManaged: true,
        user: mockRestUser({ login: 'nekfeu', name: 'Ken Samaras', email: '' }),
      });

      expect(await ui.userNameInput.find()).toBeDisabled();
      expect(ui.emailInput.get()).toBeDisabled();
      expect(ui.scmAddButton.get()).toBeInTheDocument();
    });
  });
});

function renderUserForm(props: Partial<FCProps<typeof UserForm>> = {}) {
  return renderComponent(<UserForm isInstanceManaged={false} onClose={jest.fn()} {...props} />);
}
