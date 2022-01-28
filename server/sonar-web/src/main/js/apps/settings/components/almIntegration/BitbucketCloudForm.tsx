/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { Link } from 'react-router';
import { Alert } from '../../../../components/ui/Alert';
import { ALM_DOCUMENTATION_PATHS } from '../../../../helpers/constants';
import { translate } from '../../../../helpers/l10n';
import { AlmKeys, BitbucketCloudBindingDefinition } from '../../../../types/alm-settings';
import { AlmBindingDefinitionFormField } from './AlmBindingDefinitionFormField';

export interface BitbucketCloudFormProps {
  formData: BitbucketCloudBindingDefinition;
  onFieldChange: (fieldId: keyof BitbucketCloudBindingDefinition, value: string) => void;
}

export default function BitbucketCloudForm(props: BitbucketCloudFormProps) {
  const { formData } = props;

  return (
    <>
      <AlmBindingDefinitionFormField
        autoFocus={true}
        help={translate('settings.almintegration.form.name.bitbucketcloud.help')}
        id="name.bitbucket"
        maxLength={200}
        onFieldChange={props.onFieldChange}
        propKey="key"
        value={formData.key || ''}
      />
      <AlmBindingDefinitionFormField
        help={
          <FormattedMessage
            defaultMessage={translate('settings.almintegration.form.workspace.bitbucketcloud.help')}
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
        maxLength={80}
        onFieldChange={props.onFieldChange}
        propKey="workspace"
        value={formData.workspace || ''}
      />
      <Alert className="big-spacer-top" variant="info">
        <FormattedMessage
          defaultMessage={translate(`settings.almintegration.bitbucketcloud.info`)}
          id="settings.almintegration.bitbucketcloud.info"
          values={{
            oauth: (
              <a
                href="https://support.atlassian.com/bitbucket-cloud/docs/use-oauth-on-bitbucket-cloud/"
                rel="noopener noreferrer"
                target="_blank">
                {translate('settings.almintegration.bitbucketcloud.oauth')}
              </a>
            ),
            permission: <strong>Pull Requests: Read</strong>,
            doc_link: (
              <Link target="_blank" to={ALM_DOCUMENTATION_PATHS[AlmKeys.BitbucketCloud]}>
                {translate('learn_more')}
              </Link>
            )
          }}
        />
      </Alert>
      <AlmBindingDefinitionFormField
        id="client_id.bitbucketcloud"
        help={translate('settings.almintegration.form.oauth_key.bitbucketcloud.help')}
        onFieldChange={props.onFieldChange}
        propKey="clientId"
        value={formData.clientId || ''}
        maxLength={80}
      />
      <AlmBindingDefinitionFormField
        id="client_secret.bitbucketcloud"
        help={translate('settings.almintegration.form.oauth_secret.bitbucketcloud.help')}
        onFieldChange={props.onFieldChange}
        overwriteOnly={Boolean(formData.key)}
        propKey="clientSecret"
        value={formData.clientSecret || ''}
        maxLength={160}
        isSecret={true}
      />
    </>
  );
}
