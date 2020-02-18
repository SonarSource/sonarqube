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
import { GitlabBindingDefinition } from '../../../../types/alm-settings';
import { AlmBindingDefinitionFormField } from './AlmBindingDefinitionFormField';

export interface GitlabFormProps {
  formData: GitlabBindingDefinition;
  hideKeyField?: boolean;
  onFieldChange: (fieldId: keyof GitlabBindingDefinition, value: string) => void;
  readOnly?: boolean;
}

export default function GitlabForm(props: GitlabFormProps) {
  const { formData, hideKeyField, onFieldChange, readOnly } = props;

  return (
    <>
      {!hideKeyField && (
        <AlmBindingDefinitionFormField
          autoFocus={true}
          help={translate('settings.almintegration.form.name.gitlab.help')}
          id="name.gitlab"
          onFieldChange={onFieldChange}
          propKey="key"
          readOnly={readOnly}
          value={formData.key}
        />
      )}
      <AlmBindingDefinitionFormField
        help={
          <>
            {translate('settings.almintegration.form.url.gitlab.help1')}
            <br />
            <br />
            {translate('settings.almintegration.form.url.gitlab.help2')}
            <br />
            <em>https://gitlab.com/api/v4</em>
          </>
        }
        id="url.gitlab"
        maxLength={2000}
        onFieldChange={onFieldChange}
        optional={true}
        propKey="url"
        readOnly={readOnly}
        value={formData.url || ''}
      />
      <AlmBindingDefinitionFormField
        help={translate('settings.almintegration.form.personal_access_token.gitlab.help')}
        id="personal_access_token"
        isTextArea={true}
        onFieldChange={onFieldChange}
        propKey="personalAccessToken"
        readOnly={readOnly}
        value={formData.personalAccessToken}
      />
    </>
  );
}
