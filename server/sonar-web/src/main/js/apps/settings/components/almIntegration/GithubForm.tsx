/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { FlagMessage, Link } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { ALM_DOCUMENTATION_PATHS } from '../../../../helpers/constants';
import { useDocUrl } from '../../../../helpers/docs';
import { translate } from '../../../../helpers/l10n';
import { AlmKeys, GithubBindingDefinition } from '../../../../types/alm-settings';
import { AlmBindingDefinitionFormField } from './AlmBindingDefinitionFormField';

export interface GithubFormProps {
  formData: GithubBindingDefinition;
  onFieldChange: (fieldId: keyof GithubBindingDefinition, value: string) => void;
}

export default function GithubForm(props: GithubFormProps) {
  const { formData, onFieldChange } = props;
  const toStatic = useDocUrl(ALM_DOCUMENTATION_PATHS[AlmKeys.GitHub]);
  return (
    <>
      <FlagMessage variant="info" className="sw-mb-8">
        <span>
          <FormattedMessage
            defaultMessage={translate(`settings.almintegration.github.info`)}
            id="settings.almintegration.github.info"
            values={{
              link: <Link to={toStatic}>{translate('learn_more')}</Link>,
            }}
          />
        </span>
      </FlagMessage>
      <AlmBindingDefinitionFormField
        autoFocus
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
          </>
        }
        id="url.github"
        maxLength={2000}
        onFieldChange={onFieldChange}
        propKey="url"
        value={formData.url}
      />

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
        isSecret
      />
      <AlmBindingDefinitionFormField
        id="private_key"
        help={translate('settings.almintegration.form.private_key.github.help')}
        isTextArea
        onFieldChange={onFieldChange}
        overwriteOnly={Boolean(formData.key)}
        propKey="privateKey"
        value={formData.privateKey}
        maxLength={2500}
        isSecret
      />
      <AlmBindingDefinitionFormField
        id="webhook_secret.github"
        help={translate('settings.almintegration.form.webhook_secret.github.help')}
        maxLength={160}
        onFieldChange={onFieldChange}
        overwriteOnly={Boolean(formData.key)}
        propKey="webhookSecret"
        value={formData.webhookSecret}
        isSecret
        optional
      />
    </>
  );
}
