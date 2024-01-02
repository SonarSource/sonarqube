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
import * as React from 'react';
import BoxedTabs from '../../../../components/controls/BoxedTabs';
import { translate } from '../../../../helpers/l10n';
import { getBaseUrl } from '../../../../helpers/system';
import {
  AlmKeys,
  AlmSettingsBindingDefinitions,
  AlmSettingsBindingStatus,
} from '../../../../types/alm-settings';
import { Dict } from '../../../../types/types';
import { AlmTabs } from './AlmIntegration';
import AlmTab from './AlmTab';
import DeleteModal from './DeleteModal';

export interface AlmIntegrationRendererProps {
  branchesEnabled: boolean;
  currentAlmTab: AlmTabs;
  definitionKeyForDeletion?: string;
  definitions: AlmSettingsBindingDefinitions;
  definitionStatus: Dict<AlmSettingsBindingStatus>;
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
    key: AlmKeys.GitHub,
    label: (
      <>
        <img
          alt="github"
          className="spacer-right"
          height={16}
          src={`${getBaseUrl()}/images/alm/github.svg`}
        />
        GitHub
      </>
    ),
  },
  {
    key: AlmKeys.BitbucketServer,
    label: (
      <>
        <img
          alt="bitbucket"
          className="spacer-right"
          height={16}
          src={`${getBaseUrl()}/images/alm/bitbucket.svg`}
        />
        Bitbucket
      </>
    ),
  },
  {
    key: AlmKeys.Azure,
    label: (
      <>
        <img
          alt="azure"
          className="spacer-right"
          height={16}
          src={`${getBaseUrl()}/images/alm/azure.svg`}
        />
        Azure DevOps
      </>
    ),
  },
  {
    key: AlmKeys.GitLab,
    label: (
      <>
        <img
          alt="gitlab"
          className="spacer-right"
          height={16}
          src={`${getBaseUrl()}/images/alm/gitlab.svg`}
        />
        GitLab
      </>
    ),
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

  return (
    <>
      <header className="page-header">
        <h1 className="page-title">{translate('settings.almintegration.title')}</h1>
      </header>

      <div className="markdown small spacer-top big-spacer-bottom">
        {translate('settings.almintegration.description')}
      </div>

      <BoxedTabs onSelect={props.onSelectAlmTab} selected={currentAlmTab} tabs={tabs} />

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

      {definitionKeyForDeletion && (
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
