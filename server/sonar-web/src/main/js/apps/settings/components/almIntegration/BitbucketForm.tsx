/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { FormattedMessage } from 'react-intl';
import RadioToggle from 'sonar-ui-common/components/controls/RadioToggle';
import { translate } from 'sonar-ui-common/helpers/l10n';
import {
  AlmKeys,
  BitbucketBindingDefinition,
  BitbucketCloudBindingDefinition,
  isBitbucketBindingDefinition,
  isBitbucketCloudBindingDefinition
} from '../../../../types/alm-settings';
import { AlmBindingDefinitionFormField } from './AlmBindingDefinitionFormField';

export interface BitbucketFormProps {
  formData: BitbucketBindingDefinition | BitbucketCloudBindingDefinition;
  isCreating: boolean;
  onFieldChange: (
    fieldId: keyof (BitbucketBindingDefinition & BitbucketCloudBindingDefinition),
    value: string
  ) => void;
  onSelectVariant: (variant: AlmKeys.BitbucketServer | AlmKeys.BitbucketCloud) => void;
  variant?: AlmKeys.BitbucketServer | AlmKeys.BitbucketCloud;
}

export default function BitbucketForm(props: BitbucketFormProps) {
  const { formData, isCreating, variant } = props;

  return (
    <div>
      {isCreating && (
        <>
          <strong>{translate('settings.almintegration.form.choose_bitbucket_variant')}</strong>
          <RadioToggle
            className="little-spacer-top big-spacer-bottom"
            name="variant"
            onCheck={props.onSelectVariant}
            options={[
              {
                label: 'Bitbucket Server',
                value: AlmKeys.BitbucketServer
              },
              { label: 'Bitbucket Cloud', value: AlmKeys.BitbucketCloud }
            ]}
            value={variant}
          />
        </>
      )}

      {variant === AlmKeys.BitbucketServer && isBitbucketBindingDefinition(formData) && (
        <div>
          <AlmBindingDefinitionFormField
            autoFocus={true}
            help={translate('settings.almintegration.form.name.bitbucket.help')}
            id="name.bitbucket"
            maxLength={100}
            onFieldChange={props.onFieldChange}
            propKey="key"
            value={formData.key}
          />
          <AlmBindingDefinitionFormField
            help={
              <FormattedMessage
                defaultMessage={translate('settings.almintegration.form.url.bitbucket.help')}
                id="settings.almintegration.form.url.bitbucket.help"
                values={{ example: 'https://bitbucket-server.your-company.com' }}
              />
            }
            id="url.bitbucket"
            maxLength={2000}
            onFieldChange={props.onFieldChange}
            propKey="url"
            value={formData.url}
          />
          <AlmBindingDefinitionFormField
            help={
              <FormattedMessage
                defaultMessage={translate(
                  'settings.almintegration.form.personal_access_token.bitbucket.help'
                )}
                id="settings.almintegration.form.personal_access_token.bitbucket.help"
                values={{
                  pat: (
                    <a
                      href="https://confluence.atlassian.com/bitbucketserver0515/personal-access-tokens-961275199.html"
                      rel="noopener noreferrer"
                      target="_blank">
                      {translate(
                        'settings.almintegration.form.personal_access_token.bitbucket.help.url'
                      )}
                    </a>
                  )
                }}
              />
            }
            id="personal_access_token"
            isTextArea={true}
            onFieldChange={props.onFieldChange}
            overwriteOnly={Boolean(formData.key)}
            propKey="personalAccessToken"
            value={formData.personalAccessToken}
          />
        </div>
      )}

      {variant === AlmKeys.BitbucketCloud && isBitbucketCloudBindingDefinition(formData) && (
        <div>
          <AlmBindingDefinitionFormField
            autoFocus={true}
            help={translate('settings.almintegration.form.name.bitbucketcloud.help')}
            id="name.bitbucket"
            maxLength={100}
            onFieldChange={props.onFieldChange}
            propKey="key"
            value={formData.key}
          />
          <AlmBindingDefinitionFormField
            help={
              <FormattedMessage
                defaultMessage={translate(
                  'settings.almintegration.form.workspace.bitbucketcloud.help'
                )}
                id="settings.almintegration.form.workspace.bitbucketcloud.help"
                values={{
                  example: (
                    <>
                      {'https://bitbucket.org/'}
                      <strong>{'{workspace}'}</strong>
                      {'/{repository}'}
                    </>
                  )
                }}
              />
            }
            id="workspace.bitbucketcloud"
            maxLength={2000}
            onFieldChange={props.onFieldChange}
            propKey="workspace"
            value={formData.workspace}
          />
          <AlmBindingDefinitionFormField
            help={translate('settings.almintegration.form.oauth_key.bitbucketcloud.help')}
            id="client_id.bitbucketcloud"
            onFieldChange={props.onFieldChange}
            propKey="clientId"
            value={formData.clientId}
          />
          <AlmBindingDefinitionFormField
            help={translate('settings.almintegration.form.oauth_secret.bitbucketcloud.help')}
            id="client_secret.bitbucketcloud"
            onFieldChange={props.onFieldChange}
            overwriteOnly={Boolean(formData.key)}
            propKey="clientSecret"
            value={formData.clientSecret}
          />
        </div>
      )}
    </div>
  );
}
