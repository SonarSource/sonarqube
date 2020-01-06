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
import { withAppState } from '../../../../components/hoc/withAppState';
import { AlmSettingsBindingDefinitions, ALM_KEYS } from '../../../../types/alm-settings';
import AzureTab from './AzureTab';
import BitbucketTab from './BitbucketTab';
import DeleteModal from './DeleteModal';
import GithubTab from './GithubTab';
import GitlabTab from './GitlabTab';

export interface PRDecorationTabsProps {
  appState: Pick<T.AppState, 'multipleAlmEnabled'>;
  currentAlm: ALM_KEYS;
  definitionKeyForDeletion?: string;
  definitions: AlmSettingsBindingDefinitions;
  loading: boolean;
  onCancel: () => void;
  onConfirmDelete: (definitionKey: string) => void;
  onDelete: (definitionKey: string) => void;
  onSelectAlm: (alm: ALM_KEYS) => void;
  onUpdateDefinitions: () => void;
  projectCount?: number;
}

export const almName = {
  [ALM_KEYS.AZURE]: 'Azure DevOps Server',
  [ALM_KEYS.BITBUCKET]: 'Bitbucket Server',
  [ALM_KEYS.GITHUB]: 'GitHub',
  [ALM_KEYS.GITLAB]: 'GitLab'
};

export function PRDecorationTabs(props: PRDecorationTabsProps) {
  const {
    appState: { multipleAlmEnabled },
    definitionKeyForDeletion,
    definitions,
    currentAlm,
    loading,
    projectCount
  } = props;

  return (
    <>
      <header className="page-header">
        <h1 className="page-title">{translate('settings.pr_decoration.title')}</h1>
      </header>

      <div className="markdown small spacer-top big-spacer-bottom">
        {translate('settings.pr_decoration.description')}
      </div>
      <BoxedTabs
        onSelect={props.onSelectAlm}
        selected={currentAlm}
        tabs={[
          {
            key: ALM_KEYS.GITHUB,
            label: (
              <>
                <img
                  alt="github"
                  className="spacer-right"
                  height={16}
                  src={`${getBaseUrl()}/images/alm/github.svg`}
                />
                {almName[ALM_KEYS.GITHUB]}
              </>
            )
          },
          {
            key: ALM_KEYS.BITBUCKET,
            label: (
              <>
                <img
                  alt="bitbucket"
                  className="spacer-right"
                  height={16}
                  src={`${getBaseUrl()}/images/alm/bitbucket.svg`}
                />
                {almName[ALM_KEYS.BITBUCKET]}
              </>
            )
          },
          {
            key: ALM_KEYS.AZURE,
            label: (
              <>
                <img
                  alt="azure"
                  className="spacer-right"
                  height={16}
                  src={`${getBaseUrl()}/images/alm/azure.svg`}
                />
                {almName[ALM_KEYS.AZURE]}
              </>
            )
          },
          {
            key: ALM_KEYS.GITLAB,
            label: (
              <>
                <img
                  alt="gitlab"
                  className="spacer-right"
                  height={16}
                  src={`${getBaseUrl()}/images/alm/gitlab.svg`}
                />
                {almName[ALM_KEYS.GITLAB]}
              </>
            )
          }
        ]}
      />

      <div className="boxed-group boxed-group-inner">
        {currentAlm === ALM_KEYS.AZURE && (
          <AzureTab
            definitions={definitions.azure}
            loading={loading}
            multipleAlmEnabled={Boolean(multipleAlmEnabled)}
            onDelete={props.onDelete}
            onUpdateDefinitions={props.onUpdateDefinitions}
          />
        )}
        {currentAlm === ALM_KEYS.BITBUCKET && (
          <BitbucketTab
            definitions={definitions.bitbucket}
            loading={loading}
            multipleAlmEnabled={Boolean(multipleAlmEnabled)}
            onDelete={props.onDelete}
            onUpdateDefinitions={props.onUpdateDefinitions}
          />
        )}
        {currentAlm === ALM_KEYS.GITHUB && (
          <GithubTab
            definitions={definitions.github}
            loading={loading}
            multipleAlmEnabled={Boolean(multipleAlmEnabled)}
            onDelete={props.onDelete}
            onUpdateDefinitions={props.onUpdateDefinitions}
          />
        )}
        {currentAlm === ALM_KEYS.GITLAB && (
          <GitlabTab
            definitions={definitions.gitlab}
            loading={loading}
            multipleAlmEnabled={Boolean(multipleAlmEnabled)}
            onDelete={props.onDelete}
            onUpdateDefinitions={props.onUpdateDefinitions}
          />
        )}
      </div>

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

export default withAppState(PRDecorationTabs);
