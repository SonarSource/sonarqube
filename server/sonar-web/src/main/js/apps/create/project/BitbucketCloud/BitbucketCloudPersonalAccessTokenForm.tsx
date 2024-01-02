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
import {
  ButtonPrimary,
  FlagErrorIcon,
  FlagMessage,
  FormField,
  InputField,
  LightPrimary,
  Link,
  Spinner,
} from 'design-system';
import React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate } from '../../../../helpers/l10n';
import { AlmSettingsInstance } from '../../../../types/alm-settings';
import { usePersonalAccessToken } from '../usePersonalAccessToken';

interface Props {
  almSetting: AlmSettingsInstance;
  resetPat: boolean;
  onPersonalAccessTokenCreated: () => void;
}

export default function BitbucketCloudPersonalAccessTokenForm({
  almSetting,
  resetPat,
  onPersonalAccessTokenCreated,
}: Props) {
  const {
    username,
    password,
    firstConnection,
    validationFailed,
    touched,
    submitting,
    validationErrorMessage,
    checkingPat,
    handlePasswordChange,
    handleUsernameChange,
    handleSubmit,
  } = usePersonalAccessToken(almSetting, resetPat, onPersonalAccessTokenCreated);

  if (checkingPat) {
    return <Spinner className="sw-ml-2" loading />;
  }

  const isInvalid = validationFailed && !touched;
  const canSubmit = Boolean(password) && Boolean(username);
  const submitButtonDiabled = isInvalid || submitting || !canSubmit;

  const errorMessage =
    validationErrorMessage ?? translate('onboarding.create_project.pat_incorrect.bitbucket_cloud');

  return (
    <form className="sw-mt-3 sw-w-[50%]" onSubmit={handleSubmit}>
      <LightPrimary as="h2" className="sw-heading-md">
        {translate('onboarding.create_project.pat_form.title')}
      </LightPrimary>
      <LightPrimary as="p" className="sw-mt-2 sw-mb-4 sw-body-sm">
        {translate('onboarding.create_project.pat_form.help.bitbucket_cloud')}
      </LightPrimary>

      {isInvalid && (
        <div>
          <FlagMessage variant="error" className="sw-mb-4">
            <p>{errorMessage}</p>
          </FlagMessage>
        </div>
      )}

      {!firstConnection && (
        <FlagMessage variant="warning">
          <p>
            {translate('onboarding.create_project.pat.expired.info_message')}{' '}
            {translate('onboarding.create_project.pat.expired.info_message_contact')}
          </p>
        </FlagMessage>
      )}

      <FormField
        htmlFor="enter_username_validation"
        className="sw-mt-6 sw-mb-3"
        label={translate('onboarding.create_project.bitbucket_cloud.enter_username')}
        required
      >
        <div>
          <InputField
            size="large"
            id="enter_username_validation"
            minLength={1}
            value={username}
            onChange={handleUsernameChange}
            type="text"
            isInvalid={isInvalid}
          />
          {isInvalid && <FlagErrorIcon className="sw-ml-2" />}
        </div>
      </FormField>

      <div className="sw-mb-6">
        <FlagMessage variant="info">
          <p>
            <FormattedMessage
              id="onboarding.enter_username.instructions.bitbucket_cloud"
              defaultMessage={translate('onboarding.enter_username.instructions.bitbucket_cloud')}
              values={{
                link: (
                  <Link to="https://bitbucket.org/account/settings/">
                    {translate('onboarding.enter_username.instructions.bitbucket_cloud.link')}
                  </Link>
                ),
              }}
            />
          </p>
        </FlagMessage>
      </div>

      <FormField
        htmlFor="enter_password_validation"
        className="sw-mt-6 sw-mb-3"
        label={translate('onboarding.create_project.bitbucket_cloud.enter_password')}
        required
      >
        <div>
          <InputField
            size="large"
            id="enter_password_validation"
            minLength={1}
            value={password}
            onChange={handlePasswordChange}
            type="text"
            isInvalid={isInvalid}
          />
          {isInvalid && <FlagErrorIcon className="sw-ml-2" />}
        </div>
      </FormField>

      <div className="sw-mb-6">
        <FlagMessage variant="info">
          <p>
            <FormattedMessage
              id="onboarding.create_project.enter_password.instructions.bitbucket_cloud"
              defaultMessage={translate(
                'onboarding.create_project.enter_password.instructions.bitbucket_cloud',
              )}
              values={{
                link: (
                  <Link to="https://bitbucket.org/account/settings/app-passwords/new">
                    {translate(
                      'onboarding.create_project.enter_password.instructions.bitbucket_cloud.link',
                    )}
                  </Link>
                ),
              }}
            />
          </p>
        </FlagMessage>
      </div>

      <ButtonPrimary type="submit" disabled={submitButtonDiabled} className="sw-mb-6">
        {translate('save')}
      </ButtonPrimary>
      <Spinner className="sw-ml-2" loading={submitting} />
    </form>
  );
}
