/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import * as classNames from 'classnames';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import ValidationInput from 'sonar-ui-common/components/controls/ValidationInput';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import { AlmSettingsInstance } from '../../../types/alm-settings';

export interface BitbucketPersonalAccessTokenFormProps {
  bitbucketSetting: AlmSettingsInstance;
  onPersonalAccessTokenCreate: (token: string) => void;
  submitting?: boolean;
}

export default function BitbucketPersonalAccessTokenForm(
  props: BitbucketPersonalAccessTokenFormProps
) {
  const {
    bitbucketSetting: { url },
    submitting = false
  } = props;
  const [personalAccessToken, setPersonalAccessToken] = React.useState('');
  const isValid = personalAccessToken.length > 0;

  return (
    <div className="display-flex-start">
      <form
        onSubmit={(e: React.SyntheticEvent<HTMLFormElement>) => {
          e.preventDefault();
          props.onPersonalAccessTokenCreate(personalAccessToken);
        }}>
        <h2 className="big">{translate('onboarding.create_project.grant_access_to_bbs.title')}</h2>
        <p className="big-spacer-top big-spacer-bottom">
          {translate('onboarding.create_project.grant_access_to_bbs.help')}
        </p>

        <ValidationInput
          error={undefined}
          id="personal_access_token"
          isInvalid={false}
          isValid={isValid}
          label={translate('onboarding.create_project.enter_pat')}
          required={true}>
          <input
            autoFocus={true}
            className={classNames('input-super-large', {
              'is-valid': isValid
            })}
            id="personal_access_token"
            minLength={1}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
              setPersonalAccessToken(e.currentTarget.value)
            }
            type="text"
            value={personalAccessToken}
          />
        </ValidationInput>

        <SubmitButton disabled={!isValid || submitting}>{translate('save')}</SubmitButton>
        <DeferredSpinner className="spacer-left" loading={submitting} />
      </form>

      <Alert className="big-spacer-left big-spacer-top" display="block" variant="info">
        <h3>{translate('onboarding.create_project.pat_help.title')}</h3>

        <p className="big-spacer-top big-spacer-bottom">
          {translate('onboarding.create_project.pat_help.bbs_help_1')}
        </p>

        {url && (
          <div className="text-middle">
            <img
              alt="" // Should be ignored by screen readers
              className="spacer-right"
              height="16"
              src={`${getBaseUrl()}/images/alm/bitbucket.svg`}
            />
            <a
              href={`${url.replace(/\/$/, '')}/plugins/servlet/access-tokens/add`}
              rel="noopener noreferrer"
              target="_blank">
              {translate('onboarding.create_project.pat_help.link')}
            </a>
          </div>
        )}

        <p className="big-spacer-top big-spacer-bottom">
          {translate('onboarding.create_project.pat_help.bbs_help_2')}
        </p>

        <ul>
          <li>
            <FormattedMessage
              defaultMessage={translate(
                'onboarding.create_project.pat_help.bbs_permission_projects'
              )}
              id="onboarding.create_project.pat_help.bbs_permission_projects"
              values={{
                perm: (
                  <strong>{translate('onboarding.create_project.pat_help.read_permission')}</strong>
                )
              }}
            />
          </li>
          <li>
            <FormattedMessage
              defaultMessage={translate('onboarding.create_project.pat_help.bbs_permission_repos')}
              id="onboarding.create_project.pat_help.bbs_permission_repos"
              values={{
                perm: (
                  <strong>{translate('onboarding.create_project.pat_help.read_permission')}</strong>
                )
              }}
            />
          </li>
        </ul>
      </Alert>
    </div>
  );
}
