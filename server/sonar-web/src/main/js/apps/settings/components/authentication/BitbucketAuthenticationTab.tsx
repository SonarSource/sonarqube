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
import { FlagMessage } from 'design-system';
import React from 'react';
import { FormattedMessage } from 'react-intl';
import DocumentationLink from '../../../../components/common/DocumentationLink';
import { translate } from '../../../../helpers/l10n';
import { useGetValueQuery } from '../../../../queries/settings';
import { AlmKeys } from '../../../../types/alm-settings';
import { ExtendedSettingDefinition } from '../../../../types/settings';
import { AUTHENTICATION_CATEGORY } from '../../constants';
import CategoryDefinitionsList from '../CategoryDefinitionsList';

interface Props {
  definitions: ExtendedSettingDefinition[];
}

export default function BitbucketAuthenticationTab(props: Readonly<Props>) {
  const { definitions } = props;

  const { data: allowToSignUpEnabled } = useGetValueQuery(
    'sonar.auth.bitbucket.allowUsersToSignUp',
  );
  const { data: workspaces } = useGetValueQuery('sonar.auth.bitbucket.workspaces');

  const isConfigurationUnsafe =
    allowToSignUpEnabled?.value === 'true' &&
    (!workspaces?.values || workspaces?.values.length === 0);

  return (
    <>
      {isConfigurationUnsafe && (
        <FlagMessage variant="error" className="sw-mb-2">
          <div>
            <FormattedMessage
              id="settings.authentication.gitlab.configuration.insecure"
              values={{
                documentation: (
                  <DocumentationLink to="/instance-administration/authentication/bitbucket-cloud/#setting-your-authentication-settings-in-sonarqube">
                    {translate('documentation')}
                  </DocumentationLink>
                ),
              }}
            />
          </div>
        </FlagMessage>
      )}
      <FlagMessage variant="info">
        <div>
          <FormattedMessage
            id="settings.authentication.help"
            defaultMessage={translate('settings.authentication.help')}
            values={{
              link: (
                <DocumentationLink to="/instance-administration/authentication/bitbucket-cloud/">
                  {translate('settings.authentication.help.link')}
                </DocumentationLink>
              ),
            }}
          />
        </div>
      </FlagMessage>
      <CategoryDefinitionsList
        category={AUTHENTICATION_CATEGORY}
        definitions={definitions}
        subCategory={AlmKeys.BitbucketServer}
        displaySubCategoryTitle={false}
        noPadding
      />
    </>
  );
}
