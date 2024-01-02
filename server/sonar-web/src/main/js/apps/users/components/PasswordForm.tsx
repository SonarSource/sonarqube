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
import * as React from 'react';
import { changePassword } from '../../../api/users';
import { CurrentUserContext } from '../../../app/components/current-user/CurrentUserContext';
import Modal from '../../../components/controls/Modal';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import { Alert } from '../../../components/ui/Alert';
import MandatoryFieldMarker from '../../../components/ui/MandatoryFieldMarker';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { addGlobalSuccessMessage } from '../../../helpers/globalMessages';
import { translate } from '../../../helpers/l10n';
import { ChangePasswordResults, RestUserDetailed, isLoggedIn } from '../../../types/users';

interface Props {
  onClose: () => void;
  user: RestUserDetailed;
}

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
    <Modal contentLabel={header} onRequestClose={props.onClose} size="small">
      <form autoComplete="off" id="user-password-form" onSubmit={handleChangePassword}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>
        <div className="modal-body">
          {errorTranslationKey && <Alert variant="error">{translate(errorTranslationKey)}</Alert>}

          <MandatoryFieldsExplanation className="modal-field" />

          {isCurrentUser && (
            <div className="modal-field">
              <label htmlFor="old-user-password">
                {translate('my_profile.password.old')}
                <MandatoryFieldMarker />
              </label>
              {/* keep this fake field to hack browser autofill */}
              <input className="hidden" aria-hidden name="old-password-fake" type="password" />
              <input
                id="old-user-password"
                name="old-password"
                onChange={(event) => setOldPassword(event.currentTarget.value)}
                required
                type="password"
                value={oldPassword}
              />
            </div>
          )}
          <div className="modal-field">
            <label htmlFor="user-password">
              {translate('my_profile.password.new')}
              <MandatoryFieldMarker />
            </label>
            {/* keep this fake field to hack browser autofill */}
            <input className="hidden" aria-hidden name="password-fake" type="password" />
            <input
              id="user-password"
              name="password"
              onChange={(event) => setNewPassword(event.currentTarget.value)}
              required
              type="password"
              value={newPassword}
            />
          </div>
          <div className="modal-field">
            <label htmlFor="confirm-user-password">
              {translate('my_profile.password.confirm')}
              <MandatoryFieldMarker />
            </label>
            {/* keep this fake field to hack browser autofill */}
            <input className="hidden" aria-hidden name="confirm-password-fake" type="password" />
            <input
              id="confirm-user-password"
              name="confirm-password"
              onChange={(event) => setConfirmPassword(event.currentTarget.value)}
              required
              type="password"
              value={confirmPassword}
            />
          </div>
        </div>
        <footer className="modal-foot">
          {submitting && <i className="spinner spacer-right" />}
          <SubmitButton disabled={submitting || !newPassword || newPassword !== confirmPassword}>
            {translate('change_verb')}
          </SubmitButton>
          <ResetButtonLink onClick={props.onClose}>{translate('cancel')}</ResetButtonLink>
        </footer>
      </form>
    </Modal>
  );
}
