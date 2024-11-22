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
import { FormattedMessage } from 'react-intl';
import DocLink from '../../../../components/common/DocLink';
import { Alert } from '../../../../components/ui/Alert';
import { ALM_DOCUMENTATION_PATHS } from '../../../../helpers/constants';
import { translate } from '../../../../helpers/l10n';
import { AlmKeys, GithubBindingDefinition } from '../../../../types/alm-settings';
import { AlmBindingDefinitionFormField } from './AlmBindingDefinitionFormField';

export interface GithubFormProps {
  formData: GithubBindingDefinition;
  onFieldChange: (fieldId: keyof GithubBindingDefinition, value: string) => void;
}

export default function GithubForm(props: GithubFormProps) {
  const { formData, onFieldChange } = props;

  return (
    <>
      <AlmBindingDefinitionFormField
        autoFocus={true}
        help={translate('settings.almintegration.form.name.github.help')}
        id="name.github"
        onFieldChange={onFieldChange}
        propKey="key"
        value={formData.key}
        maxLength={200}
      />
      <AlmBindingDefinitionFormField
        help={
          <>
            {translate('settings.almintegration.form.url.github.help1')}
            <br />
            <em>https://github.company.com/api/v3</em>
            <br />
            <br />
            {translate('settings.almintegration.form.url.github.help2')}
            <br />
            <em>https://api.github.com/</em>
            <br />
            <br />
            {translate('settings.almintegration.form.url.github.private_key_warning')}
          </>
        }
        id="url.github"
        maxLength={2000}
        onFieldChange={onFieldChange}
        propKey="url"
        value={formData.url}
      />
      <Alert className="big-spacer-top" variant="info">
        <FormattedMessage
          defaultMessage={translate(`settings.almintegration.github.info`)}
          id="settings.almintegration.github.info"
          values={{
            link: (
              <DocLink to={ALM_DOCUMENTATION_PATHS[AlmKeys.GitHub]}>
                {translate('learn_more')}
              </DocLink>
            ),
          }}
        />
      </Alert>
      <AlmBindingDefinitionFormField
        id="app_id"
        help={translate('settings.almintegration.form.app_id.github.help')}
        maxLength={80}
        onFieldChange={onFieldChange}
        propKey="appId"
        value={formData.appId}
      />
      <AlmBindingDefinitionFormField
        id="client_id.github"
        help={translate('settings.almintegration.form.client_id.github.help')}
        maxLength={80}
        onFieldChange={onFieldChange}
        propKey="clientId"
        value={formData.clientId}
      />
      <AlmBindingDefinitionFormField
        id="client_secret.github"
        help={translate('settings.almintegration.form.client_secret.github.help')}
        maxLength={160}
        onFieldChange={onFieldChange}
        overwriteOnly={Boolean(formData.key)}
        propKey="clientSecret"
        value={formData.clientSecret}
        isSecret={true}
      />
      <AlmBindingDefinitionFormField
        id="private_key"
        help={translate('settings.almintegration.form.private_key.github.help')}
        isTextArea={true}
        onFieldChange={onFieldChange}
        overwriteOnly={Boolean(formData.key)}
        propKey="privateKey"
        value={formData.privateKey}
        maxLength={2500}
        isSecret={true}
      />
      <AlmBindingDefinitionFormField
        id="webhook_secret.github"
        help={translate('settings.almintegration.form.webhook_secret.github.help')}
        maxLength={160}
        onFieldChange={onFieldChange}
        overwriteOnly={Boolean(formData.key)}
        propKey="webhookSecret"
        value={formData.webhookSecret}
        isSecret={true}
        optional={true}
      />
    </>
  );
}
