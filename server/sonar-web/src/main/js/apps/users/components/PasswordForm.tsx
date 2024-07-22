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

import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import { FlagMessage, FormField, InputField, Modal, addGlobalSuccessMessage } from 'design-system';
import * as React from 'react';
import { changePassword } from '../../../api/users';
import { CurrentUserContext } from '../../../app/components/current-user/CurrentUserContext';
import { translate } from '../../../helpers/l10n';
import { ChangePasswordResults, RestUserDetailed, isLoggedIn } from '../../../types/users';

interface Props {
  onClose: () => void;
  user: RestUserDetailed;
}

const PASSWORD_FORM_ID = 'user-password-form';

export default function PasswordForm(props: Props) {
  const { user } = props;
  const [confirmPassword, setConfirmPassword] = React.useState('');

  const [errorTranslationKey, setErrorTranslationKey] = React.useState<string | undefined>(
    undefined,
  );

  const [newPassword, setNewPassword] = React.useState('');
  const [oldPassword, setOldPassword] = React.useState('');
  const [submitting, setSubmitting] = React.useState(false);

  const userContext = React.useContext(CurrentUserContext);
  const currentUser = userContext?.currentUser;
  const isCurrentUser = isLoggedIn(currentUser) && currentUser.login === user.login;

  const handleError = (result: ChangePasswordResults) => {
    if (result === ChangePasswordResults.OldPasswordIncorrect) {
      setErrorTranslationKey('user.old_password_incorrect');
      setSubmitting(false);
    } else if (result === ChangePasswordResults.NewPasswordSameAsOld) {
      setErrorTranslationKey('user.new_password_same_as_old');
      setSubmitting(false);
    }
  };

  const handleChangePassword = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (newPassword.length > 0 && newPassword === confirmPassword) {
      setSubmitting(true);

      changePassword({
        login: user.login,
        password: newPassword,
        previousPassword: oldPassword,
      }).then(() => {
        addGlobalSuccessMessage(translate('my_profile.password.changed'));
        props.onClose();
      }, handleError);
    }
  };

  const header = translate('my_profile.password.title');

  return (
    <Modal
      headerTitle={header}
      body={
        <form
          autoComplete="off"
          id={PASSWORD_FORM_ID}
          className="sw-mb-2"
          onSubmit={handleChangePassword}
        >
          {errorTranslationKey && (
            <FlagMessage variant="error" className="sw-mb-4">
              {translate(errorTranslationKey)}
            </FlagMessage>
          )}

          {isCurrentUser && (
            <FormField
              htmlFor="old-user-password"
              label={translate('my_profile.password.old')}
              required
            >
              <InputField
                autoFocus
                id="old-user-password"
                name="old-password"
                onChange={(event) => setOldPassword(event.currentTarget.value)}
                required
                size="full"
                type="password"
                value={oldPassword}
              />
              <input className="sw-hidden" aria-hidden name="old-password-fake" type="password" />
            </FormField>
          )}

          <FormField htmlFor="user-password" label={translate('my_profile.password.new')} required>
            <InputField
              autoFocus
              id="user-password"
              name="password"
              onChange={(event) => setNewPassword(event.currentTarget.value)}
              required
              type="password"
              value={newPassword}
              size="full"
            />
            <input className="sw-hidden" aria-hidden name="password-fake" type="password" />
          </FormField>

          <FormField
            htmlFor="confirm-user-password"
            label={translate('my_profile.password.confirm')}
            required
          >
            <InputField
              autoFocus
              id="confirm-user-password"
              name="confirm-password"
              onChange={(event) => setConfirmPassword(event.currentTarget.value)}
              required
              type="password"
              value={confirmPassword}
              size="full"
            />
            <input className="sw-hidden" aria-hidden name="confirm-password-fake" type="password" />
          </FormField>
        </form>
      }
      onClose={props.onClose}
      loading={submitting}
      primaryButton={
        <Button
          form={PASSWORD_FORM_ID}
          isDisabled={submitting || !newPassword || newPassword !== confirmPassword}
          type="submit"
          variety={ButtonVariety.Primary}
        >
          {translate('change_verb')}
        </Button>
      }
      secondaryButtonLabel={translate('cancel')}
    />
  );
}
