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
import BoxedTabs from 'sonar-ui-common/components/controls/BoxedTabs';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import { AlmKeys, AlmSettingsBindingDefinitions } from '../../../../types/alm-settings';
import AzureTab from './AzureTab';
import BitbucketTab from './BitbucketTab';
import DeleteModal from './DeleteModal';
import GithubTab from './GithubTab';
import GitlabTab from './GitlabTab';

export interface AlmIntegrationRendererProps {
  branchesEnabled: boolean;
  component?: T.Component;
  currentAlm: AlmKeys;
  definitionKeyForDeletion?: string;
  definitions: AlmSettingsBindingDefinitions;
  loading: boolean;
  multipleAlmEnabled: boolean;
  onCancel: () => void;
  onConfirmDelete: (definitionKey: string) => void;
  onDelete: (definitionKey: string) => void;
  onSelectAlm: (alm: AlmKeys) => void;
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
    )
  },
  {
    key: AlmKeys.Bitbucket,
    label: (
      <>
        <img
          alt="bitbucket"
          className="spacer-right"
          height={16}
          src={`${getBaseUrl()}/images/alm/bitbucket.svg`}
        />
        Bitbucket Server
      </>
    ),
    requiresBranchesEnabled: true
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
        Azure DevOps Server
      </>
    ),
    requiresBranchesEnabled: true
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
    )
  }
];

export default function AlmIntegrationRenderer(props: AlmIntegrationRendererProps) {
  const {
    component,
    definitionKeyForDeletion,
    definitions,
    currentAlm,
    loading,
    branchesEnabled,
    multipleAlmEnabled,
    projectCount
  } = props;

  return (
    <>
      <header className="page-header">
        <h1 className="page-title">{translate('settings.almintegration.title')}</h1>
      </header>

      <div className="markdown small spacer-top big-spacer-bottom">
        {translate('settings.almintegration.description')}
      </div>
      <BoxedTabs
        onSelect={props.onSelectAlm}
        selected={currentAlm}
        tabs={tabs.filter(tab => !(tab.requiresBranchesEnabled && !branchesEnabled))}
      />

      {currentAlm === AlmKeys.Azure && (
        <AzureTab
          definitions={definitions.azure}
          loading={loading}
          multipleAlmEnabled={multipleAlmEnabled}
          onDelete={props.onDelete}
          onUpdateDefinitions={props.onUpdateDefinitions}
        />
      )}
      {currentAlm === AlmKeys.Bitbucket && (
        <BitbucketTab
          definitions={definitions.bitbucket}
          loading={loading}
          multipleAlmEnabled={multipleAlmEnabled}
          onDelete={props.onDelete}
          onUpdateDefinitions={props.onUpdateDefinitions}
        />
      )}
      {currentAlm === AlmKeys.GitHub && (
        <GithubTab
          branchesEnabled={branchesEnabled}
          component={component}
          definitions={definitions.github}
          loading={loading}
          multipleAlmEnabled={multipleAlmEnabled}
          onDelete={props.onDelete}
          onUpdateDefinitions={props.onUpdateDefinitions}
        />
      )}
      {currentAlm === AlmKeys.GitLab && (
        <GitlabTab
          branchesEnabled={branchesEnabled}
          definitions={definitions.gitlab}
          loading={loading}
          multipleAlmEnabled={multipleAlmEnabled}
          onDelete={props.onDelete}
          onUpdateDefinitions={props.onUpdateDefinitions}
        />
      )}

      {definitionKeyForDeletion && (
        <DeleteModal
          id={definitionKeyForDeletion}
          onCancel={props.onCancel}
          onDelete={props.onConfirmDelete}
          projectCount={projectCount}
        />
      )}
    </>
  );
}
