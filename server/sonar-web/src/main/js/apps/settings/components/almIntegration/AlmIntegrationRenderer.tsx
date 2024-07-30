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
import { FlagMessage, SubTitle, ToggleButton } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { Image } from '~sonar-aligned/components/common/Image';
import { translate } from '../../../../helpers/l10n';
import { isDefined } from '../../../../helpers/types';
import { useGetValuesQuery } from '../../../../queries/settings';
import {
  AlmKeys,
  AlmSettingsBindingDefinitions,
  AlmSettingsBindingStatus,
} from '../../../../types/alm-settings';
import { SettingsKey } from '../../../../types/settings';
import { Dict } from '../../../../types/types';
import { AlmTabs } from './AlmIntegration';
import AlmTab from './AlmTab';
import DeleteModal from './DeleteModal';

export interface AlmIntegrationRendererProps {
  branchesEnabled: boolean;
  currentAlmTab: AlmTabs;
  definitionKeyForDeletion?: string;
  definitionStatus: Dict<AlmSettingsBindingStatus>;
  definitions: AlmSettingsBindingDefinitions;
  loadingAlmDefinitions: boolean;
  loadingProjectCount: boolean;
  multipleAlmEnabled: boolean;
  onCancelDelete: () => void;
  onCheckConfiguration: (definitionKey: string) => void;
  onConfirmDelete: (definitionKey: string) => void;
  onDelete: (definitionKey: string) => void;
  onSelectAlmTab: (alm: AlmTabs) => void;
  onUpdateDefinitions: () => void;
  projectCount?: number;
}

const tabs = [
  {
    label: (
      <>
        <Image alt="github" className="sw-mr-2" height={16} src="/images/alm/github.svg" />
        {translate('settings.almintegration.tab.github')}
      </>
    ),
    value: AlmKeys.GitHub,
  },
  {
    label: (
      <>
        <Image alt="bitbucket" className="sw-mr-2" height={16} src="/images/alm/bitbucket.svg" />
        {translate('settings.almintegration.tab.bitbucket')}
      </>
    ),
    value: AlmKeys.BitbucketServer,
  },
  {
    label: (
      <>
        <Image alt="azure" className="sw-mr-2" height={16} src="/images/alm/azure.svg" />
        {translate('settings.almintegration.tab.azure')}
      </>
    ),
    value: AlmKeys.Azure,
  },
  {
    label: (
      <>
        <Image alt="gitlab" className="sw-mr-2" height={16} src="/images/alm/gitlab.svg" />
        {translate('settings.almintegration.tab.gitlab')}
      </>
    ),
    value: AlmKeys.GitLab,
  },
];

export default function AlmIntegrationRenderer(props: AlmIntegrationRendererProps) {
  const {
    definitionKeyForDeletion,
    definitions,
    definitionStatus,
    currentAlmTab,
    loadingAlmDefinitions,
    loadingProjectCount,
    branchesEnabled,
    multipleAlmEnabled,
    projectCount,
  } = props;

  const bindingDefinitions = {
    [AlmKeys.Azure]: definitions.azure,
    [AlmKeys.GitLab]: definitions.gitlab,
    [AlmKeys.GitHub]: definitions.github,
    [AlmKeys.BitbucketServer]: [...definitions.bitbucket, ...definitions.bitbucketcloud],
  };

  const { data, isLoading } = useGetValuesQuery([SettingsKey.ServerBaseUrl]);
  const hasServerBaseUrl = data?.length === 1 && data[0].value !== undefined;

  return (
    <>
      <header className="sw-mb-5">
        <SubTitle>{translate('settings.almintegration.title')}</SubTitle>
      </header>

      {!hasServerBaseUrl && !isLoading && branchesEnabled && (
        <FlagMessage variant="warning">
          <p>
            <FormattedMessage
              id="settings.almintegration.empty.server_base_url"
              defaultMessage={translate('settings.almintegration.empty.server_base_url')}
              values={{
                serverBaseUrl: (
                  <Link to="/admin/settings?category=general#sonar.core.serverBaseURL">
                    {translate('settings.almintegration.empty.server_base_url.setting_link')}
                  </Link>
                ),
              }}
            />
          </p>
        </FlagMessage>
      )}

      <div className="sw-my-4">{translate('settings.almintegration.description')}</div>

      <div className="sw-mb-6">
        <ToggleButton
          onChange={props.onSelectAlmTab}
          options={tabs}
          role="tablist"
          value={currentAlmTab}
        />
      </div>

      <AlmTab
        almTab={currentAlmTab}
        branchesEnabled={branchesEnabled}
        definitions={bindingDefinitions[currentAlmTab]}
        definitionStatus={definitionStatus}
        loadingAlmDefinitions={loadingAlmDefinitions}
        loadingProjectCount={loadingProjectCount}
        multipleAlmEnabled={multipleAlmEnabled}
        onCheck={props.onCheckConfiguration}
        onDelete={props.onDelete}
        onUpdateDefinitions={props.onUpdateDefinitions}
      />

      {isDefined(definitionKeyForDeletion) && (
        <DeleteModal
          id={definitionKeyForDeletion}
          onCancel={props.onCancelDelete}
          onDelete={props.onConfirmDelete}
          projectCount={projectCount}
        />
      )}
    </>
  );
}
