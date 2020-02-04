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
import * as React from 'react';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { GithubBindingDefinition } from '../../../../types/alm-settings';
import { AlmBindingDefinitionFormField } from './AlmBindingDefinitionFormField';

export interface GithubFormProps {
  formData: GithubBindingDefinition;
  hideKeyField?: boolean;
  onFieldChange: (fieldId: keyof GithubBindingDefinition, value: string) => void;
  readOnly?: boolean;
}

export default function GithubForm(props: GithubFormProps) {
  const { formData, hideKeyField, onFieldChange, readOnly } = props;

  return (
    <>
      {!hideKeyField && (
        <AlmBindingDefinitionFormField
          autoFocus={true}
          help={translate('settings.almintegration.form.name.github.help')}
          id="name.github"
          onFieldChange={onFieldChange}
          propKey="key"
          readOnly={readOnly}
          value={formData.key}
        />
      )}
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
        readOnly={readOnly}
        value={formData.url}
      />
      <AlmBindingDefinitionFormField
        id="app_id"
        maxLength={80}
        onFieldChange={onFieldChange}
        propKey="appId"
        readOnly={readOnly}
        value={formData.appId}
      />
      <AlmBindingDefinitionFormField
        id="private_key"
        isTextArea={true}
        onFieldChange={onFieldChange}
        propKey="privateKey"
        readOnly={readOnly}
        value={formData.privateKey}
      />
    </>
  );
}
