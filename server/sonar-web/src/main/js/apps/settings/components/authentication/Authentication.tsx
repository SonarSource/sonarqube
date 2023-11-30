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
import classNames from 'classnames';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { useSearchParams } from 'react-router-dom';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../../app/components/available-features/withAvailableFeatures';
import DocLink from '../../../../components/common/DocLink';
import Link from '../../../../components/common/Link';
import ScreenPositionHelper from '../../../../components/common/ScreenPositionHelper';
import BoxedTabs, { getTabId, getTabPanelId } from '../../../../components/controls/BoxedTabs';
import { Alert } from '../../../../components/ui/Alert';
import { translate } from '../../../../helpers/l10n';
import { getBaseUrl } from '../../../../helpers/system';
import { searchParamsToQuery } from '../../../../helpers/urls';
import { AlmKeys } from '../../../../types/alm-settings';
import { Feature } from '../../../../types/features';
import { ExtendedSettingDefinition } from '../../../../types/settings';
import { AUTHENTICATION_CATEGORY } from '../../constants';
import CategoryDefinitionsList from '../CategoryDefinitionsList';
import GitLabAuthenticationTab from './GitLabAuthenticationTab';
import GithubAuthenticationTab from './GithubAuthenticationTab';
import SamlAuthenticationTab, { SAML } from './SamlAuthenticationTab';

interface Props {
  definitions: ExtendedSettingDefinition[];
}

// We substract the footer height with padding (80) and the main layout padding (20)
const HEIGHT_ADJUSTMENT = 100;

export type AuthenticationTabs =
  | typeof SAML
  | AlmKeys.GitHub
  | AlmKeys.GitLab
  | AlmKeys.BitbucketServer;

export const DOCUMENTATION_LINK_SUFFIXES = {
  [SAML]: 'saml/overview',
  [AlmKeys.GitHub]: 'github',
  [AlmKeys.GitLab]: 'gitlab',
  [AlmKeys.BitbucketServer]: 'bitbucket-cloud',
};

function renderDevOpsIcon(key: string) {
  return (
    <img
      alt={key}
      className="spacer-right"
      height={16}
      src={`${getBaseUrl()}/images/alm/${key}.svg`}
    />
  );
}

export function Authentication(props: Props & WithAvailableFeaturesProps) {
  const { definitions } = props;

  const [query, setSearchParams] = useSearchParams();

  const currentTab = (query.get('tab') || SAML) as AuthenticationTabs;

  const tabs = [
    {
      key: SAML,
      label: 'SAML',
    },
    {
      key: AlmKeys.GitHub,
      label: (
        <>
          {renderDevOpsIcon(AlmKeys.GitHub)}
          GitHub
        </>
      ),
    },
    {
      key: AlmKeys.BitbucketServer,
      label: (
        <>
          {renderDevOpsIcon(AlmKeys.BitbucketServer)}
          Bitbucket
        </>
      ),
    },
    {
      key: AlmKeys.GitLab,
      label: (
        <>
          {renderDevOpsIcon(AlmKeys.GitLab)}
          GitLab
        </>
      ),
    },
  ] as const;

  const [samlDefinitions, githubDefinitions, gitlabDefinitions] = React.useMemo(
    () => [
      definitions.filter((def) => def.subCategory === SAML),
      definitions.filter((def) => def.subCategory === AlmKeys.GitHub),
      definitions.filter((def) => def.subCategory === AlmKeys.GitLab),
    ],
    [definitions],
  );

  return (
    <>
      <header className="page-header">
        <h3 className="page-title h2">{translate('settings.authentication.title')}</h3>
      </header>

      {props.hasFeature(Feature.LoginMessage) && (
        <Alert variant="info">
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
        </Alert>
      )}

      <div className="big-spacer-top huge-spacer-bottom">
        <p>{translate('settings.authentication.description')}</p>
      </div>

      <BoxedTabs
        onSelect={(tab: AuthenticationTabs) => {
          setSearchParams({ ...searchParamsToQuery(query), tab });
        }}
        selected={currentTab}
        tabs={tabs}
      />
      {/* Adding a key to force re-rendering of the tab container, so that it resets the scroll position */}
      <ScreenPositionHelper>
        {({ top }) => (
          <>
            {tabs.map((tab) => (
              <div
                style={{
                  maxHeight:
                    tab.key === AlmKeys.BitbucketServer
                      ? `calc(100vh - ${top + HEIGHT_ADJUSTMENT}px)`
                      : '',
                }}
                className={classNames('bordered overflow-y-auto tabbed-definitions', {
                  hidden: currentTab !== tab.key,
                })}
                key={tab.key}
                role="tabpanel"
                aria-labelledby={getTabId(tab.key)}
                id={getTabPanelId(tab.key)}
              >
                <div className="big-padded-top big-padded-left big-padded-right">
                  {tab.key === SAML && <SamlAuthenticationTab definitions={samlDefinitions} />}

                  {tab.key === AlmKeys.GitHub && (
                    <GithubAuthenticationTab
                      currentTab={currentTab}
                      definitions={githubDefinitions}
                    />
                  )}

                  {tab.key === AlmKeys.GitLab && (
                    <GitLabAuthenticationTab definitions={gitlabDefinitions} />
                  )}

                  {tab.key === AlmKeys.BitbucketServer && (
                    <>
                      <Alert variant="info">
                        <FormattedMessage
                          id="settings.authentication.help"
                          defaultMessage={translate('settings.authentication.help')}
                          values={{
                            link: (
                              <DocLink
                                to={`/instance-administration/authentication/${
                                  DOCUMENTATION_LINK_SUFFIXES[tab.key]
                                }/`}
                              >
                                {translate('settings.authentication.help.link')}
                              </DocLink>
                            ),
                          }}
                        />
                      </Alert>
                      <CategoryDefinitionsList
                        category={AUTHENTICATION_CATEGORY}
                        definitions={definitions}
                        subCategory={tab.key}
                        displaySubCategoryTitle={false}
                      />
                    </>
                  )}
                </div>
              </div>
            ))}
          </>
        )}
      </ScreenPositionHelper>
    </>
  );
}

export default withAvailableFeatures(Authentication);
