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
import { AxiosError, AxiosResponse } from 'axios';
import * as React from 'react';
import SimpleModal from '../../../components/controls/SimpleModal';
import { Button, ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import { Alert } from '../../../components/ui/Alert';
import MandatoryFieldMarker from '../../../components/ui/MandatoryFieldMarker';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { addGlobalErrorMessage } from '../../../helpers/globalMessages';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { parseErrorResponse } from '../../../helpers/request';
import { usePostUserMutation, useUpdateUserMutation } from '../../../queries/users';
import { RestUserDetailed } from '../../../types/users';
import UserScmAccountInput from './UserScmAccountInput';

export interface Props {
  onClose: () => void;
  user?: RestUserDetailed;
  isInstanceManaged: boolean;
}

const BAD_REQUEST = 400;
const INTERNAL_SERVER_ERROR = 500;

export default function UserForm(props: Props) {
  const { user, isInstanceManaged } = props;

  const { mutate: createUser } = usePostUserMutation();
  const { mutate: updateUser } = useUpdateUserMutation();

  const [email, setEmail] = React.useState<string>(user?.email ?? '');
  const [login, setLogin] = React.useState<string>(user?.login ?? '');
  const [name, setName] = React.useState<string>(user?.name ?? '');
  const [password, setPassword] = React.useState<string>('');
  const [scmAccounts, setScmAccounts] = React.useState<string[]>(user?.scmAccounts ?? []);
  const [error, setError] = React.useState<string | undefined>(undefined);

  const handleError = (error: AxiosError<AxiosResponse>) => {
    const { response } = error;
    const message = parseErrorResponse(response);

    if (!response || ![BAD_REQUEST, INTERNAL_SERVER_ERROR].includes(response.status)) {
      addGlobalErrorMessage(message);
    } else {
      setError(message);
    }
  };

  const handleCreateUser = () => {
    createUser(
      {
        email: email || undefined,
        login,
        name,
        password,
        scmAccounts,
      },
      { onSuccess: props.onClose, onError: handleError },
    );
  };

  const handleUpdateUser = () => {
    const { user } = props;

    updateUser(
      {
        id: user?.id!,
        data:
          isInstanceManaged || !user?.local
            ? { scmAccounts }
            : {
                email: email !== '' ? email : null,
                name,
                scmAccounts,
              },
      },
      { onSuccess: props.onClose, onError: handleError },
    );
  };

  const handleAddScmAccount = () => {
    setScmAccounts((scmAccounts) => scmAccounts.concat(''));
  };

  const handleUpdateScmAccount = (idx: number, scmAccount: string) => {
    setScmAccounts((scmAccounts) => {
      const newScmAccounts = scmAccounts.slice();
      newScmAccounts[idx] = scmAccount;
      return newScmAccounts;
    });
  };

  const handleRemoveScmAccount = (idx: number) => {
    setScmAccounts((scmAccounts) => scmAccounts.slice(0, idx).concat(scmAccounts.slice(idx + 1)));
  };

  const header = user ? translate('users.update_user') : translate('users.create_user');

  return (
    <SimpleModal
      header={header}
      onClose={props.onClose}
      onSubmit={user ? handleUpdateUser : handleCreateUser}
      size="small"
    >
      {({ onCloseClick, onFormSubmit, submitting }) => (
        <form autoComplete="off" id="user-form" onSubmit={onFormSubmit}>
          <header className="modal-head">
            <h2>{header}</h2>
          </header>

          <div className="modal-body modal-container">
            {error && <Alert variant="error">{error}</Alert>}

            {!error && user && !user.local && (
              <Alert variant="warning">{translate('users.cannot_update_delegated_user')}</Alert>
            )}

            <MandatoryFieldsExplanation className="modal-field" />

            {!user && (
              <div className="modal-field">
                <label htmlFor="create-user-login">
                  {translate('login')}
                  <MandatoryFieldMarker />
                </label>
                <input
                  autoComplete="off"
                  autoFocus
                  id="create-user-login"
                  maxLength={255}
                  minLength={3}
                  name="login"
                  onChange={(e) => setLogin(e.currentTarget.value)}
                  required={!isInstanceManaged}
                  type="text"
                  value={login}
                />
                <p className="note">{translateWithParameters('users.minimum_x_characters', 3)}</p>
              </div>
            )}
            <div className="modal-field">
              <label htmlFor="create-user-name">
                {translate('name')}
                {!isInstanceManaged && <MandatoryFieldMarker />}
              </label>
              <input
                autoComplete="off"
                autoFocus={!!user}
                disabled={(user && !user.local) || isInstanceManaged}
                id="create-user-name"
                maxLength={200}
                name="name"
                onChange={(e) => setName(e.currentTarget.value)}
                required={!isInstanceManaged}
                type="text"
                value={name}
              />
            </div>
            <div className="modal-field">
              <label htmlFor="create-user-email">{translate('users.email')}</label>
              <input
                autoComplete="off"
                disabled={(user && !user.local) || isInstanceManaged}
                id="create-user-email"
                maxLength={100}
                name="email"
                onChange={(e) => setEmail(e.currentTarget.value)}
                type="email"
                value={email}
              />
            </div>
            {!user && (
              <div className="modal-field">
                <label htmlFor="create-user-password">
                  {translate('password')}
                  <MandatoryFieldMarker />
                </label>
                <input
                  autoComplete="off"
                  id="create-user-password"
                  name="password"
                  onChange={(e) => setPassword(e.currentTarget.value)}
                  required
                  type="password"
                  value={password}
                />
              </div>
            )}
            <div className="modal-field">
              <fieldset>
                <legend>{translate('my_profile.scm_accounts')}</legend>
                {scmAccounts.map((scm, idx) => (
                  <UserScmAccountInput
                    idx={idx}
                    key={idx}
                    onChange={handleUpdateScmAccount}
                    onRemove={handleRemoveScmAccount}
                    scmAccount={scm}
                  />
                ))}
                <div className="spacer-bottom">
                  <Button className="js-scm-account-add" onClick={handleAddScmAccount}>
                    {translate('add_verb')}
                  </Button>
                </div>
              </fieldset>
              <p className="note">{translate('user.login_or_email_used_as_scm_account')}</p>
            </div>
          </div>

          <footer className="modal-foot">
            {submitting && <i className="spinner spacer-right" />}
            <SubmitButton disabled={submitting}>
              {user ? translate('update_verb') : translate('create')}
            </SubmitButton>
            <ResetButtonLink onClick={onCloseClick}>{translate('cancel')}</ResetButtonLink>
          </footer>
        </form>
      )}
    </SimpleModal>
  );
}
