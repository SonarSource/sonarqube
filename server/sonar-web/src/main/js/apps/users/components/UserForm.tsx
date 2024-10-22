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

import { Button, ButtonVariety, IconCheckCircle, IconError, Text } from '@sonarsource/echoes-react';
import { debounce } from 'lodash';
import * as React from 'react';
import { FlagMessage, FormField, InputField, Modal, Spinner } from '~design-system';
import EmailIput, { EmailChangeHandlerParams } from '../../../components/common/EmailInput';
import UserPasswordInput, {
  PasswordChangeHandlerParams,
} from '../../../components/common/UserPasswordInput';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import {
  usePostUserMutation,
  useUpdateUserMutation,
  useUsersQueries,
} from '../../../queries/users';
import { RestUserDetailed } from '../../../types/users';
import { DEBOUNCE_DELAY } from '../../background-tasks/constants';
import UserScmAccountInput from './UserScmAccountInput';

export interface Props {
  isInstanceManaged: boolean;
  onClose: () => void;
  user?: RestUserDetailed;
}

const MINIMUM_LOGIN_LENGTH = 3;
const MAXIMUM_LOGIN_LENGTH = 255;
const MINIMUM_NAME_LENGTH = 1;
const MAXIMUM_NAME_LENGTH = 200;

export default function UserForm(props: Props) {
  const { user, isInstanceManaged, onClose } = props;
  const isCreateUserForm = !user;
  const [email, setEmail] = React.useState<EmailChangeHandlerParams>({
    value: user?.email ?? '',
    isValid: false,
  });
  const [login, setLogin] = React.useState<string>(user?.login ?? '');
  const [name, setName] = React.useState<string | undefined>(user?.name);
  const [password, setPassword] = React.useState<PasswordChangeHandlerParams>({
    value: '',
    isValid: false,
  });
  const [scmAccounts, setScmAccounts] = React.useState<string[]>(user?.scmAccounts ?? []);

  const { mutate: createUser, isPending: isLoadingCreate } = usePostUserMutation();
  const { mutate: updateUser, isPending: isLoadingUserUpdate } = useUpdateUserMutation();

  const { data } = useUsersQueries<RestUserDetailed>(
    {
      q: login,
    },
    Boolean(login !== '' && isCreateUserForm),
  );

  const users = React.useMemo(() => data?.pages.flatMap((page) => page.users) ?? [], [data]);
  const isLoginTooShort = login.length < MINIMUM_LOGIN_LENGTH && login !== '';
  const isLoginAlreadyUsed = users.some((u) => u.login === login);
  const doesLoginHaveValidCharacter = login !== '' ? /^[a-zA-Z0-9._@-]+$/.test(login) : true;
  const doesLoginStartWithLetterOrNumber = login !== '' ? /^\w.*/.test(login) : true;
  const isLoginValid =
    login.length >= MINIMUM_LOGIN_LENGTH &&
    !isLoginAlreadyUsed &&
    doesLoginHaveValidCharacter &&
    doesLoginStartWithLetterOrNumber;
  const fieldsdMissing = user ? false : name === '' || login === '' || !password.isValid;
  const fieldsValid = user
    ? false
    : name === undefined || name.trim() === '' || !isLoginValid || !password.isValid;
  const nameIsValid = name !== undefined && name.trim() !== '';
  const nameIsInvalid = name !== undefined && name.trim() === '';
  const isEmailValid =
    (user && !user.local) || isInstanceManaged || email.value === '' ? false : !email.isValid;

  const handleCreateUser = (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();

    createUser(
      {
        email: email.value !== '' ? email.value : undefined,
        login,
        name: name !== undefined ? name : '',
        password: password.value,
        scmAccounts,
      },
      { onSuccess: onClose },
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
                email: email.value !== '' ? email.value : null,
                name,
                scmAccounts,
              },
      },
      { onSuccess: onClose },
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

  const changeHandler = (event: React.ChangeEvent<HTMLInputElement>) => {
    setLogin(event.target.value);
  };

  const debouncedChangeHandler = React.useMemo(() => debounce(changeHandler, DEBOUNCE_DELAY), []);

  return (
    <Modal
      headerTitle={user ? translate('users.update_user') : translate('users.create_user')}
      onClose={onClose}
      body={
        <form
          autoComplete="off"
          id="user-form"
          onSubmit={user ? handleUpdateUser : handleCreateUser}
        >
          {user && !user.local && (
            <FlagMessage className="sw-mb-4" variant="warning">
              {translate('users.cannot_update_delegated_user')}
            </FlagMessage>
          )}

          <div className="sw-mb-4">
            <MandatoryFieldsExplanation />
          </div>

          {isCreateUserForm && (
            <FormField
              label={translate('login')}
              htmlFor="create-user-login"
              required={!isInstanceManaged}
            >
              <div className="sw-flex sw-items-center">
                <InputField
                  autoFocus
                  autoComplete="off"
                  isInvalid={
                    isLoginAlreadyUsed ||
                    isLoginTooShort ||
                    !doesLoginHaveValidCharacter ||
                    !doesLoginStartWithLetterOrNumber
                  }
                  isValid={!isLoginAlreadyUsed && login.length >= MINIMUM_LOGIN_LENGTH}
                  maxLength={MAXIMUM_LOGIN_LENGTH}
                  minLength={MINIMUM_LOGIN_LENGTH}
                  size="full"
                  id="create-user-login"
                  name="login"
                  onChange={debouncedChangeHandler}
                  type="text"
                />
                {(isLoginTooShort || isLoginAlreadyUsed) && (
                  <IconError color="echoes-color-icon-danger" className="sw-ml-2" />
                )}
                {isLoginValid && (
                  <IconCheckCircle color="echoes-color-icon-success" className="sw-ml-2" />
                )}
              </div>

              {!doesLoginHaveValidCharacter && (
                <Text colorOverride="echoes-color-text-danger" className="sw-mt-2">
                  {translate('users.login_invalid_characters')}
                </Text>
              )}

              {isLoginAlreadyUsed && (
                <Text colorOverride="echoes-color-text-danger" className="sw-mt-2">
                  {translate('users.login_already_used')}
                </Text>
              )}

              {!doesLoginStartWithLetterOrNumber && (
                <Text colorOverride="echoes-color-text-danger" className="sw-mt-2">
                  {translate('users.login_start_with_letter_or_number')}
                </Text>
              )}

              {isLoginTooShort && login !== '' && (
                <Text colorOverride="echoes-color-text-danger" className="sw-mt-2">
                  {translateWithParameters('users.minimum_x_characters', MINIMUM_LOGIN_LENGTH)}
                </Text>
              )}
            </FormField>
          )}

          {isCreateUserForm && (
            <UserPasswordInput
              value={password.value}
              onChange={(password) => setPassword(password)}
            />
          )}

          <FormField
            label={translate('name')}
            htmlFor="create-user-name"
            required={!isInstanceManaged}
          >
            <div className="sw-flex sw-items-center">
              <InputField
                isValid={isCreateUserForm ? nameIsValid : undefined}
                isInvalid={nameIsInvalid}
                autoFocus={!!user}
                autoComplete="off"
                disabled={(user && !user.local) || isInstanceManaged}
                size="full"
                maxLength={MAXIMUM_NAME_LENGTH}
                id="create-user-name"
                name="name"
                onChange={(e) => setName(e.currentTarget.value)}
                type="text"
                value={name === undefined ? '' : name}
              />
              {nameIsInvalid && <IconError color="echoes-color-icon-danger" className="sw-ml-2" />}
              {isCreateUserForm && nameIsValid && (
                <IconCheckCircle color="echoes-color-icon-success" className="sw-ml-2" />
              )}
            </div>
            {nameIsInvalid && (
              <Text colorOverride="echoes-color-text-danger" className="sw-mt-2">
                {translateWithParameters('users.minimum_x_characters', MINIMUM_NAME_LENGTH)}
              </Text>
            )}
          </FormField>

          <FormField label={translate('users.email')} htmlFor="create-user-email">
            <EmailIput
              id="create-user-email"
              isDisabled={(user && !user.local) || isInstanceManaged}
              onChange={setEmail}
              value={email.value}
            />
          </FormField>

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
              <Button className="it__scm-account-add" onClick={handleAddScmAccount}>
                {translate('add_verb')}
              </Button>
            </div>
          </FormField>
        </form>
      }
      primaryButton={
        <>
          <Spinner loading={isLoadingCreate || isLoadingUserUpdate} />

          <Button
            variety={ButtonVariety.Primary}
            isDisabled={
              isLoadingCreate ||
              isLoadingUserUpdate ||
              fieldsdMissing ||
              isEmailValid ||
              fieldsValid
            }
            type="submit"
            form="user-form"
          >
            {user ? translate('update_verb') : translate('create')}
          </Button>
        </>
      }
      secondaryButtonLabel={translate('cancel')}
    />
  );
}
