/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { translate } from 'sonar-ui-common/helpers/l10n';
import { AzureBindingDefinition } from '../../../../types/alm-settings';
import { AlmDefinitionFormField } from './AlmDefinitionFormField';

export interface AzureFormModalProps {
  formData: AzureBindingDefinition;
  onFieldChange: (fieldId: keyof AzureBindingDefinition, value: string) => void;
}

export default function AzureFormModal(props: AzureFormModalProps) {
  const { formData, onFieldChange } = props;

  return (
    <>
      <AlmDefinitionFormField
        autoFocus={true}
        help={translate('settings.pr_decoration.form.azure.name.help')}
        id="azure.name"
        onFieldChange={onFieldChange}
        propKey="key"
        value={formData.key}
      />
      <AlmDefinitionFormField
        help={translate('settings.pr_decoration.form.personal_access_token.help')}
        id="personal_access_token"
        isTextArea={true}
        onFieldChange={onFieldChange}
        propKey="personalAccessToken"
        value={formData.personalAccessToken}
      />
    </>
  );
}
