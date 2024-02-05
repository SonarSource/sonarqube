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
import {
  ButtonPrimary,
  ButtonSecondary,
  FlagMessage,
  FormField,
  InputField,
  Modal,
  Spinner,
} from 'design-system';
import * as React from 'react';
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

  const { mutate: createUser, isPending: isLoadingCreate } = usePostUserMutation();
  const { mutate: updateUser, isPending: isLoadingUserUpdate } = useUpdateUserMutation();

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

  React.useEffect(() => {
    document.getElementById('it__error-message')?.scrollIntoView({
      block: 'start',
    });
  }, [error]);

  const handleClose = () => {
    props.onClose();
  };

  const handleCreateUser = (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();
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

  const handleUpdateUser = (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();
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
    <Modal
      headerTitle={header}
      onClose={handleClose}
      body={
        <form
          autoComplete="off"
          id="user-form"
          onSubmit={user ? handleUpdateUser : handleCreateUser}
        >
          {error && (
            <FlagMessage id="it__error-message" className="sw-mb-4" variant="error">
              {error}
            </FlagMessage>
          )}

          {!error && user && !user.local && (
            <FlagMessage className="sw-mb-4" variant="warning">
              {translate('users.cannot_update_delegated_user')}
            </FlagMessage>
          )}
          <div className="sw-mb-4">
            <MandatoryFieldsExplanation />
          </div>

          {!user && (
            <FormField
              description={translateWithParameters('users.minimum_x_characters', 3)}
              label={translate('login')}
              htmlFor="create-user-login"
              required={!isInstanceManaged}
            >
              <InputField
                autoFocus
                autoComplete="off"
                maxLength={255}
                minLength={3}
                size="full"
                id="create-user-login"
                name="login"
                onChange={(e) => setLogin(e.currentTarget.value)}
                type="text"
                value={login}
              />
            </FormField>
          )}

          <FormField
            label={translate('name')}
            htmlFor="create-user-name"
            required={!isInstanceManaged}
          >
            <InputField
              autoFocus={!!user}
              autoComplete="off"
              disabled={(user && !user.local) || isInstanceManaged}
              size="full"
              maxLength={200}
              id="create-user-name"
              name="name"
              onChange={(e) => setName(e.currentTarget.value)}
              type="text"
              value={name}
            />
          </FormField>

          <FormField label={translate('users.email')} htmlFor="create-user-email">
            <InputField
              autoComplete="off"
              disabled={(user && !user.local) || isInstanceManaged}
              size="full"
              maxLength={100}
              id="create-user-email"
              name="email"
              onChange={(e) => setEmail(e.currentTarget.value)}
              type="email"
              value={email}
            />
          </FormField>

          {!user && (
            <FormField required label={translate('password')} htmlFor="create-user-password">
              <InputField
                autoComplete="off"
                size="full"
                id="create-user-password"
                name="password"
                onChange={(e) => setPassword(e.currentTarget.value)}
                type="password"
                value={password}
              />
            </FormField>
          )}
          <FormField
            description={translate('user.login_or_email_used_as_scm_account')}
            label={translate('my_profile.scm_accounts')}
          >
            {scmAccounts.map((scm, idx) => (
              <UserScmAccountInput
                idx={idx}
                key={idx}
                onChange={handleUpdateScmAccount}
                onRemove={handleRemoveScmAccount}
                scmAccount={scm}
              />
            ))}
            <div>
              <ButtonSecondary className="it__scm-account-add" onClick={handleAddScmAccount}>
                {translate('add_verb')}
              </ButtonSecondary>
            </div>
          </FormField>
        </form>
      }
      primaryButton={
        <>
          <Spinner loading={isLoadingCreate || isLoadingUserUpdate} />
          <ButtonPrimary
            disabled={isLoadingCreate || isLoadingUserUpdate}
            type="submit"
            form="user-form"
          >
            {user ? translate('update_verb') : translate('create')}
          </ButtonPrimary>
        </>
      }
      secondaryButtonLabel={translate('cancel')}
    />
  );
}
