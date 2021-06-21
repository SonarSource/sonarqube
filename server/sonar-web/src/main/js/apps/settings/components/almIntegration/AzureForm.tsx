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
import { Link } from 'react-router';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { ALM_DOCUMENTATION_PATHS } from '../../../../helpers/constants';
import { AlmKeys, AzureBindingDefinition } from '../../../../types/alm-settings';
import { AlmBindingDefinitionFormField } from './AlmBindingDefinitionFormField';

export interface AzureFormProps {
  formData: AzureBindingDefinition;
  onFieldChange: (fieldId: keyof AzureBindingDefinition, value: string) => void;
}

export default function AzureForm(props: AzureFormProps) {
  const { formData, onFieldChange } = props;

  return (
    <div className="display-flex-start">
      <div className="flex-1">
        <AlmBindingDefinitionFormField
          autoFocus={true}
          help={translate('settings.almintegration.form.name.azure.help')}
          id="name.azure"
          onFieldChange={onFieldChange}
          propKey="key"
          value={formData.key}
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
          help={translate('settings.almintegration.form.personal_access_token.azure.help')}
          id="personal_access_token"
          isTextArea={true}
          onFieldChange={onFieldChange}
          overwriteOnly={Boolean(formData.key)}
          propKey="personalAccessToken"
          value={formData.personalAccessToken}
        />
      </div>
      <Alert className="huge-spacer-left flex-1" variant="info">
        <FormattedMessage
          defaultMessage={translate(`settings.almintegration.azure.info`)}
          id="settings.almintegration.azure.info"
          values={{
            link: (
              <Link target="_blank" to={ALM_DOCUMENTATION_PATHS[AlmKeys.Azure]}>
                {translate('learn_more')}
              </Link>
            )
          }}
        />
      </Alert>
    </div>
  );
}
