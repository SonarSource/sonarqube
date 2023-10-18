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
import { AlmKeys, GitlabBindingDefinition } from '../../../../types/alm-settings';
import { AlmBindingDefinitionFormField } from './AlmBindingDefinitionFormField';

export interface GitlabFormProps {
  formData: GitlabBindingDefinition;
  onFieldChange: (fieldId: keyof GitlabBindingDefinition, value: string) => void;
}

export default function GitlabForm(props: GitlabFormProps) {
  const { formData, onFieldChange } = props;
  const toStatic = useDocUrl(ALM_DOCUMENTATION_PATHS[AlmKeys.GitLab]);
  return (
    <>
      <AlmBindingDefinitionFormField
        autoFocus
        help={translate('settings.almintegration.form.name.gitlab.help')}
        id="name.gitlab"
        onFieldChange={onFieldChange}
        propKey="key"
        value={formData.key}
        maxLength={200}
      />
      <AlmBindingDefinitionFormField
        help={
          <>
            {translate('settings.almintegration.form.url.gitlab.help')}
            <br />
            <em>https://gitlab.com/api/v4</em>
          </>
        }
        id="url.gitlab"
        maxLength={2000}
        onFieldChange={onFieldChange}
        propKey="url"
        value={formData.url || ''}
      />
      <AlmBindingDefinitionFormField
        help={
          <FormattedMessage
            defaultMessage={translate(
              `settings.almintegration.form.personal_access_token.gitlab.help`,
            )}
            id="settings.almintegration.form.personal_access_token.gitlab.help"
            values={{
              pat: (
                <Link
                  to="https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html"
                  target="_blank"
                >
                  {translate('settings.almintegration.form.personal_access_token.gitlab.help.url')}
                </Link>
              ),
              permission: <strong>Reporter</strong>,
              scope: <strong>api</strong>,
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
