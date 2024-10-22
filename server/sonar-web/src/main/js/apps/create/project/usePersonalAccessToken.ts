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

import { useEffect, useState } from 'react';
import {
  checkPersonalAccessTokenIsValid,
  setAlmPersonalAccessToken,
} from '../../../api/alm-integrations';
import { translate } from '../../../helpers/l10n';
import { AlmInstanceBase } from '../../../types/alm-settings';
import { tokenExistedBefore } from './utils';

export interface PATType {
  checkingPat: boolean;
  firstConnection: boolean;
  handlePasswordChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  handleSubmit: (e: React.SyntheticEvent<HTMLFormElement>) => Promise<void>;
  handleUsernameChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  password: string;
  submitting: boolean;
  touched: boolean;
  username?: string;
  validationErrorMessage?: string;
  validationFailed: boolean;
}

export const usePersonalAccessToken = (
  almSetting: AlmInstanceBase,
  resetPat: boolean,
  onPersonalAccessTokenCreated: () => void,
): PATType => {
  const [checkingPat, setCheckingPat] = useState(false);
  const [touched, setTouched] = useState(false);
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [validationFailed, setValidationFailed] = useState(false);
  const [validationErrorMessage, setValidationErrorMessage] = useState<string | undefined>();
  const [firstConnection, setFirstConnection] = useState(false);
  const [username, setUsername] = useState('');

  useEffect(() => {
    const checkPATAndUpdateView = async () => {
      const { key } = almSetting;

      // We don't need to check PAT if we want to reset
      if (!resetPat) {
        setCheckingPat(true);
        const { patIsValid, error } = await checkPersonalAccessTokenIsValid(key)
          .then(({ status, error }) => ({ patIsValid: status, error }))
          .catch(() => ({ patIsValid: false, error: translate('default_error_message') }));
        if (patIsValid) {
          onPersonalAccessTokenCreated();
          return;
        }
        // This is the initial message when no token was provided
        if (tokenExistedBefore(error)) {
          setCheckingPat(false);
          setFirstConnection(true);
        } else {
          setCheckingPat(false);
          setValidationFailed(true);
          setValidationErrorMessage(error);
        }
      }
    };
    checkPATAndUpdateView();
  }, [almSetting, resetPat, onPersonalAccessTokenCreated]);

  const handleUsernameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setTouched(true);
    setUsername(event.target.value);
  };

  const handlePasswordChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setTouched(true);
    setPassword(event.target.value);
  };

  const handleSubmit = async (e: React.SyntheticEvent<HTMLFormElement>) => {
    const { key } = almSetting;

    e.preventDefault();
    if (password) {
      setSubmitting(true);

      await setAlmPersonalAccessToken(key, password, username).catch(() => {
        /* Set will not check pat validity. We need to check again so we will catch issue after */
      });

      const { status, error } = await checkPersonalAccessTokenIsValid(key)
        .then(({ status, error }) => ({ status, error }))
        .catch(() => ({ status: false, error: translate('default_error_message') }));

      if (status) {
        // Let's reset status,
        setCheckingPat(false);
        setTouched(false);
        setPassword('');
        setSubmitting(false);
        setUsername('');
        setValidationFailed(false);

        onPersonalAccessTokenCreated();
      } else {
        setSubmitting(false);
        setTouched(false);
        setValidationFailed(true);
        setValidationErrorMessage(error);
      }
    }
  };

  return {
    username,
    password,
    firstConnection,
    validationFailed,
    touched,
    submitting,
    checkingPat,
    validationErrorMessage,
    handleUsernameChange,
    handlePasswordChange,
    handleSubmit,
  };
};
