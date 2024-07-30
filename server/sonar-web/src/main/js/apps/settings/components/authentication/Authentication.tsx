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

import { Link } from '@sonarsource/echoes-react';
import classNames from 'classnames';
import { FlagMessage, SubTitle, ToggleButton, getTabId, getTabPanelId } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { useSearchParams } from 'react-router-dom';
import { Image } from '~sonar-aligned/components/common/Image';
import { searchParamsToQuery } from '~sonar-aligned/helpers/router';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../../app/components/available-features/withAvailableFeatures';
import { translate } from '../../../../helpers/l10n';
import { AlmKeys } from '../../../../types/alm-settings';
import { Feature } from '../../../../types/features';
import { ExtendedSettingDefinition } from '../../../../types/settings';
import BitbucketAuthenticationTab from './BitbucketAuthenticationTab';
import GitHubAuthenticationTab from './GitHubAuthenticationTab';
import GitLabAuthenticationTab from './GitLabAuthenticationTab';
import SamlAuthenticationTab, { SAML } from './SamlAuthenticationTab';

interface Props {
  definitions: ExtendedSettingDefinition[];
}

export type AuthenticationTabs =
  | typeof SAML
  | AlmKeys.GitHub
  | AlmKeys.GitLab
  | AlmKeys.BitbucketServer;

function renderDevOpsIcon(key: string) {
  return <Image alt={key} className="sw-mr-2" height={16} src={`/images/alm/${key}.svg`} />;
}

export function Authentication(props: Props & WithAvailableFeaturesProps) {
  const { definitions } = props;

  const [query, setSearchParams] = useSearchParams();

  const currentTab = (query.get('tab') ?? SAML) as AuthenticationTabs;

  const tabs = [
    {
      value: SAML,
      label: 'SAML',
    },
    {
      value: AlmKeys.GitHub,
      label: (
        <>
          {renderDevOpsIcon(AlmKeys.GitHub)}
          GitHub
        </>
      ),
    },
    {
      value: AlmKeys.BitbucketServer,
      label: (
        <>
          {renderDevOpsIcon(AlmKeys.BitbucketServer)}
          Bitbucket
        </>
      ),
    },
    {
      value: AlmKeys.GitLab,
      label: (
        <>
          {renderDevOpsIcon(AlmKeys.GitLab)}
          GitLab
        </>
      ),
    },
  ] as const;

  const [samlDefinitions, bitbucketDefinitions] = React.useMemo(
    () => [
      definitions.filter((def) => def.subCategory === SAML),
      definitions.filter((def) => def.subCategory === AlmKeys.BitbucketServer),
    ],
    [definitions],
  );

  return (
    <>
      <SubTitle as="h3">{translate('settings.authentication.title')}</SubTitle>

      {props.hasFeature(Feature.LoginMessage) && (
        <FlagMessage variant="info">
          <div>
            <FormattedMessage
              id="settings.authentication.custom_message_information"
              defaultMessage={translate('settings.authentication.custom_message_information')}
              values={{
                link: (
                  <Link to="/admin/settings?category=general#sonar.login.message">
                    {translate('settings.authentication.custom_message_information.link')}
                  </Link>
                ),
              }}
            />
          </div>
        </FlagMessage>
      )}

      <div className="sw-my-6">
        <p>{translate('settings.authentication.description')}</p>
      </div>

      <ToggleButton
        role="tablist"
        onChange={(tab: AuthenticationTabs) => {
          setSearchParams({ ...searchParamsToQuery(query), tab });
        }}
        value={currentTab}
        options={tabs}
      />
      {tabs.map((tab) => (
        <div
          className={classNames('sw-overflow-y-auto', {
            'sw-hidden': currentTab !== tab.value,
          })}
          key={tab.value}
          role="tabpanel"
          aria-labelledby={getTabId(tab.value)}
          id={getTabPanelId(tab.value)}
        >
          {currentTab === tab.value && (
            <div className="sw-mt-6">
              {tab.value === SAML && <SamlAuthenticationTab definitions={samlDefinitions} />}

              {tab.value === AlmKeys.GitHub && <GitHubAuthenticationTab />}

              {tab.value === AlmKeys.GitLab && <GitLabAuthenticationTab />}

              {tab.value === AlmKeys.BitbucketServer && (
                <BitbucketAuthenticationTab definitions={bitbucketDefinitions} />
              )}
            </div>
          )}
        </div>
      ))}
    </>
  );
}

export default withAvailableFeatures(Authentication);
