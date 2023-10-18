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

import { Link } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { ALM_DOCUMENTATION_PATHS } from '../../../../helpers/constants';
import { useDocUrl } from '../../../../helpers/docs';
import { translate } from '../../../../helpers/l10n';
import { AlmKeys, AzureBindingDefinition } from '../../../../types/alm-settings';
import { AlmBindingDefinitionFormField } from './AlmBindingDefinitionFormField';

export interface AzureFormProps {
  formData: AzureBindingDefinition;
  onFieldChange: (fieldId: keyof AzureBindingDefinition, value: string) => void;
}

export default function AzureForm(props: AzureFormProps) {
  const { formData, onFieldChange } = props;
  const toStatic = useDocUrl(ALM_DOCUMENTATION_PATHS[AlmKeys.Azure]);
  return (
    <>
      <AlmBindingDefinitionFormField
        autoFocus
        help={translate('settings.almintegration.form.name.azure.help')}
        id="name.azure"
        onFieldChange={onFieldChange}
        propKey="key"
        value={formData.key}
        maxLength={200}
      />
      <AlmBindingDefinitionFormField
        help={
          <>
            {translate('settings.almintegration.form.url.azure.help1')}
            <br />
            <em>https://ado.your-company.com/your_collection</em>
            <br />
            <br />
            {translate('settings.almintegration.form.url.azure.help2')}
            <br />
            <em>https://dev.azure.com/your_organization</em>
          </>
        }
        id="url.azure"
        maxLength={2000}
        onFieldChange={onFieldChange}
        propKey="url"
        value={formData.url || ''}
      />
      <AlmBindingDefinitionFormField
        help={
          <FormattedMessage
            defaultMessage={translate(
              'settings.almintegration.form.personal_access_token.azure.help',
            )}
            id="settings.almintegration.form.personal_access_token.azure.help"
            values={{
              pat: (
                <Link
                  to="https://docs.microsoft.com/en-us/azure/devops/organizations/accounts/use-personal-access-tokens-to-authenticate"
                  target="_blank"
                >
                  {translate('settings.almintegration.form.personal_access_token.azure.help.url')}
                </Link>
              ),
              permission: <strong>{'Code > Read & Write'}</strong>,
              doc_link: <Link to={toStatic}>{translate('learn_more')}</Link>,
            }}
          />
        }
        id="personal_access_token"
        isTextArea
        onFieldChange={onFieldChange}
        overwriteOnly={Boolean(formData.key)}
        propKey="personalAccessToken"
        value={formData.personalAccessToken}
        maxLength={2000}
        isSecret
      />
    </>
  );
}
