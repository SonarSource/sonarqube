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

import { FormattedMessage } from 'react-intl';
import { Link } from '~design-system';
import { DocLink } from '../../../../helpers/doc-links';
import { useDocUrl } from '../../../../helpers/docs';
import { translate } from '../../../../helpers/l10n';
import { BitbucketServerBindingDefinition } from '../../../../types/alm-settings';
import { AlmBindingDefinitionFormField } from './AlmBindingDefinitionFormField';

export interface BitbucketServerFormProps {
  formData: BitbucketServerBindingDefinition;
  onFieldChange: (fieldId: keyof BitbucketServerBindingDefinition, value: string) => void;
}

export default function BitbucketServerForm(props: BitbucketServerFormProps) {
  const { formData } = props;
  const toStatic = useDocUrl(DocLink.AlmBitBucketServerIntegration);
  return (
    <>
      <AlmBindingDefinitionFormField
        autoFocus
        help={translate('settings.almintegration.form.name.bitbucket.help')}
        id="name.bitbucket"
        maxLength={200}
        onFieldChange={props.onFieldChange}
        propKey="key"
        value={formData.key || ''}
      />
      <AlmBindingDefinitionFormField
        help={
          <>
            {translate('settings.almintegration.form.url.bitbucket.help')}
            <br />
            <br />
            {translate('settings.almintegration.form.url.pat_warning')}
          </>
        }
        id="url.bitbucket"
        maxLength={2000}
        onFieldChange={props.onFieldChange}
        propKey="url"
        value={formData.url || ''}
      />
      <AlmBindingDefinitionFormField
        id="personal_access_token"
        help={
          <FormattedMessage
            defaultMessage={translate(
              'settings.almintegration.form.personal_access_token.bitbucket.help',
            )}
            id="settings.almintegration.form.personal_access_token.bitbucket.help"
            values={{
              pat: (
                <Link
                  to="https://confluence.atlassian.com/bitbucketserver0515/personal-access-tokens-961275199.html"
                  target="_blank"
                >
                  {translate(
                    'settings.almintegration.form.personal_access_token.bitbucket.help.url',
                  )}
                </Link>
              ),
              permission: <strong>Read</strong>,
              doc_link: <Link to={toStatic}>{translate('learn_more')}</Link>,
            }}
          />
        }
        isTextArea
        onFieldChange={props.onFieldChange}
        overwriteOnly={Boolean(formData.key)}
        propKey="personalAccessToken"
        value={formData.personalAccessToken || ''}
        maxLength={2000}
        isSecret
      />
    </>
  );
}
